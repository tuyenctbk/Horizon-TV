package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.ui.theme.*

@Composable
fun WeatherAlertModal(
    alert: SevereWeatherAlert?,
    onDismiss: () -> Unit
) {
    if (alert == null) return

    val severityColor = when (alert.severity) {
        AlertSeverity.EMERGENCY -> SevereRed
        AlertSeverity.WARNING -> Color(0xFFEA580C)
        AlertSeverity.WATCH -> SolarGold
        AlertSeverity.ADVISORY -> HorizonCyan
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .background(SleekSurface, RoundedCornerShape(20.dp))
                .border(1.5.dp, severityColor, RoundedCornerShape(20.dp))
                .padding(24.dp)
                .testTag("weather_alert_modal")
        ) {
            // Emergency Header Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(severityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = stringResource(R.string.alert_warning),
                        tint = severityColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = severityColor
                    ) {
                        Text(
                            text = "${alert.severity} " + stringResource(R.string.alert_bulletin),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert.title,
                        color = SleekTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurfaceSecondary, RoundedCornerShape(12.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.alert_meteorological_telemetry),
                    color = SleekBluePrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.summary,
                    color = SleekTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Safety Guidelines
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(severityColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, severityColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.alert_civil_defense),
                    color = severityColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.safetyInstructions,
                    color = SleekTextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timing info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.alert_effective, alert.effectiveTime),
                    color = SleekTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = alert.expiresTime,
                    color = severityColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Acknowledge Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = severityColor),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("dismiss_alert_button")
            ) {
                Text(
                    text = stringResource(R.string.alert_acknowledge_advisory),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
