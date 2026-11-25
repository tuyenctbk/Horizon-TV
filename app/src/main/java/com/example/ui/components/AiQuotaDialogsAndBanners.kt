package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.AiQuotaSnapshot
import com.example.ui.theme.*

@Composable
fun AiSoftNudgeBanner(
    quota: AiQuotaSnapshot,
    resetCountdown: String,
    onSwitchToEcoLite: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = quota.isSoftNudgeActive && !quota.ecoLiteMode,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SleekYellowContainer,
            border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("ai_soft_nudge_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = stringResource(R.string.ai_eco_mode_toggle),
                        tint = SleekYellowText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.ai_soft_nudge_title,
                            quota.usagePercent,
                            quota.remaining,
                            resetCountdown
                        ),
                        color = SleekYellowText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onSwitchToEcoLite,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SolarGold,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("ai_switch_eco_lite_button")
                ) {
                    Text(
                        text = stringResource(R.string.ai_soft_nudge_cta),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AiQuotaRecoveryCard(
    quota: AiQuotaSnapshot,
    resetCountdown: String,
    onClaimGraceCredits: () -> Unit,
    onRetryEcoLite: () -> Unit,
    onUseLocalFallback: () -> Unit,
    onOpenByokDialog: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recoveryTitle = stringResource(R.string.ai_recovery_title)
    val graceBtnLabel = if (quota.graceClaimed) {
        stringResource(R.string.ai_recovery_grace_claimed)
    } else {
        stringResource(R.string.ai_recovery_grace_btn)
    }
    val ecoBtnLabel = stringResource(R.string.ai_recovery_eco_btn)
    val localBtnLabel = stringResource(R.string.ai_recovery_local_btn)
    val byokBtnLabel = stringResource(R.string.ai_recovery_byok_btn)
    val resetNoteLabel = stringResource(R.string.ai_recovery_reset_note, resetCountdown)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SleekSurface,
        border = BorderStroke(1.5.dp, SleekBluePrimary.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_quota_recovery_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { heading() }
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = recoveryTitle,
                        tint = SleekBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recoveryTitle,
                        color = SleekTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = SleekTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Personal Value Receipt
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SleekSurfaceSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        R.string.ai_recovery_receipt,
                        quota.briefingUsed,
                        quota.chatUsed,
                        quota.cachedSaves
                    ),
                    color = SleekTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. One-time daily +3 Grace Credits
            Button(
                onClick = onClaimGraceCredits,
                enabled = !quota.graceClaimed && !quota.hasByokKey,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekBluePrimary,
                    disabledContainerColor = SleekSurfaceSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ai_claim_grace_button")
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = graceBtnLabel, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = graceBtnLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Retry with Eco Lite AI
            OutlinedButton(
                onClick = onRetryEcoLite,
                border = BorderStroke(1.dp, SleekBluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ai_retry_eco_button")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = ecoBtnLabel, tint = SleekBluePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ecoBtnLabel,
                    color = SleekBluePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Zero-Quota Instant Local Meteorologist
            FilledTonalButton(
                onClick = onUseLocalFallback,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SleekGreenContainer,
                    contentColor = SleekGreenText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ai_local_fallback_button")
            ) {
                Icon(Icons.Default.OfflineBolt, contentDescription = localBtnLabel, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = localBtnLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Optional BYOK Unlimited Pass-Through
            TextButton(
                onClick = onOpenByokDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ai_open_byok_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = byokBtnLabel, tint = SleekTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = byokBtnLabel,
                    color = SleekTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Visible reset countdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekSurfaceDashed, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) { }
            ) {
                Icon(Icons.Default.Schedule, contentDescription = resetNoteLabel, tint = SleekTextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = resetNoteLabel,
                    color = SleekTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ByokApiKeyDialog(
    currentKey: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember(currentKey) { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    val dialogTitle = stringResource(R.string.ai_byok_dialog_title)
    val keyLabel = stringResource(R.string.ai_byok_key_label)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SleekSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) { heading() }
            ) {
                Icon(Icons.Default.Key, contentDescription = dialogTitle, tint = SleekBluePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dialogTitle,
                    color = SleekTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.ai_byok_dialog_desc),
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text(keyLabel) },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = keyLabel
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("byok_key_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyInput.isNotBlank()) {
                        onSaveKey(keyInput)
                    } else {
                        onClearKey()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                modifier = Modifier.testTag("byok_save_button")
            ) {
                Text(stringResource(R.string.ai_byok_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (currentKey.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onClearKey()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("byok_clear_button")
                    ) {
                        Text(stringResource(R.string.ai_byok_clear), color = SevereRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ai_byok_cancel), color = SleekTextSecondary)
                }
            }
        }
    )
}
