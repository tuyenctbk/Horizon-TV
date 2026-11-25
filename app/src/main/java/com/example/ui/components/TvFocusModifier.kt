package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DiurnalPeriod
import com.example.ui.theme.BroadcastCyan

/**
 * Reusable 10-foot Android TV D-Pad focus affordance modifier.
 * Apply BEFORE `.clickable(...)` so `onFocusChanged` tracks focus transitions of the target.
 * Renders a spring-animated scale lift and crisp high-contrast broadcast focus ring when focused.
 */
fun Modifier.tvFocusVisual(
    shape: Shape = RoundedCornerShape(16.dp),
    focusBorderColor: Color = BroadcastCyan,
    focusBorderWidth: Dp = 2.5.dp,
    focusedScale: Float = 1.035f
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tvFocusScale"
    )
    this
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (isFocused) {
                Modifier.border(width = focusBorderWidth, color = focusBorderColor, shape = shape)
            } else {
                Modifier
            }
        )
}

/**
 * Determines whether a 24-hour label like "14:00" or "02:00" falls during daytime hours (06:00–18:59).
 */
fun isHourDaytime(timeLabel: String): Boolean {
    val hour = timeLabel.substringBefore(":").toIntOrNull() ?: return true
    return hour in 6..18
}

/**
 * Determines whether a diurnal period represents daytime (Morning, Afternoon, Evening) vs Night.
 */
fun isDiurnalPeriodDaytime(period: DiurnalPeriod): Boolean {
    if (period.periodNameRes == R.string.period_night) return false
    return !period.periodName.equals("Night", ignoreCase = true)
}
