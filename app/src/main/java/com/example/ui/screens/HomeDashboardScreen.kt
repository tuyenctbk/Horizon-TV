package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.WeatherRepository
import com.example.ui.components.AstronomicalWidget
import com.example.ui.components.RealTimeTelemetryVisualizer
import com.example.ui.components.RechartsFiveDayForecastView
import com.example.ui.components.WeatherConditionIcon
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HomeDashboardScreen(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    weatherPackage: FullWeatherPackage?,
    favoriteCities: List<CityLocation>,
    unitSystem: UnitSystem,
    vodStories: List<VodStory>,
    onSelectCity: (CityLocation) -> Unit,
    onNavigate: (TVScreen) -> Unit,
    onPlayVodStory: (VodStory) -> Unit,
    onOpenAlertModal: (SevereWeatherAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        if (currentWeather == null || weatherPackage == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = HorizonCyan)
            }
            return
        }

        // Active Emergency Alert Ticker (if present)
        if (weatherPackage.alerts.isNotEmpty()) {
            val alert = weatherPackage.alerts.first()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekRedContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekRedText.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlertModal(alert) }
                    .padding(bottom = 12.dp)
                    .testTag("emergency_alert_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = SleekRedText, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stringResource(R.string.emergency_broadcast)}: ${alert.title}",
                            color = SleekRedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = alert.summary,
                            color = SleekTextPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_view_protocol),
                        color = SleekBluePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Primary Grid: Left = Live TV Hero Player Card, Right = Micro-Climate 36H Snapshot
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 680.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LiveTvHeroCard(
                        currentWeather = currentWeather,
                        weatherPackage = weatherPackage,
                        onNavigate = onNavigate,
                        modifier = Modifier.fillMaxWidth()
                    )
                    MicroClimateSnapshotCard(
                        currentWeather = currentWeather,
                        weatherPackage = weatherPackage,
                        unitSystem = unitSystem,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LiveTvHeroCard(
                        currentWeather = currentWeather,
                        weatherPackage = weatherPackage,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1.1f)
                    )
                    MicroClimateSnapshotCard(
                        currentWeather = currentWeather,
                        weatherPackage = weatherPackage,
                        unitSystem = unitSystem,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Atmospheric Sensor Telemetry Visualizer (Micro-sensor periodic stream)
        RealTimeTelemetryVisualizer(
            currentWeather = currentWeather,
            unitSystem = unitSystem
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5-Day Weather Forecast Visualization Component (Recharts Style)
        RechartsFiveDayForecastView(
            dailyForecast = weatherPackage.dailyDays,
            unitSystem = unitSystem
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Shortcuts & Action Cards: Radar & Traffic, Highway Flow
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 650.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionShortcutCard(
                        title = stringResource(R.string.home_card_radar_title),
                        subtitle = stringResource(R.string.home_card_radar_desc),
                        badge = stringResource(R.string.home_badge_layers),
                        icon = Icons.Default.Radar,
                        accentColor = HorizonCyan,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(TVScreen.RADAR) }
                    )
                    ActionShortcutCard(
                        title = stringResource(R.string.home_card_forecast_title),
                        subtitle = stringResource(R.string.home_card_forecast_desc),
                        badge = stringResource(R.string.home_badge_days),
                        icon = Icons.Default.Timeline,
                        accentColor = SolarGold,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(TVScreen.FORECAST) }
                    )
                    ActionShortcutCard(
                        title = stringResource(R.string.home_card_vod_title),
                        subtitle = stringResource(R.string.home_card_vod_desc),
                        badge = stringResource(R.string.home_badge_vod),
                        icon = Icons.Default.VideoLibrary,
                        accentColor = SevereRed,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(TVScreen.VOD) }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionShortcutCard(
                        title = stringResource(R.string.home_card_radar_title),
                        subtitle = stringResource(R.string.home_card_radar_desc),
                        badge = stringResource(R.string.home_badge_layers),
                        icon = Icons.Default.Radar,
                        accentColor = HorizonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(TVScreen.RADAR) }
                    )
                    ActionShortcutCard(
                        title = stringResource(R.string.home_card_forecast_title),
                        subtitle = stringResource(R.string.home_card_forecast_desc),
                        badge = stringResource(R.string.home_badge_days),
                        icon = Icons.Default.Timeline,
                        accentColor = SolarGold,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(TVScreen.FORECAST) }
                    )
                    ActionShortcutCard(
                        title = stringResource(R.string.home_card_vod_title),
                        subtitle = stringResource(R.string.home_card_vod_desc),
                        badge = stringResource(R.string.home_badge_vod),
                        icon = Icons.Default.VideoLibrary,
                        accentColor = SevereRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(TVScreen.VOD) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Astronomical Horizons Widget (Solar Arc & Moon Phase)
        AstronomicalWidget(astronomical = weatherPackage.astronomical)

        Spacer(modifier = Modifier.height(16.dp))

        // Bookmarked Favorite Cities Quick Carousel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekSurface, RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Favorites", tint = SolarGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_carousel_title),
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = stringResource(R.string.home_carousel_switch),
                    color = SleekBluePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                favoriteCities.forEach { city ->
                    val isSelected = city.name.equals(currentLocation.name, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) SleekBlueContainer else SleekSurfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) SleekBluePrimary else SleekBorder
                        ),
                        modifier = Modifier
                            .clickable { onSelectCity(city) }
                            .width(170.dp)
                            .testTag("fav_city_${city.name}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city.name,
                                    color = if (isSelected) SleekOnBlueContainer else SleekTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (isSelected) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekBluePrimary))
                                }
                            }
                            Text(
                                text = "${city.region.take(12)} • ${city.countryCode}",
                                color = if (isSelected) SleekBluePrimary else SleekTextSecondary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Coordinates: ${String.format(java.util.Locale.US, "%.1f, %.1f", city.latitude, city.longitude)}",
                                color = SleekTextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trending Dispatches Preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekSurface, RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_featured_stories),
                    color = SleekTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = stringResource(R.string.home_all_stories),
                    color = SleekBluePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigate(TVScreen.VOD) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                vodStories.take(2).forEach { story ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                            .clickable { onPlayVodStory(story) }
                            .padding(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color(story.accentColorHex).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = story.category.uppercase(),
                                color = Color(story.accentColorHex),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = story.title,
                            color = SleekTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${story.presenter} • ${story.duration}",
                            color = SleekTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(SleekSurfaceSecondary, RoundedCornerShape(12.dp))
            .border(1.dp, SleekBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionShortcutCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = SleekBlueContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = SleekOnBlueContainer, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(percent = 50), color = SleekBlueContainer) {
                        Text(
                            text = badge,
                            color = SleekOnBlueContainer,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = title,
                    color = SleekTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = SleekTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveTvHeroCard(
    currentWeather: CurrentWeather,
    weatherPackage: FullWeatherPackage,
    onNavigate: (TVScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = SleekGreenContainer) {
                    Text(
                        stringResource(R.string.home_live_badge),
                        color = SleekGreenText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.home_studio_center),
                    color = SleekTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.Tv, contentDescription = "Live TV", tint = SleekBluePrimary, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SleekDarkContainer)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                .clickable { onNavigate(TVScreen.LIVE_TV) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                WeatherConditionIcon(weatherCode = currentWeather.weatherCode, isDay = currentWeather.isDay, size = 48.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_meteorologist_duty),
                    color = SleekBlueContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = stringResource(R.string.home_continuous_broadcast),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = SleekBluePrimary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.home_switch_broadcast), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_presented_by),
                    color = Color(0xFFD4D4D4),
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (weatherPackage.headlines.isNotEmpty()) {
            Text(
                text = "${stringResource(R.string.home_news_ticker_prefix)}: ${weatherPackage.headlines.first()}",
                color = SleekBluePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun MicroClimateSnapshotCard(
    currentWeather: CurrentWeather,
    weatherPackage: FullWeatherPackage,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_local_microclimate),
                color = SleekTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SleekGreenContainer
            ) {
                Text(
                    text = stringResource(R.string.home_now_badge),
                    color = SleekGreenText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayTemp = if (unitSystem == UnitSystem.METRIC)
                "${currentWeather.tempC.roundToInt()}°C"
            else
                "${(currentWeather.tempC * 9 / 5 + 32).roundToInt()}°F"

            val displayFeels = if (unitSystem == UnitSystem.METRIC)
                "${currentWeather.feelsLikeC.roundToInt()}°C"
            else
                "${(currentWeather.feelsLikeC * 9 / 5 + 32).roundToInt()}°F"

            Column {
                Text(
                    text = displayTemp,
                    color = SleekTextPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = stringResource(R.string.home_feels_like, displayFeels),
                    color = SleekBluePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentWeather.conditionText.uppercase(),
                    color = SolarGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            WeatherConditionIcon(
                weatherCode = currentWeather.weatherCode,
                isDay = currentWeather.isDay,
                size = 60.dp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val windUnit = if (unitSystem == UnitSystem.METRIC) stringResource(R.string.unit_kmh) else stringResource(R.string.unit_mph)
            val windVal = if (unitSystem == UnitSystem.METRIC) currentWeather.windSpeedKmh.roundToInt()
            else (currentWeather.windSpeedKmh * 0.621371).roundToInt()

            TelemetryMiniChip(
                label = stringResource(R.string.home_wind_label),
                value = "$windVal $windUnit ${WeatherRepository.getWindCompass(currentWeather.windDirectionDeg)}"
            )
            TelemetryMiniChip(
                label = stringResource(R.string.home_humidity_label),
                value = "${currentWeather.humidityPercent}%"
            )
            TelemetryMiniChip(
                label = stringResource(R.string.home_aqi_label),
                value = "${currentWeather.aqi} (${currentWeather.aqiStatus})"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.home_diurnal_breakdown),
            color = SleekTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weatherPackage.diurnalPeriods.forEach { period ->
                val periodTemp = if (unitSystem == UnitSystem.METRIC) "${period.tempC.roundToInt()}°"
                else "${(period.tempC * 9 / 5 + 32).roundToInt()}°"

                val isCurr = period.isCurrent
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isCurr) SleekBlueContainer else SleekSurfaceSecondary,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isCurr) SleekBluePrimary.copy(alpha = 0.3f) else SleekBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = period.periodName.take(3).uppercase(),
                        color = if (isCurr) SleekOnBlueContainer else SleekTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    WeatherConditionIcon(weatherCode = period.weatherCode, size = 18.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = periodTemp,
                        color = if (isCurr) SleekOnBlueContainer else SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.home_rain_chance, period.popPercent),
                        color = SleekBluePrimary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
