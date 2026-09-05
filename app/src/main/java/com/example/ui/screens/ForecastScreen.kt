package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.FullWeatherPackage
import com.example.ui.components.RechartsFiveDayForecastView
import com.example.ui.components.WeatherConditionIcon
import com.example.ui.components.WeatherForecastTrendsChart
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    weatherPackage: FullWeatherPackage?,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    if (currentWeather == null || weatherPackage == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HorizonCyan)
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: 36-Hour Detailed, 1: 24-Hour Chart, 2: 14-Day Outlook

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("forecast_view")
    ) {
        // Tab Header
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 650.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.forecast_meteorological_outlook, currentLocation.name),
                        color = SleekTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = stringResource(R.string.forecast_numerical_prediction),
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(SleekSurfaceSecondary, RoundedCornerShape(percent = 50))
                            .border(1.dp, SleekBorder, RoundedCornerShape(percent = 50))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ForecastTabButton(stringResource(R.string.forecast_tab_5day), isSelected = selectedTab == 0) { selectedTab = 0 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_36hour), isSelected = selectedTab == 1) { selectedTab = 1 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_24hour), isSelected = selectedTab == 2) { selectedTab = 2 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_14day), isSelected = selectedTab == 3) { selectedTab = 3 }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.forecast_meteorological_outlook, currentLocation.name),
                            color = SleekTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = stringResource(R.string.forecast_numerical_prediction),
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Segmented Tab Selector
                    Row(
                        modifier = Modifier
                            .background(SleekSurfaceSecondary, RoundedCornerShape(percent = 50))
                            .border(1.dp, SleekBorder, RoundedCornerShape(percent = 50))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ForecastTabButton(stringResource(R.string.forecast_tab_5day), isSelected = selectedTab == 0) { selectedTab = 0 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_36hour), isSelected = selectedTab == 1) { selectedTab = 1 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_24hour), isSelected = selectedTab == 2) { selectedTab = 2 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_14day), isSelected = selectedTab == 3) { selectedTab = 3 }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> RechartsFiveDayForecastView(weatherPackage.dailyDays, unitSystem)
            1 -> ThirtySixHourDetailedView(currentWeather, weatherPackage, unitSystem)
            2 -> HourlyTrendChartView(weatherPackage, unitSystem)
            3 -> FourteenDayOutlookView(weatherPackage, unitSystem)
        }
    }
}

@Composable
private fun ForecastTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (isSelected) SleekBluePrimary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else SleekTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ThirtySixHourDetailedView(
    current: CurrentWeather,
    pkg: FullWeatherPackage,
    unitSystem: UnitSystem
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Diurnal Period Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pkg.diurnalPeriods.forEach { period ->
                    val pTemp = if (unitSystem == UnitSystem.METRIC) "${period.tempC.roundToInt()}°C"
                    else "${(period.tempC * 9 / 5 + 32).roundToInt()}°F"

                    val isCurr = period.isCurrent
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isCurr) SleekBlueContainer else SleekSurface,
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isCurr) SleekBluePrimary else SleekBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isCurr) SleekBluePrimary else SleekSurfaceSecondary
                        ) {
                            Text(
                                text = period.periodName.uppercase(),
                                color = if (isCurr) Color.White else SleekTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = period.timeRange, color = SleekTextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        WeatherConditionIcon(weatherCode = period.weatherCode, size = 34.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = pTemp, color = if (isCurr) SleekOnBlueContainer else SleekTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(text = period.conditionText, color = SolarGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = stringResource(R.string.forecast_rain_risk, period.popPercent), color = SleekBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Deep-Dive Meteorological Metric Cards Grid
            Text(
                text = stringResource(R.string.forecast_atmospheric_deepdive),
                color = SleekTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricDetailCard(
                    icon = Icons.Default.WaterDrop,
                    title = stringResource(R.string.forecast_relative_humidity),
                    value = "${current.humidityPercent}%",
                    detail = stringResource(R.string.forecast_dew_point, current.dewPointC.roundToInt()),
                    accentColor = SleekBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailCard(
                    icon = Icons.Default.Compress,
                    title = stringResource(R.string.forecast_surface_pressure),
                    value = "${current.pressureHpa.roundToInt()} hPa",
                    detail = if (current.pressureHpa > 1013) stringResource(R.string.forecast_high_pressure) else stringResource(R.string.forecast_low_pressure),
                    accentColor = SleekBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailCard(
                    icon = Icons.Default.WbSunny,
                    title = stringResource(R.string.forecast_uv_index),
                    value = stringResource(R.string.forecast_uv_of_11, current.uvIndex.roundToInt()),
                    detail = when {
                        current.uvIndex >= 8 -> stringResource(R.string.forecast_uv_very_high)
                        current.uvIndex >= 6 -> stringResource(R.string.forecast_uv_high)
                        current.uvIndex >= 3 -> stringResource(R.string.forecast_uv_moderate)
                        else -> stringResource(R.string.forecast_uv_low)
                    },
                    accentColor = SolarGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricDetailCard(
                    icon = Icons.Default.Air,
                    title = stringResource(R.string.forecast_wind_gust_vector),
                    value = "${current.windSpeedKmh.roundToInt()} km/h",
                    detail = stringResource(R.string.forecast_peak_gusts, current.windGustsKmh.roundToInt()),
                    accentColor = SleekGreenText,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailCard(
                    icon = Icons.Default.Visibility,
                    title = stringResource(R.string.forecast_atmospheric_visibility),
                    value = "${current.visibilityKm.roundToInt()} km",
                    detail = if (current.visibilityKm >= 10) stringResource(R.string.forecast_clear_horizon) else stringResource(R.string.forecast_reduced_visibility),
                    accentColor = SleekTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailCard(
                    icon = Icons.Default.FilterDrama,
                    title = stringResource(R.string.forecast_cloud_cover),
                    value = "${current.cloudCoverPercent}%",
                    detail = stringResource(R.string.forecast_sky_observation),
                    accentColor = SleekBluePrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HourlyTrendChartView(pkg: FullWeatherPackage, unitSystem: UnitSystem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WeatherForecastTrendsChart(hourlyPoints = pkg.hourlyPoints, unitSystem = unitSystem)
        }

        item {
            Text(
                text = stringResource(R.string.forecast_granular_table),
                color = SleekTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        items(pkg.hourlyPoints.take(24)) { pt ->
            val tempDisplay = if (unitSystem == UnitSystem.METRIC) "${pt.tempC.roundToInt()}°C"
            else "${(pt.tempC * 9 / 5 + 32).roundToInt()}°F"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pt.timeLabel, color = SleekBluePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(14.dp))
                    WeatherConditionIcon(weatherCode = pt.weatherCode, size = 20.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = pt.conditionText, color = SleekTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = stringResource(R.string.forecast_rain_risk, pt.popPercent), color = SleekBluePrimary, fontSize = 12.sp)
                    Text(text = stringResource(R.string.forecast_hum, pt.humidityPercent), color = SleekTextSecondary, fontSize = 11.sp)
                    Text(text = tempDisplay, color = SleekTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FourteenDayOutlookView(pkg: FullWeatherPackage, unitSystem: UnitSystem) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.forecast_14day_extended),
                color = SleekTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        items(pkg.dailyDays) { day ->
            val maxT = if (unitSystem == UnitSystem.METRIC) "${day.maxTempC.roundToInt()}°"
            else "${(day.maxTempC * 9 / 5 + 32).roundToInt()}°"

            val minT = if (unitSystem == UnitSystem.METRIC) "${day.minTempC.roundToInt()}°"
            else "${(day.minTempC * 9 / 5 + 32).roundToInt()}°"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Column
                Column(modifier = Modifier.width(120.dp)) {
                    Text(text = day.dayOfWeek, color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = day.dateLabel, color = SleekTextSecondary, fontSize = 10.sp)
                }

                // Condition Icon & Text
                Row(modifier = Modifier.width(180.dp), verticalAlignment = Alignment.CenterVertically) {
                    WeatherConditionIcon(weatherCode = day.weatherCode, size = 24.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = day.conditionText, color = SleekTextPrimary, fontSize = 12.sp, maxLines = 1)
                }

                // Precipitation volume
                Column(modifier = Modifier.width(100.dp)) {
                    Text(text = stringResource(R.string.forecast_rain, day.popPercent), color = SleekBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (day.rainMm > 0) {
                        Text(text = "${String.format(java.util.Locale.US, "%.1f", day.rainMm)} mm", color = SleekTextSecondary, fontSize = 10.sp)
                    }
                }

                // Temperature Delta Bar
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(text = minT, color = SleekBluePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(SleekBlueContainer, SolarGold, SevereRed.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = maxT, color = SleekTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun MetricDetailCard(
    icon: ImageVector,
    title: String,
    value: String,
    detail: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SleekSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SleekSurfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, color = SleekTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = value, color = SleekTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = detail, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
