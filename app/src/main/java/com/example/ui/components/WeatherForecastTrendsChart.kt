package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyPoint
import com.example.data.model.UnitSystem
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun WeatherForecastTrendsChart(
    hourlyPoints: List<HourlyPoint>,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    if (hourlyPoints.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "24-Hour Temperature & Precipitation Curve",
                color = SleekTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(SleekBluePrimary, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Temp (${if (unitSystem == UnitSystem.METRIC) "°C" else "°F"})", color = SleekBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(SleekBlueContainer, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rain POP (%)", color = SleekTextSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val pointsToRender = hourlyPoints.take(24)
        val temps = pointsToRender.map { pt ->
            if (unitSystem == UnitSystem.METRIC) pt.tempC else (pt.tempC * 9 / 5 + 32)
        }
        val minT = (temps.minOrNull() ?: 15.0) - 2.0
        val maxT = (temps.maxOrNull() ?: 25.0) + 2.0
        val tRange = (maxT - minT).coerceAtLeast(1.0)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            val chartBottom = h - 25f
            val chartTop = 15f
            val chartHeight = chartBottom - chartTop
            val stepX = w / (pointsToRender.size - 1).coerceAtLeast(1)

            // Draw horizontal reference grid lines
            for (level in 0..2) {
                val gridY = chartTop + chartHeight * (level / 2f)
                drawLine(
                    color = SleekBorderSubtle,
                    start = Offset(0f, gridY),
                    end = Offset(w, gridY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            // Draw Precipitation POP Bars along bottom
            pointsToRender.forEachIndexed { i, pt ->
                val barX = i * stepX
                val barWidth = (stepX * 0.6f).coerceAtLeast(4f)
                val barHeight = (pt.popPercent / 100f) * (chartHeight * 0.35f)
                if (barHeight > 2f) {
                    drawRoundRect(
                        color = SleekBlueContainer.copy(alpha = 0.7f),
                        topLeft = Offset(barX - barWidth / 2f, chartBottom - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Build Temperature Spline Path
            val path = Path()
            val fillPath = Path()

            pointsToRender.forEachIndexed { i, _ ->
                val t = temps[i]
                val normY = 1.0 - ((t - minT) / tRange)
                val x = i * stepX
                val y = (chartTop + normY * chartHeight).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, chartBottom)
                    fillPath.lineTo(x, y)
                } else {
                    val prevT = temps[i - 1]
                    val prevNormY = 1.0 - ((prevT - minT) / tRange)
                    val prevX = (i - 1) * stepX
                    val prevY = (chartTop + prevNormY * chartHeight).toFloat()
                    val cx = (prevX + x) / 2f
                    path.cubicTo(cx, prevY, cx, y, x, y)
                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            fillPath.lineTo((pointsToRender.size - 1) * stepX, chartBottom)
            fillPath.close()

            // Draw Gradient Area under Temperature curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SleekBluePrimary.copy(alpha = 0.15f), SleekBluePrimary.copy(alpha = 0.01f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )

            // Draw Temperature Line
            drawPath(
                path = path,
                color = SleekBluePrimary,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Draw Temperature Points & Value Dots every 3rd step
            pointsToRender.forEachIndexed { i, _ ->
                val t = temps[i]
                val normY = 1.0 - ((t - minT) / tRange)
                val x = i * stepX
                val y = (chartTop + normY * chartHeight).toFloat()

                if (i % 3 == 0 || i == pointsToRender.size - 1) {
                    drawCircle(color = SleekSurface, radius = 5f, center = Offset(x, y))
                    drawCircle(color = SleekBluePrimary, radius = 3.5f, center = Offset(x, y))
                }
            }
        }

        // Time labels along the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            pointsToRender.forEachIndexed { index, pt ->
                if (index % 4 == 0 || index == pointsToRender.size - 1) {
                    val tempVal = temps[index].roundToInt()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${tempVal}°",
                            color = SleekTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pt.timeLabel,
                            color = SleekTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
