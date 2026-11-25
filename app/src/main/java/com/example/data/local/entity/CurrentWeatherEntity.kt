package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import java.util.Locale

@Entity(tableName = "current_weather_cache")
data class CurrentWeatherEntity(
    @PrimaryKey
    val locationKey: String,
    val cityName: String,
    val region: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val tempC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val isDay: Boolean,
    val humidityPercent: Int,
    val pressureHpa: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val windGustsKmh: Double,
    val uvIndex: Double,
    val cloudCoverPercent: Int,
    val dewPointC: Double,
    val visibilityKm: Double,
    val aqi: Int,
    val aqiStatus: String,
    val pm25: Double,
    val pm10: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toCurrentWeather(): CurrentWeather = CurrentWeather(
        tempC = tempC,
        feelsLikeC = feelsLikeC,
        weatherCode = weatherCode,
        conditionText = conditionText,
        isDay = isDay,
        humidityPercent = humidityPercent,
        pressureHpa = pressureHpa,
        windSpeedKmh = windSpeedKmh,
        windDirectionDeg = windDirectionDeg,
        windGustsKmh = windGustsKmh,
        uvIndex = uvIndex,
        cloudCoverPercent = cloudCoverPercent,
        dewPointC = dewPointC,
        visibilityKm = visibilityKm,
        aqi = aqi,
        aqiStatus = aqiStatus,
        pm25 = pm25,
        pm10 = pm10,
        sunriseTime = sunriseTime,
        sunsetTime = sunsetTime
    )

    fun toCityLocation(): CityLocation = CityLocation(
        name = cityName,
        region = region,
        country = country,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        isGpsDetected = false
    )

    companion object {
        fun createKey(lat: Double, lon: Double): String =
            String.format(Locale.US, "%.3f_%.3f", lat, lon)

        fun from(
            location: CityLocation,
            current: CurrentWeather,
            cachedAtEpochMs: Long = System.currentTimeMillis()
        ): CurrentWeatherEntity = CurrentWeatherEntity(
            locationKey = createKey(location.latitude, location.longitude),
            cityName = location.name,
            region = location.region,
            country = location.country,
            countryCode = location.countryCode,
            latitude = location.latitude,
            longitude = location.longitude,
            tempC = current.tempC,
            feelsLikeC = current.feelsLikeC,
            weatherCode = current.weatherCode,
            conditionText = current.conditionText,
            isDay = current.isDay,
            humidityPercent = current.humidityPercent,
            pressureHpa = current.pressureHpa,
            windSpeedKmh = current.windSpeedKmh,
            windDirectionDeg = current.windDirectionDeg,
            windGustsKmh = current.windGustsKmh,
            uvIndex = current.uvIndex,
            cloudCoverPercent = current.cloudCoverPercent,
            dewPointC = current.dewPointC,
            visibilityKm = current.visibilityKm,
            aqi = current.aqi,
            aqiStatus = current.aqiStatus,
            pm25 = current.pm25,
            pm10 = current.pm10,
            sunriseTime = current.sunriseTime,
            sunsetTime = current.sunsetTime,
            cachedAtEpochMs = cachedAtEpochMs
        )
    }
}
