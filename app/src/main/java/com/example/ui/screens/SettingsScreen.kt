package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.ai.AiQuotaSnapshot
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.UnitSystem
import com.example.data.worker.SevereWeatherAlertNotificationHelper
import com.example.ui.components.ByokApiKeyDialog
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
    onRateApp: () -> Unit = {},
    onShareApp: () -> Unit = {},
    aiQuota: AiQuotaSnapshot? = null,
    aiResetCountdown: String = "",
    currentByokKey: String = "",
    onToggleAiEcoLite: () -> Unit = {},
    onClaimAiGraceCredits: () -> Unit = {},
    onSaveByokKey: (String) -> Unit = {},
    onClearByokKey: () -> Unit = {},
    isBackgroundAlertsEnabled: Boolean = true,
    backgroundSyncIntervalMinutes: Int = 30,
    lastBackgroundSyncSummary: String = "",
    onToggleBackgroundAlerts: () -> Unit = {},
    onSelectBackgroundInterval: (Int) -> Unit = {},
    onSyncBackgroundNow: () -> Unit = {},
    onTestPushNotification: (SevereWeatherAlert) -> Unit = {},
    onOpenOnboardingTour: () -> Unit = {},
    cachedStationCount: Int = 0,
    lastSyncedAtMs: Long? = null,
    onClearWeatherCache: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showByokDialog by remember { mutableStateOf(false) }
    var hasNotifPerm by remember {
        mutableStateOf(SevereWeatherAlertNotificationHelper.hasNotificationPermission(context))
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

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
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.settings_header_subtitle),
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_display))
        }

        item {
            SettingsActionCard(
                icon = Icons.Default.LiveTv,
                title = stringResource(R.string.settings_onboarding_title),
                subtitle = stringResource(R.string.settings_onboarding_subtitle),
                actionLabel = stringResource(R.string.settings_onboarding_action),
                accentColor = HorizonCyan,
                onClick = onOpenOnboardingTour
            )
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                        Spacer(modifier = Modifier.width(8.dp))

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
                                .semantics(mergeDescendants = true) {
                                    role = Role.RadioButton
                                    selected = isCelsius
                                }
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
                                .semantics(mergeDescendants = true) {
                                    role = Role.RadioButton
                                    selected = isFahrenheit
                                }
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
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.settings_audio_title),
                subtitle = stringResource(R.string.settings_audio_subtitle),
                isChecked = isLiveAudioPlaying,
                onToggle = onToggleLiveAudio
            )
        }

        if (aiQuota != null) {
            item {
                SettingsCategoryHeader(stringResource(R.string.settings_cat_ai))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_ai_quota_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_ai_quota_title),
                                color = SleekTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (aiQuota.hasByokKey) SleekGreenContainer else SleekBlueContainer
                            ) {
                                Text(
                                    text = if (aiQuota.hasByokKey) {
                                        stringResource(R.string.ai_quota_unlimited)
                                    } else {
                                        stringResource(R.string.ai_quota_pill, aiQuota.remaining, aiQuota.totalLimit)
                                    },
                                    color = if (aiQuota.hasByokKey) SleekGreenText else SleekOnBlueContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                R.string.ai_recovery_receipt,
                                aiQuota.briefingUsed,
                                aiQuota.chatUsed,
                                aiQuota.cachedSaves
                            ),
                            color = SleekTextSecondary,
                            fontSize = 11.sp
                        )
                        if (!aiQuota.hasByokKey && aiResetCountdown.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.ai_recovery_reset_note, aiResetCountdown),
                                color = SleekTextMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (!aiQuota.hasByokKey && !aiQuota.graceClaimed && aiQuota.remaining <= 2) {
                            val graceLabel = stringResource(R.string.ai_recovery_grace_btn)
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onClaimAiGraceCredits,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SleekBlueContainer,
                                    contentColor = SleekOnBlueContainer
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = graceLabel, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(graceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                SettingsToggleCard(
                    icon = Icons.Default.Bolt,
                    title = stringResource(R.string.settings_ai_eco_title),
                    subtitle = stringResource(R.string.settings_ai_eco_subtitle),
                    isChecked = aiQuota.ecoLiteMode,
                    onToggle = onToggleAiEcoLite
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Key,
                    title = stringResource(R.string.settings_ai_byok_title),
                    subtitle = if (aiQuota.hasByokKey) {
                        stringResource(R.string.ai_byok_status_active)
                    } else {
                        stringResource(R.string.ai_byok_status_inactive)
                    },
                    actionLabel = if (aiQuota.hasByokKey) {
                        stringResource(R.string.settings_ai_byok_action_manage)
                    } else {
                        stringResource(R.string.settings_ai_byok_action_add)
                    },
                    accentColor = if (aiQuota.hasByokKey) SleekGreenText else SleekBluePrimary,
                    onClick = { showByokDialog = true }
                )
            }
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_civil))
        }

        item {
            val testAlertTitle = stringResource(R.string.alert_test_title)
            val testAlertSummary = stringResource(R.string.alert_test_summary)
            val testAlertInstructions = stringResource(R.string.alert_test_instructions)
            val testEffectiveTime = stringResource(R.string.alert_test_effective_time)
            val testExpiresTime = stringResource(R.string.alert_test_expires_time)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_workmanager_alert_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SleekBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = stringResource(R.string.settings_bg_alert_title),
                                    tint = SleekBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isBackgroundAlertsEnabled) SleekGreenContainer else SleekSurfaceSecondary
                                    ) {
                                        Text(
                                            text = stringResource(
                                                if (isBackgroundAlertsEnabled) R.string.settings_bg_status_active
                                                else R.string.settings_bg_status_paused
                                            ),
                                            color = if (isBackgroundAlertsEnabled) SleekGreenText else SleekTextMuted,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = stringResource(R.string.settings_bg_alert_title),
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.settings_bg_alert_subtitle),
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isBackgroundAlertsEnabled,
                            onCheckedChange = {
                                if (!isBackgroundAlertsEnabled &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !hasNotifPerm
                                ) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onToggleBackgroundAlerts()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SleekBluePrimary,
                                checkedTrackColor = SleekBlueContainer,
                                uncheckedThumbColor = SleekTextMuted,
                                uncheckedTrackColor = SleekSurfaceSecondary
                            ),
                            modifier = Modifier.testTag("bg_alerts_toggle")
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                        val grantPermLabel = stringResource(R.string.settings_bg_perm_grant_btn)
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SleekYellowContainer,
                                contentColor = SleekYellowText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("grant_notif_permission_button")
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = grantPermLabel, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = grantPermLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.settings_bg_interval_label),
                        color = SleekTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val intervalOptions = listOf(
                        15 to stringResource(R.string.settings_bg_interval_15m),
                        30 to stringResource(R.string.settings_bg_interval_30m),
                        60 to stringResource(R.string.settings_bg_interval_60m)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervalOptions.forEach { (mins, label) ->
                            val isIntervalSelected = backgroundSyncIntervalMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isIntervalSelected) SleekBlueContainer else SleekSurfaceSecondary,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isIntervalSelected) SleekBluePrimary else SleekBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .semantics(mergeDescendants = true) {
                                        role = Role.RadioButton
                                        selected = isIntervalSelected
                                    }
                                    .clickable { onSelectBackgroundInterval(mins) }
                                    .testTag("bg_interval_${mins}m_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isIntervalSelected) SleekOnBlueContainer else SleekTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (lastBackgroundSyncSummary.isNotBlank()) {
                            stringResource(R.string.settings_bg_last_sync, lastBackgroundSyncSummary)
                        } else {
                            stringResource(R.string.settings_bg_last_sync_pending)
                        },
                        color = SleekTextMuted,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val syncNowLabel = stringResource(R.string.settings_bg_sync_now_btn)
                    OutlinedButton(
                        onClick = onSyncBackgroundNow,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("bg_sync_now_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = syncNowLabel, tint = SleekBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncNowLabel,
                            color = SleekBluePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_cache))
        }

        item {
            val formattedSync = remember(lastSyncedAtMs) {
                if (lastSyncedAtMs != null && lastSyncedAtMs > 0L) {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastSyncedAtMs))
                } else {
                    "--:--"
                }
            }
            val clearedMsg = stringResource(R.string.settings_cache_cleared)
            val clearLabel = stringResource(R.string.settings_cache_clear)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SleekSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_room_cache_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = SleekBluePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_cache_title),
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.settings_cache_subtitle),
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (cachedStationCount > 0) {
                            stringResource(R.string.settings_cache_status_fmt, cachedStationCount, formattedSync)
                        } else {
                            stringResource(R.string.settings_cache_empty)
                        },
                        color = SleekBluePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (cachedStationCount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                onClearWeatherCache()
                                Toast.makeText(context, clearedMsg, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_clear_cache_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = clearLabel, tint = SleekRedText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = clearLabel,
                                color = SleekRedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_community))
        }

        item {
            val shareText = stringResource(R.string.suggestion_share_text)
            val feedbackSubject = stringResource(R.string.settings_feedback_email_subject)
            val copiedMessageFormat = stringResource(R.string.settings_feedback_copied)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsActionCard(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.settings_rate_title),
                    subtitle = stringResource(R.string.settings_rate_subtitle),
                    actionLabel = stringResource(R.string.settings_action_rate),
                    accentColor = SolarGold,
                    onClick = {
                        onRateApp()
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(webIntent)
                            } catch (_: Exception) {}
                        }
                    }
                )

                val shareTitle = stringResource(R.string.settings_share_title)

                SettingsActionCard(
                    icon = Icons.Default.Share,
                    title = shareTitle,
                    subtitle = stringResource(R.string.settings_share_subtitle),
                    actionLabel = stringResource(R.string.settings_action_share),
                    accentColor = SleekBluePrimary,
                    onClick = {
                        onShareApp()
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val chooser = Intent.createChooser(sendIntent, shareTitle).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        } catch (_: Exception) {}
                    }
                )

                val emailTemplate = stringResource(R.string.settings_feedback_email_body_template)
                val feedbackChooserTitle = stringResource(R.string.settings_feedback_title)
                val recipientEmail = stringResource(R.string.settings_feedback_email_recipient)

                SettingsActionCard(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.settings_feedback_title),
                    subtitle = stringResource(R.string.settings_feedback_subtitle),
                    actionLabel = stringResource(R.string.settings_action_email),
                    accentColor = SleekGreenText,
                    onClick = {
                        val recipient = recipientEmail
                        val appVersion = try {
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            val vc = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
                            context.getString(R.string.settings_sys_platform_value, pInfo.versionName ?: BuildConfig.VERSION_NAME, vc)
                        } catch (_: Exception) {
                            context.getString(R.string.settings_sys_platform_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
                        }
                        val emailBody = String.format(
                            java.util.Locale.US,
                            emailTemplate,
                            appVersion,
                            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                            android.os.Build.VERSION.RELEASE
                        )

                        try {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$recipient")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                                putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                                putExtra(Intent.EXTRA_TEXT, emailBody)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(emailIntent)
                        } catch (_: Exception) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "message/rfc822"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                                    putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                                    putExtra(Intent.EXTRA_TEXT, emailBody)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                val chooser = Intent.createChooser(fallbackIntent, feedbackChooserTitle).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } catch (_: Exception) {
                                // Fallback when no email app is installed (e.g. Android TV devices)
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText(feedbackChooserTitle, recipient)
                                    clipboard?.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(
                                        context,
                                        String.format(copiedMessageFormat, recipient),
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                )
            }
        }

        item {
            SettingsCategoryHeader(stringResource(R.string.settings_cat_system))
        }

        item {
            val (pVersion, pVc) = remember(context) {
                try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val vc = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
                    Pair(pInfo.versionName ?: BuildConfig.VERSION_NAME, vc)
                } catch (_: Exception) {
                    Pair(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
                }
            }
            val platformInfo = stringResource(R.string.settings_sys_platform_value, pVersion, pVc)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurfaceSecondary, RoundedCornerShape(16.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemInfoRow(stringResource(R.string.settings_sys_platform), platformInfo)
                SystemInfoRow(stringResource(R.string.settings_sys_provider), stringResource(R.string.settings_sys_provider_value))
                SystemInfoRow(stringResource(R.string.settings_sys_radar), stringResource(R.string.settings_sys_radar_value))
                SystemInfoRow(stringResource(R.string.settings_sys_astronomy), stringResource(R.string.settings_sys_astronomy_value))
                SystemInfoRow(stringResource(R.string.settings_sys_interval), stringResource(R.string.settings_sys_interval_value))
            }
        }
    }

    if (showByokDialog) {
        ByokApiKeyDialog(
            currentKey = currentByokKey,
            onSaveKey = onSaveByokKey,
            onClearKey = onClearByokKey,
            onDismiss = { showByokDialog = false }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF7DD3FC),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .padding(top = 6.dp)
            .semantics { heading() }
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
                ),
                modifier = Modifier.semantics { contentDescription = title }
            )
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SleekTextSecondary, fontSize = 11.sp)
        Text(text = value, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title, $subtitle, $actionLabel"
            }
            .clickable(onClickLabel = actionLabel) { onClick() }
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
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = subtitle, color = SleekTextSecondary, fontSize = 11.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = accentColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = actionLabel,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

