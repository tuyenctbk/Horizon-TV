package com.example.ui.components

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Real-time data visualizer for temperature, humidity, and barometric pressure.
 * Uses local state with a periodic coroutine ticker to update micro-sensor telemetry every 2 seconds,
 * maintaining an active rolling sparkline history buffer and live needle/arc gauges.
 */
@Composable
fun RealTimeTelemetryVisualizer(
    currentWeather: CurrentWeather,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    // Local telemetry state updated periodically
    var liveTempC by remember(currentWeather.tempC) { mutableFloatStateOf(currentWeather.tempC.toFloat()) }
    var liveHumidity by remember(currentWeather.humidityPercent) { mutableFloatStateOf(currentWeather.humidityPercent.toFloat()) }
    var livePressureHpa by remember(currentWeather.pressureHpa) { mutableFloatStateOf(currentWeather.pressureHpa.toFloat()) }
    var updateCount by remember { mutableIntStateOf(0) }
    var isLiveUpdating by remember { mutableStateOf(true) }

    // Rolling history buffers for real-time trendlines (12 samples)
    val tempHistory = remember { mutableStateListOf<Float>().apply { repeat(12) { add(currentWeather.tempC.toFloat()) } } }
    val humidityHistory = remember { mutableStateListOf<Float>().apply { repeat(12) { add(currentWeather.humidityPercent.toFloat()) } } }
    val pressureHistory = remember { mutableStateListOf<Float>().apply { repeat(12) { add(currentWeather.pressureHpa.toFloat()) } } }

    // Periodic local state telemetry update loop
    LaunchedEffect(isLiveUpdating, currentWeather) {
        if (!isLiveUpdating) return@LaunchedEffect
        while (true) {
            delay(2000) // 2-second micro-sensor telemetry frequency
            val baseTemp = currentWeather.tempC.toFloat()
            val baseHumid = currentWeather.humidityPercent.toFloat()
            val basePress = currentWeather.pressureHpa.toFloat()

            // Micro fluctuations simulating high-precision atmospheric telemetry sensors
            val deltaTemp = (Random.nextFloat() - 0.5f) * 0.4f
            val deltaHumid = (Random.nextFloat() - 0.5f) * 1.2f
            val deltaPress = (Random.nextFloat() - 0.5f) * 0.3f

            liveTempC = (baseTemp + deltaTemp).coerceIn(-40f, 60f)
            liveHumidity = (baseHumid + deltaHumid).coerceIn(5f, 100f)
            livePressureHpa = (basePress + deltaPress).coerceIn(900f, 1080f)
            updateCount++

            // Shift history buffers
            if (tempHistory.size >= 12) tempHistory.removeAt(0)
            tempHistory.add(liveTempC)

            if (humidityHistory.size >= 12) humidityHistory.removeAt(0)
            humidityHistory.add(liveHumidity)

            if (pressureHistory.size >= 12) pressureHistory.removeAt(0)
            pressureHistory.add(livePressureHpa)
        }
    }

    // Pulse animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("realtime_telemetry_visualizer")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isLiveUpdating) SleekGreenText.copy(alpha = pulseAlpha) else SleekTextSecondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "REAL-TIME ATMOSPHERIC SENSOR TELEMETRY",
                        color = SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isLiveUpdating) "Live local stream • Active sampling (every 2.0s) • Cycle #$updateCount" else "Telemetry feed paused",
                        color = SleekTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (isLiveUpdating) SleekGreenContainer else SleekSurfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isLiveUpdating) SleekGreenText.copy(alpha = 0.4f) else SleekBorder),
                modifier = Modifier.clickable { isLiveUpdating = !isLiveUpdating }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isLiveUpdating) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle stream",
                        tint = if (isLiveUpdating) SleekGreenText else SleekTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLiveUpdating) "PAUSE" else "RESUME",
                        color = if (isLiveUpdating) SleekGreenText else SleekTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Three Column Metric Visualizer Gauges: Temperature, Humidity, Barometer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Temperature Gauge & Arc
            TemperatureGaugeCard(
                tempC = liveTempC,
                unitSystem = unitSystem,
                history = tempHistory,
                modifier = Modifier.weight(1f)
            )

            // 2. Humidity & Moisture Ring
            HumidityGaugeCard(
                humidityPercent = liveHumidity,
                history = humidityHistory,
                modifier = Modifier.weight(1f)
            )

            // 3. Barometric Pressure Aneroid Dial
            BarometerGaugeCard(
                pressureHpa = livePressureHpa,
                unitSystem = unitSystem,
                history = pressureHistory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Real-time Temperature Gauge Card with circular sweep arc, live needle, and sparkline.
 */
@Composable
private fun TemperatureGaugeCard(
    tempC: Float,
    unitSystem: UnitSystem,
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    val displayTemp = if (unitSystem == UnitSystem.METRIC) {
        String.format(java.util.Locale.US, "%.1f°C", tempC)
    } else {
        val tempF = tempC * 9f / 5f + 32f
        String.format(java.util.Locale.US, "%.1f°F", tempF)
    }

    val normalizedTemp = ((tempC + 20f) / 60f).coerceIn(0f, 1f) // -20°C to +40°C range
    val animatedAngle by animateFloatAsState(
        targetValue = 150f + normalizedTemp * 240f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tempNeedle"
    )

    Column(
        modifier = modifier
            .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TEMPERATURE", color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Thermostat, contentDescription = "Temp", tint = SevereRed, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Arc Dial Canvas
        Box(
            modifier = Modifier
                .size(90.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background arc track (from 150° to 390°, sweep 240°)
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // Value colored arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color(0xFF38BDF8),
                        0.4f to Color(0xFF10B981),
                        0.7f to Color(0xFFF59E0B),
                        1.0f to Color(0xFFEF4444)
                    ),
                    startAngle = 150f,
                    sweepAngle = normalizedTemp * 240f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // Needle pointer
                val needleRad = Math.toRadians(animatedAngle.toDouble())
                val needleLen = radius * 0.75f
                val nx = center.x + (needleLen * cos(needleRad)).toFloat()
                val ny = center.y + (needleLen * sin(needleRad)).toFloat()

                drawLine(
                    color = Color(0xFF0F172A),
                    start = center,
                    end = Offset(nx, ny),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color(0xFF0F172A), radius = 4.dp.toPx(), center = center)
            }
        }

        Text(
            text = displayTemp,
            color = SleekTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        val status = when {
            tempC < 5f -> "Chilly / Cold"
            tempC < 25f -> "Mild Comfort"
            tempC < 32f -> "Warm"
            else -> "High Heat"
        }
        Text(text = status, color = SleekBluePrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(6.dp))

        // Mini Sparkline History Canvas
        MiniSparkline(data = history, color = SleekBluePrimary, modifier = Modifier.fillMaxWidth().height(22.dp))
    }
}

/**
 * Real-time Humidity Gauge Card with percentage ring and moisture sparkline.
 */
@Composable
private fun HumidityGaugeCard(
    humidityPercent: Float,
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (humidityPercent / 100f) * 360f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "humiditySweep"
    )

    Column(
        modifier = modifier
            .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HUMIDITY", color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.WaterDrop, contentDescription = "Humidity", tint = SleekBluePrimary, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Circular Ring Canvas
        Box(
            modifier = Modifier
                .size(90.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Track
                drawCircle(
                    color = Color(0xFFE2E8F0),
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke)
                )

                // Active Progress Arc
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f%%", humidityPercent),
                    color = SleekTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "${humidityPercent.toInt()}% RH",
            color = SleekTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        val moistureStatus = when {
            humidityPercent < 30f -> "Dry Air"
            humidityPercent < 60f -> "Ideal Comfort"
            humidityPercent < 80f -> "Humid"
            else -> "High Moisture"
        }
        Text(text = moistureStatus, color = SleekGreenText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(6.dp))

        // Mini Sparkline History Canvas
        MiniSparkline(data = history, color = Color(0xFF2563EB), modifier = Modifier.fillMaxWidth().height(22.dp))
    }
}

/**
 * Real-time Barometric Pressure Gauge with aneroid dial and tendency indicator.
 */
@Composable
private fun BarometerGaugeCard(
    pressureHpa: Float,
    unitSystem: UnitSystem,
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    val displayPressure = if (unitSystem == UnitSystem.METRIC) {
        String.format(java.util.Locale.US, "%.1f hPa", pressureHpa)
    } else {
        val inHg = pressureHpa * 0.02953f
        String.format(java.util.Locale.US, "%.2f inHg", inHg)
    }

    // Normal pressure range 970 to 1040 hPa
    val normalizedPressure = ((pressureHpa - 970f) / 70f).coerceIn(0f, 1f)
    val animatedAngle by animateFloatAsState(
        targetValue = 135f + normalizedPressure * 270f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pressureAngle"
    )

    Column(
        modifier = modifier
            .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("BAROMETER", color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Speed, contentDescription = "Pressure", tint = SolarGold, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dial Canvas
        Box(
            modifier = Modifier
                .size(90.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Track
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                // Pressure segments (Low/Storm, Normal, High)
                drawArc(
                    color = SevereRed.copy(alpha = 0.8f),
                    startAngle = 135f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                drawArc(
                    color = SleekGreenText.copy(alpha = 0.8f),
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                drawArc(
                    color = SolarGold.copy(alpha = 0.8f),
                    startAngle = 335f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )

                // Needle
                val rad = Math.toRadians(animatedAngle.toDouble())
                val needleLen = radius * 0.78f
                val nx = center.x + (needleLen * cos(rad)).toFloat()
                val ny = center.y + (needleLen * sin(rad)).toFloat()

                drawLine(
                    color = Color(0xFF0F172A),
                    start = center,
                    end = Offset(nx, ny),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = SolarGold, radius = 4.dp.toPx(), center = center)
            }
        }

        Text(
            text = displayPressure,
            color = SleekTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )

        val tendency = when {
            pressureHpa < 1000f -> "Low (Storm Watch)"
            pressureHpa < 1018f -> "Normal (Steady)"
            else -> "High (Clear Skies)"
        }
        Text(text = tendency, color = SolarGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(6.dp))

        // Mini Sparkline History Canvas
        MiniSparkline(data = history, color = SolarGold, modifier = Modifier.fillMaxWidth().height(22.dp))
    }
}

/**
 * Reusable mini sparkline graph rendering smooth cubic curves across recent data buffers.
 */
@Composable
private fun MiniSparkline(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val min = (data.minOrNull() ?: 0f) - 0.5f
        val max = (data.maxOrNull() ?: 1f) + 0.5f
        val range = (max - min).coerceAtLeast(0.1f)

        val stepX = w / (data.size - 1)
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = h - ((value - min) / range) * h
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevY = h - ((data[index - 1] - min) / range) * h
                val cx = (prevX + x) / 2f
                path.cubicTo(cx, prevY, cx, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
