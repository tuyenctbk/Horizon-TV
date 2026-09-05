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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.*

@Composable
fun FloatingPiP(
    isActive: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "pipScanline")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanY"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        shadowElevation = 8.dp,
        modifier = modifier
            .width(220.dp)
            .height(130.dp)
            .testTag("floating_pip_player")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated live television weather feed backdrop
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F203C), Color(0xFF0A1324))
                    )
                )
                // Doppler radar simulated ring in PiP
                drawCircle(
                    color = HorizonCyan.copy(alpha = 0.15f),
                    radius = h * 0.45f,
                    center = Offset(w * 0.65f, h * 0.5f)
                )
                drawCircle(
                    color = RadarGreen.copy(alpha = 0.2f),
                    radius = h * 0.25f,
                    center = Offset(w * 0.65f, h * 0.5f)
                )
                // Broadcast scan beam
                drawLine(
                    color = HorizonCyan.copy(alpha = 0.15f),
                    start = Offset(0f, scanY * h),
                    end = Offset(w, scanY * h),
                    strokeWidth = 2f
                )
            }

            // Top Status Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SevereRed
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(stringResource(R.string.pip_live), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Mute
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onToggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = stringResource(R.string.pip_mute),
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    // Expand to Fullscreen
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onExpand() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.pip_expand),
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    // Close
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.pip_close),
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Bottom Presenter Tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Live Doppler 24/7",
                    color = HorizonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
