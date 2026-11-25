package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BroadcastScheduleSlot
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.data.model.VodStory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Generates a real-time 24/7 Electronic Program Guide (EPG) schedule of live and upcoming
 * meteorological VOD dispatches directly from live Open-Meteo telemetry (current weather,
 * hourly points, diurnal periods, 7-day forecast, and astronomical metrics).
 * Zero static sample data is used.
 */
object BroadcastScheduleGenerator {

    fun buildLiveBroadcastSchedule(
        context: Context?,
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        vodStories: List<VodStory>,
        unitSystem: UnitSystem,
        nowMs: Long = System.currentTimeMillis()
    ): List<BroadcastScheduleSlot> {
        val isMetric = unitSystem == UnitSystem.METRIC
        val kmhUnit = context?.getString(R.string.unit_kmh) ?: "km/h"
        val mphUnit = context?.getString(R.string.unit_mph) ?: "mph"
        val hpaUnit = context?.getString(R.string.unit_hpa) ?: "hPa"
        val inhgUnit = context?.getString(R.string.unit_inhg) ?: "inHg"
        val mmUnit = context?.getString(R.string.unit_mm) ?: "mm"
        val inUnit = context?.getString(R.string.unit_in) ?: "in"

        fun fmtTemp(c: Double): String =
            if (isMetric) "${c.roundToInt()}°C" else "${(c * 9.0 / 5.0 + 32.0).roundToInt()}°F"

        fun fmtWind(kmh: Double): String =
            if (isMetric) "${kmh.roundToInt()} $kmhUnit" else "${(kmh * 0.621371).roundToInt()} $mphUnit"

        fun fmtPress(hpa: Double): String =
            if (isMetric) "${hpa.roundToInt()} $hpaUnit"
            else String.format(Locale.getDefault(), "%.2f %s", hpa * 0.02953, inhgUnit)

        fun fmtPrecip(mm: Double): String =
            if (isMetric) String.format(Locale.getDefault(), "%.1f %s", mm, mmUnit)
            else String.format(Locale.getDefault(), "%.2f %s", mm * 0.0393701, inUnit)

        // Anchor the timeline start to the current 30-minute broadcast block
        val anchorCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val minute = get(Calendar.MINUTE)
            set(Calendar.MINUTE, if (minute < 30) 0 else 30)
        }
        val baseBlockMs = anchorCal.timeInMillis
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        val catSevere = context?.getString(R.string.vod_cat_severe) ?: ""
        val catForecasts = context?.getString(R.string.vod_cat_forecasts) ?: ""
        val catEarth = context?.getString(R.string.vod_cat_earth_sci) ?: ""
        val catClimate = context?.getString(R.string.vod_cat_climate) ?: ""

        val presenterOps = context?.getString(R.string.vod_live_presenter_ops) ?: ""
        val presenterSynoptic = context?.getString(R.string.vod_live_presenter_synoptic) ?: ""
        val presenterDynamics = context?.getString(R.string.vod_live_presenter_dynamics) ?: ""
        val presenterClimate = context?.getString(R.string.vod_live_presenter_climate) ?: ""

        val vodByIdPrefix = vodStories.associateBy { story ->
            when {
                story.id.startsWith("vod_severe") -> "severe"
                story.id.startsWith("vod_forecasts") -> "forecasts"
                story.id.startsWith("vod_earth") -> "earth"
                else -> "climate"
            }
        }

        val sevenDays = pkg.dailyDays.take(7)
        val minWeekC = sevenDays.minOfOrNull { it.minTempC } ?: current.tempC
        val maxWeekC = sevenDays.maxOfOrNull { it.maxTempC } ?: current.tempC
        val totalWeekRainMm = sevenDays.sumOf { it.rainMm }
        val peakPop12h = pkg.hourlyPoints.take(12).maxOfOrNull { it.popPercent } ?: 0

        fun buildSlot(
            slotId: String,
            channelIndex: Int,
            channelNameRes: Int,
            title: String,
            synopsis: String,
            presenter: String,
            categoryId: String,
            categoryLabel: String,
            offsetMin: Int,
            durationMin: Int,
            weatherCode: Int,
            isDay: Boolean,
            accentHex: Long
        ): BroadcastScheduleSlot {
            val startMs = baseBlockMs + offsetMin * 60_000L
            val endMs = startMs + durationMin * 60_000L
            val startStr = timeFmt.format(Date(startMs))
            val durationStr = String.format(Locale.US, "%02d:00", durationMin.coerceAtMost(59))
            val linkedVod = VodStory(
                id = slotId,
                category = categoryLabel,
                title = title,
                presenter = presenter,
                duration = durationStr,
                timestamp = context?.getString(R.string.vod_live_timestamp_fmt, startStr) ?: startStr,
                description = synopsis,
                videoUrl = "",
                accentColorHex = accentHex
            )
            val channelCode = context?.getString(R.string.epg_channel_code_fmt, channelIndex + 1)
                ?: String.format(Locale.US, "CH %02d", channelIndex + 1)
            return BroadcastScheduleSlot(
                id = slotId,
                channelIndex = channelIndex,
                channelNumber = channelCode,
                channelNameRes = channelNameRes,
                programTitle = title,
                programSynopsis = synopsis,
                presenter = presenter,
                categoryId = categoryId,
                categoryLabel = categoryLabel,
                startEpochMs = startMs,
                endEpochMs = endMs,
                durationMinutes = durationMin,
                weatherCode = weatherCode,
                isDay = isDay,
                accentColorHex = accentHex,
                linkedVodStory = linkedVod
            )
        }

        val slots = mutableListOf<BroadcastScheduleSlot>()
        val locSlug = location.name.lowercase(Locale.ROOT).replace(" ", "_")

        // Channel 01: Horizon Main (Current Live Desk + Upcoming Hourly + Diurnal Briefings)
        val ch1FirstVod = vodByIdPrefix["forecasts"]
        val hr1 = pkg.hourlyPoints.getOrNull(1)
        val hr3 = pkg.hourlyPoints.getOrNull(3)
        val nextDiurnal = pkg.diurnalPeriods.firstOrNull { !it.isCurrent } ?: pkg.diurnalPeriods.firstOrNull()

        slots += buildSlot(
            slotId = "epg_ch1_0_$locSlug",
            channelIndex = 0,
            channelNameRes = R.string.epg_channel_main,
            title = ch1FirstVod?.title
                ?: (context?.getString(R.string.vod_live_forecast_title, location.name) ?: location.name),
            synopsis = ch1FirstVod?.description
                ?: (context?.getString(
                    R.string.vod_live_forecast_desc,
                    location.name,
                    fmtTemp(minWeekC),
                    fmtTemp(maxWeekC),
                    fmtPrecip(totalWeekRainMm)
                ) ?: ""),
            presenter = presenterSynoptic,
            categoryId = "forecasts",
            categoryLabel = catForecasts,
            offsetMin = 0,
            durationMin = 60,
            weatherCode = current.weatherCode,
            isDay = current.isDay,
            accentHex = 0xFF0284C7
        )

        val hr1Code = hr1?.weatherCode ?: current.weatherCode
        val hr1Cond = context?.getString(WeatherRepository.getWeatherDescriptionRes(hr1Code)) ?: current.conditionText
        slots += buildSlot(
            slotId = "epg_ch1_1_$locSlug",
            channelIndex = 0,
            channelNameRes = R.string.epg_channel_main,
            title = context?.getString(R.string.epg_slot_title_hourly, location.name) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_hourly,
                location.name,
                hr1Cond,
                hr1?.timeLabel ?: timeFmt.format(Date(baseBlockMs + 60 * 60_000L)),
                fmtTemp(hr1?.tempC ?: current.tempC),
                hr1?.popPercent ?: peakPop12h,
                hr1?.humidityPercent ?: current.humidityPercent
            ) ?: "",
            presenter = presenterSynoptic,
            categoryId = "forecasts",
            categoryLabel = catForecasts,
            offsetMin = 60,
            durationMin = 60,
            weatherCode = hr1Code,
            isDay = current.isDay,
            accentHex = 0xFF0EA5E9
        )

        val diurnalCode = nextDiurnal?.weatherCode ?: (hr3?.weatherCode ?: current.weatherCode)
        val diurnalName = nextDiurnal?.let {
            if (it.periodNameRes != 0) context?.getString(it.periodNameRes) else it.periodName
        } ?: ""
        val diurnalCond = context?.getString(WeatherRepository.getWeatherDescriptionRes(diurnalCode)) ?: current.conditionText
        slots += buildSlot(
            slotId = "epg_ch1_2_$locSlug",
            channelIndex = 0,
            channelNameRes = R.string.epg_channel_main,
            title = context?.getString(R.string.epg_slot_title_diurnal, location.name, diurnalName) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_diurnal,
                diurnalName,
                location.name,
                nextDiurnal?.timeRange ?: "",
                fmtTemp(nextDiurnal?.tempC ?: current.tempC),
                diurnalCond,
                nextDiurnal?.popPercent ?: peakPop12h
            ) ?: "",
            presenter = presenterSynoptic,
            categoryId = "forecasts",
            categoryLabel = catForecasts,
            offsetMin = 120,
            durationMin = 90,
            weatherCode = diurnalCode,
            isDay = true,
            accentHex = 0xFF10B981
        )

        slots += buildSlot(
            slotId = "epg_ch1_3_$locSlug",
            channelIndex = 0,
            channelNameRes = R.string.epg_channel_main,
            title = context?.getString(R.string.epg_slot_title_overnight, location.name) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_overnight,
                location.name,
                fmtTemp(maxWeekC),
                fmtTemp(minWeekC),
                fmtPrecip(totalWeekRainMm)
            ) ?: "",
            presenter = presenterSynoptic,
            categoryId = "forecasts",
            categoryLabel = catForecasts,
            offsetMin = 210,
            durationMin = 90,
            weatherCode = sevenDays.getOrNull(1)?.weatherCode ?: current.weatherCode,
            isDay = false,
            accentHex = 0xFF2563EB
        )

        // Channel 02: Storm Watch (Doppler Ops & Precipitation Tracking)
        val severeVod = vodByIdPrefix["severe"]
        slots += buildSlot(
            slotId = "epg_ch2_0_$locSlug",
            channelIndex = 1,
            channelNameRes = R.string.epg_channel_storm,
            title = severeVod?.title
                ?: (context?.getString(R.string.vod_live_severe_title, location.name) ?: location.name),
            synopsis = severeVod?.description ?: "",
            presenter = presenterOps,
            categoryId = "severe",
            categoryLabel = catSevere,
            offsetMin = 0,
            durationMin = 90,
            weatherCode = current.weatherCode,
            isDay = current.isDay,
            accentHex = 0xFFDC2626
        )

        val peakHr = pkg.hourlyPoints.take(12).maxByOrNull { it.popPercent }
        slots += buildSlot(
            slotId = "epg_ch2_1_$locSlug",
            channelIndex = 1,
            channelNameRes = R.string.epg_channel_storm,
            title = context?.getString(R.string.epg_slot_title_precip_watch, location.name) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_precip,
                location.name,
                peakPop12h,
                fmtWind(current.windSpeedKmh),
                fmtPress(current.pressureHpa)
            ) ?: "",
            presenter = presenterOps,
            categoryId = "severe",
            categoryLabel = catSevere,
            offsetMin = 90,
            durationMin = 90,
            weatherCode = peakHr?.weatherCode ?: current.weatherCode,
            isDay = current.isDay,
            accentHex = 0xFFEF4444
        )

        slots += buildSlot(
            slotId = "epg_ch2_2_$locSlug",
            channelIndex = 1,
            channelNameRes = R.string.epg_channel_storm,
            title = severeVod?.title
                ?: (context?.getString(R.string.vod_live_severe_title, location.name) ?: location.name),
            synopsis = context?.getString(
                R.string.epg_slot_desc_precip,
                location.name,
                sevenDays.maxOfOrNull { it.popPercent } ?: peakPop12h,
                fmtWind(current.windGustsKmh),
                fmtPress(current.pressureHpa)
            ) ?: "",
            presenter = presenterOps,
            categoryId = "severe",
            categoryLabel = catSevere,
            offsetMin = 180,
            durationMin = 120,
            weatherCode = sevenDays.maxByOrNull { it.popPercent }?.weatherCode ?: current.weatherCode,
            isDay = true,
            accentHex = 0xFFB91C1C
        )

        // Channel 03: Synoptic 7D (Atmospheric Column & Multi-Day Projections)
        val earthVod = vodByIdPrefix["earth"]
        slots += buildSlot(
            slotId = "epg_ch3_0_$locSlug",
            channelIndex = 2,
            channelNameRes = R.string.epg_channel_synoptic,
            title = earthVod?.title
                ?: (context?.getString(R.string.vod_live_earth_title, location.name) ?: location.name),
            synopsis = earthVod?.description ?: "",
            presenter = presenterDynamics,
            categoryId = "earth",
            categoryLabel = catEarth,
            offsetMin = 0,
            durationMin = 60,
            weatherCode = current.weatherCode,
            isDay = current.isDay,
            accentHex = 0xFF0284C7
        )

        val day2 = sevenDays.getOrNull(1)
        val day3 = sevenDays.getOrNull(2)
        slots += buildSlot(
            slotId = "epg_ch3_1_$locSlug",
            channelIndex = 2,
            channelNameRes = R.string.epg_channel_synoptic,
            title = context?.getString(R.string.epg_slot_title_overnight, location.name) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_overnight,
                location.name,
                fmtTemp(day2?.maxTempC ?: maxWeekC),
                fmtTemp(day2?.minTempC ?: minWeekC),
                fmtPrecip(day2?.rainMm ?: 0.0)
            ) ?: "",
            presenter = presenterSynoptic,
            categoryId = "forecasts",
            categoryLabel = catForecasts,
            offsetMin = 60,
            durationMin = 120,
            weatherCode = day2?.weatherCode ?: current.weatherCode,
            isDay = true,
            accentHex = 0xFF059669
        )

        slots += buildSlot(
            slotId = "epg_ch3_2_$locSlug",
            channelIndex = 2,
            channelNameRes = R.string.epg_channel_synoptic,
            title = context?.getString(R.string.vod_live_forecast_title, location.name) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_overnight,
                location.name,
                fmtTemp(day3?.maxTempC ?: maxWeekC),
                fmtTemp(day3?.minTempC ?: minWeekC),
                fmtPrecip(totalWeekRainMm)
            ) ?: "",
            presenter = presenterDynamics,
            categoryId = "earth",
            categoryLabel = catEarth,
            offsetMin = 180,
            durationMin = 120,
            weatherCode = day3?.weatherCode ?: current.weatherCode,
            isDay = true,
            accentHex = 0xFF0369A1
        )

        // Channel 04: Earth & Sky (Climate, AQI, Solar & Lunar Cycles)
        val climateVod = vodByIdPrefix["climate"]
        slots += buildSlot(
            slotId = "epg_ch4_0_$locSlug",
            channelIndex = 3,
            channelNameRes = R.string.epg_channel_climate,
            title = climateVod?.title
                ?: (context?.getString(R.string.vod_live_climate_title, location.name) ?: location.name),
            synopsis = climateVod?.description ?: "",
            presenter = presenterClimate,
            categoryId = "climate",
            categoryLabel = catClimate,
            offsetMin = 0,
            durationMin = 90,
            weatherCode = current.weatherCode,
            isDay = current.isDay,
            accentHex = 0xFF8B5CF6
        )

        val hr5 = pkg.hourlyPoints.getOrNull(5)
        val hr5Code = hr5?.weatherCode ?: current.weatherCode
        val hr5Cond = context?.getString(WeatherRepository.getWeatherDescriptionRes(hr5Code)) ?: current.conditionText
        slots += buildSlot(
            slotId = "epg_ch4_1_$locSlug",
            channelIndex = 3,
            channelNameRes = R.string.epg_channel_climate,
            title = context?.getString(R.string.epg_slot_title_hourly, location.name) ?: location.name,
            synopsis = context?.getString(
                R.string.epg_slot_desc_hourly,
                location.name,
                hr5Cond,
                hr5?.timeLabel ?: timeFmt.format(Date(baseBlockMs + 90 * 60_000L)),
                fmtTemp(hr5?.tempC ?: current.tempC),
                hr5?.popPercent ?: 0,
                hr5?.humidityPercent ?: current.humidityPercent
            ) ?: "",
            presenter = presenterClimate,
            categoryId = "climate",
            categoryLabel = catClimate,
            offsetMin = 90,
            durationMin = 90,
            weatherCode = hr5Code,
            isDay = current.isDay,
            accentHex = 0xFF7C3AED
        )

        slots += buildSlot(
            slotId = "epg_ch4_2_$locSlug",
            channelIndex = 3,
            channelNameRes = R.string.epg_channel_climate,
            title = climateVod?.title
                ?: (context?.getString(R.string.vod_live_climate_title, location.name) ?: location.name),
            synopsis = climateVod?.description ?: "",
            presenter = presenterClimate,
            categoryId = "climate",
            categoryLabel = catClimate,
            offsetMin = 180,
            durationMin = 120,
            weatherCode = sevenDays.getOrNull(3)?.weatherCode ?: current.weatherCode,
            isDay = false,
            accentHex = 0xFF6D28D9
        )

        return slots
    }
}
