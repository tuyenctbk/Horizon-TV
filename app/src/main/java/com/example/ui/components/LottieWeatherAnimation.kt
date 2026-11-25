package com.example.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.R
import com.example.data.repository.WeatherRepository

/**
 * Maps WMO meteorological weather codes to high-production Lottie vector broadcast animation assets.
 * Covers: Sun / Clear Sky, Clouds / Overcast / Fog, Rain / Showers / Drizzle, Snow / Wintry Mix, Thunderstorm.
 */
@RawRes
fun getLottieWeatherRawRes(weatherCode: Int, isDay: Boolean = true): Int {
    return when (weatherCode) {
        0 -> if (isDay) R.raw.weather_sun else R.raw.weather_clouds
        1, 2, 3 -> R.raw.weather_clouds
        45, 48 -> R.raw.weather_clouds
        51, 53, 55 -> R.raw.weather_rain
        61, 63, 65 -> R.raw.weather_rain
        71, 73, 75, 77 -> R.raw.weather_snow
        80, 81, 82 -> R.raw.weather_rain
        85, 86 -> R.raw.weather_snow
        95, 96, 99 -> R.raw.weather_thunder
        else -> R.raw.weather_clouds
    }
}

/**
 * Broadcast-grade animated weather condition component powered by Airbnb Lottie.
 * Renders an immersive looping 60fps vector animation for Sun, Rain, Snow, Clouds, and Thunderstorms,
 * with a zero-delay accessible fallback to [WeatherConditionIcon] while loading or in headless testing.
 */
@Composable
fun LottieWeatherAnimation(
    weatherCode: Int,
    modifier: Modifier = Modifier,
    isDay: Boolean = true,
    size: Dp = 32.dp,
    contentDescriptionOverride: String? = null,
    enableAnimation: Boolean = true,
    tintFallback: Color? = null
) {
    val rawRes = getLottieWeatherRawRes(weatherCode, isDay)
    val resolvedDescription = contentDescriptionOverride
        ?: stringResource(WeatherRepository.getWeatherDescriptionRes(weatherCode))

    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(rawRes)
    )
    val composition by compositionResult

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = enableAnimation
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("lottie_weather_animation_${weatherCode}")
            .semantics(mergeDescendants = true) {
                this.contentDescription = resolvedDescription
            },
        contentAlignment = Alignment.Center
    ) {
        if (composition != null && enableAnimation) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(size)
            )
        } else {
            WeatherConditionIcon(
                weatherCode = weatherCode,
                isDay = isDay,
                size = size,
                tint = tintFallback,
                contentDescriptionOverride = resolvedDescription
            )
        }
    }
}
