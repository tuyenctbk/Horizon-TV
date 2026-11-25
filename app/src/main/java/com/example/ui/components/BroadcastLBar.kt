package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.HourlyPoint
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.UnitSystem
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Reusable 24/7 News Channel Broadcast L-Bar component rendered at the bottom of the screen.
 * Combines:
 * 1) Left Station Bug & Live Digital Time/Date Module with current city temperature.
 * 2) Upper Crawling Meteorological News & Breaking Alert Ticker.
 * 3) Lower 12-Hour Synoptic Forecast Progression Strip + optional left-sidebar toggle.
 */
@Composable
fun BroadcastLBar(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    headlines: List<String>,
    hourlyPoints: List<HourlyPoint>,
    unitSystem: UnitSystem,
    activeAlerts: List<SevereWeatherAlert> = emptyList(),
    showSidebarToggle: Boolean = false,
    isSidebarExpanded: Boolean = false,
    onToggleSidebar: (() -> Unit)? = null,
    onAlertClick: ((SevereWeatherAlert) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var liveTimeText by remember { mutableStateOf("") }
    var liveDateZoneText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            val locale = Locale.getDefault()
            liveTimeText = SimpleDateFormat("HH:mm:ss", locale).format(now)
            liveDateZoneText = SimpleDateFormat("EEE d MMM • z", locale).format(now).uppercase(locale)
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "broadcastLBarPulse")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    val primaryAlert = activeAlerts.firstOrNull()
    val hasActiveAlert = primaryAlert != null

    val tempFormatted = remember(currentWeather, unitSystem) {
        if (currentWeather == null) "--°"
        else if (unitSystem == UnitSystem.METRIC) "${currentWeather.tempC.roundToInt()}°C"
        else "${(currentWeather.tempC * 9.0 / 5.0 + 32.0).roundToInt()}°F"
    }

    val channelLiveLabel = stringResource(R.string.broadcast_lbar_channel_live)
    val breakingOrDispatchLabel = stringResource(
        if (hasActiveAlert) R.string.broadcast_lbar_breaking
        else R.string.broadcast_lbar_dispatch
    )

    Surface(
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (hasActiveAlert) SevereRed.copy(alpha = 0.65f) else SleekBorder
        ),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("broadcast_lbar_component")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT MODULE: 24/7 Station Callsign, Live Ticking Time Display & City Bug
            Column(
                modifier = Modifier
                    .widthIn(min = 132.dp, max = 176.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = if (hasActiveAlert) {
                                listOf(Color(0xFF450A0A), Color(0xFF1E0505))
                            } else {
                                listOf(Color(0xFF0B1936), Color(0xFF060E20))
                            }
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$channelLiveLabel, $liveTimeText, ${currentLocation.name}, $tempFormatted"
                    }
                    .testTag("broadcast_lbar_clock_box"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SevereRed.copy(alpha = liveDotAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = channelLiveLabel,
                            color = BroadcastCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Live Digital Clock
                Text(
                    text = liveTimeText,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.testTag("broadcast_lbar_time_text")
                )

                // Date + Timezone & City Temp Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentLocation.name.uppercase(Locale.getDefault()),
                        color = Color(0xFFCBD5E1),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentWeather != null) {
                            LottieWeatherAnimation(
                                weatherCode = currentWeather.weatherCode,
                                isDay = currentWeather.isDay,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = tempFormatted,
                            color = SolarGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            VerticalDivider(
                color = if (hasActiveAlert) SevereRed.copy(alpha = 0.5f) else SleekBorder,
                modifier = Modifier.fillMaxHeight()
            )

            // RIGHT DUAL-DECK MODULE: Upper Crawling News Ticker + Lower 12H Synoptic Strip
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // UPPER DECK: Crawling Meteorological News Ticker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekSurfaceSecondary)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (hasActiveAlert) SevereRed else SleekBluePrimary,
                        modifier = if (primaryAlert != null && onAlertClick != null) {
                            Modifier
                                .tvFocusVisual(shape = RoundedCornerShape(4.dp), focusBorderColor = BroadcastAmber)
                                .clickable(role = Role.Button) { onAlertClick(primaryAlert) }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = "$breakingOrDispatchLabel: ${primaryAlert.title}"
                                }
                        } else {
                            Modifier.semantics(mergeDescendants = true) {
                                contentDescription = breakingOrDispatchLabel
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            if (hasActiveAlert) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = breakingOrDispatchLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = breakingOrDispatchLabel,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    val separator = stringResource(R.string.broadcast_lbar_ticker_separator)
                    val defaultTicker = stringResource(R.string.lbar_default_ticker)
                    val tickerContent = remember(headlines, separator, defaultTicker) {
                        if (headlines.isNotEmpty()) headlines.joinToString(separator)
                        else defaultTicker
                    }

                    Text(
                        text = tickerContent,
                        color = SleekTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = "$breakingOrDispatchLabel: $tickerContent"
                            }
                            .testTag("broadcast_lbar_ticker_marquee")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = liveDateZoneText,
                        color = SleekTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // LOWER DECK: 12-Hour Synoptic Weather Progression + Optional L-Bar Sidebar Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SleekBlueContainer
                    ) {
                        Text(
                            text = stringResource(R.string.broadcast_lbar_hourly_label),
                            color = SleekBluePrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        hourlyPoints.take(12).forEach { pt ->
                            val tempLabel = if (unitSystem == UnitSystem.METRIC) {
                                "${pt.tempC.roundToInt()}°"
                            } else {
                                "${(pt.tempC * 9.0 / 5.0 + 32.0).roundToInt()}°"
                            }
                            val condName = stringResource(WeatherRepository.getWeatherDescriptionRes(pt.weatherCode))
                            val rainDesc = if (pt.popPercent >= 20) ", ${stringResource(R.string.forecast_rain, pt.popPercent)}" else ""

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = "${pt.timeLabel}, $condName, $tempLabel$rainDesc"
                                }
                            ) {
                                Text(
                                    text = pt.timeLabel,
                                    color = SleekTextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                LottieWeatherAnimation(
                                    weatherCode = pt.weatherCode,
                                    isDay = isHourDaytime(pt.timeLabel),
                                    size = 18.dp
                                )
                                Text(
                                    text = tempLabel,
                                    color = SleekTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (pt.popPercent >= 20) {
                                    Text(
                                        text = "${pt.popPercent}%",
                                        color = SleekBluePrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (showSidebarToggle && onToggleSidebar != null) {
                        val toggleLabel = stringResource(R.string.broadcast_lbar_sidebar_toggle)
                        val toggleStateLabel = stringResource(
                            if (isSidebarExpanded) R.string.lbar_collapse else R.string.lbar_expand
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isSidebarExpanded) SleekBlueContainer else SleekSurfaceSecondary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier
                                .tvFocusVisual(shape = RoundedCornerShape(percent = 50), focusBorderColor = SleekBluePrimary)
                                .clickable(role = Role.Button) { onToggleSidebar() }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = toggleLabel
                                    stateDescription = toggleStateLabel
                                }
                                .testTag("broadcast_lbar_sidebar_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSidebarExpanded) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                                    contentDescription = toggleLabel,
                                    tint = SleekBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = toggleLabel,
                                    color = SleekBluePrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
