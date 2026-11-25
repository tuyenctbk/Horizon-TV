package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HorizonWeatherDatabase
import com.example.data.model.AstronomicalData
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecastDay
import com.example.data.model.DiurnalPeriod
import com.example.data.model.HourlyPoint
import com.example.data.model.UnitSystem
import com.example.data.repository.BroadcastScheduleGenerator
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomCacheAndBroadcastScheduleTest {

    private lateinit var context: Context
    private lateinit var database: HorizonWeatherDatabase
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = HorizonWeatherDatabase.createInMemory(context)
        repository = WeatherRepository(
            appContext = context,
            injectedCacheDao = database.weatherCacheDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun buildTestWeatherPackage(city: CityLocation): Pair<CurrentWeather, FullWeatherPackage> {
        val current = CurrentWeather(
            tempC = 21.5,
            feelsLikeC = 20.8,
            weatherCode = 2,
            conditionText = context.getString(WeatherRepository.getWeatherDescriptionRes(2)),
            isDay = true,
            humidityPercent = 58,
            pressureHpa = 1015.4,
            windSpeedKmh = 14.2,
            windDirectionDeg = 215,
            windGustsKmh = 24.0,
            uvIndex = 5.4,
            cloudCoverPercent = 40,
            dewPointC = 12.6,
            visibilityKm = 16.0,
            aqi = 32,
            aqiStatus = context.getString(R.string.aqi_good),
            pm25 = 7.4,
            pm10 = 14.2,
            sunriseTime = "06:24",
            sunsetTime = "19:42"
        )

        val dailyDays = (0 until 7).map { dayIdx ->
            val code = if (dayIdx % 2 == 0) 1 else 61
            DailyForecastDay(
                dateLabel = "Day ${dayIdx + 1}",
                dayOfWeek = if (dayIdx == 0) "Today" else "D+$dayIdx",
                maxTempC = 22.0 + dayIdx,
                minTempC = 12.0 + dayIdx * 0.5,
                weatherCode = code,
                conditionText = context.getString(WeatherRepository.getWeatherDescriptionRes(code)),
                rainMm = if (dayIdx % 2 == 0) 0.0 else 4.2,
                popPercent = if (dayIdx % 2 == 0) 10 else 65,
                maxUvIndex = 6.0,
                maxWindKmh = 18.0 + dayIdx,
                apparentMaxTempC = 21.5 + dayIdx,
                apparentMinTempC = 11.5 + dayIdx * 0.5,
                sunriseTime = "06:24",
                sunsetTime = "19:42",
                maxWindGustsKmh = 28.0 + dayIdx
            )
        }

        val hourlyPoints = (0 until 12).map { hrIdx ->
            val code = if (hrIdx < 6) 2 else 61
            HourlyPoint(
                timeLabel = String.format("%02d:00", (9 + hrIdx) % 24),
                rawIsoTime = String.format("2026-11-25T%02d:00", (9 + hrIdx) % 24),
                tempC = 19.0 + hrIdx * 0.4,
                feelsLikeC = 18.5 + hrIdx * 0.4,
                weatherCode = code,
                conditionText = context.getString(WeatherRepository.getWeatherDescriptionRes(code)),
                popPercent = if (hrIdx < 6) 15 else 55,
                windSpeedKmh = 12.0 + hrIdx * 0.5,
                humidityPercent = 55 + hrIdx
            )
        }

        val diurnal = listOf(
            DiurnalPeriod(
                periodName = context.getString(R.string.period_morning),
                timeRange = "06:00 - 12:00",
                tempC = 19.5,
                feelsLikeC = 19.0,
                conditionText = current.conditionText,
                weatherCode = 2,
                popPercent = 15,
                windSpeedKmh = 12.0,
                isCurrent = true,
                periodNameRes = R.string.period_morning
            ),
            DiurnalPeriod(
                periodName = context.getString(R.string.period_afternoon),
                timeRange = "12:00 - 18:00",
                tempC = 23.0,
                feelsLikeC = 22.5,
                conditionText = current.conditionText,
                weatherCode = 61,
                popPercent = 55,
                windSpeedKmh = 16.0,
                isCurrent = false,
                periodNameRes = R.string.period_afternoon
            )
        )

        val pkg = FullWeatherPackage(
            diurnalPeriods = diurnal,
            hourlyPoints = hourlyPoints,
            dailyDays = dailyDays,
            alerts = emptyList(),
            headlines = listOf("${city.name} live telemetry active"),
            astronomical = AstronomicalData(
                moonPhaseName = context.getString(R.string.moon_phase_waxing_gibbous),
                moonIlluminationPercent = 72,
                daysUntilFullMoon = 4,
                sunriseTime = "06:24",
                sunsetTime = "19:42",
                daylightProgressPercent = 0.5f,
                moonPhaseResId = R.string.moon_phase_waxing_gibbous
            )
        )

        return current to pkg
    }

    @Test
    fun `room cache persists and restores current weather and 7-day forecast snapshot`() = runBlocking {
        val location = CityLocation(
            name = "Zurich",
            region = "Zurich",
            country = "Switzerland",
            countryCode = "CH",
            latitude = 47.3769,
            longitude = 8.5417
        )
        val (current, pkg) = buildTestWeatherPackage(location)
        val syncTimestamp = 1_764_000_000_000L

        repository.saveWeatherSnapshotToCache(
            location = location,
            currentWeather = current,
            packageData = pkg,
            cachedAtEpochMs = syncTimestamp
        )

        // Verify reactive Flows reflect the stored snapshot
        assertEquals(1, repository.observeCachedStationCount().first())
        val observedCurrent = repository.observeCachedCurrentWeather(location.latitude, location.longitude).first()
        assertNotNull(observedCurrent)
        assertEquals(21.5, observedCurrent!!.tempC, 0.01)
        assertEquals(7, repository.observeCachedSevenDayForecast(location.latitude, location.longitude).first().size)

        // Verify full offline outage snapshot restoration
        val snapshot = repository.loadCachedWeatherSnapshot(location)
        assertNotNull(snapshot)
        assertEquals(syncTimestamp, snapshot!!.cachedAtEpochMs)
        assertEquals("Zurich", snapshot.location.name)
        assertEquals(21.5, snapshot.currentWeather.tempC, 0.01)
        assertEquals(7, snapshot.weatherPackage.dailyDays.size)
        assertEquals(12, snapshot.weatherPackage.hourlyPoints.size)

        // Verify fallback to latest cached station if another uncached city is requested during outage
        val uncachedLocation = CityLocation(
            name = "Reykjavik",
            latitude = 64.1466,
            longitude = -21.9426
        )
        val fallbackSnapshot = repository.loadCachedWeatherSnapshot(
            location = uncachedLocation,
            fallbackToLatestAnyLocation = true
        )
        assertNotNull(fallbackSnapshot)
        assertEquals("Zurich", fallbackSnapshot!!.location.name)

        // Verify cache clear removes all locally archived records
        repository.clearWeatherCache()
        assertEquals(0, repository.observeCachedStationCount().first())
        assertNull(repository.loadCachedWeatherSnapshot(location))
    }

    @Test
    fun `broadcast schedule generator builds 4-channel live EPG from telemetry`() {
        val location = CityLocation(
            name = "Tokyo",
            region = "Tokyo",
            country = "Japan",
            countryCode = "JP",
            latitude = 35.6762,
            longitude = 139.6503
        )
        val (current, pkg) = buildTestWeatherPackage(location)
        val vodStories = repository.buildLiveVodDispatches(location, current, pkg, UnitSystem.METRIC)
        val fixedNowMs = 1_764_000_900_000L

        val slots = BroadcastScheduleGenerator.buildLiveBroadcastSchedule(
            context = context,
            location = location,
            current = current,
            pkg = pkg,
            vodStories = vodStories,
            unitSystem = UnitSystem.METRIC,
            nowMs = fixedNowMs
        )

        // Expect all 4 broadcast channels (CH 01 - CH 04) populated with live + upcoming programs
        val channelIndices = slots.map { it.channelIndex }.toSet()
        assertEquals(setOf(0, 1, 2, 3), channelIndices)
        assertTrue(slots.size >= 12)

        // Every slot must have valid chronological bounds, localized non-empty text, and a linked VOD story
        slots.forEach { slot ->
            assertTrue("Slot end time must exceed start time", slot.endEpochMs > slot.startEpochMs)
            assertTrue("Program title must not be blank", slot.programTitle.isNotBlank())
            assertTrue("Program synopsis must not be blank", slot.programSynopsis.isNotBlank())
            assertTrue("Presenter must not be blank", slot.presenter.isNotBlank())
            assertEquals(slot.id, slot.linkedVodStory.id)
        }

        // At least one slot per channel must be ON AIR at fixedNowMs
        for (ch in 0..3) {
            val onAirOnChannel = slots.any {
                it.channelIndex == ch && fixedNowMs in it.startEpochMs until it.endEpochMs
            }
            assertTrue("Channel $ch should have an active ON AIR slot", onAirOnChannel)
        }
    }
}
