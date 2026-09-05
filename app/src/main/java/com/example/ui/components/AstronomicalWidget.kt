package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AstronomicalData
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AstronomicalWidget(
    astronomical: AstronomicalData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = "Sun and Moon",
                    tint = SolarGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CELESTIAL & LUNAR HORIZON",
                    color = SleekTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "REAL SYNODIC CYCLE",
                color = SleekBluePrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Dynamic Solar Arc
            Column(
                modifier = Modifier.weight(1.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sunrise ${astronomical.sunriseTime}",
                        color = SleekTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Sunset ${astronomical.sunsetTime}",
                        color = SleekTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Canvas Solar Arc
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Horizon baseline
                    drawLine(
                        color = SleekBorder,
                        start = Offset(0f, h - 10f),
                        end = Offset(w, h - 10f),
                        strokeWidth = 2f
                    )

                    // Arc path (parabola / sine curve)
                    val arcPath = Path()
                    arcPath.moveTo(0f, h - 10f)
                    val steps = 40
                    for (i in 0..steps) {
                        val fraction = i / steps.toFloat()
                        val x = fraction * w
                        val y = (h - 10f) - sin(fraction * PI.toFloat()) * (h - 20f)
                        arcPath.lineTo(x, y)
                    }

                    drawPath(
                        path = arcPath,
                        color = SolarGold.copy(alpha = 0.6f),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )
                    )

                    // Current Sun Position along the arc
                    val sunFraction = astronomical.daylightProgressPercent
                    val sunX = sunFraction * w
                    val sunY = (h - 10f) - sin(sunFraction * PI.toFloat()) * (h - 20f)

                    // Glow circle
                    drawCircle(
                        color = SolarGold.copy(alpha = 0.25f),
                        radius = 12f,
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = SolarGold,
                        radius = 6f,
                        center = Offset(sunX, sunY)
                    )
                }

                Text(
                    text = if (astronomical.daylightProgressPercent in 0.01f..0.99f)
                        "Daylight Progress: ${(astronomical.daylightProgressPercent * 100).toInt()}% of solar arc elapsed"
                    else "Night Horizon: Sun is currently below local horizon",
                    color = SleekBluePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right: Lunar Phase Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SleekBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Brightness2,
                            contentDescription = "Moon Phase",
                            tint = SleekOnBlueContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = astronomical.moonPhaseName,
                            color = SleekTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${astronomical.moonIlluminationPercent}% Illumination",
                            color = SleekBluePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${astronomical.daysUntilFullMoon} days until next Full Moon",
                    color = SleekTextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
