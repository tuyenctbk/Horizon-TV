package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.VodStory
import com.example.ui.components.tvFocusVisual
import com.example.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import kotlinx.coroutines.delay
import kotlin.math.sin

private data class VodCategoryItem(val id: String, val nameRes: Int)

@Composable
fun VideoOnDemandScreen(
    stories: List<VodStory>,
    activePlayingStory: VodStory?,
    onPlayStory: (VodStory?) -> Unit,
    onOpenSchedule: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf(
            VodCategoryItem("all", R.string.vod_cat_all),
            VodCategoryItem("severe", R.string.vod_cat_severe),
            VodCategoryItem("forecasts", R.string.vod_cat_forecasts),
            VodCategoryItem("earth", R.string.vod_cat_earth_sci),
            VodCategoryItem("climate", R.string.vod_cat_climate)
        )
    }
    var selectedCategoryId by remember { mutableStateOf("all") }

    val filteredStories = remember(selectedCategoryId, stories) {
        if (selectedCategoryId == "all") stories
        else when (selectedCategoryId) {
            "severe" -> stories.filter { it.id.startsWith("vod_severe") || it.category.contains("Severe", ignoreCase = true) }
            "forecasts" -> stories.filter { it.id.startsWith("vod_forecasts") || it.category.contains("Forecast", ignoreCase = true) }
            "earth" -> stories.filter { it.id.startsWith("vod_earth") || it.category.contains("Earth", ignoreCase = true) }
            "climate" -> stories.filter { it.id.startsWith("vod_climate") || it.category.contains("Climate", ignoreCase = true) }
            else -> stories
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("vod_hub_view")
    ) {
        // VOD Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vod_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.vod_subtitle),
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onOpenSchedule != null) {
                    val guideLabel = stringResource(R.string.epg_open_guide_btn)
                    Surface(
                        onClick = onOpenSchedule,
                        shape = RoundedCornerShape(8.dp),
                        color = SleekBlueContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBluePrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("vod_open_schedule_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewTimeline,
                                contentDescription = guideLabel,
                                tint = SleekOnBlueContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = guideLabel,
                                color = SleekOnBlueContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(6.dp), color = SevereRed) {
                    Text(
                        text = stringResource(R.string.vod_quality_badge),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it.id }) { cat ->
                val isSelected = cat.id == selectedCategoryId
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) SleekBluePrimary else SleekSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) SleekBluePrimary else SleekBorder
                    ),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .tvFocusVisual(shape = RoundedCornerShape(20.dp), focusBorderColor = BroadcastCyan)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = isSelected
                        }
                        .clickable { selectedCategoryId = cat.id }
                        .testTag("vod_cat_${cat.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(cat.nameRes).uppercase(),
                            color = if (isSelected) Color.White else SleekTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stories List with smooth category transition
        AnimatedContent(
            targetState = filteredStories,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "vodCategoryTransition",
            modifier = Modifier.fillMaxSize()
        ) { displayedStories ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedStories, key = { it.id }) { story ->
                    VodStoryItemCard(
                        story = story,
                        onClick = { onPlayStory(story) }
                    )
                }
            }
        }
    }

    // Fullscreen TV Broadcast Video Player Modal
    if (activePlayingStory != null) {
        VodPlayerModal(
            story = activePlayingStory,
            allStories = stories,
            onDismiss = { onPlayStory(null) },
            onSwitchStory = { onPlayStory(it) }
        )
    }
}

@Composable
private fun VodStoryItemCard(
    story: VodStory,
    onClick: () -> Unit
) {
    val accent = Color(story.accentColorHex)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SleekSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .tvFocusVisual(shape = RoundedCornerShape(18.dp), focusBorderColor = accent, focusedScale = 1.02f)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "${story.category}: ${story.title}, ${story.duration}, ${story.presenter}"
            }
            .clickable(onClickLabel = story.title) { onClick() }
            .testTag("vod_card_${story.id}")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            val isCompact = maxWidth < 400.dp
            if (isCompact) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    VodThumbnailBox(
                        story = story,
                        accent = accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VodStoryTextContent(story = story, accent = accent)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VodThumbnailBox(
                        story = story,
                        accent = accent,
                        modifier = Modifier
                            .width(164.dp)
                            .height(94.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        VodStoryTextContent(story = story, accent = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun VodThumbnailBox(
    story: VodStory,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.copy(alpha = 0.55f), Color(0xFF0A1428))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = SleekBluePrimary,
            shadowElevation = 4.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.common_play_pause),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Duration badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = story.duration,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VodStoryTextContent(
    story: VodStory,
    accent: Color
) {
    Column {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = accent.copy(alpha = 0.14f)
        ) {
            Text(
                text = story.category.uppercase(),
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = story.title,
            color = SleekTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = story.description,
            color = SleekTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.vod_published, story.presenter, story.timestamp),
            color = SleekBluePrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun VodPlayerModal(
    story: VodStory,
    allStories: List<VodStory>,
    onDismiss: () -> Unit,
    onSwitchStory: (VodStory) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    val totalSeconds = remember(story.duration) {
        val parts = story.duration.split(":")
        if (parts.size == 2) {
            val min = parts[0].toIntOrNull() ?: 0
            val sec = parts[1].toIntOrNull() ?: 0
            (min * 60 + sec).coerceAtLeast(10)
        } else 240
    }
    var currentSeconds by remember(story.id) { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, story.id) {
        if (!isPlaying) return@LaunchedEffect
        while (true) {
            delay(1000)
            if (currentSeconds < totalSeconds) {
                currentSeconds++
            } else {
                currentSeconds = 0
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vodWave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val studioBg = Color(0xFF0B1324)
    val studioCard = Color(0xFF152036)
    val studioBorder = Color(0xFF283A5A)
    val studioCyan = Color(0xFF38BDF8)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .background(studioBg, RoundedCornerShape(20.dp))
                .border(1.5.dp, studioCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            // Player Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(story.accentColorHex)) {
                        Text(
                            text = story.category.uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = story.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val streamStateLabel = if (isPlaying) stringResource(R.string.vod_streaming_feed) else stringResource(R.string.vod_paused)
            // Studio Player Canvas Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, studioBorder, RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "${story.title}: $streamStateLabel" }
                ) {
                    val w = size.width
                    val h = size.height

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(story.accentColorHex).copy(alpha = 0.35f), Color(0xFF030712))
                        )
                    )

                    if (isPlaying) {
                        for (i in 0..14) {
                            val x = (i / 14f) * w
                            val barH = (sin((i.toDouble() + waveOffset.toDouble() * 6.0) * 1.4) * 34.0 + 42.0).toFloat()
                            drawLine(
                                color = studioCyan.copy(alpha = 0.35f),
                                start = Offset(x, h / 2f - barH),
                                end = Offset(x, h / 2f + barH),
                                strokeWidth = 7f
                            )
                        }
                    }
                }

                // Center Play/Pause Toggle
                val playPauseDesc = stringResource(R.string.common_play_pause)
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(14.dp))
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            stateDescription = streamStateLabel
                        }
                        .clickable(onClickLabel = playPauseDesc) { isPlaying = !isPlaying }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
                        contentDescription = streamStateLabel,
                        tint = studioCyan,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = streamStateLabel,
                        color = studioCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Bottom Video Controls Bar with Progress
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    val progress = (currentSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                    val elapsedText = String.format(java.util.Locale.US, "%02d:%02d", currentSeconds / 60, currentSeconds % 60)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .semantics { contentDescription = "$elapsedText / ${story.duration}" },
                        color = studioCyan,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = playPauseDesc,
                                    tint = studioCyan
                                )
                            }
                            val muteStateLabel = if (isMuted) stringResource(R.string.livetv_muted) else stringResource(R.string.livetv_audio_on)
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                                    .semantics { stateDescription = muteStateLabel }
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(R.string.home_audio),
                                    tint = Color(0xFFCBD5E1)
                                )
                            }
                            Text(text = "$elapsedText / ${story.duration}", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        }

                        Text(
                            text = stringResource(R.string.vod_produced_by),
                            color = studioCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = story.description,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Related Stories
            Text(
                text = stringResource(R.string.vod_more_in_category),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allStories.filter { it.id != story.id }.take(2).forEach { rel ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = studioCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, studioBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "${rel.title}, ${rel.presenter}, ${rel.duration}"
                            }
                            .clickable(onClickLabel = rel.title) { onSwitchStory(rel) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = rel.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${rel.presenter} • ${rel.duration}",
                                color = studioCyan,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
