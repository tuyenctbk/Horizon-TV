package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.data.repository.FullWeatherPackage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val TAG = "MeteorologistAiRepo"

sealed interface AiBriefingCallResult {
    data class Success(val briefing: AiWeatherBriefing) : AiBriefingCallResult
    data class Error(
        val message: String,
        val isRateLimited: Boolean = false
    ) : AiBriefingCallResult
}

sealed interface AiChatStreamEvent {
    data class Chunk(
        val accumulatedText: String,
        val tierUsed: AiCapabilityTier,
        val modelLabel: String
    ) : AiChatStreamEvent

    data class Complete(
        val fullText: String,
        val tierUsed: AiCapabilityTier,
        val modelLabel: String,
        val isFromCache: Boolean = false
    ) : AiChatStreamEvent

    data class Error(
        val message: String,
        val isRateLimited: Boolean = false
    ) : AiChatStreamEvent
}

/**
 * Multi-tier AI repository for Horizon Weather TV.
 * - Default: Firebase AI Logic (gemini-3.8-flash primary, gemini-3.5-flash-lite Eco Lite cascade)
 * - Optional BYOK: Direct user-supplied Gemini API key pass-through when configured in Settings
 * - Hygiene: 30-min per-city briefing cache, 45s prompt fingerprint dedup, 6-turn sliding window
 */
class MeteorologistAiRepository(
    private val quotaManager: AiQuotaManager,
    private val appContext: Context? = null
) {
    private data class CachedBriefing(
        val briefing: AiWeatherBriefing,
        val cachedAtMs: Long
    )

    private data class CachedChatReply(
        val text: String,
        val tierUsed: AiCapabilityTier,
        val modelLabel: String,
        val cachedAtMs: Long
    )

    private val briefingCache = mutableMapOf<String, CachedBriefing>()
    private val chatDedupCache = mutableMapOf<String, CachedChatReply>()

    private val byokHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val briefingSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "headline" to Schema.string(description = "Punchy broadcast weather headline under 12 words"),
                "broadcastSummary" to Schema.string(description = "Concise 2-sentence meteorological summary grounded in exact numbers"),
                "outfitAndCommute" to Schema.string(description = "Practical clothing and commute guidance for today"),
                "outdoorSafety" to Schema.string(description = "Actionable UV, air quality, wind, or storm safety advice"),
                "bestWindowToday" to Schema.string(description = "Best time window outdoors today with temperature and rain risk")
            )
        )
    }

    private val briefingGenConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = briefingSchema
        }
    }

    // Lazy initialization prevents Firebase.ai evaluation during ViewModel construction.
    private val primaryBriefingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
            generationConfig = briefingGenConfig
        )
    }

    private val ecoBriefingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = AiModelConfig.ECONOMY_LITE_MODEL,
            generationConfig = briefingGenConfig
        )
    }

    private val primaryChatModel by lazy {
        Firebase.ai.generativeModel(modelName = AiModelConfig.PRIMARY_REASONING_MODEL)
    }

    private val ecoChatModel by lazy {
        Firebase.ai.generativeModel(modelName = AiModelConfig.ECONOMY_LITE_MODEL)
    }

    fun getCachedBriefing(location: CityLocation, unitSystem: UnitSystem): AiWeatherBriefing? {
        val key = briefingCacheKey(location, unitSystem)
        val entry = briefingCache[key] ?: return null
        val age = System.currentTimeMillis() - entry.cachedAtMs
        return if (age <= AiModelConfig.BRIEFING_CACHE_TTL_MS) {
            entry.briefing.copy(isFromCache = true)
        } else {
            briefingCache.remove(key)
            null
        }
    }

    suspend fun generateWeatherBriefing(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        forceRefresh: Boolean = false,
        forceEcoLite: Boolean = false
    ): AiBriefingCallResult {
        val cacheKey = briefingCacheKey(location, unitSystem)
        if (!forceRefresh) {
            val cached = getCachedBriefing(location, unitSystem)
            if (cached != null) {
                quotaManager.recordCacheSave()
                return AiBriefingCallResult.Success(cached)
            }
        }

        val byokKey = quotaManager.getByokKey()
        val snapshot = quotaManager.getSnapshot()
        val preferEco = forceEcoLite || snapshot.shouldPreferEcoLite
        val prompt = buildBriefingPrompt(location, current, pkg, unitSystem)

        if (byokKey.isNotBlank()) {
            return generateBriefingViaByok(
                apiKey = byokKey,
                modelName = if (preferEco) AiModelConfig.ECONOMY_LITE_MODEL else AiModelConfig.PRIMARY_REASONING_MODEL,
                prompt = prompt,
                location = location,
                cacheKey = cacheKey
            )
        }

        val initialTier = if (preferEco) AiCapabilityTier.ECO_LITE_CLOUD else AiCapabilityTier.PRIMARY_CLOUD
        val primaryResult = invokeFirebaseBriefing(initialTier, prompt, location, cacheKey)

        // Automatic Tier 1 -> Tier 2 Eco Lite Cascade on 429 / RESOURCE_EXHAUSTED
        if (primaryResult is AiBriefingCallResult.Error &&
            primaryResult.isRateLimited &&
            initialTier == AiCapabilityTier.PRIMARY_CLOUD
        ) {
            quotaManager.recordFallbackCascade()
            Log.w(TAG, "Primary tier rate-limited; cascading automatically to Eco Lite model.")
            return invokeFirebaseBriefing(AiCapabilityTier.ECO_LITE_CLOUD, prompt, location, cacheKey)
        }

        return primaryResult
    }

    private suspend fun invokeFirebaseBriefing(
        tier: AiCapabilityTier,
        prompt: String,
        location: CityLocation,
        cacheKey: String
    ): AiBriefingCallResult {
        val model = if (tier == AiCapabilityTier.ECO_LITE_CLOUD) ecoBriefingModel else primaryBriefingModel
        val label = if (tier == AiCapabilityTier.ECO_LITE_CLOUD) {
            appContext?.getString(R.string.ai_tier_eco) ?: "Eco Lite 3.5"
        } else {
            appContext?.getString(R.string.ai_tier_primary) ?: "Gemini 3.8 Flash"
        }

        return try {
            val response = withTimeout(AiModelConfig.INFERENCE_TIMEOUT_MS) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: return AiBriefingCallResult.Error(
                    appContext?.getString(R.string.ai_err_empty_briefing) ?: "Model returned an empty briefing response."
                )

            val briefing = parseBriefingJson(
                rawJson = jsonText,
                locationName = location.name,
                tier = tier,
                modelLabel = label
            )
            briefingCache[cacheKey] = CachedBriefing(briefing, System.currentTimeMillis())
            AiBriefingCallResult.Success(briefing)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Briefing generation timed out", e)
            AiBriefingCallResult.Error(
                appContext?.getString(R.string.ai_err_briefing_timeout) ?: "AI briefing timed out after 45 seconds. Please retry."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI briefing failed ($label)", e)
            val msg = e.localizedMessage ?: (appContext?.getString(R.string.ai_err_briefing_failed) ?: "AI briefing failed.")
            AiBriefingCallResult.Error(msg, isRateLimited = isRateLimitException(msg))
        }
    }

    fun streamMeteorologistAnswer(
        question: String,
        recentHistory: List<AiChatMessage>,
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        forceEcoLite: Boolean = false
    ): Flow<AiChatStreamEvent> = flow {
        val fingerprint = chatFingerprint(location, question, unitSystem)
        val cached = chatDedupCache[fingerprint]
        if (cached != null && (System.currentTimeMillis() - cached.cachedAtMs) <= AiModelConfig.PROMPT_DEDUP_TTL_MS) {
            quotaManager.recordCacheSave()
            emit(
                AiChatStreamEvent.Complete(
                    fullText = cached.text,
                    tierUsed = cached.tierUsed,
                    modelLabel = cached.modelLabel,
                    isFromCache = true
                )
            )
            return@flow
        }

        val byokKey = quotaManager.getByokKey()
        val snapshot = quotaManager.getSnapshot()
        val preferEco = forceEcoLite || snapshot.shouldPreferEcoLite
        val fullPrompt = buildChatPrompt(question, recentHistory, location, current, pkg, unitSystem)

        if (byokKey.isNotBlank()) {
            val modelName = if (preferEco) AiModelConfig.ECONOMY_LITE_MODEL else AiModelConfig.PRIMARY_REASONING_MODEL
            val byokLabel = appContext?.getString(R.string.ai_byok_model_label_fmt, modelName) ?: "BYOK • $modelName"
            val byokResult = invokeByokText(byokKey, modelName, fullPrompt)
            byokResult.fold(
                onSuccess = { reply ->
                    chatDedupCache[fingerprint] = CachedChatReply(
                        text = reply,
                        tierUsed = AiCapabilityTier.BYOK_PASS_THROUGH,
                        modelLabel = byokLabel,
                        cachedAtMs = System.currentTimeMillis()
                    )
                    emit(
                        AiChatStreamEvent.Complete(
                            fullText = reply,
                            tierUsed = AiCapabilityTier.BYOK_PASS_THROUGH,
                            modelLabel = byokLabel
                        )
                    )
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: (appContext?.getString(R.string.ai_err_byok_failed) ?: "BYOK request failed.")
                    emit(AiChatStreamEvent.Error(msg, isRateLimited = isRateLimitException(msg)))
                }
            )
            return@flow
        }

        val initialTier = if (preferEco) AiCapabilityTier.ECO_LITE_CLOUD else AiCapabilityTier.PRIMARY_CLOUD
        val streamOutcome = collectFirebaseStream(
            tier = initialTier,
            prompt = fullPrompt,
            onChunk = { accumulated, tier, label ->
                emit(AiChatStreamEvent.Chunk(accumulated, tier, label))
            }
        )

        if (streamOutcome is StreamOutcome.Success) {
            chatDedupCache[fingerprint] = CachedChatReply(
                text = streamOutcome.text,
                tierUsed = streamOutcome.tier,
                modelLabel = streamOutcome.label,
                cachedAtMs = System.currentTimeMillis()
            )
            emit(
                AiChatStreamEvent.Complete(
                    fullText = streamOutcome.text,
                    tierUsed = streamOutcome.tier,
                    modelLabel = streamOutcome.label
                )
            )
        } else if (streamOutcome is StreamOutcome.Failure &&
            streamOutcome.isRateLimited &&
            initialTier == AiCapabilityTier.PRIMARY_CLOUD
        ) {
            quotaManager.recordFallbackCascade()
            Log.w(TAG, "Primary chat tier rate-limited; cascading to Eco Lite stream.")
            val cascadeOutcome = collectFirebaseStream(
                tier = AiCapabilityTier.ECO_LITE_CLOUD,
                prompt = fullPrompt,
                onChunk = { accumulated, tier, label ->
                    emit(AiChatStreamEvent.Chunk(accumulated, tier, label))
                }
            )
            when (cascadeOutcome) {
                is StreamOutcome.Success -> {
                    chatDedupCache[fingerprint] = CachedChatReply(
                        text = cascadeOutcome.text,
                        tierUsed = cascadeOutcome.tier,
                        modelLabel = cascadeOutcome.label,
                        cachedAtMs = System.currentTimeMillis()
                    )
                    emit(
                        AiChatStreamEvent.Complete(
                            fullText = cascadeOutcome.text,
                            tierUsed = cascadeOutcome.tier,
                            modelLabel = cascadeOutcome.label
                        )
                    )
                }
                is StreamOutcome.Failure -> {
                    emit(AiChatStreamEvent.Error(cascadeOutcome.message, cascadeOutcome.isRateLimited))
                }
            }
        } else if (streamOutcome is StreamOutcome.Failure) {
            emit(AiChatStreamEvent.Error(streamOutcome.message, streamOutcome.isRateLimited))
        }
    }

    private sealed interface StreamOutcome {
        data class Success(val text: String, val tier: AiCapabilityTier, val label: String) : StreamOutcome
        data class Failure(val message: String, val isRateLimited: Boolean) : StreamOutcome
    }

    private suspend fun collectFirebaseStream(
        tier: AiCapabilityTier,
        prompt: String,
        onChunk: suspend (String, AiCapabilityTier, String) -> Unit
    ): StreamOutcome {
        val model = if (tier == AiCapabilityTier.ECO_LITE_CLOUD) ecoChatModel else primaryChatModel
        val label = if (tier == AiCapabilityTier.ECO_LITE_CLOUD) {
            appContext?.getString(R.string.ai_tier_eco) ?: "Eco Lite 3.5"
        } else {
            appContext?.getString(R.string.ai_tier_primary) ?: "Gemini 3.8 Flash"
        }
        val accumulated = StringBuilder()

        return try {
            withTimeout(AiModelConfig.INFERENCE_TIMEOUT_MS) {
                model.generateContentStream(content { text(prompt) }).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        onChunk(accumulated.toString(), tier, label)
                    }
                }
            }
            val finalText = accumulated.toString().trim()
            if (finalText.isNotEmpty()) {
                StreamOutcome.Success(finalText, tier, label)
            } else {
                StreamOutcome.Failure(
                    appContext?.getString(R.string.ai_err_empty_response) ?: "Model returned an empty response.",
                    isRateLimited = false
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            StreamOutcome.Failure(
                appContext?.getString(R.string.ai_err_response_timeout) ?: "AI response timed out after 45 seconds. Please retry.",
                isRateLimited = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI stream failed ($label)", e)
            val msg = e.localizedMessage ?: (appContext?.getString(R.string.ai_err_chat_failed) ?: "AI chat failed.")
            StreamOutcome.Failure(msg, isRateLimited = isRateLimitException(msg))
        }
    }

    private suspend fun generateBriefingViaByok(
        apiKey: String,
        modelName: String,
        prompt: String,
        location: CityLocation,
        cacheKey: String
    ): AiBriefingCallResult {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                    })
                }
                val rawText = executeByokRequest(apiKey, modelName, payload)
                val byokLabel = appContext?.getString(R.string.ai_byok_model_label_fmt, modelName) ?: "BYOK • $modelName"
                val briefing = parseBriefingJson(
                    rawJson = rawText,
                    locationName = location.name,
                    tier = AiCapabilityTier.BYOK_PASS_THROUGH,
                    modelLabel = byokLabel
                )
                briefingCache[cacheKey] = CachedBriefing(briefing, System.currentTimeMillis())
                AiBriefingCallResult.Success(briefing)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "BYOK briefing failed", e)
                val msg = e.localizedMessage ?: (appContext?.getString(R.string.ai_err_byok_failed) ?: "BYOK request failed.")
                AiBriefingCallResult.Error(msg, isRateLimited = isRateLimitException(msg))
            }
        }
    }

    private suspend fun invokeByokText(
        apiKey: String,
        modelName: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
            }
            Result.success(executeByokRequest(apiKey, modelName, payload))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "BYOK chat failed", e)
            Result.failure(e)
        }
    }

    private fun executeByokRequest(apiKey: String, modelName: String, payload: JSONObject): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(body)
            .build()

        byokHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: ${extractGoogleErrorMessage(responseBody)}")
            }
            val root = JSONObject(responseBody)
            val text = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
                .orEmpty()
                .trim()
            if (text.isBlank()) throw IllegalStateException("BYOK model returned an empty response.")
            return text
        }
    }

    private fun parseBriefingJson(
        rawJson: String,
        locationName: String,
        tier: AiCapabilityTier,
        modelLabel: String
    ): AiWeatherBriefing {
        val obj = JSONObject(rawJson)
        return AiWeatherBriefing(
            locationName = locationName,
            headline = obj.optString("headline").ifBlank {
                appContext?.getString(R.string.ai_briefing_default_headline_fmt, locationName) ?: "$locationName Weather Briefing"
            },
            broadcastSummary = obj.optString("broadcastSummary"),
            outfitAndCommute = obj.optString("outfitAndCommute"),
            outdoorSafety = obj.optString("outdoorSafety"),
            bestWindowToday = obj.optString("bestWindowToday"),
            tierUsed = tier,
            modelLabel = modelLabel,
            generatedAtMs = System.currentTimeMillis(),
            isFromCache = false
        )
    }

    private fun buildTelemetryContext(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem
    ): String {
        val tempUnit = if (unitSystem == UnitSystem.METRIC) "°C" else "°F"
        val windUnit = if (unitSystem == UnitSystem.METRIC) "km/h" else "mph"
        fun fmtT(c: Double) = if (unitSystem == UnitSystem.METRIC) "${c.roundToInt()}$tempUnit" else "${(c * 9.0 / 5.0 + 32.0).roundToInt()}$tempUnit"
        fun fmtW(k: Double) = if (unitSystem == UnitSystem.METRIC) "${k.roundToInt()} $windUnit" else "${(k * 0.621371).roundToInt()} $windUnit"

        val hourlySummary = pkg.hourlyPoints.take(12).joinToString("; ") {
            "${it.timeLabel} ${fmtT(it.tempC)} rain:${it.popPercent}%"
        }
        val dailySummary = pkg.dailyDays.take(7).joinToString("; ") {
            "${it.dayOfWeek}(${it.dateLabel}): ${fmtT(it.minTempC)}..${fmtT(it.maxTempC)} ${it.conditionText} rain:${it.popPercent}%"
        }
        val alertSummary = pkg.alerts.firstOrNull()?.let { "${it.severity}: ${it.title}" } ?: "None"

        return """
            Station: ${location.name}, ${location.region} ${location.country}
            Current: ${fmtT(current.tempC)} (feels ${fmtT(current.feelsLikeC)}), ${current.conditionText}, Humidity ${current.humidityPercent}%, Wind ${fmtW(current.windSpeedKmh)} (gusts ${fmtW(current.windGustsKmh)}), UV ${current.uvIndex.roundToInt()}/11, US AQI ${current.aqi} (${current.aqiStatus}), Sunrise ${current.sunriseTime}, Sunset ${current.sunsetTime}
            Alerts: $alertSummary
            Next 12h: $hourlySummary
            7-Day: $dailySummary
        """.trimIndent()
    }

    private fun buildBriefingPrompt(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem
    ): String {
        val language = Locale.getDefault().displayLanguage.ifBlank { "English" }
        return """
            You are Horizon Weather TV's chief broadcast meteorologist.
            Write a crisp, high-value weather briefing in $language using ONLY the live telemetry below.
            Keep every field concise (1-2 clear sentences), specific to the real numbers, and practical.
            
            LIVE TELEMETRY:
            ${buildTelemetryContext(location, current, pkg, unitSystem)}
        """.trimIndent()
    }

    private fun buildChatPrompt(
        question: String,
        recentHistory: List<AiChatMessage>,
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem
    ): String {
        val slidingTurns = recentHistory
            .filter { !it.isStreaming && it.text.isNotBlank() }
            .takeLast(AiModelConfig.MAX_SLIDING_CHAT_TURNS)
            .joinToString("\n") { msg ->
                val speaker = if (msg.isUser) "Viewer" else "Meteorologist"
                "$speaker: ${msg.text.take(260)}"
            }

        return """
            You are Horizon Weather TV's live broadcast meteorologist. Answer the viewer concisely (2-4 clear sentences max) using the live Open-Meteo telemetry below. Avoid markdown tables or excessive formatting.
            
            LIVE TELEMETRY:
            ${buildTelemetryContext(location, current, pkg, unitSystem)}
            
            ${if (slidingTurns.isNotBlank()) "RECENT CONVERSATION:\n$slidingTurns\n" else ""}
            VIEWER QUESTION: $question
        """.trimIndent()
    }

    private fun briefingCacheKey(location: CityLocation, unitSystem: UnitSystem): String {
        return "${location.latitude.roundToInt()}_${location.longitude.roundToInt()}_${location.name}_${unitSystem.name}"
    }

    private fun chatFingerprint(location: CityLocation, question: String, unitSystem: UnitSystem): String {
        val normalized = question.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
        return "${location.name}_${unitSystem.name}_${normalized.hashCode()}"
    }

    private fun isRateLimitException(message: String): Boolean {
        val lower = message.lowercase(Locale.US)
        return lower.contains("429") ||
            lower.contains("resource_exhausted") ||
            lower.contains("quota") ||
            lower.contains("rate limit")
    }

    private fun extractGoogleErrorMessage(raw: String): String {
        return try {
            JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty().ifBlank { raw.take(140) }
        } catch (_: Exception) {
            raw.take(140)
        }
    }
}
