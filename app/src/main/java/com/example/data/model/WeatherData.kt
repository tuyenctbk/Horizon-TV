package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UnitSystem {
    METRIC,    // °C, km/h, mm, hPa
    IMPERIAL   // °F, mph, in, inHg
}

enum class TVScreen {
    HOME,
    FORECAST,
    LIVE_TV,
    RADAR,
    VOD,
    SEARCH,
    SETTINGS
}

data class CityLocation(
    val name: String,
    val region: String = "",
    val country: String = "",
    val countryCode: String = "",
    val latitude: Double,
    val longitude: Double,
    val isGpsDetected: Boolean = false
)

// Open-Meteo Geocoding Response
@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "admin1") val admin1: String? = null
)

// IP Geolocation Response
@JsonClass(generateAdapter = true)
data class IpApiResponse(
    val ip: String? = null,
    val city: String? = null,
    val region: String? = null,
    @Json(name = "country_name") val countryName: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// Open-Meteo Weather Response
@JsonClass(generateAdapter = true)
data class OpenMeteoWeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val current: CurrentUnitsAndData? = null,
    val hourly: HourlyData? = null,
    val daily: DailyData? = null
)

@JsonClass(generateAdapter = true)
data class CurrentUnitsAndData(
    val time: String? = null,
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double = 0.0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    val rain: Double = 0.0,
    val showers: Double = 0.0,
    val snowfall: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "cloud_cover") val cloudCover: Double = 0.0,
    @Json(name = "pressure_msl") val pressureMsl: Double = 1013.25,
    @Json(name = "surface_pressure") val surfacePressure: Double = 1013.25,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Double = 0.0,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double = 0.0,
    @Json(name = "uv_index") val uvIndex: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class HourlyData(
    val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Double> = emptyList(),
    @Json(name = "dew_point_2m") val dewPoint2m: List<Double> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int> = emptyList(),
    val precipitation: List<Double> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "pressure_msl") val pressureMsl: List<Double> = emptyList(),
    @Json(name = "surface_pressure") val surfacePressure: List<Double> = emptyList(),
    @Json(name = "cloud_cover") val cloudCover: List<Double> = emptyList(),
    val visibility: List<Double> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double> = emptyList(),
    @Json(name = "wind_direction_10m") val windDirection10m: List<Double> = emptyList(),
    @Json(name = "wind_gusts_10m") val windGusts10m: List<Double> = emptyList(),
    @Json(name = "uv_index") val uvIndex: List<Double> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyData(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double> = emptyList(),
    @Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double> = emptyList(),
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double> = emptyList(),
    @Json(name = "precipitation_sum") val precipitationSum: List<Double> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int> = emptyList(),
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double> = emptyList(),
    @Json(name = "wind_gusts_10m_max") val windGusts10mMax: List<Double> = emptyList()
)

// Open-Meteo Air Quality Response
@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
    val current: AirQualityCurrent? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrent(
    @Json(name = "us_aqi") val usAqi: Int = 35,
    val pm10: Double = 12.0,
    @Json(name = "pm2_5") val pm25: Double = 8.0,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double = 220.0,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double = 15.0,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: Double = 3.0,
    val ozone: Double = 45.0
)

// Standardized Domain Models
data class CurrentWeather(
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
    val sunsetTime: String
)

data class DiurnalPeriod(
    val periodName: String, // "Morning", "Afternoon", "Evening", "Night"
    val timeRange: String,  // "06:00 - 12:00"
    val tempC: Double,
    val feelsLikeC: Double,
    val conditionText: String,
    val weatherCode: Int,
    val popPercent: Int,    // Probability of precipitation
    val windSpeedKmh: Double,
    val isCurrent: Boolean = false
)

data class HourlyPoint(
    val timeLabel: String,  // "14:00"
    val rawIsoTime: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val popPercent: Int,
    val windSpeedKmh: Double,
    val humidityPercent: Int
)

data class DailyForecastDay(
    val dateLabel: String,     // "Tomorrow", "Wed Sep 05"
    val dayOfWeek: String,     // "Wednesday"
    val maxTempC: Double,
    val minTempC: Double,
    val weatherCode: Int,
    val conditionText: String,
    val rainMm: Double,
    val popPercent: Int,
    val maxUvIndex: Double,
    val maxWindKmh: Double
)

data class SevereWeatherAlert(
    val id: String,
    val severity: AlertSeverity,
    val title: String,
    val summary: String,
    val safetyInstructions: String,
    val effectiveTime: String,
    val expiresTime: String,
    val source: String = "National Meteorology Center Broadcast"
)

enum class AlertSeverity {
    ADVISORY,
    WATCH,
    WARNING,
    EMERGENCY
}

data class AstronomicalData(
    val moonPhaseName: String,
    val moonIlluminationPercent: Int,
    val daysUntilFullMoon: Int,
    val sunriseTime: String,
    val sunsetTime: String,
    val daylightProgressPercent: Float // 0f to 1f
)

data class VodStory(
    val id: String,
    val category: String, // "Severe Weather", "Forecasts", "Earth Science", "Climate & Nature"
    val title: String,
    val presenter: String,
    val duration: String,
    val timestamp: String,
    val description: String,
    val videoUrl: String,
    val accentColorHex: Long
)
