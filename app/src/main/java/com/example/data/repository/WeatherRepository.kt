package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

class WeatherRepository {

    private val weatherService = ApiClient.weatherService
    private val airQualityService = ApiClient.airQualityService
    private val geocodingService = ApiClient.geocodingService
    private val ipLocationService = ApiClient.ipLocationService

    // Default Fallback Location (New York, NY)
    val defaultLocation = CityLocation(
        name = "New York",
        region = "New York",
        country = "United States",
        countryCode = "US",
        latitude = 40.7128,
        longitude = -74.0060,
        isGpsDetected = false
    )

    // Default bookmarked cities
    fun getDefaultFavoriteCities(): List<CityLocation> = listOf(
        defaultLocation,
        CityLocation(name = "London", region = "Greater London", country = "United Kingdom", countryCode = "GB", latitude = 51.5074, longitude = -0.1278),
        CityLocation(name = "Tokyo", region = "Tokyo", country = "Japan", countryCode = "JP", latitude = 35.6762, longitude = 139.6503),
        CityLocation(name = "Sydney", region = "New South Wales", country = "Australia", countryCode = "AU", latitude = -33.8688, longitude = 151.2093),
        CityLocation(name = "Paris", region = "Île-de-France", country = "France", countryCode = "FR", latitude = 48.8566, longitude = 2.3522),
        CityLocation(name = "Toronto", region = "Ontario", country = "Canada", countryCode = "CA", latitude = 43.6532, longitude = -79.3832)
    )

    suspend fun detectRealLocation(): CityLocation = withContext(Dispatchers.IO) {
        try {
            val ipData = ipLocationService.detectIpLocation()
            if (ipData.latitude != null && ipData.longitude != null && !ipData.city.isNullOrBlank()) {
                return@withContext CityLocation(
                    name = ipData.city,
                    region = ipData.region ?: "",
                    country = ipData.countryName ?: "",
                    countryCode = ipData.countryCode ?: "",
                    latitude = ipData.latitude,
                    longitude = ipData.longitude,
                    isGpsDetected = true
                )
            }
        } catch (_: Exception) {
            // Fall back gracefully
        }
        defaultLocation
    }

    suspend fun searchCities(query: String): List<CityLocation> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val response = geocodingService.searchCities(name = query.trim(), count = 8)
            val results = response.results ?: return@withContext emptyList()
            results.map {
                CityLocation(
                    name = it.name,
                    region = it.admin1 ?: "",
                    country = it.country ?: "",
                    countryCode = it.countryCode ?: "",
                    latitude = it.latitude,
                    longitude = it.longitude,
                    isGpsDetected = false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchRealWeatherData(
        latitude: Double,
        longitude: Double
    ): Pair<CurrentWeather, FullWeatherPackage> = withContext(Dispatchers.IO) {
        val forecastDeferred = weatherService.getForecast(latitude, longitude)
        val aqiDeferred = try {
            airQualityService.getAirQuality(latitude, longitude)
        } catch (_: Exception) {
            null
        }

        val currentData = forecastDeferred.current ?: CurrentUnitsAndData()
        val hourlyData = forecastDeferred.hourly ?: HourlyData()
        val dailyData = forecastDeferred.daily ?: DailyData()
        val aqiData = aqiDeferred?.current ?: AirQualityCurrent()

        val weatherCode = currentData.weatherCode
        val conditionText = getWeatherDescription(weatherCode)
        val aqiLevel = aqiData.usAqi
        val aqiStatus = getAqiStatus(aqiLevel)

        val sunrise = dailyData.sunrise.firstOrNull()?.substringAfter("T") ?: "06:00"
        val sunset = dailyData.sunset.firstOrNull()?.substringAfter("T") ?: "19:30"

        val currentWeather = CurrentWeather(
            tempC = currentData.temperature2m,
            feelsLikeC = currentData.apparentTemperature,
            weatherCode = weatherCode,
            conditionText = conditionText,
            isDay = currentData.isDay == 1,
            humidityPercent = currentData.relativeHumidity2m.roundToInt(),
            pressureHpa = currentData.surfacePressure,
            windSpeedKmh = currentData.windSpeed10m,
            windDirectionDeg = currentData.windDirection10m.roundToInt(),
            windGustsKmh = currentData.windGusts10m,
            uvIndex = currentData.uvIndex,
            cloudCoverPercent = currentData.cloudCover.roundToInt(),
            dewPointC = hourlyData.dewPoint2m.firstOrNull() ?: (currentData.temperature2m - 4.0),
            visibilityKm = (hourlyData.visibility.firstOrNull() ?: 10000.0) / 1000.0,
            aqi = aqiLevel,
            aqiStatus = aqiStatus,
            pm25 = aqiData.pm25,
            pm10 = aqiData.pm10,
            sunriseTime = sunrise,
            sunsetTime = sunset
        )

        val diurnalPeriods = computeDiurnalPeriods(hourlyData, dailyData)
        val hourlyPoints = computeHourlyPoints(hourlyData)
        val dailyDays = computeDailyDays(dailyData)
        val alerts = evaluateSevereWeatherAlerts(currentWeather, dailyDays)
        val headlines = generateRealLocalHeadlines(currentWeather, diurnalPeriods, alerts)
        val astronomical = calculateAstronomicalData(sunrise, sunset)

        val packageData = FullWeatherPackage(
            diurnalPeriods = diurnalPeriods,
            hourlyPoints = hourlyPoints,
            dailyDays = dailyDays,
            alerts = alerts,
            headlines = headlines,
            astronomical = astronomical
        )

        Pair(currentWeather, packageData)
    }

    private fun computeDiurnalPeriods(hourly: HourlyData, daily: DailyData): List<DiurnalPeriod> {
        val nowCal = Calendar.getInstance()
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)

        val periods = mutableListOf<DiurnalPeriod>()
        val periodConfigs = listOf(
            Triple("Morning", "06:00 - 12:00", 6..11),
            Triple("Afternoon", "12:00 - 18:00", 12..17),
            Triple("Evening", "18:00 - 22:00", 18..21),
            Triple("Night", "22:00 - 06:00", 22..24)
        )

        for (cfg in periodConfigs) {
            val hourRange = cfg.third
            val matchedTemps = mutableListOf<Double>()
            val matchedPops = mutableListOf<Int>()
            val matchedCodes = mutableListOf<Int>()
            val matchedWinds = mutableListOf<Double>()

            hourly.time.forEachIndexed { index, timeStr ->
                val hourInt = timeStr.substringAfter("T").substringBefore(":").toIntOrNull() ?: 0
                if (hourInt in hourRange && matchedTemps.size < 6) {
                    hourly.temperature2m.getOrNull(index)?.let { matchedTemps.add(it) }
                    hourly.precipitationProbability.getOrNull(index)?.let { matchedPops.add(it) }
                    hourly.weatherCode.getOrNull(index)?.let { matchedCodes.add(it) }
                    hourly.windSpeed10m.getOrNull(index)?.let { matchedWinds.add(it) }
                }
            }

            val avgTemp = if (matchedTemps.isNotEmpty()) matchedTemps.average() else 20.0
            val maxPop = if (matchedPops.isNotEmpty()) matchedPops.maxOrNull() ?: 0 else 10
            val code = matchedCodes.firstOrNull() ?: 0
            val wind = if (matchedWinds.isNotEmpty()) matchedWinds.average() else 12.0

            val isCurrent = when (cfg.first) {
                "Morning" -> currentHour in 6..11
                "Afternoon" -> currentHour in 12..17
                "Evening" -> currentHour in 18..21
                "Night" -> currentHour >= 22 || currentHour < 6
                else -> false
            }

            periods.add(
                DiurnalPeriod(
                    periodName = cfg.first,
                    timeRange = cfg.second,
                    tempC = avgTemp,
                    feelsLikeC = avgTemp - 1.0,
                    conditionText = getWeatherDescription(code),
                    weatherCode = code,
                    popPercent = maxPop,
                    windSpeedKmh = wind,
                    isCurrent = isCurrent
                )
            )
        }

        return periods
    }

    private fun computeHourlyPoints(hourly: HourlyData): List<HourlyPoint> {
        val points = mutableListOf<HourlyPoint>()
        val count = minOf(hourly.time.size, 24)
        for (i in 0 until count) {
            val iso = hourly.time.getOrNull(i) ?: ""
            val timeLabel = if (iso.contains("T")) iso.substringAfter("T").take(5) else "12:00"
            val temp = hourly.temperature2m.getOrNull(i) ?: 20.0
            val feels = hourly.apparentTemperature.getOrNull(i) ?: temp
            val code = hourly.weatherCode.getOrNull(i) ?: 0
            val pop = hourly.precipitationProbability.getOrNull(i) ?: 0
            val wind = hourly.windSpeed10m.getOrNull(i) ?: 10.0
            val humidity = hourly.relativeHumidity2m.getOrNull(i)?.roundToInt() ?: 50

            points.add(
                HourlyPoint(
                    timeLabel = timeLabel,
                    rawIsoTime = iso,
                    tempC = temp,
                    feelsLikeC = feels,
                    weatherCode = code,
                    conditionText = getWeatherDescription(code),
                    popPercent = pop,
                    windSpeedKmh = wind,
                    humidityPercent = humidity
                )
            )
        }
        return points
    }

    private fun computeDailyDays(daily: DailyData): List<DailyForecastDay> {
        val days = mutableListOf<DailyForecastDay>()
        val count = minOf(daily.time.size, 14)
        val sdfParse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.US)
        val sdfShort = SimpleDateFormat("EEE, MMM d", Locale.US)

        for (i in 0 until count) {
            val dateStr = daily.time.getOrNull(i) ?: ""
            var dayOfWeek = "Day $i"
            var label = dateStr

            try {
                val parsed = sdfParse.parse(dateStr)
                if (parsed != null) {
                    dayOfWeek = if (i == 0) "Today" else if (i == 1) "Tomorrow" else sdfDayOfWeek.format(parsed)
                    label = sdfShort.format(parsed)
                }
            } catch (_: Exception) {}

            val maxT = daily.temperature2mMax.getOrNull(i) ?: 24.0
            val minT = daily.temperature2mMin.getOrNull(i) ?: 16.0
            val code = daily.weatherCode.getOrNull(i) ?: 0
            val rain = daily.precipitationSum.getOrNull(i) ?: 0.0
            val pop = daily.precipitationProbabilityMax.getOrNull(i) ?: 10
            val uv = daily.uvIndexMax.getOrNull(i) ?: 5.0
            val wind = daily.windSpeed10mMax.getOrNull(i) ?: 15.0

            days.add(
                DailyForecastDay(
                    dateLabel = label,
                    dayOfWeek = dayOfWeek,
                    maxTempC = maxT,
                    minTempC = minT,
                    weatherCode = code,
                    conditionText = getWeatherDescription(code),
                    rainMm = rain,
                    popPercent = pop,
                    maxUvIndex = uv,
                    maxWindKmh = wind
                )
            )
        }
        return days
    }

    private fun evaluateSevereWeatherAlerts(
        current: CurrentWeather,
        dailyDays: List<DailyForecastDay>
    ): List<SevereWeatherAlert> {
        val alerts = mutableListOf<SevereWeatherAlert>()

        // 1. High Wind Warning
        if (current.windGustsKmh >= 65.0 || current.windSpeedKmh >= 50.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_wind",
                    severity = AlertSeverity.WARNING,
                    title = "HIGH WIND WARNING",
                    summary = "Sustained winds at ${current.windSpeedKmh.roundToInt()} km/h with dangerous peak gusts up to ${current.windGustsKmh.roundToInt()} km/h.",
                    safetyInstructions = "Secure loose outdoor furniture, avoid travel in high-profile vehicles, and watch for downed tree limbs and power lines.",
                    effectiveTime = "Active now",
                    expiresTime = "Expires in 6 hours"
                )
            )
        }

        // 2. Extreme Heat Advisory
        if (current.tempC >= 35.0 || current.feelsLikeC >= 38.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_heat",
                    severity = AlertSeverity.ADVISORY,
                    title = "EXCESSIVE HEAT ADVISORY",
                    summary = "Dangerous heat index reaching ${current.feelsLikeC.roundToInt()}°C under strong insolation.",
                    safetyInstructions = "Drink plenty of fluids, remain in air-conditioned rooms, avoid prolonged sun exposure, and check up on vulnerable relatives.",
                    effectiveTime = "Until 20:00 local time",
                    expiresTime = "Tonight"
                )
            )
        }

        // 3. Hard Freeze Warning
        if (current.tempC <= -5.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_freeze",
                    severity = AlertSeverity.WARNING,
                    title = "HARD FREEZE WARNING",
                    summary = "Sub-freezing temperatures at ${current.tempC.roundToInt()}°C posing frostbite risk and pipe hazards.",
                    safetyInstructions = "Protect exposed plumbing, bring sensitive plants indoors, and ensure pets have adequate warm shelter.",
                    effectiveTime = "Active now",
                    expiresTime = "Tomorrow 09:00"
                )
            )
        }

        // 4. Thunderstorm / Flood Warning
        val todayForecast = dailyDays.firstOrNull()
        if (current.weatherCode in listOf(95, 96, 99) || (todayForecast != null && todayForecast.rainMm > 25.0)) {
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_storm",
                    severity = AlertSeverity.EMERGENCY,
                    title = "SEVERE THUNDERSTORM / FLOOD ADVISORY",
                    summary = "Active convective activity bringing rapid rainfall accumulation and cloud-to-ground lightning.",
                    safetyInstructions = "Seek sturdy indoor shelter immediately. Avoid low-lying flooded roadways: Turn around, don't drown.",
                    effectiveTime = "Immediate broadcast",
                    expiresTime = "Until convective cell passes"
                )
            )
        }

        // 5. Air Quality Alert
        if (current.aqi >= 151) {
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_aqi",
                    severity = AlertSeverity.WATCH,
                    title = "AIR QUALITY ALERT (${current.aqi} AQI)",
                    summary = "Particulate pollution levels exceed safe thresholds across the metro area.",
                    safetyInstructions = "People with respiratory or heart conditions, older adults, and children should avoid prolonged outdoor exertion.",
                    effectiveTime = "Today",
                    expiresTime = "Until air dispersion improves"
                )
            )
        }

        return alerts
    }

    private fun generateRealLocalHeadlines(
        current: CurrentWeather,
        periods: List<DiurnalPeriod>,
        alerts: List<SevereWeatherAlert>
    ): List<String> {
        val list = mutableListOf<String>()

        if (alerts.isNotEmpty()) {
            alerts.forEach {
                list.add("EMERGENCY BROADCAST: ${it.title} - ${it.summary}")
            }
        }

        list.add("CURRENT OBSERVATION: ${current.conditionText.uppercase()}, ${current.tempC.roundToInt()}°C (Feels like ${current.feelsLikeC.roundToInt()}°C) with ${current.humidityPercent}% humidity.")
        list.add("WIND TELEMETRY: Winds traveling from ${getWindCompass(current.windDirectionDeg)} at ${current.windSpeedKmh.roundToInt()} km/h, peak gusts at ${current.windGustsKmh.roundToInt()} km/h.")
        list.add("AIR QUALITY INDEX: US AQI reads ${current.aqi} (${current.aqiStatus}) with PM2.5 at ${String.format(Locale.US, "%.1f", current.pm25)} µg/m³.")
        list.add("SOLAR TIMING: First light sunrise logged at ${current.sunriseTime} | Sunset dusk horizon scheduled at ${current.sunsetTime}.")

        val nextPeriod = periods.firstOrNull { !it.isCurrent } ?: periods.firstOrNull()
        if (nextPeriod != null) {
            list.add("OUTLOOK FOR ${nextPeriod.periodName.uppercase()}: Expecting ${nextPeriod.conditionText} near ${nextPeriod.tempC.roundToInt()}°C with ${nextPeriod.popPercent}% probability of precipitation.")
        }

        return list
    }

    fun calculateAstronomicalData(sunrise: String, sunset: String): AstronomicalData {
        val now = System.currentTimeMillis()
        // Reference new moon: Jan 11, 2024 at 11:57 UTC = 1704974220000L
        val refNewMoonMs = 1704974220000L
        val synodicMs = 29.53058770576 * 24 * 60 * 60 * 1000
        val diffMs = (now - refNewMoonMs) % synodicMs.toLong()
        val phaseDays = (diffMs.toDouble() / (24 * 60 * 60 * 1000)).let { if (it < 0) it + 29.53 else it }

        val illumination = ((1.0 - cos(2 * PI * phaseDays / 29.530588)) / 2.0 * 100.0).roundToInt().coerceIn(0, 100)

        val phaseName = when {
            phaseDays < 1.8 -> "New Moon"
            phaseDays < 7.4 -> "Waxing Crescent"
            phaseDays < 9.2 -> "First Quarter"
            phaseDays < 13.8 -> "Waxing Gibbous"
            phaseDays < 15.7 -> "Full Moon"
            phaseDays < 22.1 -> "Waning Gibbous"
            phaseDays < 23.9 -> "Last Quarter"
            else -> "Waning Crescent"
        }

        val daysUntilFull = ((14.765 - phaseDays + 29.53) % 29.53).roundToInt()

        // Solar daylight progress
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val riseH = sunrise.substringBefore(":").toIntOrNull() ?: 6
        val riseM = sunrise.substringAfter(":").take(2).toIntOrNull() ?: 0
        val riseTotal = riseH * 60 + riseM

        val setH = sunset.substringBefore(":").toIntOrNull() ?: 19
        val setM = sunset.substringAfter(":").take(2).toIntOrNull() ?: 30
        val setTotal = setH * 60 + setM

        val progress = if (currentMinutes < riseTotal) {
            0.0f
        } else if (currentMinutes > setTotal) {
            1.0f
        } else {
            ((currentMinutes - riseTotal).toFloat() / (setTotal - riseTotal).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        }

        return AstronomicalData(
            moonPhaseName = phaseName,
            moonIlluminationPercent = illumination,
            daysUntilFullMoon = daysUntilFull,
            sunriseTime = sunrise,
            sunsetTime = sunset,
            daylightProgressPercent = progress
        )
    }

    fun getSampleVodStories(): List<VodStory> = listOf(
        VodStory(
            id = "vod_1",
            category = "Severe Weather",
            title = "Inside the Eye of a Category 5 Atlantic Super-Storm",
            presenter = "Chief Meteorologist Marcus Vance",
            duration = "14:20",
            timestamp = "Today 16:30",
            description = "High-definition Doppler dissection of atmospheric barometric collapses and eyewall mesovortices.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            accentColorHex = 0xFFDC2626
        ),
        VodStory(
            id = "vod_2",
            category = "Earth Science",
            title = "Jet Stream Fractures & Arctic Polar Vortex Dynamics",
            presenter = "Dr. Elena Rostova",
            duration = "09:45",
            timestamp = "Yesterday 20:00",
            description = "How Rossby wave undulations trigger unprecedented temperature swings across northern temperate latitudes.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            accentColorHex = 0xFF0284C7
        ),
        VodStory(
            id = "vod_3",
            category = "Forecasts",
            title = "Continental 30-Day Precipitation & Drought Outlook",
            presenter = "Atmospheric Analyst Sarah Chen",
            duration = "08:12",
            timestamp = "2 hours ago",
            description = "Global climate indices and sea-surface anomalies signaling major multi-week rain systems.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            accentColorHex = 0xFF10B981
        ),
        VodStory(
            id = "vod_4",
            category = "Climate & Nature",
            title = "Noctilucent Mesospheric Clouds: Night Shimmers Explored",
            presenter = "Prof. Kenji Takahashi",
            duration = "11:55",
            timestamp = "3 days ago",
            description = "Spectacular ice crystal formations dancing on the threshold of space 80 kilometers above Earth.",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            accentColorHex = 0xFF8B5CF6
        )
    )

    companion object {
        fun getWeatherDescription(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            71 -> "Slight Snow"
            73 -> "Moderate Snow"
            75 -> "Heavy Snow"
            77 -> "Snow Grains"
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            85 -> "Slight Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with Hail"
            99 -> "Severe Hail Thunderstorm"
            else -> "Atmospheric Conditions"
        }

        fun getAqiStatus(aqi: Int): String = when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for Sensitive Groups"
            aqi <= 200 -> "Unhealthy"
            aqi <= 300 -> "Very Unhealthy"
            else -> "Hazardous"
        }

        fun getWindCompass(degrees: Int): String {
            val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
            val index = (((degrees % 360) + 360) % 360 / 22.5 + 0.5).toInt() % 16
            return directions[index]
        }
    }
}

data class FullWeatherPackage(
    val diurnalPeriods: List<DiurnalPeriod>,
    val hourlyPoints: List<HourlyPoint>,
    val dailyDays: List<DailyForecastDay>,
    val alerts: List<SevereWeatherAlert>,
    val headlines: List<String>,
    val astronomical: AstronomicalData
)
