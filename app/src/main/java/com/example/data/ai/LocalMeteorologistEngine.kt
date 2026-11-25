package com.example.data.ai

import android.content.Context
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.data.repository.FullWeatherPackage
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tier 3 Zero-Cost Local Meteorology Engine.
 * Synthesizes deterministic weather briefings and local reflex answers directly from live
 * Open-Meteo telemetry with 0 cloud tokens whenever offline or when cloud quota is paused.
 */
object LocalMeteorologistEngine {

    fun buildDeterministicBriefing(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        context: Context? = null
    ): AiWeatherBriefing {
        val today = pkg.dailyDays.firstOrNull()
        val tempStr = formatTemp(current.tempC, unitSystem)
        val feelsStr = formatTemp(current.feelsLikeC, unitSystem)
        val highStr = formatTemp(today?.maxTempC ?: current.tempC, unitSystem)
        val lowStr = formatTemp(today?.minTempC ?: current.tempC, unitSystem)
        val windStr = formatWind(current.windSpeedKmh, unitSystem)
        val maxPop = pkg.hourlyPoints.take(12).maxOfOrNull { it.popPercent } ?: today?.popPercent ?: 0

        val headline = when {
            current.weatherCode >= 95 -> context?.getString(R.string.local_ai_headline_storm_fmt, location.name, tempStr)
                ?: "Storm watch in ${location.name} • $tempStr"
            maxPop >= 60 -> context?.getString(R.string.local_ai_headline_rain_fmt, location.name, highStr)
                ?: "Wet intervals ahead for ${location.name} • High $highStr"
            current.tempC >= 30 -> context?.getString(R.string.local_ai_headline_heat_fmt, location.name, highStr)
                ?: "Warm conditions in ${location.name} • Peak $highStr"
            current.tempC <= 4 -> context?.getString(R.string.local_ai_headline_cold_fmt, location.name, lowStr)
                ?: "Crisp cold air over ${location.name} • Low $lowStr"
            else -> context?.getString(R.string.local_ai_headline_default_fmt, current.conditionText, location.name, tempStr)
                ?: "${current.conditionText} across ${location.name} • $tempStr"
        }

        val summary = buildString {
            append(
                context?.getString(
                    R.string.local_ai_summary_intro_fmt,
                    location.name,
                    tempStr,
                    feelsStr,
                    current.conditionText.lowercase(Locale.getDefault())
                ) ?: "${location.name} is currently $tempStr (feels like $feelsStr) with ${current.conditionText.lowercase(Locale.US)} skies. "
            )
            append(
                context?.getString(
                    R.string.local_ai_summary_range_fmt,
                    highStr,
                    lowStr,
                    windStr,
                    current.humidityPercent
                ) ?: "Expect a high near $highStr and an overnight low of $lowStr with winds around $windStr and ${current.humidityPercent}% humidity. "
            )
            if (maxPop >= 35) {
                append(
                    context?.getString(R.string.local_ai_summary_pop_high_fmt, maxPop)
                        ?: "Precipitation risk peaks at $maxPop% over the next 12 hours."
                )
            } else {
                append(
                    context?.getString(R.string.local_ai_summary_pop_low_fmt, maxPop)
                        ?: "Precipitation probability stays low ($maxPop% peak) through the next 12 hours."
                )
            }
        }

        val outfit = buildString {
            when {
                current.feelsLikeC <= 5 -> append(
                    context?.getString(R.string.local_ai_outfit_cold)
                        ?: "Insulated coat, warm mid-layer, and gloves recommended. "
                )
                current.feelsLikeC <= 14 -> append(
                    context?.getString(R.string.local_ai_outfit_cool)
                        ?: "Light jacket or layered sweater suits today's temperatures. "
                )
                current.feelsLikeC <= 24 -> append(
                    context?.getString(R.string.local_ai_outfit_mild)
                        ?: "Comfortable breathable layers work well from morning to evening. "
                )
                else -> append(
                    context?.getString(R.string.local_ai_outfit_hot)
                        ?: "Lightweight breathable fabrics and hydration are essential in the heat. "
                )
            }
            if (maxPop >= 40) {
                append(
                    context?.getString(R.string.local_ai_commute_rain)
                        ?: "Pack a compact umbrella or waterproof shell for commuting."
                )
            } else if (current.windSpeedKmh >= 28) {
                append(
                    context?.getString(R.string.local_ai_commute_wind)
                        ?: "A wind-resistant outer layer will help against brisk gusts."
                )
            } else {
                append(
                    context?.getString(R.string.local_ai_commute_dry)
                        ?: "Dry road and walk conditions expected for most commutes."
                )
            }
        }

        val safety = buildString {
            val uvInt = current.uvIndex.roundToInt()
            val uvNote = when {
                current.uvIndex >= 8 -> context?.getString(R.string.local_ai_uv_very_high_fmt, uvInt)
                    ?: "Very high UV ($uvInt/11): SPF 30+, hat, and shade advised."
                current.uvIndex >= 6 -> context?.getString(R.string.local_ai_uv_high_fmt, uvInt)
                    ?: "High UV ($uvInt/11): wear sunscreen during midday hours."
                current.uvIndex >= 3 -> context?.getString(R.string.local_ai_uv_moderate_fmt, uvInt)
                    ?: "Moderate UV ($uvInt/11): sunglasses recommended."
                else -> context?.getString(R.string.local_ai_uv_low_fmt, uvInt)
                    ?: "Low UV exposure ($uvInt/11)."
            }
            val aqiNote = context?.getString(R.string.local_ai_aqi_note_fmt, current.aqiStatus, current.aqi)
                ?: "Air quality is ${current.aqiStatus} (US AQI ${current.aqi})."
            append("$uvNote $aqiNote")
            if (pkg.alerts.isNotEmpty()) {
                append(
                    context?.getString(R.string.local_ai_active_advisory_fmt, pkg.alerts.first().title)
                        ?: " Active advisory: ${pkg.alerts.first().title}."
                )
            }
        }

        val bestWindow = findBestOutdoorWindow(pkg, unitSystem, context)

        return AiWeatherBriefing(
            locationName = location.name,
            headline = headline,
            broadcastSummary = summary,
            outfitAndCommute = outfit,
            outdoorSafety = safety,
            bestWindowToday = bestWindow,
            tierUsed = AiCapabilityTier.LOCAL_METEOROLOGY,
            modelLabel = context?.getString(R.string.ai_local_model_label) ?: "Local Meteorology Engine",
            isFromCache = false
        )
    }

    fun answerLocally(
        question: String,
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        context: Context? = null
    ): String {
        val q = question.lowercase(Locale.US)
        val today = pkg.dailyDays.firstOrNull()
        val next12 = pkg.hourlyPoints.take(12)
        val peakRainHour = next12.maxByOrNull { it.popPercent }

        return when {
            listOf("rain", "umbrella", "commute", "wet", "storm", "lluvia").any { q.contains(it) } -> {
                val pop = peakRainHour?.popPercent ?: today?.popPercent ?: 0
                val time = peakRainHour?.timeLabel ?: (context?.getString(R.string.local_ai_later_today) ?: "later today")
                if (pop >= 40) {
                    val accumLabel = today?.rainMm?.let { String.format(Locale.US, "%.1f mm", it) }
                        ?: (context?.getString(R.string.local_ai_light_accumulation) ?: "light accumulation")
                    context?.getString(R.string.local_ai_reply_rain_high_fmt, location.name, pop, time, accumLabel)
                        ?: "Local telemetry for ${location.name} shows up to $pop% precipitation risk around $time ($accumLabel). Bringing rain gear is recommended."
                } else {
                    context?.getString(R.string.local_ai_reply_rain_low_fmt, location.name, pop, time)
                        ?: "Rain risk stays low in ${location.name} over the next 12 hours (peaking at $pop% near $time). Commute conditions look largely dry."
                }
            }

            listOf("wear", "cloth", "jacket", "outfit", "coat", "ropa").any { q.contains(it) } -> {
                val briefing = buildDeterministicBriefing(location, current, pkg, unitSystem, context)
                val tempStr = formatTemp(current.tempC, unitSystem)
                val feelsStr = formatTemp(current.feelsLikeC, unitSystem)
                context?.getString(R.string.local_ai_reply_wear_fmt, location.name, tempStr, feelsStr, briefing.outfitAndCommute)
                    ?: "For ${location.name} ($tempStr, feels like $feelsStr): ${briefing.outfitAndCommute}"
            }

            listOf("run", "walk", "exercise", "outdoor", "bike", "window", "correr").any { q.contains(it) } -> {
                val window = findBestOutdoorWindow(pkg, unitSystem, context)
                val windStr = formatWind(current.windSpeedKmh, unitSystem)
                context?.getString(R.string.local_ai_reply_run_fmt, location.name, window, current.aqi, current.aqiStatus, windStr)
                    ?: "Best outdoor window in ${location.name}: $window. Current US AQI is ${current.aqi} (${current.aqiStatus}) and wind is $windStr."
            }

            listOf("week", "7-day", "7 day", "weekend", "semana", "fin de semana").any { q.contains(it) } -> {
                val days = pkg.dailyDays.take(7)
                if (days.isEmpty()) {
                    context?.getString(R.string.local_ai_reply_week_loading_fmt, location.name)
                        ?: "7-day outlook is currently loading for ${location.name}."
                } else {
                    val weekHigh = days.maxByOrNull { it.maxTempC }
                    val weekLow = days.minByOrNull { it.minTempC }
                    val wetDays = days.count { it.popPercent >= 35 }
                    val highDay = weekHigh?.dayOfWeek.orEmpty()
                    val highTemp = formatTemp(weekHigh?.maxTempC ?: current.tempC, unitSystem)
                    val lowDay = weekLow?.dayOfWeek.orEmpty()
                    val lowTemp = formatTemp(weekLow?.minTempC ?: current.tempC, unitSystem)
                    context?.getString(R.string.local_ai_reply_week_fmt, location.name, highDay, highTemp, lowDay, lowTemp, wetDays)
                        ?: "7-day summary for ${location.name}: warmest on $highDay ($highTemp), coolest on $lowDay ($lowTemp), with $wetDays day(s) showing elevated rain chance."
                }
            }

            listOf("uv", "sun", "aqi", "air", "pollen", "wind", "aire").any { q.contains(it) } -> {
                val uvInt = current.uvIndex.roundToInt()
                val pm25Str = String.format(Locale.US, "%.1f", current.pm25)
                val windStr = formatWind(current.windSpeedKmh, unitSystem)
                val gustStr = formatWind(current.windGustsKmh, unitSystem)
                context?.getString(
                    R.string.local_ai_reply_uv_aqi_fmt,
                    location.name,
                    uvInt,
                    current.aqi,
                    current.aqiStatus,
                    pm25Str,
                    windStr,
                    gustStr
                ) ?: "Atmospheric check for ${location.name}: UV index is $uvInt/11, US AQI is ${current.aqi} (${current.aqiStatus}, PM2.5 $pm25Str), wind $windStr with gusts to $gustStr."
            }

            else -> {
                val briefing = buildDeterministicBriefing(location, current, pkg, unitSystem, context)
                context?.getString(R.string.local_ai_reply_default_fmt, briefing.broadcastSummary, briefing.bestWindowToday)
                    ?: "${briefing.broadcastSummary} Best outdoor window: ${briefing.bestWindowToday}."
            }
        }
    }

    private fun findBestOutdoorWindow(
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        context: Context? = null
    ): String {
        val candidates = pkg.hourlyPoints.take(16)
        if (candidates.isEmpty()) {
            return context?.getString(
                R.string.local_ai_best_window_daylight_fmt,
                pkg.astronomical.sunriseTime,
                pkg.astronomical.sunsetTime
            ) ?: "Daylight hours (${pkg.astronomical.sunriseTime} – ${pkg.astronomical.sunsetTime})"
        }
        val best = candidates.minByOrNull { pt ->
            val rainPenalty = pt.popPercent * 2.0
            val windPenalty = pt.windSpeedKmh * 0.8
            val comfortPenalty = kotlin.math.abs(pt.tempC - 21.0) * 1.5
            rainPenalty + windPenalty + comfortPenalty
        } ?: candidates.first()

        val tempStr = formatTemp(best.tempC, unitSystem)
        val windStr = formatWind(best.windSpeedKmh, unitSystem)
        return context?.getString(
            R.string.local_ai_best_window_hour_fmt,
            best.timeLabel,
            tempStr,
            best.popPercent,
            windStr
        ) ?: "Around ${best.timeLabel} ($tempStr, ${best.popPercent}% rain risk, $windStr)"
    }

    private fun formatTemp(tempC: Double, unitSystem: UnitSystem): String {
        return if (unitSystem == UnitSystem.METRIC) {
            "${tempC.roundToInt()}°C"
        } else {
            "${(tempC * 9.0 / 5.0 + 32.0).roundToInt()}°F"
        }
    }

    private fun formatWind(kmh: Double, unitSystem: UnitSystem): String {
        return if (unitSystem == UnitSystem.METRIC) {
            "${kmh.roundToInt()} km/h"
        } else {
            "${(kmh * 0.621371).roundToInt()} mph"
        }
    }
}
