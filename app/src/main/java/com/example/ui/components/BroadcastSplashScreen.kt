package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BroadcastSplashScreen(
    isLoading: Boolean,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashRadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var syncStep by remember { mutableIntStateOf(0) }
    val syncSteps = listOf(
        "INITIALIZING HIGH-PRECISION METEOROLOGICAL TELEMETRY...",
        "ACQUIRING GLOBAL NUMERICAL FORECASTING GRIDS...",
        "SYNCHRONIZING DUAL-POL DOPPLER REFLECTIVITY SWEEPS...",
        "CALIBRATING ATMOSPHERIC SENSORS & REAL-TIME ALERTS..."
    )

    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                delay(1200)
                syncStep = (syncStep + 1) % syncSteps.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF06142E),
                        Color(0xFF040A18),
                        Color(0xFF02040A)
                    )
                )
            )
            .testTag("broadcast_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Radar Dish Crest
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF050D1F))
                    .border(2.dp, HorizonCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension / 2f - 8.dp.toPx()

                    // Radar range rings
                    listOf(0.33f, 0.66f, 1f).forEach { frac ->
                        drawCircle(
                            color = HorizonCyan.copy(alpha = 0.25f),
                            radius = maxR * frac,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // Crosshairs
                    drawLine(
                        color = HorizonCyan.copy(alpha = 0.2f),
                        start = Offset(cx, cy - maxR),
                        end = Offset(cx, cy + maxR),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = HorizonCyan.copy(alpha = 0.2f),
                        start = Offset(cx - maxR, cy),
                        end = Offset(cx + maxR, cy),
                        strokeWidth = 1f
                    )

                    // Sweeping beam
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = cx + (maxR * cos(rad)).toFloat()
                    val endY = cy + (maxR * sin(rad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(HorizonCyan, Color.Transparent),
                            start = Offset(cx, cy),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    // Echo dots
                    drawCircle(color = RadarGreen.copy(alpha = 0.7f), radius = 6f, center = Offset(cx + 25f, cy - 20f))
                    drawCircle(color = SolarGold.copy(alpha = 0.8f), radius = 4f, center = Offset(cx + 28f, cy - 18f))
                    drawCircle(color = HorizonCyan, radius = 5f, center = Offset(cx, cy))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Station Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SevereRed
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E3A8A)
                ) {
                    Text(
                        text = "CH 104 • 24/7",
                        color = HorizonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HORIZON WEATHER TV",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Text(
                text = "CONTINUOUS METEOROLOGICAL BROADCAST NETWORK",
                color = HorizonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (errorMessage != null) {
                // Error Reconnect State
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33DC2626),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SevereRed),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = stringResource(R.string.common_warning),
                            tint = SevereRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.error_signal_interrupted),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            color = TextSecondarySilver,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.error_reconnect_button), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Live Sync Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    LinearProgressIndicator(
                        color = HorizonCyan,
                        trackColor = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = syncSteps[syncStep],
                        color = HorizonCyan.copy(alpha = pulseAlpha),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.loading_connecting),
                        color = TextSecondarySilver,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
