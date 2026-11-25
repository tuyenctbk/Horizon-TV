package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.data.model.UnitSystem
import com.example.data.repository.UserPreferencesManager
import com.example.data.repository.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * WorkManager CoroutineWorker that periodically fetches live local weather telemetry from
 * Open-Meteo in the background and triggers high-priority push notifications whenever severe
 * weather alerts are detected in the user's area.
 */
class SevereWeatherAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SevereWeatherWorker"
        const val KEY_FORCE_NOTIFY = "key_force_notify"
        const val KEY_OUTPUT_LOCATION = "key_output_location"
        const val KEY_OUTPUT_ALERTS_COUNT = "key_output_alerts_count"
        const val KEY_OUTPUT_NOTIFIED_COUNT = "key_output_notified_count"

        // Allows Robolectric unit tests to supply a custom weather payload deterministically
        @Volatile
        var testWeatherFetcher: (suspend (Double, Double) -> Pair<com.example.data.model.CurrentWeather, com.example.data.repository.FullWeatherPackage>)? = null
    }

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesManager(applicationContext)
        val forceNotify = inputData.getBoolean(KEY_FORCE_NOTIFY, false)

        if (!prefs.isBackgroundAlertsEnabled() && !forceNotify) {
            Log.d(TAG, "Background weather alert polling is disabled by user preference.")
            return Result.success()
        }

        val repository = WeatherRepository(applicationContext)

        return try {
            val targetLocation = prefs.getLastSelectedLocation() ?: repository.detectRealLocation()
            val fetcher = testWeatherFetcher
            val (currentWeather, weatherPackage) = if (fetcher != null) {
                fetcher(targetLocation.latitude, targetLocation.longitude)
            } else {
                repository.fetchRealWeatherData(
                    latitude = targetLocation.latitude,
                    longitude = targetLocation.longitude,
                    location = targetLocation
                )
            }

            val alerts = weatherPackage.alerts
            var notifiedCount = 0
            val nowMs = System.currentTimeMillis()

            for (alert in alerts) {
                val dedupKey = "${targetLocation.name.lowercase(Locale.US)}_${alert.id}"
                if (forceNotify || prefs.shouldNotifySevereAlert(dedupKey, nowMs = nowMs)) {
                    val posted = SevereWeatherAlertNotificationHelper.postSevereWeatherNotification(
                        context = applicationContext,
                        locationName = targetLocation.name,
                        alert = alert
                    )
                    if (posted) {
                        prefs.markSevereAlertNotified(dedupKey, nowMs = nowMs)
                        notifiedCount++
                    }
                }
            }

            val unitSystem = prefs.getUnitSystem()
            val tempLabel = if (unitSystem == UnitSystem.METRIC) {
                "${currentWeather.tempC.roundToInt()}°C"
            } else {
                "${(currentWeather.tempC * 9.0 / 5.0 + 32.0).roundToInt()}°F"
            }
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
            val conditionLocalized = applicationContext.getString(
                WeatherRepository.getWeatherDescriptionRes(currentWeather.weatherCode)
            )
            val statusSummary = if (alerts.isEmpty()) {
                applicationContext.getString(
                    com.example.R.string.bg_sync_summary_no_alerts,
                    targetLocation.name,
                    tempLabel,
                    conditionLocalized,
                    timeStr
                )
            } else {
                applicationContext.getString(
                    com.example.R.string.bg_sync_summary_with_alerts,
                    targetLocation.name,
                    tempLabel,
                    alerts.size,
                    timeStr
                )
            }
            prefs.recordBackgroundSyncResult(nowMs, statusSummary)

            val output = Data.Builder()
                .putString(KEY_OUTPUT_LOCATION, targetLocation.name)
                .putInt(KEY_OUTPUT_ALERTS_COUNT, alerts.size)
                .putInt(KEY_OUTPUT_NOTIFIED_COUNT, notifiedCount)
                .build()

            Result.success(output)
        } catch (e: Exception) {
            Log.e(TAG, "Background severe weather check failed", e)
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
