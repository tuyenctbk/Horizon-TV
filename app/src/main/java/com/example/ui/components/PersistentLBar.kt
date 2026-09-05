package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Persistent L-bar UI component that reserves space along the screen edges:
 * - Left Edge: Weather telemetry, live station clock, current city metrics, and diurnal forecasts.
 * - Bottom Edge: Real-time scrolling emergency news ticker and hourly forecast progression.
 */
@Composable
fun PersistentLBarLayout(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    weatherPackage: FullWeatherPackage?,
    unitSystem: UnitSystem,
    isLBarVisible: Boolean = true,
    onToggleLBar: () -> Unit,
    content: @Composable () -> Unit
) {
    var liveClockString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm:ss z", Locale.US)
        while (true) {
            liveClockString = timeFmt.format(Date())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("persistent_lbar_container")
    ) {
        // Main Row: Left Sidebar + Child Content
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // LEFT EDGE TELEMETRY BAR
            if (isLBarVisible && currentWeather != null && weatherPackage != null) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(SleekSurface)
                        .border(1.dp, SleekBorder)
                        .padding(12.dp)
                        .testTag("persistent_lbar_left_panel")
                ) {
                    // Header with Live Clock & Station Branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SleekRedContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.lbar_badge_live),
                                    color = SleekRedText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.lbar_title),
                                color = SleekBluePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = SleekSurfaceSecondary,
                            modifier = Modifier.clickable { onToggleLBar() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = stringResource(R.string.lbar_collapse),
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = liveClockString,
                        color = SleekTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )

                    HorizontalDivider(color = SleekBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Location & Primary Temperature
                    Text(
                        text = currentLocation.name.uppercase(),
                        color = SleekTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentLocation.region.take(10)} • ${currentLocation.countryCode}",
                        color = SleekTextSecondary,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val tempStr = if (unitSystem == UnitSystem.METRIC) "${currentWeather.tempC.roundToInt()}°C"
                    else "${(currentWeather.tempC * 9 / 5 + 32).roundToInt()}°F"

                    val feelsStr = if (unitSystem == UnitSystem.METRIC) "${currentWeather.feelsLikeC.roundToInt()}°"
                    else "${(currentWeather.feelsLikeC * 9 / 5 + 32).roundToInt()}°"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = tempStr,
                                color = SleekTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = stringResource(R.string.lbar_feels_like, feelsStr),
                                color = SleekBluePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        WeatherConditionIcon(weatherCode = currentWeather.weatherCode, isDay = currentWeather.isDay, size = 36.dp)
                    }

                    Text(
                        text = currentWeather.conditionText.uppercase(),
                        color = SolarGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Compact Telemetry Rows
                    LBarMetricItem(label = stringResource(R.string.lbar_wind), value = "${currentWeather.windSpeedKmh.roundToInt()} km/h ${WeatherRepository.getWindCompass(currentWeather.windDirectionDeg)}")
                    LBarMetricItem(label = stringResource(R.string.lbar_humidity), value = "${currentWeather.humidityPercent}%")
                    LBarMetricItem(label = stringResource(R.string.lbar_barometer), value = "${currentWeather.pressureHpa.roundToInt()} hPa")
                    LBarMetricItem(label = stringResource(R.string.lbar_dew_point), value = "${currentWeather.dewPointC.roundToInt()}°C")

                    HorizontalDivider(color = SleekBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Diurnal 3-Period Forecasts
                    Text(
                        text = stringResource(R.string.lbar_diurnal_outlook),
                        color = SleekBluePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    weatherPackage.diurnalPeriods.take(3).forEach { p ->
                        val pTemp = if (unitSystem == UnitSystem.METRIC) "${p.tempC.roundToInt()}°"
                        else "${(p.tempC * 9 / 5 + 32).roundToInt()}°"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(SleekSurfaceSecondary, RoundedCornerShape(6.dp))
                                .border(1.dp, SleekBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = p.periodName.take(8), color = SleekTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(text = stringResource(R.string.lbar_rain_suffix, p.popPercent), color = SleekBluePrimary, fontSize = 8.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WeatherConditionIcon(weatherCode = p.weatherCode, size = 14.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = pTemp, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (!isLBarVisible && currentWeather != null) {
                // Collapsed docking tab
                Surface(
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    color = SleekSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .clickable { onToggleLBar() }
                        .align(Alignment.CenterVertically)
                        .padding(vertical = 12.dp)
                        .testTag("persistent_lbar_expand_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.lbar_expand), tint = SleekBluePrimary, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.lbar_expand), color = SleekBluePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // MAIN CONTENT VIEWPORT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }

        // BOTTOM EDGE TICKER SCROLLER & HOURLY STRIP
        if (isLBarVisible && weatherPackage != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurface)
                    .border(1.dp, SleekBorder)
                    .testTag("persistent_lbar_bottom_ticker")
            ) {
                // Crawling Live News & Emergency Ticker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekSurfaceSecondary)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = SleekBluePrimary
                    ) {
                        Text(
                            text = stringResource(R.string.lbar_dispatch_title),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))

                    val headlineItems = weatherPackage.headlines
                    val defaultTicker = stringResource(R.string.lbar_default_ticker)
                    val tickerText = if (headlineItems.isNotEmpty()) {
                        headlineItems.joinToString("   ■   ")
                    } else {
                        defaultTicker
                    }

                    Text(
                        text = tickerText,
                        color = SleekTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                // Horizontal Timeline Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weatherPackage.hourlyPoints.take(12).forEach { pt ->
                        val t = if (unitSystem == UnitSystem.METRIC) "${pt.tempC.roundToInt()}°"
                        else "${(pt.tempC * 9 / 5 + 32).roundToInt()}°"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = pt.timeLabel, color = SleekTextSecondary, fontSize = 10.sp)
                            WeatherConditionIcon(weatherCode = pt.weatherCode, size = 15.dp)
                            Text(text = t, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LBarMetricItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SleekTextSecondary, fontSize = 10.sp)
        Text(text = value, color = SleekTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
