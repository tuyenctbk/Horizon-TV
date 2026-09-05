package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun WeatherConditionIcon(
    weatherCode: Int,
    isDay: Boolean = true,
    size: Dp = 32.dp,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    val (icon, defaultColor) = when (weatherCode) {
        0 -> if (isDay) Pair(Icons.Default.WbSunny, SolarGold) else Pair(Icons.Default.NightsStay, HorizonCyan)
        1, 2 -> if (isDay) Pair(Icons.Default.WbCloudy, HorizonSky) else Pair(Icons.Default.NightsStay, HorizonSky)
        3 -> Pair(Icons.Default.Cloud, TextSecondarySilver)
        45, 48 -> Pair(Icons.Default.Deblur, TextMuted)
        51, 53, 55 -> Pair(Icons.Default.Grain, HorizonSky)
        61, 63 -> Pair(Icons.Default.WaterDrop, HorizonSky)
        65 -> Pair(Icons.Default.Thunderstorm, HorizonCyan)
        71, 73, 75, 77 -> Pair(Icons.Default.AcUnit, TextPrimaryWhite)
        80, 81, 82 -> Pair(Icons.Default.Umbrella, HorizonSky)
        85, 86 -> Pair(Icons.Default.AcUnit, HorizonSky)
        95, 96, 99 -> Pair(Icons.Default.FlashOn, SevereRed)
        else -> Pair(Icons.Default.WbCloudy, HorizonSky)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Weather Condition Icon",
        tint = tint ?: defaultColor,
        modifier = modifier.size(size)
    )
}
