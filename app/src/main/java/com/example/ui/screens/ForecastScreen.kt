package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.WeatherRepository
import com.example.data.repository.FullWeatherPackage
import com.example.ui.components.LottieWeatherAnimation
import com.example.ui.components.RechartsFiveDayForecastView
import com.example.ui.components.SevenDayForecastComponent
import com.example.ui.components.WeatherForecastTrendsChart
import com.example.ui.components.isDiurnalPeriodDaytime
import com.example.ui.components.isHourDaytime
import com.example.ui.components.tvFocusVisual
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    weatherPackage: FullWeatherPackage?,
    unitSystem: UnitSystem,
    isOfflineMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (currentWeather == null || weatherPackage == null) {
        val loadingDesc = stringResource(R.string.seven_day_loading)
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = BroadcastCyan,
                modifier = Modifier.semantics { contentDescription = loadingDesc }
            )
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: 7-Day, 1: 5-Day Chart, 2: Atmospheric, 3: 24h Trends, 4: 14-Day

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
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.forecast_numerical_prediction),
                        color = Color(0xFFCBD5E1),
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
                        ForecastTabButton(stringResource(R.string.forecast_tab_7day), isSelected = selectedTab == 0, tag = "forecast_tab_7day") { selectedTab = 0 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_5day), isSelected = selectedTab == 1, tag = "forecast_tab_5day") { selectedTab = 1 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_36hour), isSelected = selectedTab == 2, tag = "forecast_tab_36hour") { selectedTab = 2 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_24hour), isSelected = selectedTab == 3, tag = "forecast_tab_24hour") { selectedTab = 3 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_14day), isSelected = selectedTab == 4, tag = "forecast_tab_14day") { selectedTab = 4 }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.forecast_meteorological_outlook, currentLocation.name),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = stringResource(R.string.forecast_numerical_prediction),
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Segmented Tab Selector
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .background(SleekSurfaceSecondary, RoundedCornerShape(percent = 50))
                            .border(1.dp, SleekBorder, RoundedCornerShape(percent = 50))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ForecastTabButton(stringResource(R.string.forecast_tab_7day), isSelected = selectedTab == 0, tag = "forecast_tab_7day") { selectedTab = 0 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_5day), isSelected = selectedTab == 1, tag = "forecast_tab_5day") { selectedTab = 1 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_36hour), isSelected = selectedTab == 2, tag = "forecast_tab_36hour") { selectedTab = 2 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_24hour), isSelected = selectedTab == 3, tag = "forecast_tab_24hour") { selectedTab = 3 }
                        ForecastTabButton(stringResource(R.string.forecast_tab_14day), isSelected = selectedTab == 4, tag = "forecast_tab_14day") { selectedTab = 4 }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { if (forward) it / 16 else -it / 16 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { if (forward) -it / 16 else it / 16 })
            },
            label = "forecastTabTransition",
            modifier = Modifier.weight(1f)
        ) { tab ->
            when (tab) {
                0 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SevenDayForecastComponent(
                            location = currentLocation,
                            unitSystem = unitSystem,
                            currentTempC = currentWeather.tempC,
                            initialForecast = weatherPackage.dailyDays,
                            isOfflineMode = isOfflineMode
                        )
                    }
                }
                1 -> RechartsFiveDayForecastView(weatherPackage.dailyDays, unitSystem)
                2 -> ThirtySixHourDetailedView(currentWeather, weatherPackage, unitSystem)
                3 -> HourlyTrendChartView(weatherPackage, unitSystem)
                4 -> FourteenDayOutlookView(weatherPackage, unitSystem)
            }
        }
    }
}

@Composable
private fun ForecastTabButton(
    label: String,
    isSelected: Boolean,
    tag: String = "",
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SleekBluePrimary else Color.Transparent,
        animationSpec = tween(180),
        label = "forecastTabBg"
    )
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = bgColor,
        modifier = Modifier
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
            .clip(RoundedCornerShape(percent = 50))
            .tvFocusVisual(shape = RoundedCornerShape(percent = 50), focusBorderColor = SleekBluePrimary)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
            }
            .clickable { onClick() }
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        val kmhUnit = stringResource(R.string.unit_kmh)
        val mphUnit = stringResource(R.string.unit_mph)
        val hpaUnit = stringResource(R.string.unit_hpa)
        val inhgUnit = stringResource(R.string.unit_inhg)
        val kmUnit = stringResource(R.string.unit_km)
        val miUnit = stringResource(R.string.unit_mi)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Diurnal Period Cards Grid
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val chunkedPeriods = pkg.diurnalPeriods.chunked(2)
                        chunkedPeriods.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { period ->
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
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(percent = 50),
                                            color = if (isCurr) SleekBluePrimary else SleekSurfaceSecondary
                                        ) {
                                            Text(
                                                text = stringResource(period.periodNameRes).uppercase(),
                                                color = if (isCurr) Color.White else SleekTextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = period.timeRange, color = SleekTextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LottieWeatherAnimation(
                                            weatherCode = period.weatherCode,
                                            isDay = isDiurnalPeriodDaytime(period),
                                            size = 32.dp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = pTemp, color = if (isCurr) SleekOnBlueContainer else SleekTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                        Text(text = stringResource(WeatherRepository.getWeatherDescriptionRes(period.weatherCode)), color = SolarGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = stringResource(R.string.forecast_rain_risk, period.popPercent), color = SleekBluePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
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
                                        text = stringResource(period.periodNameRes).uppercase(),
                                        color = if (isCurr) Color.White else SleekTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = period.timeRange, color = SleekTextSecondary, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                LottieWeatherAnimation(
                                    weatherCode = period.weatherCode,
                                    isDay = isDiurnalPeriodDaytime(period),
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = pTemp, color = if (isCurr) SleekOnBlueContainer else SleekTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                Text(text = stringResource(WeatherRepository.getWeatherDescriptionRes(period.weatherCode)), color = SolarGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = stringResource(R.string.forecast_rain_risk, period.popPercent), color = SleekBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                // Deep-Dive Meteorological Metric Cards Grid
                Text(
                    text = stringResource(R.string.forecast_atmospheric_deepdive),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.semantics { heading() }
                )
            }

            val dewStr = if (unitSystem == UnitSystem.METRIC) "${current.dewPointC.roundToInt()}°C"
            else "${(current.dewPointC * 9 / 5 + 32).roundToInt()}°F"

            val pressStr = if (unitSystem == UnitSystem.METRIC) "${current.pressureHpa.roundToInt()} $hpaUnit"
            else String.format(java.util.Locale.getDefault(), "%.2f %s", current.pressureHpa * 0.02953, inhgUnit)

            val windSpeedStr = if (unitSystem == UnitSystem.METRIC) "${current.windSpeedKmh.roundToInt()} $kmhUnit"
            else "${(current.windSpeedKmh * 0.621371).roundToInt()} $mphUnit"

            val peakGustStr = if (unitSystem == UnitSystem.METRIC) "${current.windGustsKmh.roundToInt()} $kmhUnit"
            else "${(current.windGustsKmh * 0.621371).roundToInt()} $mphUnit"

            val visStr = if (unitSystem == UnitSystem.METRIC) "${current.visibilityKm.roundToInt()} $kmUnit"
            else "${(current.visibilityKm * 0.621371).roundToInt()} $miUnit"

            val card1 = @Composable {
                MetricDetailCard(
                    icon = Icons.Default.WaterDrop,
                    title = stringResource(R.string.forecast_relative_humidity),
                    value = "${current.humidityPercent}%",
                    detail = stringResource(R.string.forecast_dew_point_detail, dewStr),
                    accentColor = SleekBluePrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val card2 = @Composable {
                MetricDetailCard(
                    icon = Icons.Default.Compress,
                    title = stringResource(R.string.forecast_surface_pressure),
                    value = pressStr,
                    detail = if (current.pressureHpa > 1013) stringResource(R.string.forecast_high_pressure) else stringResource(R.string.forecast_low_pressure),
                    accentColor = SleekBluePrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val card3 = @Composable {
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val card4 = @Composable {
                MetricDetailCard(
                    icon = Icons.Default.Air,
                    title = stringResource(R.string.forecast_wind_gust_vector),
                    value = windSpeedStr,
                    detail = stringResource(R.string.forecast_peak_gusts, peakGustStr),
                    accentColor = SleekGreenText,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val card5 = @Composable {
                MetricDetailCard(
                    icon = Icons.Default.Visibility,
                    title = stringResource(R.string.forecast_atmospheric_visibility),
                    value = visStr,
                    detail = if (current.visibilityKm >= 10) stringResource(R.string.forecast_clear_horizon) else stringResource(R.string.forecast_reduced_visibility),
                    accentColor = SleekTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val card6 = @Composable {
                MetricDetailCard(
                    icon = Icons.Default.FilterDrama,
                    title = stringResource(R.string.forecast_cloud_cover),
                    value = "${current.cloudCoverPercent}%",
                    detail = stringResource(R.string.forecast_sky_observation),
                    accentColor = SleekBluePrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isCompact) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) { card1() }
                        Box(modifier = Modifier.weight(1f)) { card2() }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) { card3() }
                        Box(modifier = Modifier.weight(1f)) { card4() }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) { card5() }
                        Box(modifier = Modifier.weight(1f)) { card6() }
                    }
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { card1() }
                        Box(modifier = Modifier.weight(1f)) { card2() }
                        Box(modifier = Modifier.weight(1f)) { card3() }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { card4() }
                        Box(modifier = Modifier.weight(1f)) { card5() }
                        Box(modifier = Modifier.weight(1f)) { card6() }
                    }
                }
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
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.semantics { heading() }
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
                    LottieWeatherAnimation(
                        weatherCode = pt.weatherCode,
                        isDay = isHourDaytime(pt.timeLabel),
                        size = 22.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = stringResource(WeatherRepository.getWeatherDescriptionRes(pt.weatherCode)), color = SleekTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompact = maxWidth < 560.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.forecast_14day_extended),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.semantics { heading() }
                )
            }

            items(pkg.dailyDays) { day ->
                val maxT = if (unitSystem == UnitSystem.METRIC) "${day.maxTempC.roundToInt()}°"
                else "${(day.maxTempC * 9 / 5 + 32).roundToInt()}°"

                val minT = if (unitSystem == UnitSystem.METRIC) "${day.minTempC.roundToInt()}°"
                else "${(day.minTempC * 9 / 5 + 32).roundToInt()}°"

                if (isCompact) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SleekSurface, RoundedCornerShape(14.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                LottieWeatherAnimation(weatherCode = day.weatherCode, size = 26.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${day.dayOfWeek} • ${day.dateLabel}",
                                        color = SleekTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(WeatherRepository.getWeatherDescriptionRes(day.weatherCode)),
                                        color = SleekTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = minT, color = SleekBluePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(SleekBlueContainer, SolarGold, SevereRed.copy(alpha = 0.8f))
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = maxT, color = SleekTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.forecast_rain, day.popPercent),
                                color = SleekBluePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (day.rainMm > 0) {
                                val rainUnit = if (unitSystem == UnitSystem.METRIC) stringResource(R.string.unit_mm) else stringResource(R.string.unit_in)
                                val rainValue = if (unitSystem == UnitSystem.METRIC) {
                                    String.format(java.util.Locale.US, "%.1f", day.rainMm)
                                } else {
                                    String.format(java.util.Locale.US, "%.2f", day.rainMm * 0.0393701)
                                }
                                Text(text = "$rainValue $rainUnit", color = SleekTextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SleekSurface, RoundedCornerShape(14.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(120.dp)) {
                            Text(text = day.dayOfWeek, color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = day.dateLabel, color = SleekTextSecondary, fontSize = 10.sp)
                        }

                        Row(modifier = Modifier.width(180.dp), verticalAlignment = Alignment.CenterVertically) {
                            LottieWeatherAnimation(weatherCode = day.weatherCode, size = 26.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(WeatherRepository.getWeatherDescriptionRes(day.weatherCode)),
                                color = SleekTextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(modifier = Modifier.width(100.dp)) {
                            Text(text = stringResource(R.string.forecast_rain, day.popPercent), color = SleekBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (day.rainMm > 0) {
                                val rainUnit = if (unitSystem == UnitSystem.METRIC) stringResource(R.string.unit_mm) else stringResource(R.string.unit_in)
                                val rainValue = if (unitSystem == UnitSystem.METRIC) {
                                    String.format(java.util.Locale.US, "%.1f", day.rainMm)
                                } else {
                                    String.format(java.util.Locale.US, "%.2f", day.rainMm * 0.0393701)
                                }
                                Text(text = "$rainValue $rainUnit", color = SleekTextSecondary, fontSize = 10.sp)
                            }
                        }

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
            .semantics(mergeDescendants = true) {
                contentDescription = "$title: $value, $detail"
            }
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
