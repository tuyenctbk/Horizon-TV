package com.example.data.local.entity

import androidx.room.Entity
import com.example.data.model.DailyForecastDay

@Entity(
    tableName = "daily_forecast_cache",
    primaryKeys = ["locationKey", "dayIndex"]
)
data class DailyForecastEntity(
    val locationKey: String,
    val dayIndex: Int,
    val dateLabel: String,
    val dayOfWeek: String,
    val maxTempC: Double,
    val minTempC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val rainMm: Double,
    val popPercent: Int,
    val maxUvIndex: Double,
    val maxWindKmh: Double,
    val apparentMaxTempC: Double,
    val apparentMinTempC: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val maxWindGustsKmh: Double,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDailyForecastDay(): DailyForecastDay = DailyForecastDay(
        dateLabel = dateLabel,
        dayOfWeek = dayOfWeek,
        maxTempC = maxTempC,
        minTempC = minTempC,
        weatherCode = weatherCode,
        conditionText = conditionText,
        rainMm = rainMm,
        popPercent = popPercent,
        maxUvIndex = maxUvIndex,
        maxWindKmh = maxWindKmh,
        apparentMaxTempC = apparentMaxTempC,
        apparentMinTempC = apparentMinTempC,
        sunriseTime = sunriseTime,
        sunsetTime = sunsetTime,
        maxWindGustsKmh = maxWindGustsKmh
    )

    companion object {
        fun from(
            locationKey: String,
            dayIndex: Int,
            day: DailyForecastDay,
            cachedAtEpochMs: Long = System.currentTimeMillis()
        ): DailyForecastEntity = DailyForecastEntity(
            locationKey = locationKey,
            dayIndex = dayIndex,
            dateLabel = day.dateLabel,
            dayOfWeek = day.dayOfWeek,
            maxTempC = day.maxTempC,
            minTempC = day.minTempC,
            weatherCode = day.weatherCode,
            conditionText = day.conditionText,
            rainMm = day.rainMm,
            popPercent = day.popPercent,
            maxUvIndex = day.maxUvIndex,
            maxWindKmh = day.maxWindKmh,
            apparentMaxTempC = day.apparentMaxTempC,
            apparentMinTempC = day.apparentMinTempC,
            sunriseTime = day.sunriseTime,
            sunsetTime = day.sunsetTime,
            maxWindGustsKmh = day.maxWindGustsKmh,
            cachedAtEpochMs = cachedAtEpochMs
        )
    }
}
