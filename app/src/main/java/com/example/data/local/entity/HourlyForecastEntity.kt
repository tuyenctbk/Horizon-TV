package com.example.data.local.entity

import androidx.room.Entity
import com.example.data.model.HourlyPoint

@Entity(
    tableName = "hourly_forecast_cache",
    primaryKeys = ["locationKey", "hourIndex"]
)
data class HourlyForecastEntity(
    val locationKey: String,
    val hourIndex: Int,
    val timeLabel: String,
    val rawIsoTime: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val popPercent: Int,
    val windSpeedKmh: Double,
    val humidityPercent: Int,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toHourlyPoint(): HourlyPoint = HourlyPoint(
        timeLabel = timeLabel,
        rawIsoTime = rawIsoTime,
        tempC = tempC,
        feelsLikeC = feelsLikeC,
        weatherCode = weatherCode,
        conditionText = conditionText,
        popPercent = popPercent,
        windSpeedKmh = windSpeedKmh,
        humidityPercent = humidityPercent
    )

    companion object {
        fun from(
            locationKey: String,
            hourIndex: Int,
            point: HourlyPoint,
            cachedAtEpochMs: Long = System.currentTimeMillis()
        ): HourlyForecastEntity = HourlyForecastEntity(
            locationKey = locationKey,
            hourIndex = hourIndex,
            timeLabel = point.timeLabel,
            rawIsoTime = point.rawIsoTime,
            tempC = point.tempC,
            feelsLikeC = point.feelsLikeC,
            weatherCode = point.weatherCode,
            conditionText = point.conditionText,
            popPercent = point.popPercent,
            windSpeedKmh = point.windSpeedKmh,
            humidityPercent = point.humidityPercent,
            cachedAtEpochMs = cachedAtEpochMs
        )
    }
}
