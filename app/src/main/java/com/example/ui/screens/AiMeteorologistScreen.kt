package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.AiCapabilityTier
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AiWeatherBriefing
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.data.repository.FullWeatherPackage
import com.example.ui.components.AiQuotaRecoveryCard
import com.example.ui.components.AiSoftNudgeBanner
import com.example.ui.components.ByokApiKeyDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiBriefingUiState
import com.example.ui.viewmodel.AiMeteorologistViewModel
import java.util.Locale

@Composable
fun AiMeteorologistScreen(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    weatherPackage: FullWeatherPackage?,
    unitSystem: UnitSystem,
    aiViewModel: AiMeteorologistViewModel,
    modifier: Modifier = Modifier
) {
    if (currentWeather == null || weatherPackage == null) {
        val loadingDesc = stringResource(R.string.ai_briefing_loading)
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = HorizonCyan,
                modifier = Modifier.semantics { contentDescription = loadingDesc }
            )
        }
        return
    }

    val context = LocalContext.current
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    var questionInput by remember { mutableStateOf("") }
    var showByokDialog by remember { mutableStateOf(false) }
    var isSpeakingTts by remember { mutableStateOf(false) }

    // On-device Android TextToSpeech (100% free, zero API cost)
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine.language = Locale.getDefault()
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeakingTts = true
                    }
                    override fun onDone(utteranceId: String?) {
                        isSpeakingTts = false
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeakingTts = false
                    }
                })
                ttsEngine = engine
            }
        }
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsEngine = null
            isSpeakingTts = false
        }
    }

    LaunchedEffect(Unit) {
        aiViewModel.refreshQuota()
    }

    val resetCountdown = remember(uiState.quota.millisUntilReset) {
        aiViewModel.formatResetCountdown(uiState.quota.millisUntilReset)
    }

    val activeTierLabel = when {
        uiState.quota.hasByokKey -> stringResource(R.string.ai_tier_byok)
        uiState.quota.shouldPreferEcoLite -> stringResource(R.string.ai_tier_eco)
        else -> stringResource(R.string.ai_tier_primary)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_meteorologist_screen")
    ) {
        // Studio Header + Free-Tier Quota Telemetry Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_desk_title, currentLocation.name.uppercase()),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.ai_desk_subtitle),
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Tier & Quota Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = if (uiState.quota.hasByokKey) SleekGreenContainer else SleekBlueContainer,
                modifier = Modifier.semantics(mergeDescendants = true) { }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.quota.hasByokKey) Icons.Default.Key else Icons.Default.AutoAwesome,
                        contentDescription = activeTierLabel,
                        tint = if (uiState.quota.hasByokKey) SleekGreenText else SleekOnBlueContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = activeTierLabel,
                        color = if (uiState.quota.hasByokKey) SleekGreenText else SleekOnBlueContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            val quotaText = if (uiState.quota.hasByokKey) {
                stringResource(R.string.ai_quota_unlimited)
            } else {
                stringResource(R.string.ai_quota_pill, uiState.quota.remaining, uiState.quota.totalLimit)
            }
            // Quota Pill (tap to open Quota & Value Receipt card)
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = quotaText
                    }
                    .clickable { aiViewModel.toggleQuotaRecoveryCard(!uiState.showQuotaRecoveryCard) }
                    .testTag("ai_quota_pill_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DataUsage, contentDescription = quotaText, tint = SleekBluePrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = quotaText,
                        color = SleekTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Eco Lite Quick Toggle Chip
            val ecoModeLabel = stringResource(R.string.ai_eco_mode_toggle)
            FilterChip(
                selected = uiState.quota.ecoLiteMode,
                onClick = { aiViewModel.toggleEcoLiteMode() },
                label = {
                    Text(
                        text = ecoModeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Bolt, contentDescription = ecoModeLabel, modifier = Modifier.size(14.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SleekSurface,
                    labelColor = SleekTextSecondary,
                    selectedContainerColor = SleekGreenContainer,
                    selectedLabelColor = SleekGreenText,
                    selectedLeadingIconColor = SleekGreenText
                ),
                modifier = Modifier.testTag("ai_eco_lite_chip")
            )

            if (!uiState.quota.hasByokKey) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = stringResource(R.string.ai_quota_reset_in, resetCountdown),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stage 1: Soft Nudge Banner at >= 80% Usage
            item {
                AiSoftNudgeBanner(
                    quota = uiState.quota,
                    resetCountdown = resetCountdown,
                    onSwitchToEcoLite = { aiViewModel.enableEcoLiteMode() }
                )
            }

            // Stage 2: Smart Quota Recovery Card
            if (uiState.showQuotaRecoveryCard || uiState.quota.isExhausted) {
                item {
                    AiQuotaRecoveryCard(
                        quota = uiState.quota,
                        resetCountdown = resetCountdown,
                        onClaimGraceCredits = { aiViewModel.claimDailyGraceCredits() },
                        onRetryEcoLite = {
                            aiViewModel.enableEcoLiteMode()
                            aiViewModel.generateBriefing(
                                location = currentLocation,
                                current = currentWeather,
                                pkg = weatherPackage,
                                unitSystem = unitSystem,
                                forceRefresh = true,
                                forceEcoLite = true
                            )
                        },
                        onUseLocalFallback = {
                            aiViewModel.useLocalFallbackBriefing(
                                location = currentLocation,
                                current = currentWeather,
                                pkg = weatherPackage,
                                unitSystem = unitSystem
                            )
                            if (uiState.lastQuestion.isNotBlank()) {
                                aiViewModel.sendLocalReflexAnswer(
                                    question = uiState.lastQuestion,
                                    location = currentLocation,
                                    current = currentWeather,
                                    pkg = weatherPackage,
                                    unitSystem = unitSystem
                                )
                            }
                        },
                        onOpenByokDialog = { showByokDialog = true },
                        onDismiss = { aiViewModel.toggleQuotaRecoveryCard(false) }
                    )
                }
            }

            // 1-Tap AI Broadcast Briefing Card
            item {
                AiBroadcastBriefingCard(
                    briefingState = uiState.briefingState,
                    isSpeakingTts = isSpeakingTts,
                    onGenerate = { forceRefresh ->
                        aiViewModel.generateBriefing(
                            location = currentLocation,
                            current = currentWeather,
                            pkg = weatherPackage,
                            unitSystem = unitSystem,
                            forceRefresh = forceRefresh
                        )
                    },
                    onUseLocalFallback = {
                        aiViewModel.useLocalFallbackBriefing(
                            location = currentLocation,
                            current = currentWeather,
                            pkg = weatherPackage,
                            unitSystem = unitSystem
                        )
                    },
                    onToggleSpeak = { briefing ->
                        val engine = ttsEngine
                        if (engine != null) {
                            if (isSpeakingTts) {
                                engine.stop()
                                isSpeakingTts = false
                            } else {
                                val script = "${briefing.headline}. ${briefing.broadcastSummary} ${briefing.outfitAndCommute} ${briefing.outdoorSafety}"
                                engine.speak(script, TextToSpeech.QUEUE_FLUSH, null, "horizon_briefing_tts")
                            }
                        }
                    }
                )
            }

            // Ask AI Meteorologist Section Header + Quick Prompts
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SleekSurface, RoundedCornerShape(20.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val chatSectionLabel = stringResource(R.string.ai_chat_section)
                            Icon(Icons.Default.Forum, contentDescription = chatSectionLabel, tint = SleekBluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = chatSectionLabel,
                                color = SleekTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.semantics { heading() }
                            )
                        }
                        if (uiState.messages.isNotEmpty()) {
                            TextButton(onClick = { aiViewModel.clearChat() }) {
                                Text(
                                    text = stringResource(R.string.ai_chat_clear),
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val quickPrompts = listOf(
                        stringResource(R.string.ai_prompt_commute),
                        stringResource(R.string.ai_prompt_run),
                        stringResource(R.string.ai_prompt_wear),
                        stringResource(R.string.ai_prompt_weekend)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrompts.forEachIndexed { idx, prompt ->
                            AssistChip(
                                onClick = {
                                    aiViewModel.sendChatMessage(
                                        question = prompt,
                                        location = currentLocation,
                                        current = currentWeather,
                                        pkg = weatherPackage,
                                        unitSystem = unitSystem
                                    )
                                },
                                label = {
                                    Text(
                                        text = prompt,
                                        color = SleekOnBlueContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SleekBlueContainer.copy(alpha = 0.7f)
                                ),
                                border = null,
                                modifier = Modifier.testTag("ai_quick_prompt_$idx")
                            )
                        }
                    }

                    if (uiState.messages.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ai_chat_empty_hint, currentLocation.name),
                            color = SleekTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Transparent Cloud Error Bar with 1-Tap Local Reflex Fallback
                    uiState.chatError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SleekRedContainer,
                            border = BorderStroke(1.dp, SevereRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = err,
                                    color = SevereRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            aiViewModel.sendLocalReflexAnswer(
                                                question = uiState.lastQuestion,
                                                location = currentLocation,
                                                current = currentWeather,
                                                pkg = weatherPackage,
                                                unitSystem = unitSystem
                                            )
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = SleekGreenContainer,
                                            contentColor = SleekGreenText
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ai_local_fallback_chip),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (uiState.chatRateLimited) {
                                        OutlinedButton(
                                            onClick = {
                                                aiViewModel.enableEcoLiteMode()
                                                aiViewModel.sendChatMessage(
                                                    question = uiState.lastQuestion,
                                                    location = currentLocation,
                                                    current = currentWeather,
                                                    pkg = weatherPackage,
                                                    unitSystem = unitSystem,
                                                    forceEcoLite = true
                                                )
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.ai_recovery_eco_btn),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Streaming Chat Messages
            items(uiState.messages, key = { it.id }) { msg ->
                AiChatBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Input Bar
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SleekSurface,
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.ai_chat_placeholder),
                            color = SleekTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        val q = questionInput.trim()
                        if (q.isNotEmpty()) {
                            questionInput = ""
                            aiViewModel.sendChatMessage(
                                question = q,
                                location = currentLocation,
                                current = currentWeather,
                                pkg = weatherPackage,
                                unitSystem = unitSystem
                            )
                        }
                    },
                    enabled = questionInput.isNotBlank() && !uiState.isStreamingChat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (questionInput.isNotBlank() && !uiState.isStreamingChat) SleekBluePrimary else SleekSurfaceSecondary
                        )
                        .testTag("ai_chat_send_button")
                ) {
                    if (uiState.isStreamingChat) {
                        val streamingDesc = stringResource(R.string.ai_briefing_loading)
                        CircularProgressIndicator(
                            color = SleekBluePrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .semantics { contentDescription = streamingDesc }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.ai_chat_send),
                            tint = if (questionInput.isNotBlank()) Color.White else SleekTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showByokDialog) {
        ByokApiKeyDialog(
            currentKey = aiViewModel.getCurrentByokKey(),
            onSaveKey = { aiViewModel.saveByokKey(it) },
            onClearKey = { aiViewModel.clearByokKey() },
            onDismiss = { showByokDialog = false }
        )
    }
}

@Composable
private fun AiBroadcastBriefingCard(
    briefingState: AiBriefingUiState,
    isSpeakingTts: Boolean,
    onGenerate: (forceRefresh: Boolean) -> Unit,
    onUseLocalFallback: () -> Unit,
    onToggleSpeak: (AiWeatherBriefing) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SleekSurface,
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_broadcast_briefing_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val briefingSectionLabel = stringResource(R.string.ai_briefing_section)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SleekBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = briefingSectionLabel, tint = SleekBluePrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = briefingSectionLabel,
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                if (briefingState is AiBriefingUiState.Success) {
                    val refreshLabel = stringResource(R.string.ai_briefing_refresh)
                    TextButton(
                        onClick = { onGenerate(true) },
                        modifier = Modifier.testTag("ai_briefing_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = refreshLabel, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(refreshLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (briefingState) {
                is AiBriefingUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.ai_briefing_idle_hint),
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val generateLabel = stringResource(R.string.ai_briefing_generate)
                        val localFallbackLabel = stringResource(R.string.ai_local_fallback_chip)
                        Button(
                            onClick = { onGenerate(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("ai_generate_briefing_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = generateLabel, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(generateLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = onUseLocalFallback,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SleekGreenContainer,
                                contentColor = SleekGreenText
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("ai_briefing_local_quick_button")
                        ) {
                            Icon(Icons.Default.OfflineBolt, contentDescription = localFallbackLabel, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                is AiBriefingUiState.Loading -> {
                    val loadingLabel = stringResource(R.string.ai_briefing_loading)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = SleekBluePrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .semantics { contentDescription = loadingLabel }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = loadingLabel,
                            color = SleekTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is AiBriefingUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SleekRedContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = briefingState.message,
                                color = SevereRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onGenerate(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(stringResource(R.string.seven_day_retry), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                FilledTonalButton(
                                    onClick = onUseLocalFallback,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = SleekGreenContainer,
                                        contentColor = SleekGreenText
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(stringResource(R.string.ai_local_fallback_chip), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                is AiBriefingUiState.Success -> {
                    val b = briefingState.briefing
                    val badgeBg = when {
                        b.tierUsed == AiCapabilityTier.LOCAL_METEOROLOGY -> SleekGreenContainer
                        b.isFromCache -> SleekYellowContainer
                        else -> SleekBlueContainer
                    }
                    val badgeFg = when {
                        b.tierUsed == AiCapabilityTier.LOCAL_METEOROLOGY -> SleekGreenText
                        b.isFromCache -> SleekYellowText
                        else -> SleekOnBlueContainer
                    }
                    val badgeLabel = when {
                        b.tierUsed == AiCapabilityTier.LOCAL_METEOROLOGY -> stringResource(R.string.ai_tier_local)
                        b.isFromCache -> "${b.modelLabel} • ${stringResource(R.string.ai_tier_cached)}"
                        else -> b.modelLabel
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(percent = 50), color = badgeBg) {
                            Text(
                                text = badgeLabel,
                                color = badgeFg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }

                        // Zero-Cost On-Device Voice Anchor Button
                        val voiceActionLabel = stringResource(
                            if (isSpeakingTts) R.string.ai_briefing_stop_audio else R.string.ai_briefing_listen
                        )
                        FilledTonalButton(
                            onClick = { onToggleSpeak(b) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSpeakingTts) SleekRedContainer else SleekSurfaceSecondary,
                                contentColor = if (isSpeakingTts) SevereRed else SleekBluePrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { stateDescription = voiceActionLabel }
                                .testTag("ai_voice_anchor_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeakingTts) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = voiceActionLabel,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = voiceActionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = b.headline,
                        color = SleekTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 21.sp,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = b.broadcastSummary,
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BriefingDetailRow(
                            icon = Icons.Default.Checkroom,
                            label = stringResource(R.string.ai_briefing_outfit),
                            value = b.outfitAndCommute,
                            accent = SleekBluePrimary
                        )
                        BriefingDetailRow(
                            icon = Icons.Default.HealthAndSafety,
                            label = stringResource(R.string.ai_briefing_safety),
                            value = b.outdoorSafety,
                            accent = SolarGold
                        )
                        BriefingDetailRow(
                            icon = Icons.Default.WbSunny,
                            label = stringResource(R.string.ai_briefing_window),
                            value = b.bestWindowToday,
                            accent = SleekGreenText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefingDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SleekSurfaceSecondary, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = SleekTextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AiChatBubble(message: AiChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) SleekBluePrimary else SleekSurface,
            border = if (message.isUser) null else BorderStroke(1.dp, SleekBorder),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .semantics(mergeDescendants = true) {
                    if (!message.isUser) {
                        liveRegion = LiveRegionMode.Polite
                    }
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.isUser && message.modelLabel.isNotBlank()) {
                    Text(
                        text = if (message.isFromCache) stringResource(R.string.ai_cached_label_fmt, message.modelLabel) else message.modelLabel,
                        color = if (message.tierUsed == AiCapabilityTier.LOCAL_METEOROLOGY) SleekGreenText else SleekBluePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = if (message.text.isBlank() && message.isStreaming) "…" else message.text,
                    color = if (message.isUser) Color.White else SleekTextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
