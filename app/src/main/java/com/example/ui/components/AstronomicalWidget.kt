package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AstronomicalData
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AstronomicalWidget(
    astronomical: AstronomicalData,
    modifier: Modifier = Modifier
) {
    val animatedSunProgress by animateFloatAsState(
        targetValue = astronomical.daylightProgressPercent.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "sunArcProgress"
    )

    val sunriseLabel = stringResource(R.string.astro_sunrise, astronomical.sunriseTime)
    val sunsetLabel = stringResource(R.string.astro_sunset, astronomical.sunsetTime)
    val daylightProgressLabel = if (astronomical.daylightProgressPercent in 0.01f..0.99f) {
        stringResource(R.string.astro_daylight_progress, (astronomical.daylightProgressPercent * 100).toInt())
    } else {
        stringResource(R.string.astro_night_horizon)
    }
    val moonPhaseName = stringResource(astronomical.moonPhaseResId)
    val illuminationLabel = stringResource(R.string.astro_illumination, astronomical.moonIlluminationPercent)
    val daysToFullLabel = stringResource(R.string.astro_days_to_full, astronomical.daysUntilFullMoon)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = stringResource(R.string.astro_sun_and_moon),
                    tint = SolarGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.astro_title),
                    color = SleekTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = stringResource(R.string.astro_synodic),
                color = SleekBluePrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 500.dp

            val solarBlock = @Composable { blockModifier: Modifier ->
                Column(
                    modifier = blockModifier.semantics(mergeDescendants = true) {
                        contentDescription = "$sunriseLabel. $sunsetLabel. $daylightProgressLabel"
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = sunriseLabel,
                            color = SleekTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = sunsetLabel,
                            color = SleekTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .semantics {
                                contentDescription = "$sunriseLabel, $sunsetLabel, $daylightProgressLabel"
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        drawLine(
                            color = SleekBorder,
                            start = Offset(0f, h - 10f),
                            end = Offset(w, h - 10f),
                            strokeWidth = 2f
                        )

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

                        val sunX = animatedSunProgress * w
                        val sunY = (h - 10f) - sin(animatedSunProgress * PI.toFloat()) * (h - 20f)

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
                        text = daylightProgressLabel,
                        color = SleekBluePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val lunarBlock = @Composable { blockModifier: Modifier ->
                Column(
                    modifier = blockModifier
                        .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$moonPhaseName, $illuminationLabel, $daysToFullLabel"
                        },
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
                                contentDescription = stringResource(R.string.astro_moon_phase),
                                tint = SleekOnBlueContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = moonPhaseName,
                                color = SleekTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = illuminationLabel,
                                color = SleekBluePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = daysToFullLabel,
                        color = SleekTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    solarBlock(Modifier.fillMaxWidth())
                    lunarBlock(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    solarBlock(Modifier.weight(1.3f))
                    lunarBlock(Modifier.weight(1f))
                }
            }
        }
    }
}
