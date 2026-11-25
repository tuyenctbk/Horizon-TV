package com.example.data.ai

/**
 * Centralized model-agnostic capability tiers & free-tier guardrail constants.
 * Model identifiers are defined here only — never hardcoded in screens or ViewModels.
 */
object AiModelConfig {
    // Tier 1: Primary Standard (billing not required on Firebase AI Logic)
    const val PRIMARY_REASONING_MODEL = "gemini-3.8-flash"

    // Tier 2: High-Efficiency Eco Lite (billing not required; higher RPM / lower token footprint)
    const val ECONOMY_LITE_MODEL = "gemini-3.5-flash-lite"

    // Free-tier quota & hygiene guardrails
    const val DAILY_BASE_QUOTA = 10
    const val DAILY_GRACE_BONUS = 3
    const val SOFT_NUDGE_THRESHOLD = 0.80f
    const val BRIEFING_CACHE_TTL_MS = 30L * 60L * 1000L // 30 minutes per city
    const val PROMPT_DEDUP_TTL_MS = 45_000L             // 45 seconds dedup window
    const val MAX_SLIDING_CHAT_TURNS = 6                // Last 6 turns sent to cloud
    const val INFERENCE_TIMEOUT_MS = 45_000L            // 45-second timeout window
}

enum class AiCapabilityTier {
    PRIMARY_CLOUD,
    ECO_LITE_CLOUD,
    BYOK_PASS_THROUGH,
    LOCAL_METEOROLOGY
}

enum class AiFeatureType {
    BRIEFING,
    CHAT
}

data class AiWeatherBriefing(
    val locationName: String,
    val headline: String,
    val broadcastSummary: String,
    val outfitAndCommute: String,
    val outdoorSafety: String,
    val bestWindowToday: String,
    val tierUsed: AiCapabilityTier,
    val modelLabel: String,
    val generatedAtMs: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false
)

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val tierUsed: AiCapabilityTier? = null,
    val modelLabel: String = "",
    val isStreaming: Boolean = false,
    val isFromCache: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AiQuotaSnapshot(
    val dateIso: String,
    val briefingUsed: Int,
    val chatUsed: Int,
    val cachedSaves: Int,
    val dailyBaseLimit: Int = AiModelConfig.DAILY_BASE_QUOTA,
    val bonusGraceCredits: Int = 0,
    val graceClaimed: Boolean = false,
    val ecoLiteMode: Boolean = false,
    val fallbackCascadeCount: Int = 0,
    val hasByokKey: Boolean = false,
    val millisUntilReset: Long = 0L
) {
    val totalUsed: Int
        get() = briefingUsed + chatUsed

    val totalLimit: Int
        get() = dailyBaseLimit + bonusGraceCredits

    val remaining: Int
        get() = (totalLimit - totalUsed).coerceAtLeast(0)

    val usageFraction: Float
        get() = (totalUsed.toFloat() / totalLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val usagePercent: Int
        get() = (usageFraction * 100f).toInt().coerceIn(0, 100)

    val isSoftNudgeActive: Boolean
        get() = !hasByokKey && usageFraction >= AiModelConfig.SOFT_NUDGE_THRESHOLD && remaining > 0

    val isExhausted: Boolean
        get() = !hasByokKey && remaining <= 0

    val shouldPreferEcoLite: Boolean
        get() = !hasByokKey && (ecoLiteMode || usageFraction >= AiModelConfig.SOFT_NUDGE_THRESHOLD)
}
