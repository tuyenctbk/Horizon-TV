package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun FilmGrainVignette(
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Subtle CRT horizontal scanlines (very refined for sleek aesthetic)
        var y = 0f
        val lineSpacing = 8f
        while (y < h) {
            drawLine(
                color = Color.Black.copy(alpha = 0.02f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            y += lineSpacing
        }

        // Subtle sleek vignette shading on screen perimeter
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0x050B57D0),
                    Color(0x14000000)
                ),
                center = Offset(w / 2f, h / 2f),
                radius = maxOf(w, h) * 0.75f
            )
        )
    }
}
