package com.example.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.repository.UserPreferencesManager
import java.util.concurrent.TimeUnit

/**
 * Schedules and manages Periodic and On-Demand WorkManager jobs for severe weather alerts.
 */
object WeatherAlertWorkScheduler {

    const val PERIODIC_WORK_NAME = "horizon_tv_periodic_severe_weather_worker"
    const val IMMEDIATE_WORK_NAME = "horizon_tv_immediate_severe_weather_worker"
    const val WORK_TAG = "horizon_severe_weather_tag"

    fun syncPeriodicSchedule(
        context: Context,
        replaceExisting: Boolean = false
    ) {
        val prefs = UserPreferencesManager(context)
        if (!prefs.isBackgroundAlertsEnabled()) {
            cancelPeriodicChecks(context)
            return
        }

        val intervalMinutes = prefs.getBackgroundSyncIntervalMinutes().coerceAtLeast(15).toLong()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SevereWeatherAlertWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15L,
                TimeUnit.MINUTES
            )
            .build()

        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        getWorkManagerSafe(context)?.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            policy,
            periodicRequest
        )
    }

    fun triggerImmediateBackgroundCheck(
        context: Context,
        forceNotify: Boolean = true
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putBoolean(SevereWeatherAlertWorker.KEY_FORCE_NOTIFY, forceNotify)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SevereWeatherAlertWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(WORK_TAG)
            .build()

        getWorkManagerSafe(context)?.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun cancelPeriodicChecks(context: Context) {
        getWorkManagerSafe(context)?.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    private fun getWorkManagerSafe(context: Context): WorkManager? {
        return try {
            WorkManager.getInstance(context)
        } catch (e: IllegalStateException) {
            try {
                WorkManager.initialize(
                    context.applicationContext,
                    androidx.work.Configuration.Builder().build()
                )
                WorkManager.getInstance(context)
            } catch (_: Exception) {
                null
            }
        }
    }
}
