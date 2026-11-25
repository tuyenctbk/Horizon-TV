package com.example.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.example.R
import com.example.data.api.ApiClient
import com.example.data.local.HorizonWeatherDatabase
import com.example.data.local.dao.WeatherCacheDao
import com.example.data.local.entity.CurrentWeatherEntity
import com.example.data.local.entity.DailyForecastEntity
import com.example.data.local.entity.HourlyForecastEntity
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

data class CachedWeatherSnapshot(
    val location: CityLocation,
    val currentWeather: CurrentWeather,
    val weatherPackage: FullWeatherPackage,
    val cachedAtEpochMs: Long
)

class WeatherRepository(
    private val appContext: Context? = null,
    private val injectedCacheDao: WeatherCacheDao? = null
) {

    private val weatherService = ApiClient.weatherService
    private val airQualityService = ApiClient.airQualityService
    private val geocodingService = ApiClient.geocodingService
    private val ipLocationService = ApiClient.ipLocationService
    private val rainViewerService = ApiClient.rainViewerService

    private val weatherCacheDao: WeatherCacheDao? by lazy {
        injectedCacheDao ?: appContext?.let {
            HorizonWeatherDatabase.getInstance(it).weatherCacheDao()
        }
    }

    fun observeCachedCurrentWeather(latitude: Double, longitude: Double): Flow<CurrentWeatherEntity?> {
        val dao = weatherCacheDao ?: return flowOf(null)
        return dao.observeCurrentWeather(CurrentWeatherEntity.createKey(latitude, longitude))
    }

    fun observeCachedSevenDayForecast(latitude: Double, longitude: Double): Flow<List<DailyForecastEntity>> {
        val dao = weatherCacheDao ?: return flowOf(emptyList())
        return dao.observeDailyForecast(CurrentWeatherEntity.createKey(latitude, longitude))
    }

    fun observeCachedStationCount(): Flow<Int> {
        val dao = weatherCacheDao ?: return flowOf(0)
        return dao.observeCachedStationCount()
    }

    suspend fun clearWeatherCache() = withContext(Dispatchers.IO) {
        weatherCacheDao?.clearAllCache()
    }

    suspend fun loadCachedWeatherSnapshot(
        location: CityLocation,
        fallbackToLatestAnyLocation: Boolean = true
    ): CachedWeatherSnapshot? = withContext(Dispatchers.IO) {
        val dao = weatherCacheDao ?: return@withContext null
        val requestedKey = CurrentWeatherEntity.createKey(location.latitude, location.longitude)
        val currentEntity = (dao.getCurrentWeatherOnce(requestedKey)
            ?: if (fallbackToLatestAnyLocation) dao.getLatestCurrentWeatherOnce() else null)
            ?: return@withContext null

        val key = currentEntity.locationKey
        val currentWeather = currentEntity.toCurrentWeather()
        val dailyDays = dao.getDailyForecastOnce(key).map { it.toDailyForecastDay() }
        val hourlyPoints = dao.getHourlyForecastOnce(key).map { it.toHourlyPoint() }

        val diurnalPeriods = computeDiurnalPeriodsFromHourlyPoints(hourlyPoints, currentWeather)
        val alerts = evaluateSevereWeatherAlerts(currentWeather, dailyDays)
        val headlines = generateRealLocalHeadlines(currentWeather, diurnalPeriods, alerts)
        val astronomical = calculateAstronomicalData(currentWeather.sunriseTime, currentWeather.sunsetTime)

        val resolvedLocation = if (key == requestedKey) location else currentEntity.toCityLocation()

        CachedWeatherSnapshot(
            location = resolvedLocation,
            currentWeather = currentWeather,
            weatherPackage = FullWeatherPackage(
                diurnalPeriods = diurnalPeriods,
                hourlyPoints = hourlyPoints,
                dailyDays = dailyDays,
                alerts = alerts,
                headlines = headlines,
                astronomical = astronomical
            ),
            cachedAtEpochMs = currentEntity.cachedAtEpochMs
        )
    }

    suspend fun saveWeatherSnapshotToCache(
        location: CityLocation,
        currentWeather: CurrentWeather,
        packageData: FullWeatherPackage,
        cachedAtEpochMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val dao = weatherCacheDao ?: return@withContext
        val key = CurrentWeatherEntity.createKey(location.latitude, location.longitude)
        val currentEntity = CurrentWeatherEntity.from(location, currentWeather, cachedAtEpochMs)
        val dailyEntities = packageData.dailyDays.mapIndexed { idx, day ->
            DailyForecastEntity.from(key, idx, day, cachedAtEpochMs)
        }
        val hourlyEntities = packageData.hourlyPoints.mapIndexed { idx, pt ->
            HourlyForecastEntity.from(key, idx, pt, cachedAtEpochMs)
        }
        dao.saveFullWeatherSnapshot(
            current = currentEntity,
            daily = dailyEntities,
            hourly = hourlyEntities
        )
    }

    suspend fun fetchLiveRadarMetadata(): LiveRadarPackage? = withContext(Dispatchers.IO) {
        try {
            val response = rainViewerService.getWeatherMaps()
            val host = response.host.ifBlank { "https://tilecache.rainviewer.com" }.trimEnd('/')
            val pastFrames = response.radar?.past.orEmpty()
                .filter { it.path.isNotBlank() }
                .takeLast(6)
                .map { RadarTimelineFrame(timestampSec = it.time, tilePath = it.path, isNowcast = false) }
            val nowcastFrames = response.radar?.nowcast.orEmpty()
                .filter { it.path.isNotBlank() }
                .take(3)
                .map { RadarTimelineFrame(timestampSec = it.time, tilePath = it.path, isNowcast = true) }
            val satFrames = response.satellite?.infrared.orEmpty()
                .filter { it.path.isNotBlank() }
                .takeLast(6)
                .map { RadarTimelineFrame(timestampSec = it.time, tilePath = it.path, isNowcast = false, isSatellite = true) }

            val combinedPrecip = pastFrames + nowcastFrames
            if (combinedPrecip.isEmpty() && satFrames.isEmpty()) return@withContext null

            LiveRadarPackage(
                host = host,
                precipitationFrames = combinedPrecip,
                satelliteFrames = satFrames
            )
        } catch (_: Exception) {
            null
        }
    }

    // Default Fallback Location (resolved from string resources)
    val defaultLocation: CityLocation
        get() = CityLocation(
            name = appContext?.getString(R.string.default_city_name) ?: "",
            region = appContext?.getString(R.string.default_city_region) ?: "",
            country = appContext?.getString(R.string.default_city_country) ?: "",
            countryCode = appContext?.getString(R.string.default_city_country_code) ?: "",
            latitude = 40.7128,
            longitude = -74.0060,
            isGpsDetected = false
        )

    // Default bookmarked cities (empty initially to allow user customization)
    fun getDefaultFavoriteCities(): List<CityLocation> = emptyList()

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
        longitude: Double,
        location: CityLocation? = null
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
        val conditionText = appContext?.getString(getWeatherDescriptionRes(weatherCode))
            ?: getWeatherDescription(weatherCode)
        val aqiLevel = aqiData.usAqi
        val aqiStatus = appContext?.getString(getAqiStatusRes(aqiLevel))
            ?: getAqiStatus(aqiLevel)

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

        val diurnalPeriods = computeDiurnalPeriods(hourlyData, dailyData, currentData)
        val hourlyPoints = computeHourlyPoints(hourlyData, currentData)
        val dailyDays = computeDailyDays(dailyData, currentData)
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

        val cacheLocation = location ?: CityLocation(
            name = defaultLocation.name,
            region = defaultLocation.region,
            country = defaultLocation.country,
            countryCode = defaultLocation.countryCode,
            latitude = latitude,
            longitude = longitude
        )
        runCatching {
            saveWeatherSnapshotToCache(cacheLocation, currentWeather, packageData)
        }

        Pair(currentWeather, packageData)
    }

    suspend fun fetchSevenDayForecast(
        latitude: Double,
        longitude: Double
    ): List<DailyForecastDay> = fetchSevenDayForecastWithCache(latitude, longitude).first

    suspend fun fetchSevenDayForecastWithCache(
        latitude: Double,
        longitude: Double
    ): Pair<List<DailyForecastDay>, Boolean> = withContext(Dispatchers.IO) {
        val locationKey = CurrentWeatherEntity.createKey(latitude, longitude)
        try {
            val response = weatherService.getSevenDayForecast(latitude = latitude, longitude = longitude)
            val dailyData = response.daily ?: DailyData()
            val days = computeDailyDays(dailyData, CurrentUnitsAndData(), maxDays = 7)
            if (days.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                runCatching {
                    weatherCacheDao?.replaceDailyForecast(
                        locationKey = locationKey,
                        daily = days.mapIndexed { idx, d ->
                            DailyForecastEntity.from(locationKey, idx, d, nowMs)
                        }
                    )
                }
            }
            Pair(days, false)
        } catch (e: Exception) {
            val cached = weatherCacheDao?.getDailyForecastOnce(locationKey)
                ?.take(7)
                ?.map { it.toDailyForecastDay() }
                .orEmpty()
            if (cached.isNotEmpty()) {
                Pair(cached, true)
            } else {
                throw e
            }
        }
    }

    private fun computeDiurnalPeriodsFromHourlyPoints(
        hourlyPoints: List<HourlyPoint>,
        currentWeather: CurrentWeather
    ): List<DiurnalPeriod> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val periodConfigs = listOf(
            Triple("Morning", "06:00 - 12:00", 6..11),
            Triple("Afternoon", "12:00 - 18:00", 12..17),
            Triple("Evening", "18:00 - 22:00", 18..21),
            Triple("Night", "22:00 - 06:00", 22..24)
        )
        return periodConfigs.map { cfg ->
            val matched = hourlyPoints.filter { pt ->
                val h = pt.timeLabel.substringBefore(":").toIntOrNull() ?: -1
                h in cfg.third
            }
            val avgTemp = if (matched.isNotEmpty()) matched.map { it.tempC }.average() else currentWeather.tempC
            val maxPop = matched.maxOfOrNull { it.popPercent } ?: 0
            val code = matched.firstOrNull()?.weatherCode ?: currentWeather.weatherCode
            val wind = if (matched.isNotEmpty()) matched.map { it.windSpeedKmh }.average() else currentWeather.windSpeedKmh
            val (isCurrent, periodRes) = when (cfg.first) {
                "Morning" -> Pair(currentHour in 6..11, R.string.period_morning)
                "Afternoon" -> Pair(currentHour in 12..17, R.string.period_afternoon)
                "Evening" -> Pair(currentHour in 18..21, R.string.period_evening)
                "Night" -> Pair(currentHour >= 22 || currentHour < 6, R.string.period_night)
                else -> Pair(false, R.string.period_morning)
            }
            DiurnalPeriod(
                periodName = appContext?.getString(periodRes) ?: cfg.first,
                timeRange = cfg.second,
                tempC = avgTemp,
                feelsLikeC = avgTemp - (currentWeather.tempC - currentWeather.feelsLikeC),
                conditionText = appContext?.getString(getWeatherDescriptionRes(code)) ?: getWeatherDescription(code),
                weatherCode = code,
                popPercent = maxPop,
                windSpeedKmh = wind,
                isCurrent = isCurrent,
                periodNameRes = periodRes
            )
        }
    }

    private fun computeDiurnalPeriods(
        hourly: HourlyData,
        daily: DailyData,
        currentData: CurrentUnitsAndData
    ): List<DiurnalPeriod> {
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

            val avgTemp = if (matchedTemps.isNotEmpty()) matchedTemps.average() else currentData.temperature2m
            val maxPop = if (matchedPops.isNotEmpty()) matchedPops.maxOrNull() ?: 0 else 0
            val code = matchedCodes.firstOrNull() ?: currentData.weatherCode
            val wind = if (matchedWinds.isNotEmpty()) matchedWinds.average() else currentData.windSpeed10m

            val (isCurrent, periodRes) = when (cfg.first) {
                "Morning" -> Pair(currentHour in 6..11, R.string.period_morning)
                "Afternoon" -> Pair(currentHour in 12..17, R.string.period_afternoon)
                "Evening" -> Pair(currentHour in 18..21, R.string.period_evening)
                "Night" -> Pair(currentHour >= 22 || currentHour < 6, R.string.period_night)
                else -> Pair(false, R.string.period_morning)
            }

            periods.add(
                DiurnalPeriod(
                    periodName = cfg.first,
                    timeRange = cfg.second,
                    tempC = avgTemp,
                    feelsLikeC = avgTemp - (currentData.temperature2m - currentData.apparentTemperature),
                    conditionText = getWeatherDescription(code),
                    weatherCode = code,
                    popPercent = maxPop,
                    windSpeedKmh = wind,
                    isCurrent = isCurrent,
                    periodNameRes = periodRes
                )
            )
        }

        return periods
    }

    private fun computeHourlyPoints(
        hourly: HourlyData,
        currentData: CurrentUnitsAndData
    ): List<HourlyPoint> {
        val points = mutableListOf<HourlyPoint>()
        val count = minOf(hourly.time.size, 24)
        for (i in 0 until count) {
            val iso = hourly.time.getOrNull(i) ?: ""
            val timeLabel = if (iso.contains("T")) iso.substringAfter("T").take(5) else "12:00"
            val temp = hourly.temperature2m.getOrNull(i) ?: currentData.temperature2m
            val feels = hourly.apparentTemperature.getOrNull(i) ?: currentData.apparentTemperature
            val code = hourly.weatherCode.getOrNull(i) ?: currentData.weatherCode
            val pop = hourly.precipitationProbability.getOrNull(i) ?: 0
            val wind = hourly.windSpeed10m.getOrNull(i) ?: currentData.windSpeed10m
            val humidity = hourly.relativeHumidity2m.getOrNull(i)?.roundToInt() ?: currentData.relativeHumidity2m.roundToInt()

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

    private fun computeDailyDays(
        daily: DailyData,
        currentData: CurrentUnitsAndData,
        maxDays: Int = 14
    ): List<DailyForecastDay> {
        val days = mutableListOf<DailyForecastDay>()
        val count = minOf(daily.time.size, maxDays)
        val activeLocale = Locale.getDefault()
        val sdfParse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfDayOfWeek = SimpleDateFormat("EEEE", activeLocale)
        val sdfShort = SimpleDateFormat("EEE, MMM d", activeLocale)
        val todayStr = appContext?.getString(R.string.seven_day_today) ?: "Today"
        val tomorrowStr = appContext?.getString(R.string.seven_day_tomorrow) ?: "Tomorrow"

        for (i in 0 until count) {
            val dateStr = daily.time.getOrNull(i) ?: ""
            var dayOfWeek = dateStr
            var label = dateStr

            try {
                val parsed = sdfParse.parse(dateStr)
                if (parsed != null) {
                    dayOfWeek = when (i) {
                        0 -> todayStr
                        1 -> tomorrowStr
                        else -> sdfDayOfWeek.format(parsed)
                    }
                    label = sdfShort.format(parsed)
                }
            } catch (_: Exception) {}

            val maxT = daily.temperature2mMax.getOrNull(i) ?: currentData.temperature2m
            val minT = daily.temperature2mMin.getOrNull(i) ?: (currentData.temperature2m - 5.0)
            val appMax = daily.apparentTemperatureMax.getOrNull(i) ?: maxT
            val appMin = daily.apparentTemperatureMin.getOrNull(i) ?: minT
            val code = daily.weatherCode.getOrNull(i) ?: currentData.weatherCode
            val rain = daily.precipitationSum.getOrNull(i) ?: 0.0
            val pop = daily.precipitationProbabilityMax.getOrNull(i) ?: 0
            val uv = daily.uvIndexMax.getOrNull(i) ?: currentData.uvIndex
            val wind = daily.windSpeed10mMax.getOrNull(i) ?: currentData.windSpeed10m
            val gusts = daily.windGusts10mMax.getOrNull(i) ?: wind
            val sunrise = daily.sunrise.getOrNull(i)?.substringAfter("T")?.take(5) ?: ""
            val sunset = daily.sunset.getOrNull(i)?.substringAfter("T")?.take(5) ?: ""
            val localizedCondition = appContext?.getString(getWeatherDescriptionRes(code)) ?: getWeatherDescription(code)

            days.add(
                DailyForecastDay(
                    dateLabel = label,
                    dayOfWeek = dayOfWeek,
                    maxTempC = maxT,
                    minTempC = minT,
                    weatherCode = code,
                    conditionText = localizedCondition,
                    rainMm = rain,
                    popPercent = pop,
                    maxUvIndex = uv,
                    maxWindKmh = wind,
                    apparentMaxTempC = appMax,
                    apparentMinTempC = appMin,
                    sunriseTime = sunrise,
                    sunsetTime = sunset,
                    maxWindGustsKmh = gusts
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
            val windKmh = current.windSpeedKmh.roundToInt()
            val gustKmh = current.windGustsKmh.roundToInt()
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_wind",
                    severity = AlertSeverity.WARNING,
                    title = appContext?.getString(R.string.alert_wind_title) ?: "HIGH WIND WARNING",
                    summary = appContext?.getString(R.string.alert_wind_summary, windKmh, gustKmh)
                        ?: "Sustained winds at $windKmh km/h with dangerous peak gusts up to $gustKmh km/h.",
                    safetyInstructions = appContext?.getString(R.string.alert_wind_instructions)
                        ?: "Secure loose outdoor furniture, avoid travel in high-profile vehicles, and watch for downed tree limbs and power lines.",
                    effectiveTime = appContext?.getString(R.string.alert_effective_now) ?: "Active now",
                    expiresTime = appContext?.getString(R.string.alert_wind_expires) ?: "Expires in 6 hours"
                )
            )
        }

        // 2. Extreme Heat Advisory
        if (current.tempC >= 35.0 || current.feelsLikeC >= 38.0) {
            val feelsC = current.feelsLikeC.roundToInt()
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_heat",
                    severity = AlertSeverity.ADVISORY,
                    title = appContext?.getString(R.string.alert_heat_title) ?: "EXCESSIVE HEAT ADVISORY",
                    summary = appContext?.getString(R.string.alert_heat_summary, feelsC)
                        ?: "Dangerous heat index reaching ${feelsC}°C under strong insolation.",
                    safetyInstructions = appContext?.getString(R.string.alert_heat_instructions)
                        ?: "Drink plenty of fluids, remain in air-conditioned rooms, avoid prolonged sun exposure, and check up on vulnerable relatives.",
                    effectiveTime = appContext?.getString(R.string.alert_heat_effective) ?: "Until 20:00 local time",
                    expiresTime = appContext?.getString(R.string.alert_heat_expires) ?: "Tonight"
                )
            )
        }

        // 3. Hard Freeze Warning
        if (current.tempC <= -5.0) {
            val tempC = current.tempC.roundToInt()
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_freeze",
                    severity = AlertSeverity.WARNING,
                    title = appContext?.getString(R.string.alert_freeze_title) ?: "HARD FREEZE WARNING",
                    summary = appContext?.getString(R.string.alert_freeze_summary, tempC)
                        ?: "Sub-freezing temperatures at ${tempC}°C posing frostbite risk and pipe hazards.",
                    safetyInstructions = appContext?.getString(R.string.alert_freeze_instructions)
                        ?: "Protect exposed plumbing, bring sensitive plants indoors, and ensure pets have adequate warm shelter.",
                    effectiveTime = appContext?.getString(R.string.alert_effective_now) ?: "Active now",
                    expiresTime = appContext?.getString(R.string.alert_freeze_expires) ?: "Tomorrow 09:00"
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
                    title = appContext?.getString(R.string.alert_storm_title) ?: "SEVERE THUNDERSTORM / FLOOD ADVISORY",
                    summary = appContext?.getString(R.string.alert_storm_summary)
                        ?: "Active convective activity bringing rapid rainfall accumulation and cloud-to-ground lightning.",
                    safetyInstructions = appContext?.getString(R.string.alert_storm_instructions)
                        ?: "Seek sturdy indoor shelter immediately. Avoid low-lying flooded roadways: Turn around, don't drown.",
                    effectiveTime = appContext?.getString(R.string.alert_storm_effective) ?: "Immediate broadcast",
                    expiresTime = appContext?.getString(R.string.alert_storm_expires) ?: "Until convective cell passes"
                )
            )
        }

        // 5. Air Quality Alert
        if (current.aqi >= 151) {
            alerts.add(
                SevereWeatherAlert(
                    id = "alert_aqi",
                    severity = AlertSeverity.WATCH,
                    title = appContext?.getString(R.string.alert_aqi_title, current.aqi) ?: "AIR QUALITY ALERT (${current.aqi} AQI)",
                    summary = appContext?.getString(R.string.alert_aqi_summary)
                        ?: "Particulate pollution levels exceed safe thresholds across the metro area.",
                    safetyInstructions = appContext?.getString(R.string.alert_aqi_instructions)
                        ?: "People with respiratory or heart conditions, older adults, and children should avoid prolonged outdoor exertion.",
                    effectiveTime = appContext?.getString(R.string.alert_aqi_effective) ?: "Today",
                    expiresTime = appContext?.getString(R.string.alert_aqi_expires) ?: "Until air dispersion improves"
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
                val emergencyLine = appContext?.getString(R.string.headline_emergency_fmt, it.title, it.summary)
                    ?: "EMERGENCY BROADCAST: ${it.title} - ${it.summary}"
                list.add(emergencyLine)
            }
        }

        val tempC = current.tempC.roundToInt()
        val tempF = (current.tempC * 9 / 5 + 32).roundToInt()
        val feelsC = current.feelsLikeC.roundToInt()
        val feelsF = (current.feelsLikeC * 9 / 5 + 32).roundToInt()
        val windKmh = current.windSpeedKmh.roundToInt()
        val windMph = (current.windSpeedKmh * 0.621371).roundToInt()
        val gustKmh = current.windGustsKmh.roundToInt()
        val gustMph = (current.windGustsKmh * 0.621371).roundToInt()

        val conditionLocalized = (appContext?.getString(getWeatherDescriptionRes(current.weatherCode)) ?: current.conditionText)
            .uppercase(Locale.getDefault())
        val aqiStatusLocalized = appContext?.getString(getAqiStatusRes(current.aqi)) ?: current.aqiStatus
        val pm25Formatted = String.format(Locale.getDefault(), "%.1f", current.pm25)
        val compass = getWindCompass(current.windDirectionDeg)

        list.add(
            appContext?.getString(
                R.string.headline_current_obs_fmt,
                conditionLocalized,
                tempC,
                tempF,
                feelsC,
                feelsF,
                current.humidityPercent
            ) ?: "CURRENT OBSERVATION: $conditionLocalized, $tempC°C / $tempF°F (Feels like $feelsC°C / $feelsF°F) with ${current.humidityPercent}% humidity."
        )
        list.add(
            appContext?.getString(
                R.string.headline_wind_fmt,
                compass,
                windKmh,
                windMph,
                gustKmh,
                gustMph
            ) ?: "WIND TELEMETRY: Winds traveling from $compass at $windKmh km/h ($windMph mph), peak gusts at $gustKmh km/h ($gustMph mph)."
        )
        list.add(
            appContext?.getString(
                R.string.headline_aqi_fmt,
                current.aqi,
                aqiStatusLocalized,
                pm25Formatted
            ) ?: "AIR QUALITY INDEX: US AQI reads ${current.aqi} ($aqiStatusLocalized) with PM2.5 at $pm25Formatted µg/m³."
        )
        list.add(
            appContext?.getString(
                R.string.headline_solar_fmt,
                current.sunriseTime,
                current.sunsetTime
            ) ?: "SOLAR TIMING: First light sunrise logged at ${current.sunriseTime} | Sunset dusk horizon scheduled at ${current.sunsetTime}."
        )

        val nextPeriod = periods.firstOrNull { !it.isCurrent } ?: periods.firstOrNull()
        if (nextPeriod != null) {
            val pC = nextPeriod.tempC.roundToInt()
            val pF = (nextPeriod.tempC * 9 / 5 + 32).roundToInt()
            val periodNameLocalized = (appContext?.getString(nextPeriod.periodNameRes) ?: nextPeriod.periodName)
                .uppercase(Locale.getDefault())
            val periodCondLocalized = appContext?.getString(getWeatherDescriptionRes(nextPeriod.weatherCode))
                ?: nextPeriod.conditionText
            list.add(
                appContext?.getString(
                    R.string.headline_outlook_fmt,
                    periodNameLocalized,
                    periodCondLocalized,
                    pC,
                    pF,
                    nextPeriod.popPercent
                ) ?: "OUTLOOK FOR $periodNameLocalized: Expecting $periodCondLocalized near $pC°C / $pF°F with ${nextPeriod.popPercent}% probability of precipitation."
            )
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

        val (phaseName, phaseRes) = when {
            phaseDays < 1.8 -> Pair("New Moon", R.string.moon_phase_new)
            phaseDays < 7.4 -> Pair("Waxing Crescent", R.string.moon_phase_waxing_crescent)
            phaseDays < 9.2 -> Pair("First Quarter", R.string.moon_phase_first_quarter)
            phaseDays < 13.8 -> Pair("Waxing Gibbous", R.string.moon_phase_waxing_gibbous)
            phaseDays < 15.7 -> Pair("Full Moon", R.string.moon_phase_full)
            phaseDays < 22.1 -> Pair("Waning Gibbous", R.string.moon_phase_waning_gibbous)
            phaseDays < 23.9 -> Pair("Last Quarter", R.string.moon_phase_last_quarter)
            else -> Pair("Waning Crescent", R.string.moon_phase_waning_crescent)
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
            moonPhaseName = appContext?.getString(phaseRes) ?: phaseName,
            moonIlluminationPercent = illumination,
            daysUntilFullMoon = daysUntilFull,
            sunriseTime = sunrise,
            sunsetTime = sunset,
            daylightProgressPercent = progress,
            moonPhaseResId = phaseRes
        )
    }

    /**
     * Synthesizes live, location-specific meteorological VOD dispatches directly from real
     * Open-Meteo telemetry (`current` and `pkg`), with zero static sample data.
     */
    fun buildLiveVodDispatches(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem
    ): List<VodStory> {
        val isMetric = unitSystem == UnitSystem.METRIC
        val kmhUnit = appContext?.getString(R.string.unit_kmh) ?: "km/h"
        val mphUnit = appContext?.getString(R.string.unit_mph) ?: "mph"
        val hpaUnit = appContext?.getString(R.string.unit_hpa) ?: "hPa"
        val inhgUnit = appContext?.getString(R.string.unit_inhg) ?: "inHg"
        val kmUnit = appContext?.getString(R.string.unit_km) ?: "km"
        val miUnit = appContext?.getString(R.string.unit_mi) ?: "mi"
        val mmUnit = appContext?.getString(R.string.unit_mm) ?: "mm"
        val inUnit = appContext?.getString(R.string.unit_in) ?: "in"

        fun fmtTemp(c: Double): String =
            if (isMetric) "${c.roundToInt()}°C" else "${(c * 9.0 / 5.0 + 32.0).roundToInt()}°F"
        fun fmtWind(kmh: Double): String =
            if (isMetric) "${kmh.roundToInt()} $kmhUnit" else "${(kmh * 0.621371).roundToInt()} $mphUnit"
        fun fmtPress(hpa: Double): String =
            if (isMetric) "${hpa.roundToInt()} $hpaUnit" else String.format(Locale.getDefault(), "%.2f %s", hpa * 0.02953, inhgUnit)
        fun fmtVis(km: Double): String =
            if (isMetric) "${km.roundToInt()} $kmUnit" else "${(km * 0.621371).roundToInt()} $miUnit"
        fun fmtPrecip(mm: Double): String =
            if (isMetric) String.format(Locale.getDefault(), "%.1f %s", mm, mmUnit)
            else String.format(Locale.getDefault(), "%.2f %s", mm * 0.0393701, inUnit)

        val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val timestampLabel = appContext?.getString(R.string.vod_live_timestamp_fmt, nowTimeStr)
            ?: "Live Telemetry • $nowTimeStr"

        val conditionLocalized = appContext?.getString(getWeatherDescriptionRes(current.weatherCode))
            ?: current.conditionText
        val aqiStatusLocalized = appContext?.getString(getAqiStatusRes(current.aqi))
            ?: current.aqiStatus
        val moonLocalized = appContext?.getString(pkg.astronomical.moonPhaseResId)
            ?: pkg.astronomical.moonPhaseName

        val sevenDays = pkg.dailyDays.take(7)
        val minWeekC = sevenDays.minOfOrNull { it.minTempC } ?: current.tempC
        val maxWeekC = sevenDays.maxOfOrNull { it.maxTempC } ?: current.tempC
        val totalWeekRainMm = sevenDays.sumOf { it.rainMm }

        val catSevere = appContext?.getString(R.string.vod_cat_severe) ?: "Severe Weather"
        val catForecasts = appContext?.getString(R.string.vod_cat_forecasts) ?: "Forecasts"
        val catEarth = appContext?.getString(R.string.vod_cat_earth_sci) ?: "Earth Science"
        val catClimate = appContext?.getString(R.string.vod_cat_climate) ?: "Climate & Nature"

        return listOf(
            VodStory(
                id = "vod_severe_${location.name.lowercase(Locale.ROOT)}",
                category = catSevere,
                title = appContext?.getString(R.string.vod_live_severe_title, location.name)
                    ?: "${location.name} Live Doppler & Convective Risk Analysis",
                presenter = appContext?.getString(R.string.vod_live_presenter_ops)
                    ?: "Horizon Doppler Operations Desk",
                duration = "06:40",
                timestamp = timestampLabel,
                description = appContext?.getString(
                    R.string.vod_live_severe_desc,
                    location.name,
                    conditionLocalized,
                    fmtWind(current.windSpeedKmh),
                    fmtWind(current.windGustsKmh),
                    fmtPress(current.pressureHpa)
                ) ?: "Real-time reflectivity sweep over ${location.name} tracking $conditionLocalized with surface winds at ${fmtWind(current.windSpeedKmh)} (peak gusts ${fmtWind(current.windGustsKmh)}) and barometric pressure at ${fmtPress(current.pressureHpa)}.",
                videoUrl = "",
                accentColorHex = 0xFFDC2626
            ),
            VodStory(
                id = "vod_forecasts_${location.name.lowercase(Locale.ROOT)}",
                category = catForecasts,
                title = appContext?.getString(R.string.vod_live_forecast_title, location.name)
                    ?: "${location.name} 7-Day Synoptic Temperature & Precipitation Outlook",
                presenter = appContext?.getString(R.string.vod_live_presenter_synoptic)
                    ?: "Synoptic Forecasting Division",
                duration = "05:15",
                timestamp = timestampLabel,
                description = appContext?.getString(
                    R.string.vod_live_forecast_desc,
                    location.name,
                    fmtTemp(minWeekC),
                    fmtTemp(maxWeekC),
                    fmtPrecip(totalWeekRainMm)
                ) ?: "Multi-day numerical forecast for ${location.name} projecting temperatures between ${fmtTemp(minWeekC)} and ${fmtTemp(maxWeekC)} with ${fmtPrecip(totalWeekRainMm)} total 7-day precipitation accumulation.",
                videoUrl = "",
                accentColorHex = 0xFF10B981
            ),
            VodStory(
                id = "vod_earth_${location.name.lowercase(Locale.ROOT)}",
                category = catEarth,
                title = appContext?.getString(R.string.vod_live_earth_title, location.name)
                    ?: "${location.name} Boundary Layer Wind Shear & Barometric Profile",
                presenter = appContext?.getString(R.string.vod_live_presenter_dynamics)
                    ?: "Atmospheric Dynamics Lab",
                duration = "07:30",
                timestamp = timestampLabel,
                description = appContext?.getString(
                    R.string.vod_live_earth_desc,
                    location.name,
                    current.humidityPercent,
                    fmtTemp(current.dewPointC),
                    fmtVis(current.visibilityKm),
                    current.cloudCoverPercent
                ) ?: "Micro-telemetry breakdown of ${location.name} atmospheric column: ${current.humidityPercent}% relative humidity, dew point ${fmtTemp(current.dewPointC)}, visibility ${fmtVis(current.visibilityKm)}, and ${current.cloudCoverPercent}% cloud cover.",
                videoUrl = "",
                accentColorHex = 0xFF0284C7
            ),
            VodStory(
                id = "vod_climate_${location.name.lowercase(Locale.ROOT)}",
                category = catClimate,
                title = appContext?.getString(R.string.vod_live_climate_title, location.name)
                    ?: "${location.name} Solar Radiation, Lunar Phase & Air Quality Report",
                presenter = appContext?.getString(R.string.vod_live_presenter_climate)
                    ?: "Environmental & Air Quality Desk",
                duration = "04:50",
                timestamp = timestampLabel,
                description = appContext?.getString(
                    R.string.vod_live_climate_desc,
                    location.name,
                    current.aqi,
                    aqiStatusLocalized,
                    String.format(Locale.getDefault(), "%.1f", current.pm25),
                    current.uvIndex.roundToInt(),
                    current.sunriseTime,
                    current.sunsetTime,
                    moonLocalized
                ) ?: "Environmental dispatch for ${location.name}: US AQI at ${current.aqi} ($aqiStatusLocalized, PM2.5 ${String.format(Locale.getDefault(), "%.1f", current.pm25)} µg/m³), UV index ${current.uvIndex.roundToInt()}/11, sunrise ${current.sunriseTime}, sunset ${current.sunsetTime} ($moonLocalized).",
                videoUrl = "",
                accentColorHex = 0xFF8B5CF6
            )
        )
    }

    fun buildLiveBroadcastSchedule(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        vodStories: List<VodStory>,
        unitSystem: UnitSystem,
        nowMs: Long = System.currentTimeMillis()
    ): List<BroadcastScheduleSlot> = BroadcastScheduleGenerator.buildLiveBroadcastSchedule(
        context = appContext,
        location = location,
        current = current,
        pkg = pkg,
        vodStories = vodStories,
        unitSystem = unitSystem,
        nowMs = nowMs
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

        @StringRes
        fun getWeatherDescriptionRes(code: Int): Int = when (code) {
            0 -> R.string.wmo_clear_sky
            1 -> R.string.wmo_mainly_clear
            2 -> R.string.wmo_partly_cloudy
            3 -> R.string.wmo_overcast
            45 -> R.string.wmo_fog
            48 -> R.string.wmo_depositing_rime_fog
            51 -> R.string.wmo_light_drizzle
            53 -> R.string.wmo_moderate_drizzle
            55 -> R.string.wmo_dense_drizzle
            61 -> R.string.wmo_slight_rain
            63 -> R.string.wmo_moderate_rain
            65 -> R.string.wmo_heavy_rain
            71 -> R.string.wmo_slight_snow
            73 -> R.string.wmo_moderate_snow
            75 -> R.string.wmo_heavy_snow
            77 -> R.string.wmo_snow_grains
            80 -> R.string.wmo_slight_rain_showers
            81 -> R.string.wmo_moderate_rain_showers
            82 -> R.string.wmo_violent_rain_showers
            85 -> R.string.wmo_slight_snow_showers
            86 -> R.string.wmo_heavy_snow_showers
            95 -> R.string.wmo_thunderstorm
            96 -> R.string.wmo_thunderstorm_hail
            99 -> R.string.wmo_severe_hail_thunderstorm
            else -> R.string.wmo_atmospheric_conditions
        }

        @StringRes
        fun getAqiStatusRes(aqi: Int): Int = when {
            aqi <= 50 -> R.string.aqi_good
            aqi <= 100 -> R.string.aqi_moderate
            aqi <= 150 -> R.string.aqi_unhealthy_sensitive
            aqi <= 200 -> R.string.aqi_unhealthy
            aqi <= 300 -> R.string.aqi_very_unhealthy
            else -> R.string.aqi_hazardous
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
