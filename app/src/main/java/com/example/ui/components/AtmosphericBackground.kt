package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.TVMidnightNavy

@Composable
fun AtmosphericBackground(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Television broadcast atmospheric palettes tailored to conditions
    val (targetTopColor, targetBottomColor) = when {
        weatherCode in listOf(95, 96, 99) -> {
            // Thunderstorm: atmospheric deep storm violet
            Pair(Color(0xFF0F0B1E), Color(0xFF080612))
        }
        weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> {
            // Rain / Showers: atmospheric cool slate cyan navy
            Pair(Color(0xFF0A1526), Color(0xFF070E1A))
        }
        weatherCode in listOf(71, 73, 75, 77, 85, 86) -> {
            // Snow / Arctic: frosty deep cobalt navy
            Pair(Color(0xFF0E1A2C), Color(0xFF091220))
        }
        weatherCode in listOf(1, 2, 3, 45, 48) -> {
            // Cloudy / Overcast / Fog: atmospheric muted graphite navy
            if (isDay) Pair(Color(0xFF101622), Color(0xFF0B0F18))
            else Pair(Color(0xFF0A0D14), Color(0xFF06090E))
        }
        else -> {
            // Clear sky: radiant deep sapphire navy (day) or starlit cosmic black (night)
            if (isDay) Pair(Color(0xFF0E1C33), Color(0xFF091222))
            else Pair(Color(0xFF080C16), Color(0xFF04070D))
        }
    }

    val topColor by animateColorAsState(targetTopColor, animationSpec = tween(1200), label = "topColor")
    val bottomColor by animateColorAsState(targetBottomColor, animationSpec = tween(1200), label = "bottomColor")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            )
    ) {
        // High fidelity dynamic particle animations according to condition (rain streaks/ripples, snow drift, sunbeams & motes, stars, fog)
        WeatherParticleCanvas(
            weatherCode = weatherCode,
            isDay = isDay,
            modifier = Modifier.fillMaxSize()
        )

        content()
    }
}
