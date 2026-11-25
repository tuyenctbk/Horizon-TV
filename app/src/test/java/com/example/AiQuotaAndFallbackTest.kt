package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiCapabilityTier
import com.example.data.ai.AiFeatureType
import com.example.data.ai.AiModelConfig
import com.example.data.ai.AiQuotaManager
import com.example.data.ai.LocalMeteorologistEngine
import com.example.data.model.AstronomicalData
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecastDay
import com.example.data.model.HourlyPoint
import com.example.data.model.UnitSystem
import com.example.data.repository.FullWeatherPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiQuotaAndFallbackTest {

    private var fakeTimeMs: Long = 0L

    @Before
    fun setUp() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        fakeTimeMs = cal.timeInMillis
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("horizon_tv_ai_quota_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun quota_softNudgeAt80Percent_and_deductWithRefund() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = AiQuotaManager(ctx) { fakeTimeMs }

        repeat(7) { assertTrue(manager.reserveCredit(AiFeatureType.CHAT)) }
        assertFalse(manager.getSnapshot().isSoftNudgeActive)

        // 8th credit hits 80% of 10 -> Soft Nudge activates + prefers Eco Lite
        assertTrue(manager.reserveCredit(AiFeatureType.BRIEFING))
        val at80 = manager.getSnapshot()
        assertEquals(8, at80.totalUsed)
        assertEquals(2, at80.remaining)
        assertTrue(at80.isSoftNudgeActive)
        assertTrue(at80.shouldPreferEcoLite)

        // Refund restores credit immediately
        manager.refundCredit(AiFeatureType.BRIEFING)
        assertEquals(7, manager.getSnapshot().totalUsed)
        assertEquals(3, manager.getSnapshot().remaining)
    }

    @Test
    fun quota_graceCreditsSingleClaim_and_dailyRolloverPreservesSettings() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = AiQuotaManager(ctx) { fakeTimeMs }

        manager.setEcoLiteMode(true)
        repeat(AiModelConfig.DAILY_BASE_QUOTA) {
            assertTrue(manager.reserveCredit(AiFeatureType.CHAT))
        }
        assertTrue(manager.getSnapshot().isExhausted)
        assertFalse(manager.reserveCredit(AiFeatureType.CHAT))

        // Claim +3 Grace Credits once
        assertTrue(manager.claimDailyGraceCredits())
        assertFalse(manager.claimDailyGraceCredits())
        assertEquals(3, manager.getSnapshot().remaining)
        assertTrue(manager.reserveCredit(AiFeatureType.BRIEFING))

        // Advance clock by 24 hours -> automatic rollover reset, preserving ecoLiteMode
        fakeTimeMs += 24L * 60L * 60L * 1000L
        val nextDay = manager.getSnapshot()
        assertEquals(0, nextDay.totalUsed)
        assertEquals(AiModelConfig.DAILY_BASE_QUOTA, nextDay.remaining)
        assertFalse(nextDay.graceClaimed)
        assertTrue(nextDay.ecoLiteMode)
    }

    @Test
    fun quota_byokUnlocksUnlimitedPassThrough() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val manager = AiQuotaManager(ctx) { fakeTimeMs }

        repeat(AiModelConfig.DAILY_BASE_QUOTA) { manager.reserveCredit(AiFeatureType.CHAT) }
        assertTrue(manager.getSnapshot().isExhausted)

        manager.saveByokKey("AIzaSyTestUserKey12345")
        val withByok = manager.getSnapshot()
        assertTrue(withByok.hasByokKey)
        assertFalse(withByok.isExhausted)
        assertTrue(manager.reserveCredit(AiFeatureType.BRIEFING))

        manager.clearByokKey()
        assertFalse(manager.getSnapshot().hasByokKey)
    }

    @Test
    fun localMeteorologistEngine_generatesGroundedBriefingAndReflexReplies() {
        val location = CityLocation(
            name = "Seattle",
            region = "Washington",
            country = "United States",
            countryCode = "US",
            latitude = 47.6062,
            longitude = -122.3321
        )
        val current = CurrentWeather(
            tempC = 14.0,
            feelsLikeC = 12.5,
            weatherCode = 61,
            conditionText = "Slight Rain",
            isDay = true,
            humidityPercent = 82,
            pressureHpa = 1008.0,
            windSpeedKmh = 18.0,
            windDirectionDeg = 210,
            windGustsKmh = 30.0,
            uvIndex = 2.0,
            cloudCoverPercent = 90,
            dewPointC = 11.0,
            visibilityKm = 12.0,
            aqi = 24,
            aqiStatus = "Good",
            pm25 = 6.2,
            pm10 = 10.5,
            sunriseTime = "07:20",
            sunsetTime = "16:35"
        )
        val pkg = FullWeatherPackage(
            diurnalPeriods = emptyList(),
            hourlyPoints = listOf(
                HourlyPoint("14:00", "2026-11-24T14:00", 14.0, 12.5, 61, "Slight Rain", 75, 18.0, 82),
                HourlyPoint("17:00", "2026-11-24T17:00", 13.0, 12.0, 2, "Partly Cloudy", 15, 10.0, 70)
            ),
            dailyDays = listOf(
                DailyForecastDay("Tue, Nov 24", "Today", 15.0, 9.0, 61, "Slight Rain", 6.4, 75, 2.0, 18.0),
                DailyForecastDay("Wed, Nov 25", "Tomorrow", 13.0, 7.0, 2, "Partly Cloudy", 0.0, 15, 3.0, 12.0)
            ),
            alerts = emptyList(),
            astronomical = AstronomicalData("Waxing Gibbous", 70, 4, "07:20", "16:35", 0.5f),
            headlines = emptyList()
        )

        val briefing = LocalMeteorologistEngine.buildDeterministicBriefing(location, current, pkg, UnitSystem.METRIC)
        assertEquals(AiCapabilityTier.LOCAL_METEOROLOGY, briefing.tierUsed)
        assertTrue(briefing.headline.contains("Seattle"))
        assertTrue(briefing.outfitAndCommute.contains("umbrella", ignoreCase = true))

        val rainAnswer = LocalMeteorologistEngine.answerLocally("Will it rain on my commute?", location, current, pkg, UnitSystem.METRIC)
        assertTrue(rainAnswer.contains("75%"))

        val weekAnswer = LocalMeteorologistEngine.answerLocally("Summarize the 7-day outlook", location, current, pkg, UnitSystem.METRIC)
        assertTrue(weekAnswer.contains("Seattle"))
    }
}
