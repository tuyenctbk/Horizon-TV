package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    AnimatedVisibility(
        visible = isActive,
        enter = scaleIn(tween(220), initialScale = 0.85f) + fadeIn(tween(200)),
        exit = scaleOut(tween(180), targetScale = 0.85f) + fadeOut(tween(160)),
        modifier = modifier
    ) {
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

        val pipChannelDesc = stringResource(R.string.pip_channel_tag)
        val pipMuteDesc = stringResource(R.string.pip_mute)
        val pipExpandDesc = stringResource(R.string.pip_expand)
        val pipCloseDesc = stringResource(R.string.pip_close)
        val muteStateDesc = stringResource(if (isMuted) R.string.livetv_muted else R.string.livetv_audio_on)

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .width(228.dp)
                .height(134.dp)
                .semantics { contentDescription = pipChannelDesc }
                .testTag("floating_pip_player")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Live television weather feed backdrop
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = pipChannelDesc }
                ) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F203C), Color(0xFF0A1324))
                        )
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                        radius = h * 0.45f,
                        center = Offset(w * 0.65f, h * 0.5f)
                    )
                    drawCircle(
                        color = RadarGreen.copy(alpha = 0.2f),
                        radius = h * 0.25f,
                        center = Offset(w * 0.65f, h * 0.5f)
                    )
                    drawLine(
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
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
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                stringResource(R.string.pip_live),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable(role = Role.Button) { onToggleMute() }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = pipMuteDesc
                                    stateDescription = muteStateDesc
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = pipMuteDesc,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable(role = Role.Button) { onExpand() }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = pipExpandDesc
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = pipExpandDesc,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable(role = Role.Button) { onClose() }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = pipCloseDesc
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = pipCloseDesc,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Bottom Presenter Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = pipChannelDesc,
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
