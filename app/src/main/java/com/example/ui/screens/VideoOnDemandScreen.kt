package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.VodStory
import com.example.ui.theme.*
import kotlin.math.sin

private data class VodCategoryItem(val id: String, val nameRes: Int)

@Composable
fun VideoOnDemandScreen(
    stories: List<VodStory>,
    activePlayingStory: VodStory?,
    onPlayStory: (VodStory?) -> Unit,
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
            "severe" -> stories.filter { it.category.contains("Severe", ignoreCase = true) }
            "forecasts" -> stories.filter { it.category.contains("Forecast", ignoreCase = true) }
            "earth" -> stories.filter { it.category.contains("Earth", ignoreCase = true) }
            "climate" -> stories.filter { it.category.contains("Climate", ignoreCase = true) }
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
            Column {
                Text(
                    text = stringResource(R.string.vod_title),
                    color = TextPrimaryWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = stringResource(R.string.vod_subtitle),
                    color = TextSecondarySilver,
                    fontSize = 11.sp
                )
            }

            Surface(shape = RoundedCornerShape(4.dp), color = SevereRed) {
                Text(
                    text = stringResource(R.string.vod_quality_badge),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat.id == selectedCategoryId
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) HorizonCyan else TVSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) HorizonCyan else TVCardBorder
                    ),
                    modifier = Modifier.clickable { selectedCategoryId = cat.id }
                ) {
                    Text(
                        text = stringResource(cat.nameRes).uppercase(),
                        color = if (isSelected) Color.Black else TextSecondarySilver,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stories Grid
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredStories) { story ->
                VodStoryItemCard(
                    story = story,
                    onClick = { onPlayStory(story) }
                )
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TVCardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, TVCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("vod_card_${story.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Video Thumbnail with Play Button
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(story.accentColorHex).copy(alpha = 0.4f), Color(0xFF0F172A))
                        )
                    )
                    .border(1.dp, TVCardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = HorizonCyan.copy(alpha = 0.9f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.common_play_pause), tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = story.duration, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Story Info
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(story.accentColorHex).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = story.category.uppercase(),
                        color = Color(story.accentColorHex),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.title,
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.description,
                    color = TextSecondarySilver,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.vod_published, story.presenter, story.timestamp),
                    color = HorizonSky,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun VodPlayerModal(
    story: VodStory,
    allStories: List<VodStory>,
    onDismiss: () -> Unit,
    onSwitchStory: (VodStory) -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

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

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .background(TVMidnightNavy, RoundedCornerShape(16.dp))
                .border(1.5.dp, HorizonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            // Player Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        color = TextPrimaryWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = TextSecondarySilver)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated TV Player Canvas Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, TVCardBorder, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Deep television field gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(story.accentColorHex).copy(alpha = 0.3f), Color(0xFF030712))
                        )
                    )

                    // Atmospheric sound/radar wave simulation
                    if (isPlaying) {
                        for (i in 0..12) {
                            val x = (i / 12f) * w
                            val barH = (sin((i.toDouble() + waveOffset.toDouble() * 6.0) * 1.5) * 40.0 + 50.0).toFloat()
                            drawLine(
                                color = HorizonCyan.copy(alpha = 0.4f),
                                start = Offset(x, h / 2f - barH),
                                end = Offset(x, h / 2f + barH),
                                strokeWidth = 8f
                            )
                        }
                    }
                }

                // Center Watermark / Status
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayCircleOutline else Icons.Default.PauseCircleOutline,
                        contentDescription = stringResource(R.string.vod_streaming_feed),
                        tint = HorizonCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPlaying) stringResource(R.string.vod_streaming_feed) else stringResource(R.string.vod_paused),
                        color = HorizonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Bottom Video Controls Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.common_play_pause),
                                tint = HorizonCyan
                            )
                        }
                        IconButton(onClick = { isMuted = !isMuted }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = stringResource(R.string.home_audio),
                                tint = TextSecondarySilver
                            )
                        }
                        Text(text = "03:42 / ${story.duration}", color = TextSecondarySilver, fontSize = 10.sp)
                    }

                    Text(text = stringResource(R.string.vod_produced_by), color = HorizonSky, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = story.description,
                color = TextSecondarySilver,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Related Stories
            Text(
                text = stringResource(R.string.vod_more_in_category),
                color = TextPrimaryWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allStories.filter { it.id != story.id }.take(2).forEach { rel ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TVSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TVCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSwitchStory(rel) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = rel.title, color = TextPrimaryWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = "${rel.presenter} • ${rel.duration}", color = HorizonSky, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
