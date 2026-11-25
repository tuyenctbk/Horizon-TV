package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BroadcastScheduleSlot
import com.example.data.model.CityLocation
import com.example.data.model.VodStory
import com.example.ui.components.LottieWeatherAnimation
import com.example.ui.components.tvFocusVisual
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class EpgLayoutMode { GRID, LIST }

private data class EpgCategoryFilter(val id: String, val labelRes: Int)

@Composable
fun BroadcastScheduleScreen(
    currentLocation: CityLocation,
    scheduleSlots: List<BroadcastScheduleSlot>,
    remindedSlotIds: Set<String>,
    activePlayingStory: VodStory?,
    allVodStories: List<VodStory>,
    isOfflineCacheMode: Boolean = false,
    onPlayStory: (VodStory?) -> Unit,
    onToggleReminder: (String) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var layoutMode by remember { mutableStateOf(EpgLayoutMode.GRID) }
    var selectedCategoryId by remember { mutableStateOf("all") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val categories = remember {
        listOf(
            EpgCategoryFilter("all", R.string.vod_cat_all),
            EpgCategoryFilter("severe", R.string.vod_cat_severe),
            EpgCategoryFilter("forecasts", R.string.vod_cat_forecasts),
            EpgCategoryFilter("earth", R.string.vod_cat_earth_sci),
            EpgCategoryFilter("climate", R.string.vod_cat_climate)
        )
    }

    val filteredSlots = remember(scheduleSlots, selectedCategoryId) {
        if (selectedCategoryId == "all") scheduleSlots
        else scheduleSlots.filter { it.categoryId == selectedCategoryId }
    }

    val nowPlayingSpotlight = remember(scheduleSlots, nowMs) {
        scheduleSlots.firstOrNull { nowMs in it.startEpochMs until it.endEpochMs }
            ?: scheduleSlots.firstOrNull()
    }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val clockFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val pulseTransition = rememberInfiniteTransition(label = "epgLivePulse")
    val liveDotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "epgLiveDot"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("broadcast_schedule_screen")
    ) {
        // EPG Broadcast Studio Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SevereRed
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = liveDotAlpha))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.epg_badge_247),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    if (isOfflineCacheMode) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SolarGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = stringResource(R.string.offline_badge_cached),
                                color = SolarGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = clockFmt.format(Date(nowMs)),
                        color = BroadcastCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.epg_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.epg_subtitle, currentLocation.name),
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Grid / List Layout Switcher
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SleekSurface,
                border = BorderStroke(1.dp, SleekBorder)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    EpgModeToggleChip(
                        label = stringResource(R.string.epg_view_grid),
                        icon = Icons.Default.GridView,
                        isSelected = layoutMode == EpgLayoutMode.GRID,
                        onClick = { layoutMode = EpgLayoutMode.GRID },
                        testTag = "epg_view_grid"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    EpgModeToggleChip(
                        label = stringResource(R.string.epg_view_list),
                        icon = Icons.Default.ViewAgenda,
                        isSelected = layoutMode == EpgLayoutMode.LIST,
                        onClick = { layoutMode = EpgLayoutMode.LIST },
                        testTag = "epg_view_list"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it.id }) { cat ->
                val isSelected = cat.id == selectedCategoryId
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) SleekBluePrimary else SleekSurface,
                    border = BorderStroke(1.dp, if (isSelected) SleekBluePrimary else SleekBorder),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .tvFocusVisual(shape = RoundedCornerShape(20.dp), focusBorderColor = BroadcastCyan)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            selected = isSelected
                        }
                        .clickable { selectedCategoryId = cat.id }
                        .testTag("epg_cat_${cat.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(cat.labelRes).uppercase(),
                            color = if (isSelected) Color.White else SleekTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSlots.isEmpty()) {
            // Friendly Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("epg_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SleekSurface,
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier.widthIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = SleekBluePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.epg_empty_title),
                            color = SleekTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.epg_empty_subtitle),
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("epg_empty_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.common_refresh))
                        }
                    }
                }
            }
        } else {
            // Now Airing Spotlight Banner
            if (nowPlayingSpotlight != null) {
                EpgNowAiringCard(
                    slot = nowPlayingSpotlight,
                    nowMs = nowMs,
                    liveDotAlpha = liveDotAlpha,
                    timeFmt = timeFmt,
                    onWatchNow = { onPlayStory(nowPlayingSpotlight.linkedVodStory) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main EPG Grid or Program List
            AnimatedContent(
                targetState = layoutMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "epgLayoutTransition",
                modifier = Modifier.weight(1f)
            ) { mode ->
                when (mode) {
                    EpgLayoutMode.GRID -> {
                        EpgTimelineGridView(
                            slots = filteredSlots,
                            remindedSlotIds = remindedSlotIds,
                            nowMs = nowMs,
                            liveDotAlpha = liveDotAlpha,
                            timeFmt = timeFmt,
                            onSlotClick = { slot -> onPlayStory(slot.linkedVodStory) },
                            onToggleReminder = onToggleReminder
                        )
                    }
                    EpgLayoutMode.LIST -> {
                        EpgChronologicalListView(
                            slots = filteredSlots,
                            remindedSlotIds = remindedSlotIds,
                            nowMs = nowMs,
                            liveDotAlpha = liveDotAlpha,
                            timeFmt = timeFmt,
                            onWatchSlot = { slot -> onPlayStory(slot.linkedVodStory) },
                            onToggleReminder = onToggleReminder
                        )
                    }
                }
            }
        }
    }

    if (activePlayingStory != null) {
        val storyPool = remember(allVodStories, scheduleSlots) {
            (allVodStories + scheduleSlots.map { it.linkedVodStory }).distinctBy { it.id }
        }
        VodPlayerModal(
            story = activePlayingStory,
            allStories = storyPool,
            onDismiss = { onPlayStory(null) },
            onSwitchStory = { onPlayStory(it) }
        )
    }
}

@Composable
private fun EpgModeToggleChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isSelected) SleekBluePrimary else Color.Transparent,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(9.dp))
            .tvFocusVisual(shape = RoundedCornerShape(9.dp), focusBorderColor = SleekBluePrimary)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = label
            }
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else SleekTextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else SleekTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EpgNowAiringCard(
    slot: BroadcastScheduleSlot,
    nowMs: Long,
    liveDotAlpha: Float,
    timeFmt: SimpleDateFormat,
    onWatchNow: () -> Unit
) {
    val accent = Color(slot.accentColorHex)
    val durationMs = (slot.endEpochMs - slot.startEpochMs).coerceAtLeast(1L)
    val progress = ((nowMs - slot.startEpochMs).toFloat() / durationMs.toFloat()).coerceIn(0.08f, 1f)
    val slotRange = stringResource(
        R.string.epg_time_slot_fmt,
        timeFmt.format(Date(slot.startEpochMs)),
        timeFmt.format(Date(slot.endEpochMs))
    )
    val watchLabel = stringResource(R.string.epg_watch_now)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C1830),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("epg_now_airing_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accent.copy(alpha = 0.24f), Color(0xFF0A1428))
                    )
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SevereRed
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = liveDotAlpha))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.epg_on_air),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${slot.channelNumber} • ${stringResource(slot.channelNameRes)}",
                            color = BroadcastCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = slotRange,
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LottieWeatherAnimation(
                            weatherCode = slot.weatherCode,
                            isDay = slot.isDay,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = slot.programTitle,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = slot.presenter,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        onClick = onWatchNow,
                        shape = RoundedCornerShape(percent = 50),
                        color = SleekBluePrimary,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("epg_spotlight_watch_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = watchLabel,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = watchLabel,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.14f)
                )
            }
        }
    }
}

@Composable
private fun EpgTimelineGridView(
    slots: List<BroadcastScheduleSlot>,
    remindedSlotIds: Set<String>,
    nowMs: Long,
    liveDotAlpha: Float,
    timeFmt: SimpleDateFormat,
    onSlotClick: (BroadcastScheduleSlot) -> Unit,
    onToggleReminder: (String) -> Unit
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    val minStartMs = remember(slots) { slots.minOfOrNull { it.startEpochMs } ?: nowMs }
    val maxEndMs = remember(slots) { slots.maxOfOrNull { it.endEpochMs } ?: (minStartMs + 300 * 60_000L) }
    val totalMinutes = remember(minStartMs, maxEndMs) {
        ((maxEndMs - minStartMs) / 60_000L).toInt().coerceAtLeast(180)
    }

    // 3.6.dp per minute => 30 min = 108.dp, 60 min = 216.dp, 90 min = 324.dp
    val dpPerMinute = 3.6f
    val halfHourCount = (totalMinutes / 30).coerceAtLeast(6)
    val timelineTotalWidth = (totalMinutes * dpPerMinute).dp
    val channelColWidth = 96.dp
    val nowOffsetMinutes = ((nowMs - minStartMs) / 60_000f).coerceIn(0f, totalMinutes.toFloat())
    val nowOffsetDp = (nowOffsetMinutes * dpPerMinute).dp

    val groupedByChannel = remember(slots) {
        slots.groupBy { it.channelIndex }.toSortedMap()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1324),
        border = BorderStroke(1.dp, Color(0xFF1F304D)),
        modifier = Modifier
            .fillMaxSize()
            .testTag("epg_grid_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Timeline Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1D35))
                    .border(width = 1.dp, color = Color(0xFF1F304D)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(channelColWidth)
                        .height(38.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(R.string.epg_timeline_header),
                        color = BroadcastCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                ) {
                    Box(modifier = Modifier.width(timelineTotalWidth).height(38.dp)) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            for (i in 0 until halfHourCount) {
                                val markerMs = minStartMs + i * 30 * 60_000L
                                Box(
                                    modifier = Modifier
                                        .width((30 * dpPerMinute).dp)
                                        .fillMaxHeight()
                                        .border(width = 0.5.dp, color = Color(0xFF1E2E4A))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = timeFmt.format(Date(markerMs)),
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Red NOW playhead pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SevereRed,
                            modifier = Modifier
                                .offset(x = (nowOffsetDp - 14.dp).coerceAtLeast(0.dp))
                                .align(Alignment.BottomStart)
                                .padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.epg_now_line),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Channel Rows
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
            ) {
                groupedByChannel.forEach { (channelIdx, channelSlots) ->
                    val firstSlot = channelSlots.first()
                    val channelAccent = Color(firstSlot.accentColorHex)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                            .border(width = 0.5.dp, color = Color(0xFF1B2B46)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Pinned Channel Callsign Cell
                        Column(
                            modifier = Modifier
                                .width(channelColWidth)
                                .fillMaxHeight()
                                .background(Color(0xFF0E1A30))
                                .border(width = 0.5.dp, color = Color(0xFF1F304D))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = channelAccent.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, channelAccent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = firstSlot.channelNumber,
                                    color = channelAccent,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(firstSlot.channelNameRes),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 13.sp
                            )
                        }

                        // Horizontally Scrollable Program Blocks Row
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .horizontalScroll(horizontalScroll)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(timelineTotalWidth)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 6.dp)
                                ) {
                                    channelSlots.sortedBy { it.startEpochMs }.forEach { slot ->
                                        val startOffsetMin = ((slot.startEpochMs - minStartMs) / 60_000f).coerceAtLeast(0f)
                                        val slotOffsetDp = (startOffsetMin * dpPerMinute).dp
                                        val blockWidth = ((slot.durationMinutes * dpPerMinute) - 4f)
                                            .coerceAtLeast(110f).dp
                                        val isOnAir = nowMs in slot.startEpochMs until slot.endEpochMs
                                        val isReminded = slot.id in remindedSlotIds
                                        val accent = Color(slot.accentColorHex)
                                        val slotTimeRange = stringResource(
                                            R.string.epg_time_slot_fmt,
                                            timeFmt.format(Date(slot.startEpochMs)),
                                            timeFmt.format(Date(slot.endEpochMs))
                                        )
                                        val statusLabel = when {
                                            isOnAir -> stringResource(R.string.epg_on_air)
                                            slot.startEpochMs <= nowMs + 60 * 60_000L -> stringResource(R.string.epg_up_next)
                                            else -> stringResource(R.string.epg_upcoming)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isOnAir) Color(0xFF15294A) else Color(0xFF101C32),
                                            border = BorderStroke(
                                                width = if (isOnAir) 1.5.dp else 1.dp,
                                                color = if (isOnAir) accent else Color(0xFF233657)
                                            ),
                                            modifier = Modifier
                                                .offset(x = slotOffsetDp)
                                                .width(blockWidth)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(10.dp))
                                                .tvFocusVisual(shape = RoundedCornerShape(10.dp), focusBorderColor = BroadcastCyan, focusedScale = 1.02f)
                                                .semantics(mergeDescendants = true) {
                                                    role = Role.Button
                                                    contentDescription = "${slot.channelNumber}, $statusLabel, ${slot.programTitle}, $slotTimeRange"
                                                }
                                                .clickable(onClickLabel = slot.programTitle) { onSlotClick(slot) }
                                                .testTag("epg_grid_slot_${slot.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (isOnAir) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(SevereRed.copy(alpha = liveDotAlpha))
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                        Text(
                                                            text = statusLabel,
                                                            color = if (isOnAir) SevereRed else accent,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = slotTimeRange,
                                                            color = Color(0xFF94A3B8),
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }

                                                    if (!isOnAir) {
                                                        val remindDesc = stringResource(
                                                            if (isReminded) R.string.epg_reminder_set_desc else R.string.epg_reminder_unset_desc,
                                                            slot.programTitle
                                                        )
                                                        Icon(
                                                            imageVector = if (isReminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                                            contentDescription = remindDesc,
                                                            tint = if (isReminded) SolarGold else Color(0xFF64748B),
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable { onToggleReminder(slot.id) }
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    LottieWeatherAnimation(
                                                        weatherCode = slot.weatherCode,
                                                        isDay = slot.isDay,
                                                        size = 22.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = slot.programTitle,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Text(
                                                    text = "${slot.presenter} • ${stringResource(R.string.epg_duration_min, slot.durationMinutes)}",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                // Vertical Red NOW Playhead Line across channel row
                                Box(
                                    modifier = Modifier
                                        .offset(x = nowOffsetDp)
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(SevereRed.copy(alpha = 0.85f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgChronologicalListView(
    slots: List<BroadcastScheduleSlot>,
    remindedSlotIds: Set<String>,
    nowMs: Long,
    liveDotAlpha: Float,
    timeFmt: SimpleDateFormat,
    onWatchSlot: (BroadcastScheduleSlot) -> Unit,
    onToggleReminder: (String) -> Unit
) {
    val sortedSlots = remember(slots) {
        slots.sortedWith(compareBy<BroadcastScheduleSlot> { it.startEpochMs }.thenBy { it.channelIndex })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("epg_list_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(sortedSlots, key = { it.id }) { slot ->
            val isOnAir = nowMs in slot.startEpochMs until slot.endEpochMs
            val isUpNext = !isOnAir && slot.startEpochMs <= nowMs + 60 * 60_000L
            val isReminded = slot.id in remindedSlotIds
            val accent = Color(slot.accentColorHex)

            val startStr = timeFmt.format(Date(slot.startEpochMs))
            val endStr = timeFmt.format(Date(slot.endEpochMs))
            val timeRange = stringResource(R.string.epg_time_slot_fmt, startStr, endStr)
            val durationStr = stringResource(R.string.epg_duration_min, slot.durationMinutes)
            val statusText = when {
                isOnAir -> stringResource(R.string.epg_on_air)
                isUpNext -> stringResource(R.string.epg_up_next)
                else -> stringResource(R.string.epg_upcoming)
            }
            val watchLabel = stringResource(R.string.epg_watch_now)
            val remindLabel = stringResource(if (isReminded) R.string.epg_reminded else R.string.epg_remind)
            val remindDesc = stringResource(
                if (isReminded) R.string.epg_reminder_set_desc else R.string.epg_reminder_unset_desc,
                slot.programTitle
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isOnAir) Color(0xFF13223F) else SleekSurface,
                border = BorderStroke(
                    width = if (isOnAir) 1.5.dp else 1.dp,
                    color = if (isOnAir) accent.copy(alpha = 0.7f) else SleekBorder
                ),
                shadowElevation = if (isOnAir) 4.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("epg_list_card_${slot.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isOnAir) SevereRed else accent.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    if (isOnAir) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = liveDotAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = statusText,
                                        color = if (isOnAir) Color.White else accent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${slot.channelNumber} • ${stringResource(slot.channelNameRes)}",
                                color = if (isOnAir) BroadcastCyan else SleekBluePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "$timeRange • $durationStr",
                            color = if (isOnAir) Color(0xFFCBD5E1) else SleekTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.15f))
                                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            LottieWeatherAnimation(
                                weatherCode = slot.weatherCode,
                                isDay = slot.isDay,
                                size = 30.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = slot.programTitle,
                                color = if (isOnAir) Color.White else SleekTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = slot.programSynopsis,
                                color = if (isOnAir) Color(0xFFCBD5E1) else SleekTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${slot.categoryLabel} • ${slot.presenter}",
                                color = if (isOnAir) BroadcastCyan else SleekBluePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isOnAir) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val durationMs = (slot.endEpochMs - slot.startEpochMs).coerceAtLeast(1L)
                        val progress = ((nowMs - slot.startEpochMs).toFloat() / durationMs.toFloat()).coerceIn(0.08f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = accent,
                            trackColor = Color.White.copy(alpha = 0.14f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onWatchSlot(slot) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnAir) SleekBluePrimary else accent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("epg_watch_${slot.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = watchLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isOnAir) {
                            OutlinedButton(
                                onClick = { onToggleReminder(slot.id) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isReminded) SolarGold else SleekBorder
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        contentDescription = remindDesc
                                        stateDescription = remindLabel
                                    }
                                    .testTag("epg_remind_${slot.id}")
                            ) {
                                Icon(
                                    imageVector = if (isReminded) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (isReminded) SolarGold else SleekTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = remindLabel,
                                    color = if (isReminded) SolarGold else SleekTextPrimary,
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
