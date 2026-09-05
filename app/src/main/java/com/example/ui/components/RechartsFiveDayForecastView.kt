package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecastDay
import com.example.data.model.UnitSystem
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class RechartsDisplayMode {
    COMBINED,
    TEMPERATURE_ONLY,
    PRECIPITATION_ONLY
}

@Composable
fun RechartsFiveDayForecastView(
    dailyForecast: List<DailyForecastDay>,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    if (dailyForecast.isEmpty()) return

    // Limit to 5 days
    val fiveDays = remember(dailyForecast) { dailyForecast.take(5) }
    var selectedDayIndex by remember { mutableStateOf(0) }
    var displayMode by remember { mutableStateOf(RechartsDisplayMode.COMBINED) }

    val tempUnit = if (unitSystem == UnitSystem.METRIC) "°C" else "°F"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("five_day_forecast_recharts")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SleekBlueContainer
                    ) {
                        Text(
                            text = "RECHARTS VISUALIZATION",
                            color = SleekOnBlueContainer,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5-DAY SYNOPTIC METEOROLOGICAL FORECAST",
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Area curve temperature gradients, Cartesian gridlines & precipitation chance bars",
                    color = SleekTextSecondary,
                    fontSize = 11.sp
                )
            }

            // Segmented mode toggle buttons
            Row(
                modifier = Modifier
                    .background(SleekSurfaceSecondary, RoundedCornerShape(percent = 50))
                    .border(1.dp, SleekBorder, RoundedCornerShape(percent = 50))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                RechartsModeTab("ALL", isSelected = displayMode == RechartsDisplayMode.COMBINED, testTag = "recharts_tab_all") {
                    displayMode = RechartsDisplayMode.COMBINED
                }
                RechartsModeTab("TEMP", isSelected = displayMode == RechartsDisplayMode.TEMPERATURE_ONLY, testTag = "recharts_tab_temp") {
                    displayMode = RechartsDisplayMode.TEMPERATURE_ONLY
                }
                RechartsModeTab("RAIN", isSelected = displayMode == RechartsDisplayMode.PRECIPITATION_ONLY, testTag = "recharts_tab_rain") {
                    displayMode = RechartsDisplayMode.PRECIPITATION_ONLY
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5-Day Horizontal Indicator Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fiveDays.forEachIndexed { index, day ->
                val isSelected = index == selectedDayIndex
                val maxTempDisplay = if (unitSystem == UnitSystem.METRIC)
                    "${day.maxTempC.roundToInt()}°"
                else
                    "${(day.maxTempC * 9 / 5 + 32).roundToInt()}°"

                val minTempDisplay = if (unitSystem == UnitSystem.METRIC)
                    "${day.minTempC.roundToInt()}°"
                else
                    "${(day.minTempC * 9 / 5 + 32).roundToInt()}°"

                val dayName = if (index == 0) "TODAY" else day.dayOfWeek.take(3).uppercase()

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SleekBlueContainer else SleekSurfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) SleekBluePrimary else SleekBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedDayIndex = index }
                        .testTag("forecast_day_$index")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayName,
                            color = if (isSelected) SleekOnBlueContainer else SleekTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = day.dateLabel.takeLast(6).trim(),
                            color = if (isSelected) SleekBluePrimary else SleekTextSecondary,
                            fontSize = 9.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        WeatherConditionIcon(weatherCode = day.weatherCode, size = 26.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = maxTempDisplay,
                                color = SolarGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ",
                                color = SleekTextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = minTempDisplay,
                                color = HorizonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = "Rain",
                                tint = HorizonCyan,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${day.popPercent}%",
                                color = HorizonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recharts Canvas Chart Area
        val maxTemps = fiveDays.map {
            if (unitSystem == UnitSystem.METRIC) it.maxTempC else (it.maxTempC * 9 / 5 + 32)
        }
        val minTemps = fiveDays.map {
            if (unitSystem == UnitSystem.METRIC) it.minTempC else (it.minTempC * 9 / 5 + 32)
        }

        val allTemps = maxTemps + minTemps
        val globalMinTemp = (allTemps.minOrNull() ?: 10.0) - 3.0
        val globalMaxTemp = (allTemps.maxOrNull() ?: 30.0) + 3.0
        val tempRange = (globalMaxTemp - globalMinTemp).coerceAtLeast(1.0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SleekDarkContainer)
                .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                .padding(top = 16.dp, bottom = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val chartTop = 15f
                val chartBottom = h - 25f
                val chartH = chartBottom - chartTop

                val numDays = fiveDays.size
                val stepX = w / numDays

                // 1. Cartesian Gridlines (Recharts dashed aesthetic)
                val gridLevels = 4
                for (i in 0..gridLevels) {
                    val y = chartTop + chartH * (i / gridLevels.toFloat())
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Calculate points
                val maxPoints = maxTemps.mapIndexed { idx, t ->
                    val x = stepX * idx + stepX / 2f
                    val normY = (t - globalMinTemp) / tempRange
                    val y = chartBottom - (normY.toFloat() * chartH)
                    Offset(x, y)
                }

                val minPoints = minTemps.mapIndexed { idx, t ->
                    val x = stepX * idx + stepX / 2f
                    val normY = (t - globalMinTemp) / tempRange
                    val y = chartBottom - (normY.toFloat() * chartH)
                    Offset(x, y)
                }

                // 2. Precipitation Bars (if visible)
                if (displayMode == RechartsDisplayMode.COMBINED || displayMode == RechartsDisplayMode.PRECIPITATION_ONLY) {
                    fiveDays.forEachIndexed { idx, day ->
                        val centerX = stepX * idx + stepX / 2f
                        val barWidth = 24.dp.toPx()
                        val popRatio = (day.popPercent / 100f).coerceIn(0f, 1f)
                        val barMaxH = chartH * 0.45f
                        val barH = popRatio * barMaxH
                        val barTop = chartBottom - barH

                        if (barH > 2f) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        HorizonCyan.copy(alpha = 0.55f),
                                        HorizonCyan.copy(alpha = 0.15f)
                                    ),
                                    startY = barTop,
                                    endY = chartBottom
                                ),
                                topLeft = Offset(centerX - barWidth / 2f, barTop),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // 3. Max Temperature Smooth Area & Line Curve
                if (displayMode == RechartsDisplayMode.COMBINED || displayMode == RechartsDisplayMode.TEMPERATURE_ONLY) {
                    val maxPath = Path()
                    val maxFillPath = Path()

                    maxPoints.forEachIndexed { i, pt ->
                        if (i == 0) {
                            maxPath.moveTo(pt.x, pt.y)
                            maxFillPath.moveTo(pt.x, chartBottom)
                            maxFillPath.lineTo(pt.x, pt.y)
                        } else {
                            val prev = maxPoints[i - 1]
                            val cx1 = prev.x + (pt.x - prev.x) / 2f
                            val cy1 = prev.y
                            val cx2 = prev.x + (pt.x - prev.x) / 2f
                            val cy2 = pt.y
                            maxPath.cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                            maxFillPath.cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                        }
                    }
                    maxFillPath.lineTo(maxPoints.last().x, chartBottom)
                    maxFillPath.close()

                    // Draw Max Temp gradient area fill
                    drawPath(
                        path = maxFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SolarGold.copy(alpha = 0.28f),
                                SolarGold.copy(alpha = 0.02f)
                            ),
                            startY = chartTop,
                            endY = chartBottom
                        )
                    )

                    // Draw Max Temp stroke curve
                    drawPath(
                        path = maxPath,
                        color = SolarGold,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 4. Min Temperature Smooth Area & Line Curve
                    val minPath = Path()
                    val minFillPath = Path()

                    minPoints.forEachIndexed { i, pt ->
                        if (i == 0) {
                            minPath.moveTo(pt.x, pt.y)
                            minFillPath.moveTo(pt.x, chartBottom)
                            minFillPath.lineTo(pt.x, pt.y)
                        } else {
                            val prev = minPoints[i - 1]
                            val cx1 = prev.x + (pt.x - prev.x) / 2f
                            val cy1 = prev.y
                            val cx2 = prev.x + (pt.x - prev.x) / 2f
                            val cy2 = pt.y
                            minPath.cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                            minFillPath.cubicTo(cx1, cy1, cx2, cy2, pt.x, pt.y)
                        }
                    }
                    minFillPath.lineTo(minPoints.last().x, chartBottom)
                    minFillPath.close()

                    // Draw Min Temp gradient area fill
                    drawPath(
                        path = minFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                HorizonCyan.copy(alpha = 0.22f),
                                HorizonCyan.copy(alpha = 0.01f)
                            ),
                            startY = chartTop,
                            endY = chartBottom
                        )
                    )

                    // Draw Min Temp stroke curve
                    drawPath(
                        path = minPath,
                        color = HorizonCyan,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw circular nodes
                    maxPoints.forEachIndexed { i, pt ->
                        val isCurr = i == selectedDayIndex
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = if (isCurr) 6.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = SolarGold,
                            radius = if (isCurr) 4.5.dp.toPx() else 3.dp.toPx(),
                            center = pt
                        )
                    }

                    minPoints.forEachIndexed { i, pt ->
                        val isCurr = i == selectedDayIndex
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = if (isCurr) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = HorizonCyan,
                            radius = if (isCurr) 3.5.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // 5. Active Cursor Line for Selected Day
                if (selectedDayIndex in fiveDays.indices) {
                    val cursorX = stepX * selectedDayIndex + stepX / 2f
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(cursorX, 0f),
                        end = Offset(cursorX, h),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }
            }

            // Interactive Recharts Floating Tooltip Card for Selected Day
            val activeDay = fiveDays.getOrNull(selectedDayIndex)
            if (activeDay != null) {
                val maxT = if (unitSystem == UnitSystem.METRIC)
                    "${activeDay.maxTempC.roundToInt()}°C"
                else
                    "${(activeDay.maxTempC * 9 / 5 + 32).roundToInt()}°F"

                val minT = if (unitSystem == UnitSystem.METRIC)
                    "${activeDay.minTempC.roundToInt()}°C"
                else
                    "${(activeDay.minTempC * 9 / 5 + 32).roundToInt()}°F"

                val isRightSide = selectedDayIndex >= 3
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = if (isRightSide) Alignment.TopStart else Alignment.TopEnd
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xEE111827),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${activeDay.dayOfWeek.uppercase()} (${activeDay.dateLabel})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                WeatherConditionIcon(weatherCode = activeDay.weatherCode, size = 16.dp)
                            }
                            Text(
                                text = activeDay.conditionText,
                                color = SleekBluePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Max Temp (High):", color = Color(0xFFD1D5DB), fontSize = 10.sp)
                                Text(maxT, color = SolarGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Min Temp (Low):", color = Color(0xFFD1D5DB), fontSize = 10.sp)
                                Text(minT, color = HorizonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Precipitation POP:", color = Color(0xFFD1D5DB), fontSize = 10.sp)
                                Text("${activeDay.popPercent}% (${activeDay.rainMm} mm)", color = HorizonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Max Wind / UV:", color = Color(0xFF9CA3AF), fontSize = 9.sp)
                                val windDisplay = if (unitSystem == UnitSystem.METRIC)
                                    "${activeDay.maxWindKmh.roundToInt()} km/h"
                                else
                                    "${(activeDay.maxWindKmh * 0.621371).roundToInt()} mph"
                                Text("$windDisplay • UV ${activeDay.maxUvIndex.roundToInt()}", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendIndicator(color = SolarGold, label = "High Temp Curve ($tempUnit)")
                LegendIndicator(color = HorizonCyan, label = "Low Temp Curve ($tempUnit)")
                LegendIndicator(color = HorizonCyan.copy(alpha = 0.5f), label = "Rain Probability (POP %)")
            }

            Text(
                text = "CLICK ANY DAY TO INSPECT TOOLTIP",
                color = SleekTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RechartsModeTab(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (isSelected) SleekBluePrimary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else SleekTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, color = SleekTextSecondary, fontSize = 10.sp)
    }
}
