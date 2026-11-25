package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.data.model.AlertSeverity
import com.example.data.model.AstronomicalData
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.SevereWeatherAlert
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.UserPreferencesManager
import com.example.data.worker.SevereWeatherAlertNotificationHelper
import com.example.data.worker.SevereWeatherAlertWorker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SevereWeatherAlertWorkerTest {

    private lateinit var context: Context
    private lateinit var prefs: UserPreferencesManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context.getSharedPreferences("horizon_tv_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        prefs = UserPreferencesManager(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        SevereWeatherAlertWorker.testWeatherFetcher = null
        notificationManager.cancelAll()
    }

    @Test
    fun `UserPreferencesManager persists background alert settings and deduplicates alerts`() {
        assertTrue("Background alerts should default to enabled", prefs.isBackgroundAlertsEnabled())
        assertEquals(30, prefs.getBackgroundSyncIntervalMinutes())

        prefs.setBackgroundAlertsEnabled(false)
        prefs.setBackgroundSyncIntervalMinutes(15)
        assertFalse(prefs.isBackgroundAlertsEnabled())
        assertEquals(15, prefs.getBackgroundSyncIntervalMinutes())

        val alertKey = "miami_tornado_warning"
        val t0 = 1_700_000_000_000L
        assertTrue(prefs.shouldNotifySevereAlert(alertKey, nowMs = t0))

        prefs.markSevereAlertNotified(alertKey, nowMs = t0)
        assertFalse(
            "Should suppress duplicate alert within 3-hour cooldown",
            prefs.shouldNotifySevereAlert(alertKey, nowMs = t0 + 30 * 60 * 1000L)
        )
        assertTrue(
            "Should allow alert after 3-hour cooldown expires",
            prefs.shouldNotifySevereAlert(alertKey, nowMs = t0 + 4 * 60 * 60 * 1000L)
        )
    }

    @Test
    fun `SevereWeatherAlertWorker fetches telemetry and posts high-priority push notification`() = runBlocking {
        prefs.saveLastSelectedLocation(
            CityLocation(
                name = "Miami",
                region = "Florida",
                country = "United States",
                countryCode = "US",
                latitude = 25.7617,
                longitude = -80.1918
            )
        )
        prefs.setBackgroundAlertsEnabled(true)

        val mockAlert = SevereWeatherAlert(
            id = "hurricane_watch_01",
            severity = AlertSeverity.EMERGENCY,
            title = "SEVERE THUNDERSTORM & GALE WARNING",
            summary = "Sustained winds exceeding 88 km/h and heavy squalls detected.",
            safetyInstructions = "Seek sturdy indoor shelter immediately and monitor Civil Defense.",
            effectiveTime = "Immediate",
            expiresTime = "Next 6 Hours"
        )

        SevereWeatherAlertWorker.testWeatherFetcher = { _, _ ->
            val current = CurrentWeather(
                tempC = 27.4,
                feelsLikeC = 31.0,
                weatherCode = 99,
                conditionText = "Thunderstorm with Heavy Hail",
                isDay = true,
                humidityPercent = 89,
                pressureHpa = 994.0,
                windSpeedKmh = 92.0,
                windDirectionDeg = 115,
                windGustsKmh = 114.0,
                uvIndex = 2.0,
                cloudCoverPercent = 98,
                dewPointC = 24.5,
                visibilityKm = 3.2,
                aqi = 28,
                aqiStatus = "GOOD",
                pm25 = 8.2,
                pm10 = 14.0,
                sunriseTime = "06:45",
                sunsetTime = "19:30"
            )
            val pkg = FullWeatherPackage(
                diurnalPeriods = emptyList(),
                hourlyPoints = emptyList(),
                dailyDays = emptyList(),
                alerts = listOf(mockAlert),
                headlines = listOf("Severe gale warning in effect"),
                astronomical = AstronomicalData(
                    moonPhaseName = "Waxing Gibbous",
                    moonIlluminationPercent = 78,
                    daysUntilFullMoon = 4,
                    sunriseTime = "06:45",
                    sunsetTime = "19:30",
                    daylightProgressPercent = 0.5f
                )
            )
            Pair(current, pkg)
        }

        val worker = TestListenableWorkerBuilder<SevereWeatherAlertWorker>(context).build()
        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val success = result as ListenableWorker.Result.Success
        assertEquals("Miami", success.outputData.getString(SevereWeatherAlertWorker.KEY_OUTPUT_LOCATION))
        assertEquals(1, success.outputData.getInt(SevereWeatherAlertWorker.KEY_OUTPUT_ALERTS_COUNT, 0))
        assertEquals(1, success.outputData.getInt(SevereWeatherAlertWorker.KEY_OUTPUT_NOTIFIED_COUNT, 0))

        val postedNotifications = Shadows.shadowOf(notificationManager).allNotifications
        assertEquals("Expected 1 high-priority push notification", 1, postedNotifications.size)

        val channel = notificationManager.getNotificationChannel(SevereWeatherAlertNotificationHelper.CHANNEL_ID)
        assertNotNull("High-priority severe weather NotificationChannel must exist", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)

        // Second immediate run without forceNotify should deduplicate and not post a duplicate notification
        val secondWorker = TestListenableWorkerBuilder<SevereWeatherAlertWorker>(context).build()
        val secondResult = secondWorker.doWork() as ListenableWorker.Result.Success
        assertEquals(1, secondResult.outputData.getInt(SevereWeatherAlertWorker.KEY_OUTPUT_ALERTS_COUNT, 0))
        assertEquals(0, secondResult.outputData.getInt(SevereWeatherAlertWorker.KEY_OUTPUT_NOTIFIED_COUNT, -1))
        assertTrue(prefs.getLastBackgroundSyncSummary().contains("Miami"))
    }
}
