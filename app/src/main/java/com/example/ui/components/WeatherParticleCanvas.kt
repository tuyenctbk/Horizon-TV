package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class RainParticle(
    val xRatio: Float,
    val yRatio: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float,
    val strokeWidth: Float
)

private data class SplashParticle(
    val xRatio: Float,
    val triggerPhase: Float,
    val maxRadius: Float
)

private data class SnowParticle(
    val xRatio: Float,
    val yRatio: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val phase: Float
)

private data class SunMoteParticle(
    val xRatio: Float,
    val yRatio: Float,
    val speed: Float,
    val radius: Float,
    val maxAlpha: Float,
    val phase: Float
)

private data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val twinkleSpeed: Float,
    val phase: Float
)

@Composable
fun WeatherParticleCanvas(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val isRain = weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)
    val isSnow = weatherCode in listOf(71, 73, 75, 77, 85, 86)
    val isThunderstorm = weatherCode in listOf(95, 96, 99)
    val isClearDay = (weatherCode == 0 || weatherCode == 1 || weatherCode == 2) && isDay
    val isClearNight = (weatherCode == 0 || weatherCode == 1 || weatherCode == 2) && !isDay
    val isFogOrCloudy = weatherCode in listOf(3, 45, 48)

    // Master continuous time animation loop
    val infiniteTransition = rememberInfiniteTransition(label = "weatherParticleTransition")

    val masterProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "masterProgress"
    )

    // Secondary slower clock for gentle floating sun motes and fog
    val slowClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowClock"
    )

    // Thunderstorm lightning strobe pulse
    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 7000
                0.0f at 0
                0.0f at 5200
                0.24f at 5280
                0.04f at 5340
                0.32f at 5420
                0.0f at 5550
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lightningFlash"
    )

    // Solar beam rotation oscillation for sunny weather
    val sunBeamAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunBeamAngle"
    )

    // Deterministic particle populations
    val rainParticles = remember {
        List(55) {
            val isForeground = it % 4 == 0
            RainParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                speed = if (isForeground) Random.nextFloat() * 0.4f + 0.9f else Random.nextFloat() * 0.3f + 0.5f,
                length = if (isForeground) Random.nextFloat() * 16f + 24f else Random.nextFloat() * 10f + 14f,
                alpha = if (isForeground) 0.65f else 0.35f,
                strokeWidth = if (isForeground) 2.2f else 1.2f
            )
        }
    }

    val splashParticles = remember {
        List(14) {
            SplashParticle(
                xRatio = Random.nextFloat(),
                triggerPhase = Random.nextFloat(),
                maxRadius = Random.nextFloat() * 10f + 6f
            )
        }
    }

    val snowParticles = remember {
        List(50) {
            val isBokeh = it % 6 == 0
            SnowParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                speed = if (isBokeh) Random.nextFloat() * 0.25f + 0.35f else Random.nextFloat() * 0.2f + 0.15f,
                radius = if (isBokeh) Random.nextFloat() * 3.5f + 3.0f else Random.nextFloat() * 1.8f + 1.2f,
                alpha = if (isBokeh) 0.85f else 0.60f,
                swayAmplitude = Random.nextFloat() * 18f + 8f,
                swayFrequency = Random.nextFloat() * 1.5f + 1.0f,
                phase = (Random.nextFloat() * 2 * PI).toFloat()
            )
        }
    }

    val sunMotes = remember {
        List(35) {
            SunMoteParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                speed = Random.nextFloat() * 0.12f + 0.05f,
                radius = Random.nextFloat() * 2.5f + 1.2f,
                maxAlpha = Random.nextFloat() * 0.45f + 0.30f,
                phase = (Random.nextFloat() * 2 * PI).toFloat()
            )
        }
    }

    val starParticles = remember {
        List(45) {
            StarParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat() * 0.75f,
                radius = Random.nextFloat() * 1.8f + 0.8f,
                twinkleSpeed = Random.nextFloat() * 2f + 1f,
                phase = (Random.nextFloat() * 2 * PI).toFloat()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // ==========================================
        // 1. RAIN / THUNDERSTORM PARTICLE ANIMATION
        // ==========================================
        if (isRain) {
            // Draw rain streaks with parallax velocity
            rainParticles.forEach { p ->
                val curProgress = (p.yRatio + masterProgress * p.speed * 1.8f) % 1f
                val curY = curProgress * (h + p.length) - p.length
                val curX = p.xRatio * w - (curProgress * 25f) // Slight slant for realistic wind tilt

                drawLine(
                    color = Color(0xFF64B5F6).copy(alpha = p.alpha),
                    start = Offset(curX, curY),
                    end = Offset(curX - 5f, curY + p.length),
                    strokeWidth = p.strokeWidth
                )
            }

            // Draw splash rings at the floor
            splashParticles.forEach { s ->
                val cycle = (masterProgress + s.triggerPhase) % 1f
                if (cycle < 0.45f) {
                    val expand = cycle / 0.45f
                    val ringR = s.maxRadius * expand
                    val ringAlpha = (1f - expand) * 0.45f
                    drawOval(
                        color = Color(0xFF90CAF9).copy(alpha = ringAlpha),
                        topLeft = Offset(s.xRatio * w - ringR, h - 8f - ringR * 0.3f),
                        size = Size(ringR * 2, ringR * 0.6f),
                        style = Stroke(width = 1.2f)
                    )
                }
            }

            // Lightning flash overlay
            if (isThunderstorm && lightningAlpha > 0.01f) {
                drawRect(color = Color(0xFFE8EAED).copy(alpha = lightningAlpha))
            }
        }

        // ==========================================
        // 2. SNOW / FLURRIES PARTICLE ANIMATION
        // ==========================================
        else if (isSnow) {
            snowParticles.forEach { flake ->
                val progress = (flake.yRatio + masterProgress * flake.speed * 0.6f) % 1f
                val curY = progress * (h + 20f) - 10f
                val sway = sin(flake.phase + slowClock * flake.swayFrequency) * flake.swayAmplitude
                val curX = flake.xRatio * w + sway

                // Soft snowflake with gentle halo glow
                drawCircle(
                    color = Color(0xFFE1F5FE).copy(alpha = flake.alpha * 0.35f),
                    radius = flake.radius * 1.7f,
                    center = Offset(curX, curY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = flake.alpha),
                    radius = flake.radius,
                    center = Offset(curX, curY)
                )
            }
        }

        // ==========================================
        // 3. SUN / SUNNY DAY PARTICLE ANIMATION
        // ==========================================
        else if (isClearDay) {
            // Radiant sunbeams from top-right corner
            val sunCenter = Offset(w * 0.92f, h * 0.05f)

            // Warm corona glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FFD54F),
                        Color(0x18FFB300),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = sunCenter
            )

            // Floating solar dust motes / sun sparkles
            sunMotes.forEach { mote ->
                // Motes float upward gently
                val progress = (mote.yRatio - masterProgress * mote.speed) % 1f
                val normY = if (progress < 0f) progress + 1f else progress
                val curY = normY * h
                val curX = mote.xRatio * w + sin(mote.phase + slowClock) * 12f

                // Breathing alpha sparkle
                val pulse = (sin(mote.phase + slowClock * 2f) + 1f) / 2f
                val alpha = mote.maxAlpha * (0.4f + pulse * 0.6f)

                // Soft golden glow
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = alpha * 0.4f),
                    radius = mote.radius * 2.2f,
                    center = Offset(curX, curY)
                )
                drawCircle(
                    color = Color(0xFFFFF9C4).copy(alpha = alpha),
                    radius = mote.radius,
                    center = Offset(curX, curY)
                )
            }
        }

        // ==========================================
        // 4. CLEAR NIGHT / STARLIGHT ANIMATION
        // ==========================================
        else if (isClearNight) {
            starParticles.forEach { star ->
                val twinkle = (sin(star.phase + masterProgress * 2 * PI.toFloat() * star.twinkleSpeed) + 1f) / 2f
                val alpha = 0.25f + twinkle * 0.70f
                drawCircle(
                    color = Color(0xFFE2E8F0).copy(alpha = alpha),
                    radius = star.radius,
                    center = Offset(star.xRatio * w, star.yRatio * h)
                )
            }

            // Subtle moon halo in upper right
            val moonCenter = Offset(w * 0.88f, h * 0.08f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2080D8FF),
                        Color.Transparent
                    ),
                    center = moonCenter,
                    radius = w * 0.35f
                ),
                radius = w * 0.35f,
                center = moonCenter
            )
        }

        // ==========================================
        // 5. CLOUDY / OVERCAST / FOG HAZE ANIMATION
        // ==========================================
        else if (isFogOrCloudy) {
            // Horizontal drifting atmospheric mist bands
            val mistY1 = h * 0.30f
            val mistY2 = h * 0.65f
            val drift1 = (slowClock * 15f) % (w * 0.3f)
            val drift2 = (-slowClock * 12f) % (w * 0.3f)

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x0CFFFFFF),
                        Color(0x18B0BEC5),
                        Color(0x0CFFFFFF),
                        Color.Transparent
                    ),
                    startX = 0f + drift1,
                    endX = w + drift1
                ),
                topLeft = Offset(0f, mistY1 - 40f),
                size = Size(w, 80f)
            )

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x1090A4AE),
                        Color(0x1CFFFFFF),
                        Color(0x1090A4AE),
                        Color.Transparent
                    ),
                    startX = 0f + drift2,
                    endX = w + drift2
                ),
                topLeft = Offset(0f, mistY2 - 40f),
                size = Size(w, 80f)
            )
        }
    }
}
