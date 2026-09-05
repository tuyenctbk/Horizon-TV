package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.ui.theme.*

/**
 * High-visibility reactive emergency weather alert banner triggered when new events
 * are pushed to the meteorological telemetry stream.
 */
@Composable
fun EmergencyBroadcastBanner(
    alert: SevereWeatherAlert?,
    onOpenModal: (SevereWeatherAlert) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (alert == null) return@AnimatedVisibility

        val severityColor = when (alert.severity) {
            AlertSeverity.EMERGENCY -> SevereRed
            AlertSeverity.WARNING -> Color(0xFFFF6D00)
            AlertSeverity.WATCH -> SolarGold
            AlertSeverity.ADVISORY -> HorizonCyan
        }

        val infiniteTransition = rememberInfiniteTransition(label = "bannerPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, severityColor),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("reactive_emergency_alert_banner")
        ) {
            Row(
                modifier = Modifier
                    .background(severityColor.copy(alpha = pulseAlpha * 0.35f))
                    .clickable { onOpenModal(alert) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Siren Flashing Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(severityColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency Alert",
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Alert details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = severityColor
                        ) {
                            Text(
                                text = stringResource(R.string.emergency_broadcast),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.emergency_exp, alert.expiresTime),
                            color = SleekTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = alert.title,
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = alert.summary,
                        color = SleekTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Action Pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = severityColor,
                    modifier = Modifier.clickable { onOpenModal(alert) }
                ) {
                    Text(
                        text = stringResource(R.string.emergency_safety_protocol),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.emergency_dismiss), tint = SleekTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
