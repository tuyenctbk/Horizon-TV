package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.UnitSystem
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    unitSystem: UnitSystem,
    isCrtGrainEnabled: Boolean,
    isVirtualRemoteVisible: Boolean,
    isLiveAudioPlaying: Boolean,
    onToggleUnits: () -> Unit,
    onToggleCrtGrain: () -> Unit,
    onToggleVirtualRemote: () -> Unit,
    onToggleLiveAudio: () -> Unit,
    onTriggerTestAlert: (SevereWeatherAlert) -> Unit,
    onSelectUnitSystem: (UnitSystem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_header_title),
                color = SleekTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = stringResource(R.string.settings_header_subtitle),
                color = SleekTextSecondary,
                fontSize = 12.sp
            )
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_display))
        }

        item {
            SettingsToggleCard(
                icon = Icons.Default.Tv,
                title = stringResource(R.string.settings_crt_title),
                subtitle = stringResource(R.string.settings_crt_subtitle),
                isChecked = isCrtGrainEnabled,
                onToggle = onToggleCrtGrain
            )
        }

        item {
            SettingsToggleCard(
                icon = Icons.Default.SettingsRemote,
                title = stringResource(R.string.settings_remote_title),
                subtitle = stringResource(R.string.settings_remote_subtitle),
                isChecked = isVirtualRemoteVisible,
                onToggle = onToggleVirtualRemote
            )
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_units))
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth().testTag("temperature_units_setting_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SleekSurfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = stringResource(R.string.settings_units_title), tint = SolarGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_units_title),
                                    color = SleekTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.settings_units_subtitle),
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SleekGreenContainer
                        ) {
                            Text(
                                text = stringResource(R.string.settings_saved_locally),
                                color = SleekGreenText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Two-Choice Selector (Celsius vs Fahrenheit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Celsius Option
                        val isCelsius = unitSystem == UnitSystem.METRIC
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCelsius) SleekBlueContainer else SleekSurfaceSecondary,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isCelsius) 1.5.dp else 1.dp,
                                if (isCelsius) SleekBluePrimary else SleekBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectUnitSystem(UnitSystem.METRIC)
                                }
                                .testTag("unit_celsius_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            2.dp,
                                            if (isCelsius) SleekBluePrimary else SleekTextSecondary,
                                            CircleShape
                                        )
                                        .background(if (isCelsius) SleekBluePrimary else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCelsius) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_celsius_title),
                                        color = if (isCelsius) SleekOnBlueContainer else SleekTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_celsius_subtitle),
                                        color = if (isCelsius) SleekBluePrimary else SleekTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Fahrenheit Option
                        val isFahrenheit = unitSystem == UnitSystem.IMPERIAL
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFahrenheit) SleekBlueContainer else SleekSurfaceSecondary,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isFahrenheit) 1.5.dp else 1.dp,
                                if (isFahrenheit) SleekBluePrimary else SleekBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectUnitSystem(UnitSystem.IMPERIAL)
                                }
                                .testTag("unit_fahrenheit_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            2.dp,
                                            if (isFahrenheit) SleekBluePrimary else SleekTextSecondary,
                                            CircleShape
                                        )
                                        .background(if (isFahrenheit) SleekBluePrimary else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFahrenheit) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_fahrenheit_title),
                                        color = if (isFahrenheit) SleekOnBlueContainer else SleekTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_fahrenheit_subtitle),
                                        color = if (isFahrenheit) SleekBluePrimary else SleekTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_audio))
        }

        item {
            SettingsToggleCard(
                icon = Icons.Default.VolumeUp,
                title = stringResource(R.string.settings_audio_title),
                subtitle = stringResource(R.string.settings_audio_subtitle),
                isChecked = isLiveAudioPlaying,
                onToggle = onToggleLiveAudio
            )
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_civil))
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.settings_ebs_title), tint = SleekRedText, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = stringResource(R.string.settings_ebs_title), color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.settings_ebs_subtitle), color = SleekTextSecondary, fontSize = 11.sp)
                        }
                    }

                    val testAlertTitle = stringResource(R.string.alert_test_title)
                    val testAlertSummary = stringResource(R.string.alert_test_summary)
                    val testAlertInstructions = stringResource(R.string.alert_test_instructions)

                    Button(
                        onClick = {
                            val testAlert = SevereWeatherAlert(
                                id = "test_ebs_broadcast",
                                title = testAlertTitle,
                                severity = AlertSeverity.EMERGENCY,
                                effectiveTime = "NOW",
                                expiresTime = "IN 45 MINUTES",
                                summary = testAlertSummary,
                                safetyInstructions = testAlertInstructions
                            )
                            onTriggerTestAlert(testAlert)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekRedContainer),
                        shape = RoundedCornerShape(percent = 50)
                    ) {
                        Text(stringResource(R.string.settings_ebs_btn), color = SleekRedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_system))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemInfoRow(stringResource(R.string.settings_sys_platform), "Horizon TV Android Broadcast v2.4")
                SystemInfoRow(stringResource(R.string.settings_sys_provider), "Open-Meteo Global Forecasting Engine (Free & Open)")
                SystemInfoRow(stringResource(R.string.settings_sys_radar), "Dual-Pol 500kW Doppler Multi-Layer Engine")
                SystemInfoRow(stringResource(R.string.settings_sys_astronomy), "Synodic 29.53-Day Lunar & Daylight Solar Arc Algorithm")
                SystemInfoRow(stringResource(R.string.settings_sys_interval), "Automatic 45-Second Telemetry Polling Active")
            }
        }
    }
}

@Composable
private fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        color = SleekBluePrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekSurfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = SleekBluePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = SleekTextSecondary, fontSize = 11.sp)
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SleekBluePrimary,
                    checkedTrackColor = SleekBlueContainer,
                    uncheckedThumbColor = SleekTextMuted,
                    uncheckedTrackColor = SleekSurfaceSecondary
                )
            )
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SleekTextSecondary, fontSize = 11.sp)
        Text(text = value, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
