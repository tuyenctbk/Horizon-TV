package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                val collapseLabel = stringResource(R.string.lbar_collapse)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics(mergeDescendants = true) { heading() }
                        ) {
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
                            modifier = Modifier
                                .clickable(role = Role.Button) { onToggleLBar() }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = collapseLabel
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = collapseLabel,
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
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
                        LottieWeatherAnimation(weatherCode = currentWeather.weatherCode, isDay = currentWeather.isDay, size = 36.dp)
                    }

                    Text(
                        text = stringResource(WeatherRepository.getWeatherDescriptionRes(currentWeather.weatherCode)).uppercase(),
                        color = SolarGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Compact Telemetry Rows
                    val kmhUnit = stringResource(R.string.unit_kmh)
                    val mphUnit = stringResource(R.string.unit_mph)
                    val hpaUnit = stringResource(R.string.unit_hpa)
                    val inhgUnit = stringResource(R.string.unit_inhg)

                    val windVal = if (unitSystem == UnitSystem.METRIC) "${currentWeather.windSpeedKmh.roundToInt()} $kmhUnit"
                    else "${(currentWeather.windSpeedKmh * 0.621371).roundToInt()} $mphUnit"

                    val baroVal = if (unitSystem == UnitSystem.METRIC) "${currentWeather.pressureHpa.roundToInt()} $hpaUnit"
                    else String.format(java.util.Locale.getDefault(), "%.2f %s", currentWeather.pressureHpa * 0.02953, inhgUnit)

                    val dewVal = if (unitSystem == UnitSystem.METRIC) "${currentWeather.dewPointC.roundToInt()}°C"
                    else "${(currentWeather.dewPointC * 9 / 5 + 32).roundToInt()}°F"

                    LBarMetricItem(label = stringResource(R.string.lbar_wind), value = "$windVal ${WeatherRepository.getWindCompass(currentWeather.windDirectionDeg)}")
                    LBarMetricItem(label = stringResource(R.string.lbar_humidity), value = "${currentWeather.humidityPercent}%")
                    LBarMetricItem(label = stringResource(R.string.lbar_barometer), value = baroVal)
                    LBarMetricItem(label = stringResource(R.string.lbar_dew_point), value = dewVal)

                    HorizontalDivider(color = SleekBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Diurnal 3-Period Forecasts
                    Text(
                        text = stringResource(R.string.lbar_diurnal_outlook),
                        color = SleekBluePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    weatherPackage.diurnalPeriods.take(3).forEach { p ->
                        val pTemp = if (unitSystem == UnitSystem.METRIC) "${p.tempC.roundToInt()}°"
                        else "${(p.tempC * 9 / 5 + 32).roundToInt()}°"
                        val pName = stringResource(p.periodNameRes)
                        val pRain = stringResource(R.string.lbar_rain_suffix, p.popPercent)
                        val pCond = stringResource(WeatherRepository.getWeatherDescriptionRes(p.weatherCode))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(SleekSurfaceSecondary, RoundedCornerShape(6.dp))
                                .border(1.dp, SleekBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "$pName: $pTemp, $pCond, $pRain"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = pName.take(8), color = SleekTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(text = pRain, color = SleekBluePrimary, fontSize = 8.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LottieWeatherAnimation(weatherCode = p.weatherCode, size = 16.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = pTemp, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (!isLBarVisible && currentWeather != null) {
                val expandLabel = stringResource(R.string.lbar_expand)
                // Collapsed docking tab
                Surface(
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    color = SleekSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .clickable(role = Role.Button) { onToggleLBar() }
                        .align(Alignment.CenterVertically)
                        .padding(vertical = 12.dp)
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = expandLabel
                        }
                        .testTag("persistent_lbar_expand_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = expandLabel, tint = SleekBluePrimary, modifier = Modifier.size(16.dp))
                        Text(expandLabel, color = SleekBluePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

        // CONSISTENT 24/7 NEWS CHANNEL BOTTOM BROADCAST L-BAR (Ticker + Live Time + 12H Strip)
        if (weatherPackage != null) {
            BroadcastLBar(
                currentLocation = currentLocation,
                currentWeather = currentWeather,
                headlines = weatherPackage.headlines,
                hourlyPoints = weatherPackage.hourlyPoints,
                unitSystem = unitSystem,
                activeAlerts = weatherPackage.alerts,
                showSidebarToggle = true,
                isSidebarExpanded = isLBarVisible,
                onToggleSidebar = onToggleLBar,
                modifier = Modifier.testTag("persistent_lbar_bottom_ticker")
            )
        }
    }
}

@Composable
private fun LBarMetricItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SleekTextSecondary, fontSize = 10.sp)
        Text(text = value, color = SleekTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
