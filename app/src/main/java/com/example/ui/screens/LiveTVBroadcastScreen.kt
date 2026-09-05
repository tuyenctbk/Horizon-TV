package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.WeatherRepository
import com.example.ui.components.WeatherConditionIcon
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LiveTVBroadcastScreen(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    weatherPackage: FullWeatherPackage?,
    unitSystem: UnitSystem,
    isAudioPlaying: Boolean,
    onToggleAudio: () -> Unit,
    onTogglePip: () -> Unit,
    onOpenAlertModal: (SevereWeatherAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentWeather == null || weatherPackage == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HorizonCyan)
        }
        return
    }

    var liveClockString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm:ss z", Locale.US)
        while (true) {
            liveClockString = timeFmt.format(Date())
            delay(1000)
        }
    }

    // Radar beam animation inside TV broadcast window
    val infiniteTransition = rememberInfiniteTransition(label = "broadcastRadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    // Crawler offset for news ticker
    val tickerTransition = rememberInfiniteTransition(label = "tickerAnimation")
    val tickerOffset by tickerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tickerOffset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .testTag("live_tv_broadcast_view")
    ) {
        // Main Area: Authentic L-Bar on Left + Video Broadcast Stream on Right
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // L-BAR LEFT SIDEBAR (The Iconic Weather Channel / TV Network L-Bar - Sleek Theme)
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .fillMaxHeight()
                    .background(SleekSurface)
                    .border(1.dp, SleekBorder)
                    .padding(14.dp)
            ) {
                // Station Callsign & Live Clock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(4.dp), color = SleekRedContainer) {
                            Text(
                                text = stringResource(R.string.common_live),
                                color = SleekRedText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
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
                    Text(
                        text = stringResource(R.string.lbar_channel_tag),
                        color = SolarGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = liveClockString,
                    color = SleekTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(color = SleekBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Current City & Temperature Big Display
                Text(
                    text = currentLocation.name.uppercase(),
                    color = SleekTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentLocation.region.uppercase()} • ${currentLocation.countryCode}",
                    color = SleekTextSecondary,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = stringResource(R.string.home_feels_like, feelsStr),
                            color = SleekBluePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    WeatherConditionIcon(weatherCode = currentWeather.weatherCode, isDay = currentWeather.isDay, size = 42.dp)
                }

                Text(
                    text = currentWeather.conditionText.uppercase(),
                    color = SolarGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Current Metrics Rows
                LBarMetricRow(label = stringResource(R.string.home_wind), value = "${currentWeather.windSpeedKmh.roundToInt()} km/h ${WeatherRepository.getWindCompass(currentWeather.windDirectionDeg)}")
                LBarMetricRow(label = stringResource(R.string.home_humidity), value = "${currentWeather.humidityPercent}%")
                LBarMetricRow(label = stringResource(R.string.home_barometer), value = "${currentWeather.pressureHpa.roundToInt()} hPa")
                LBarMetricRow(label = stringResource(R.string.home_dew_point), value = "${currentWeather.dewPointC.roundToInt()}°")
                LBarMetricRow(label = stringResource(R.string.home_air_quality), value = "${currentWeather.aqi} (${currentWeather.aqiStatus})")

                HorizontalDivider(color = SleekBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Diurnal Forecasts (Afternoon, Evening, Night)
                Text(
                    text = stringResource(R.string.livetv_next_periods),
                    color = SleekBluePrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                weatherPackage.diurnalPeriods.take(3).forEach { p ->
                    val pTemp = if (unitSystem == UnitSystem.METRIC) "${p.tempC.roundToInt()}°"
                    else "${(p.tempC * 9 / 5 + 32).roundToInt()}°"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .background(SleekSurfaceSecondary, RoundedCornerShape(8.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = p.periodName, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.forecast_rain, p.popPercent), color = SleekBluePrimary, fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeatherConditionIcon(weatherCode = p.weatherCode, size = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = pTemp, color = SleekTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // RIGHT: LIVE BROADCAST MAIN STAGE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF030712))
            ) {
                // Live Radar Simulation Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // Deep atmospheric gradient
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0B1936), Color(0xFF040A18), Color(0xFF01040A)),
                            center = Offset(cx, cy),
                            radius = maxOf(w, h) * 0.7f
                        )
                    )

                    // Concentric Radar Range Rings
                    listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { fraction ->
                        drawCircle(
                            color = HorizonCyan.copy(alpha = 0.2f),
                            radius = (minOf(w, h) / 2f) * fraction,
                            center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                        )
                    }

                    // Crosshair vectors
                    drawLine(
                        color = HorizonCyan.copy(alpha = 0.15f),
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = HorizonCyan.copy(alpha = 0.15f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1f
                    )

                    // Active Sweep Line
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val sweepLength = minOf(w, h) * 0.45f
                    val endX = cx + (sweepLength * cos(rad)).toFloat()
                    val endY = cy + (sweepLength * sin(rad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(HorizonCyan, Color.Transparent),
                            start = Offset(cx, cy),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )

                    // Simulated weather storm cells on screen
                    drawCircle(color = RadarGreen.copy(alpha = 0.4f), radius = 45f, center = Offset(cx + 120f, cy - 80f))
                    drawCircle(color = SolarGold.copy(alpha = 0.45f), radius = 25f, center = Offset(cx + 130f, cy - 75f))
                    drawCircle(color = SevereRed.copy(alpha = 0.5f), radius = 12f, center = Offset(cx + 135f, cy - 72f))

                    drawCircle(color = RadarGreen.copy(alpha = 0.35f), radius = 60f, center = Offset(cx - 160f, cy + 90f))
                    drawCircle(color = SolarGold.copy(alpha = 0.35f), radius = 30f, center = Offset(cx - 150f, cy + 95f))
                }

                // Broadcast HUD Overlays
                // Top-Left Bug
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SevereRed
                    ) {
                        Text(
                            text = stringResource(R.string.livetv_live_broadcast),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.livetv_doppler_specs),
                        color = HorizonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top-Right Broadcast Controls (Mute & PiP)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TVCardBorder),
                        modifier = Modifier.clickable { onToggleAudio() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = stringResource(R.string.home_audio),
                                tint = if (isAudioPlaying) HorizonCyan else TextSecondarySilver,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAudioPlaying) stringResource(R.string.livetv_audio_on) else stringResource(R.string.livetv_muted),
                                color = TextPrimaryWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TVCardBorder),
                        modifier = Modifier.clickable { onTogglePip() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureInPictureAlt,
                                contentDescription = stringResource(R.string.livetv_pip_mode),
                                tint = HorizonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.livetv_pip_mode), color = TextPrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom Anchor / Meteorologist Title Card
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .border(1.dp, TVCardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.livetv_presenter_name),
                        color = SolarGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stringResource(R.string.livetv_presenter_subtitle),
                        color = TextPrimaryWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // BOTTOM NEWS CRAWL TICKER & 12-HOUR TIMELINE SCRUBBER (Sleek Styling)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekSurface)
                .border(1.dp, SleekBorder)
        ) {
            // News Crawl Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurfaceSecondary)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = SleekBluePrimary
                ) {
                    Text(
                        text = stringResource(R.string.lbar_horizon_dispatch),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                val headlineText = weatherPackage.headlines.joinToString("   ■   ")
                Text(
                    text = headlineText,
                    color = SleekTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            // 12-Hour Timeline Scrubber along bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weatherPackage.hourlyPoints.take(8).forEach { pt ->
                    val t = if (unitSystem == UnitSystem.METRIC) "${pt.tempC.roundToInt()}°"
                    else "${(pt.tempC * 9 / 5 + 32).roundToInt()}°"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = pt.timeLabel, color = SleekTextSecondary, fontSize = 10.sp)
                        WeatherConditionIcon(weatherCode = pt.weatherCode, size = 16.dp)
                        Text(text = t, color = SleekTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LBarMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SleekTextSecondary, fontSize = 11.sp)
        Text(text = value, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
