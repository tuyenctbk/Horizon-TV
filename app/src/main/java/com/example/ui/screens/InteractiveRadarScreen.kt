package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CityLocation
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveRadarScreen(
    currentLocation: CityLocation,
    selectedLayer: Int,
    isPlaying: Boolean,
    frameIndex: Int,
    onSelectLayer: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSelectFrame: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Interactive Map Pan and Zoom State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var inspectedPoint by remember { mutableStateOf<Offset?>(null) }

    // Dynamic 7 timeline labels (-30m, -20m, -10m, NOW, +10m, +20m, +30m)
    val timeLabels = remember {
        val now = System.currentTimeMillis()
        val offsets = listOf(-30, -20, -10, 0, 10, 20, 30)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        offsets.mapIndexed { idx, minOffset ->
            val t = now + minOffset * 60 * 1000L
            if (idx == 3) "NOW (${timeFmt.format(Date(t))})" else timeFmt.format(Date(t))
        }
    }

    // Animation sweep angle
    val infiniteTransition = rememberInfiniteTransition(label = "radarAnimation")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val streamlineShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "streamlineShift"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
            .padding(16.dp)
            .testTag("interactive_radar_view")
    ) {
        // Top Radar Controls & Layer Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekGreenText))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.radar_title),
                        color = SleekTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                }
                Text(
                    text = stringResource(R.string.radar_subtitle, currentLocation.name),
                    color = SleekTextSecondary,
                    fontSize = 12.sp
                )
            }

            // Layer Switcher Buttons
            Row(
                modifier = Modifier
                    .background(SleekSurfaceSecondary, RoundedCornerShape(percent = 50))
                    .border(1.dp, SleekBorder, RoundedCornerShape(percent = 50))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadarLayerButton(title = stringResource(R.string.radar_layer_precip), isSelected = selectedLayer == 0) { onSelectLayer(0) }
                RadarLayerButton(title = stringResource(R.string.radar_layer_traffic), isSelected = selectedLayer == 1) { onSelectLayer(1) }
                RadarLayerButton(title = stringResource(R.string.radar_layer_satellite), isSelected = selectedLayer == 2) { onSelectLayer(2) }
                RadarLayerButton(title = stringResource(R.string.radar_layer_wind), isSelected = selectedLayer == 3) { onSelectLayer(3) }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Doppler Radar Stage with Pan & Zoom Gestures
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF060B18))
                .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.6f, 4.0f)
                        panOffset = Offset(
                            x = (panOffset.x + pan.x).coerceIn(-600f, 600f),
                            y = (panOffset.y + pan.y).coerceIn(-600f, 600f)
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = (w / 2f) + panOffset.x
                val cy = (h / 2f) + panOffset.y

                // Deep cartographic grid background
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF09162E), Color(0xFF040A18), Color(0xFF02050D)),
                        center = Offset(cx, cy),
                        radius = maxOf(w, h) * 0.7f * zoomScale
                    )
                )

                // Concentric Range Rings (25km, 50km, 100km, 150km, 200km)
                val maxRadius = minOf(w, h) * 0.45f * zoomScale
                val ringFractions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                ringFractions.forEach { fraction ->
                    drawCircle(
                        color = HorizonCyan.copy(alpha = 0.25f),
                        radius = maxRadius * fraction,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )
                }

                // Crosshairs
                drawLine(
                    color = HorizonCyan.copy(alpha = 0.2f),
                    start = Offset(0f, cy),
                    end = Offset(w, cy),
                    strokeWidth = 1f
                )
                drawLine(
                    color = HorizonCyan.copy(alpha = 0.2f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, h),
                    strokeWidth = 1f
                )

                // LAYER 0: PRECIPITATION RADAR (Reflectivity dBZ)
                if (selectedLayer == 0) {
                    val shift = (frameIndex - 3) * 20f * zoomScale

                    // Convective storm cell 1 (Severe Core)
                    val s1x = cx + (80f * zoomScale) + shift
                    val s1y = cy - (60f * zoomScale) + shift * 0.4f
                    drawCircle(color = RadarGreen.copy(alpha = 0.55f), radius = 85f * zoomScale, center = Offset(s1x, s1y))
                    drawCircle(color = SolarGold.copy(alpha = 0.65f), radius = 48f * zoomScale, center = Offset(s1x + 10f * zoomScale, s1y + 8f * zoomScale))
                    drawCircle(color = SevereRed.copy(alpha = 0.75f), radius = 24f * zoomScale, center = Offset(s1x + 14f * zoomScale, s1y + 12f * zoomScale))
                    drawCircle(color = StormPurple.copy(alpha = 0.85f), radius = 12f * zoomScale, center = Offset(s1x + 16f * zoomScale, s1y + 14f * zoomScale))

                    // Secondary rain band & squall line
                    val s2x = cx - (140f * zoomScale) + shift
                    val s2y = cy + (80f * zoomScale) - shift * 0.3f
                    drawCircle(color = RadarGreen.copy(alpha = 0.45f), radius = 115f * zoomScale, center = Offset(s2x, s2y))
                    drawCircle(color = SolarGold.copy(alpha = 0.5f), radius = 55f * zoomScale, center = Offset(s2x + 10f * zoomScale, s2y + 10f * zoomScale))

                    // Storm Motion Vector Tracking Arrow
                    val arrowStart = Offset(s1x, s1y)
                    val arrowEnd = Offset(s1x + 60f * zoomScale, s1y - 30f * zoomScale)
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = arrowStart,
                        end = arrowEnd,
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                    )
                    drawCircle(color = Color.White, radius = 3.5f * zoomScale, center = arrowEnd)

                    // Simulated Lightning Strike coordinates (flashing pulses)
                    val strike1 = Offset(s1x + 15f * zoomScale, s1y + 15f * zoomScale)
                    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 6f * zoomScale, center = strike1)
                    drawCircle(color = HorizonCyan, radius = 3f * zoomScale, center = strike1)

                    // Radar sweep beam
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = cx + (maxRadius * cos(rad)).toFloat()
                    val endY = cy + (maxRadius * sin(rad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(HorizonCyan, Color.Transparent),
                            start = Offset(cx, cy),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f
                    )
                }

                // LAYER 1: HIGHWAY TRAFFIC CORRIDORS
                if (selectedLayer == 1) {
                    val highwayPath1 = Path()
                    highwayPath1.moveTo(cx - 240f * zoomScale, cy - 160f * zoomScale)
                    highwayPath1.cubicTo(cx - 100f * zoomScale, cy - 80f * zoomScale, cx + 50f * zoomScale, cy + 40f * zoomScale, cx + 260f * zoomScale, cy + 180f * zoomScale)
                    drawPath(path = highwayPath1, color = Color(0xFF22C55E), style = Stroke(width = 6f * zoomScale))

                    val beltway = Path()
                    beltway.moveTo(cx - 180f * zoomScale, cy + 120f * zoomScale)
                    beltway.cubicTo(cx - 80f * zoomScale, cy + 20f * zoomScale, cx + 120f * zoomScale, cy - 40f * zoomScale, cx + 220f * zoomScale, cy - 140f * zoomScale)
                    drawPath(path = beltway, color = Color(0xFFF59E0B), style = Stroke(width = 6f * zoomScale))

                    val congested = Path()
                    congested.moveTo(cx - 60f * zoomScale, cy - 120f * zoomScale)
                    congested.lineTo(cx + 40f * zoomScale, cy - 30f * zoomScale)
                    drawPath(path = congested, color = Color(0xFFEF4444), style = Stroke(width = 7f * zoomScale))

                    listOf(Offset(cx - 100f * zoomScale, cy - 80f * zoomScale), Offset(cx + 40f * zoomScale, cy - 30f * zoomScale), Offset(cx + 50f * zoomScale, cy + 40f * zoomScale)).forEach {
                        drawCircle(color = Color.White, radius = 5f * zoomScale, center = it)
                        drawCircle(color = Color(0xFF0F172A), radius = 3f * zoomScale, center = it)
                    }
                }

                // LAYER 2: SATELLITE INFRARED (IR)
                if (selectedLayer == 2) {
                    drawCircle(color = Color(0x33FFFFFF), radius = 220f * zoomScale, center = Offset(cx - 40f * zoomScale, cy - 20f * zoomScale))
                    drawCircle(color = Color(0x4494A3B8), radius = 140f * zoomScale, center = Offset(cx + 60f * zoomScale, cy - 80f * zoomScale))
                    drawCircle(color = Color(0x5538BDF8), radius = 80f * zoomScale, center = Offset(cx + 80f * zoomScale, cy - 70f * zoomScale))
                    drawCircle(color = Color(0x66F43F5E), radius = 40f * zoomScale, center = Offset(cx + 90f * zoomScale, cy - 65f * zoomScale))
                }

                // LAYER 3: WIND STREAMLINES
                if (selectedLayer == 3) {
                    val rows = 6
                    val cols = 8
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val startX = (c / cols.toFloat()) * w + (streamlineShift * 40f) % (w / cols.toFloat())
                            val startY = (r / rows.toFloat()) * h + 30f
                            val angleRad = Math.toRadians(45.0)
                            val len = 35f * zoomScale
                            val endX = startX + (len * cos(angleRad)).toFloat()
                            val endY = startY + (len * sin(angleRad)).toFloat()

                            drawLine(
                                color = HorizonCyan.copy(alpha = 0.6f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 2f
                            )
                            drawCircle(color = HorizonCyan, radius = 2f, center = Offset(endX, endY))
                        }
                    }
                }

                // Center Location Anchor Pin
                drawCircle(color = HorizonCyan, radius = 8f * zoomScale, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = 4f * zoomScale, center = Offset(cx, cy))
            }

            // Top-Left Floating Map Controls: Zoom In (+), Zoom Out (-), Recenter
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { zoomScale = (zoomScale * 1.3f).coerceAtMost(4.0f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.radar_zoom_in), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.6f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.radar_zoom_out), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            zoomScale = 1.0f
                            panOffset = Offset.Zero
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.radar_recenter), tint = HorizonCyan, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Overlay: Legend in Top-Right
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = when (selectedLayer) {
                        0 -> stringResource(R.string.radar_legend_precip_title)
                        1 -> stringResource(R.string.radar_legend_traffic_title)
                        2 -> stringResource(R.string.radar_legend_satellite_title)
                        else -> stringResource(R.string.radar_legend_wind_title)
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                when (selectedLayer) {
                    0 -> {
                        LegendRow(color = RadarGreen, label = stringResource(R.string.radar_legend_precip_light))
                        LegendRow(color = SolarGold, label = stringResource(R.string.radar_legend_precip_mod))
                        LegendRow(color = SevereRed, label = stringResource(R.string.radar_legend_precip_heavy))
                        LegendRow(color = StormPurple, label = stringResource(R.string.radar_legend_precip_hail))
                    }
                    1 -> {
                        LegendRow(color = Color(0xFF22C55E), label = stringResource(R.string.radar_legend_traffic_fast))
                        LegendRow(color = Color(0xFFF59E0B), label = stringResource(R.string.radar_legend_traffic_mod))
                        LegendRow(color = Color(0xFFEF4444), label = stringResource(R.string.radar_legend_traffic_slow))
                    }
                    2 -> {
                        LegendRow(color = Color(0xFF94A3B8), label = stringResource(R.string.radar_legend_sat_cirrus))
                        LegendRow(color = Color(0xFF38BDF8), label = stringResource(R.string.radar_legend_sat_frontal))
                        LegendRow(color = Color(0xFFF43F5E), label = stringResource(R.string.radar_legend_sat_core))
                    }
                    3 -> {
                        LegendRow(color = HorizonCyan, label = stringResource(R.string.radar_legend_wind_vector))
                    }
                }
            }

            // Overlay: Center City Label
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = panOffset.x.dp, y = (panOffset.y + 24).dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = currentLocation.name.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Radar Timeline Scrubber & Animation Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause Button
            Surface(
                shape = CircleShape,
                color = SleekBluePrimary,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onTogglePlay() }
                    .testTag("radar_play_pause_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.common_play_pause),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 7-Interval Timeline Scrubber
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeLabels.forEachIndexed { index, label ->
                    val isSelected = index == frameIndex
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = if (isSelected) SleekBluePrimary else SleekSurfaceSecondary,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) SleekBluePrimary else SleekBorder
                        ),
                        modifier = Modifier
                            .clickable { onSelectFrame(index) }
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else SleekTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarLayerButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (isSelected) SleekBluePrimary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else SleekTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = Color.White, fontSize = 9.sp)
    }
}

