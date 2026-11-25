package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local Quota Ledger & Free-Tier Guardrail Manager.
 * - Automatic daily rollover reset (preserves user Eco Lite & BYOK settings)
 * - Deduct-with-refund accounting so network errors never burn free credits
 * - One-time daily +3 Grace Credit recovery
 * - Optional on-device BYOK Gemini API key storage for unlimited direct pass-through
 */
class AiQuotaManager(
    context: Context,
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "horizon_tv_ai_quota_prefs"
        private const val KEY_DATE_ISO = "date_iso"
        private const val KEY_BRIEFING_USED = "briefing_used"
        private const val KEY_CHAT_USED = "chat_used"
        private const val KEY_CACHED_SAVES = "cached_saves"
        private const val KEY_BONUS_GRACE = "bonus_grace_credits"
        private const val KEY_GRACE_CLAIMED = "grace_claimed"
        private const val KEY_ECO_LITE_MODE = "eco_lite_mode"
        private const val KEY_CASCADE_COUNT = "fallback_cascade_count"
        private const val KEY_BYOK_GEMINI_KEY = "byok_gemini_api_key"
    }

    private fun currentDateIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(clockMs()))
    }

    private fun ensureCurrentPeriod() {
        val today = currentDateIso()
        val stored = prefs.getString(KEY_DATE_ISO, null)
        if (stored != today) {
            prefs.edit()
                .putString(KEY_DATE_ISO, today)
                .putInt(KEY_BRIEFING_USED, 0)
                .putInt(KEY_CHAT_USED, 0)
                .putInt(KEY_CACHED_SAVES, 0)
                .putInt(KEY_BONUS_GRACE, 0)
                .putBoolean(KEY_GRACE_CLAIMED, false)
                .putInt(KEY_CASCADE_COUNT, 0)
                .apply()
        }
    }

    fun getSnapshot(): AiQuotaSnapshot {
        ensureCurrentPeriod()
        val today = currentDateIso()
        return AiQuotaSnapshot(
            dateIso = today,
            briefingUsed = prefs.getInt(KEY_BRIEFING_USED, 0),
            chatUsed = prefs.getInt(KEY_CHAT_USED, 0),
            cachedSaves = prefs.getInt(KEY_CACHED_SAVES, 0),
            dailyBaseLimit = AiModelConfig.DAILY_BASE_QUOTA,
            bonusGraceCredits = prefs.getInt(KEY_BONUS_GRACE, 0),
            graceClaimed = prefs.getBoolean(KEY_GRACE_CLAIMED, false),
            ecoLiteMode = prefs.getBoolean(KEY_ECO_LITE_MODE, false),
            fallbackCascadeCount = prefs.getInt(KEY_CASCADE_COUNT, 0),
            hasByokKey = getByokKey().isNotBlank(),
            millisUntilReset = computeMillisUntilLocalMidnight()
        )
    }

    /**
     * Reserves 1 credit before a cloud AI request.
     * Returns true if allowed (or if BYOK unlimited mode is active), false if exhausted.
     */
    @Synchronized
    fun reserveCredit(feature: AiFeatureType): Boolean {
        ensureCurrentPeriod()
        val snapshot = getSnapshot()
        if (snapshot.hasByokKey) {
            incrementFeatureCounter(feature)
            return true
        }
        if (snapshot.remaining <= 0) {
            return false
        }
        incrementFeatureCounter(feature)
        return true
    }

    /**
     * Refunds 1 credit when a cloud AI request fails or times out so users never lose allowance.
     */
    @Synchronized
    fun refundCredit(feature: AiFeatureType) {
        ensureCurrentPeriod()
        val key = when (feature) {
            AiFeatureType.BRIEFING -> KEY_BRIEFING_USED
            AiFeatureType.CHAT -> KEY_CHAT_USED
        }
        val current = prefs.getInt(key, 0)
        if (current > 0) {
            prefs.edit().putInt(key, current - 1).apply()
        }
    }

    private fun incrementFeatureCounter(feature: AiFeatureType) {
        val key = when (feature) {
            AiFeatureType.BRIEFING -> KEY_BRIEFING_USED
            AiFeatureType.CHAT -> KEY_CHAT_USED
        }
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    fun recordCacheSave() {
        ensureCurrentPeriod()
        val current = prefs.getInt(KEY_CACHED_SAVES, 0)
        prefs.edit().putInt(KEY_CACHED_SAVES, current + 1).apply()
    }

    fun recordFallbackCascade() {
        ensureCurrentPeriod()
        val current = prefs.getInt(KEY_CASCADE_COUNT, 0)
        prefs.edit().putInt(KEY_CASCADE_COUNT, current + 1).apply()
    }

    @Synchronized
    fun claimDailyGraceCredits(): Boolean {
        ensureCurrentPeriod()
        if (prefs.getBoolean(KEY_GRACE_CLAIMED, false)) {
            return false
        }
        val currentBonus = prefs.getInt(KEY_BONUS_GRACE, 0)
        prefs.edit()
            .putInt(KEY_BONUS_GRACE, currentBonus + AiModelConfig.DAILY_GRACE_BONUS)
            .putBoolean(KEY_GRACE_CLAIMED, true)
            .apply()
        return true
    }

    fun setEcoLiteMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ECO_LITE_MODE, enabled).apply()
    }

    fun getByokKey(): String {
        return prefs.getString(KEY_BYOK_GEMINI_KEY, "")?.trim().orEmpty()
    }

    fun saveByokKey(rawKey: String) {
        prefs.edit().putString(KEY_BYOK_GEMINI_KEY, rawKey.trim()).apply()
    }

    fun clearByokKey() {
        prefs.edit().remove(KEY_BYOK_GEMINI_KEY).apply()
    }

    fun computeMillisUntilLocalMidnight(): Long {
        val now = clockMs()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - now).coerceAtLeast(60_000L)
    }

    fun formatResetCountdown(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) {
            appContext.getString(com.example.R.string.ai_countdown_hm_fmt, hours, minutes)
        } else {
            appContext.getString(com.example.R.string.ai_countdown_m_fmt, minutes)
        }
    }
}
