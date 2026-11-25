package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.LiveRadarPackage
import com.example.data.model.RadarTimelineFrame
import com.example.data.model.UnitSystem
import com.example.ui.components.DopplerRadarMapView
import com.example.ui.components.tvFocusVisual
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun InteractiveRadarScreen(
    currentLocation: CityLocation,
    currentWeather: CurrentWeather?,
    unitSystem: UnitSystem,
    selectedLayer: Int,
    isPlaying: Boolean,
    frameIndex: Int,
    radarPackage: LiveRadarPackage?,
    isLoadingRadar: Boolean,
    radarOpacity: Float,
    onSelectLayer: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onSelectFrame: (Int) -> Unit,
    onCycleOpacity: () -> Unit,
    onRefreshRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Real geographic map viewport state (centered on active station whenever city changes)
    var centerLat by remember(currentLocation.latitude, currentLocation.longitude) {
        mutableDoubleStateOf(currentLocation.latitude)
    }
    var centerLon by remember(currentLocation.latitude, currentLocation.longitude) {
        mutableDoubleStateOf(currentLocation.longitude)
    }
    var zoomLevel by remember(currentLocation.latitude, currentLocation.longitude) {
        mutableFloatStateOf(7.0f)
    }

    // Active timeline frames from RainViewer (or fallback timestamps while syncing)
    val activeFrames: List<RadarTimelineFrame> = remember(radarPackage, selectedLayer) {
        when (selectedLayer) {
            1 -> radarPackage?.satelliteFrames.orEmpty()
            else -> radarPackage?.precipitationFrames.orEmpty()
        }
    }

    val safeFrameIndex = if (activeFrames.isNotEmpty()) {
        frameIndex.coerceIn(0, activeFrames.lastIndex)
    } else {
        frameIndex.coerceIn(0, 6)
    }
    val currentFrame = activeFrames.getOrNull(safeFrameIndex)

    // Latest observed ("NOW") frame index
    val latestObservedIdx = remember(activeFrames) {
        activeFrames.indexOfLast { !it.isNowcast }.coerceAtLeast(0)
    }

    // Format real timestamps from RainViewer Unix epoch seconds
    val nowTemplate = stringResource(R.string.radar_timeline_now)
    val timeLabels = remember(activeFrames, nowTemplate) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        if (activeFrames.isNotEmpty()) {
            activeFrames.mapIndexed { idx, frame ->
                val formatted = timeFmt.format(Date(frame.timestampSec * 1000L))
                if (idx == latestObservedIdx && !frame.isNowcast) {
                    String.format(nowTemplate, formatted)
                } else {
                    formatted
                }
            }
        } else {
            val now = System.currentTimeMillis()
            listOf(-30, -20, -10, 0, 10, 20, 30).mapIndexed { idx, minOffset ->
                val formatted = timeFmt.format(Date(now + minOffset * 60_000L))
                if (idx == 3) String.format(nowTemplate, formatted) else formatted
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("interactive_radar_view")
    ) {
        // Adaptive Top Radar Header & Layer Switcher
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 680.dp
            if (isCompact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RadarHeaderTitle(
                        cityName = currentLocation.name,
                        hasLiveFrames = activeFrames.isNotEmpty(),
                        isLoadingRadar = isLoadingRadar,
                        isNowcast = currentFrame?.isNowcast == true
                    )
                    RadarLayerBar(
                        selectedLayer = selectedLayer,
                        onSelectLayer = onSelectLayer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadarHeaderTitle(
                        cityName = currentLocation.name,
                        hasLiveFrames = activeFrames.isNotEmpty(),
                        isLoadingRadar = isLoadingRadar,
                        isNowcast = currentFrame?.isNowcast == true
                    )
                    RadarLayerBar(
                        selectedLayer = selectedLayer,
                        onSelectLayer = onSelectLayer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real-Time Interactive Geographic Map + Doppler Radar Overlay Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
        ) {
            DopplerRadarMapView(
                stationLocation = currentLocation,
                currentWeather = currentWeather,
                unitSystem = unitSystem,
                selectedLayer = selectedLayer,
                radarHost = radarPackage?.host,
                activeFrame = currentFrame,
                radarOpacity = radarOpacity,
                centerLat = centerLat,
                centerLon = centerLon,
                zoomLevel = zoomLevel,
                onMapTransform = { lat, lon, zoom ->
                    centerLat = lat
                    centerLon = lon
                    zoomLevel = zoom
                }
            )

            // Top-Left Floating Map Controls (48dp accessible touch targets)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapControlFab(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.radar_zoom_in),
                    testTag = "radar_zoom_in_button",
                    onClick = { zoomLevel = (zoomLevel + 1.0f).coerceAtMost(11.0f) }
                )
                MapControlFab(
                    icon = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.radar_zoom_out),
                    testTag = "radar_zoom_out_button",
                    onClick = { zoomLevel = (zoomLevel - 1.0f).coerceAtLeast(3.0f) }
                )
                MapControlFab(
                    icon = Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.radar_recenter),
                    iconTint = Color(0xFF38BDF8),
                    testTag = "radar_recenter_button",
                    onClick = {
                        centerLat = currentLocation.latitude
                        centerLon = currentLocation.longitude
                        zoomLevel = 7.0f
                    }
                )
                MapControlFab(
                    icon = Icons.Default.Layers,
                    contentDescription = stringResource(R.string.radar_opacity_toggle),
                    badgeText = "${(radarOpacity * 100).roundToInt()}%",
                    testTag = "radar_opacity_button",
                    onClick = onCycleOpacity
                )
                MapControlFab(
                    icon = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.radar_refresh),
                    iconTint = if (isLoadingRadar) SolarGold else Color.White,
                    testTag = "radar_refresh_button",
                    onClick = onRefreshRadar
                )
            }

            // Top-Right Compact Legend Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color(0xDD071120), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (selectedLayer) {
                        0 -> stringResource(R.string.radar_legend_precip_title)
                        1 -> stringResource(R.string.radar_legend_satellite_title)
                        2 -> stringResource(R.string.radar_legend_wind_title)
                        else -> stringResource(R.string.radar_legend_traffic_title)
                    },
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                when (selectedLayer) {
                    0 -> {
                        LegendRow(color = Color(0xFF22C55E), label = stringResource(R.string.radar_legend_precip_light))
                        LegendRow(color = Color(0xFFFACC15), label = stringResource(R.string.radar_legend_precip_mod))
                        LegendRow(color = Color(0xFFEF4444), label = stringResource(R.string.radar_legend_precip_heavy))
                        LegendRow(color = Color(0xFFA855F7), label = stringResource(R.string.radar_legend_precip_hail))
                    }
                    1 -> {
                        LegendRow(color = Color(0xFFCBD5E1), label = stringResource(R.string.radar_legend_sat_cirrus))
                        LegendRow(color = Color(0xFF38BDF8), label = stringResource(R.string.radar_legend_sat_frontal))
                        LegendRow(color = Color(0xFFF43F5E), label = stringResource(R.string.radar_legend_sat_core))
                    }
                    2 -> {
                        LegendRow(color = Color(0xFF38BDF8), label = stringResource(R.string.radar_legend_wind_vector))
                    }
                    else -> {
                        LegendRow(color = Color(0xFF22C55E), label = stringResource(R.string.radar_legend_traffic_fast))
                        LegendRow(color = Color(0xFFFACC15), label = stringResource(R.string.radar_legend_traffic_mod))
                        LegendRow(color = Color(0xFFEF4444), label = stringResource(R.string.radar_legend_traffic_slow))
                    }
                }
            }

            // Bottom-Left Zoom Level & Gesture Hint Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color(0xAA071120), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.radar_zoom_tap_hint_fmt, zoomLevel, stringResource(R.string.radar_tap_hint)),
                    color = Color(0xFFCBD5E1),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Live Radar Timeline Scrubber & Playback Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SleekSurface, RoundedCornerShape(16.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Button (48dp touch target)
            val playPauseDesc = stringResource(R.string.common_play_pause)
            val playStateDesc = if (isPlaying) stringResource(R.string.telemetry_btn_pause) else stringResource(R.string.telemetry_btn_resume)
            Surface(
                shape = CircleShape,
                color = SleekBluePrimary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .tvFocusVisual(shape = CircleShape, focusBorderColor = BroadcastCyan)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = playPauseDesc
                        stateDescription = playStateDesc
                    }
                    .clickable(onClickLabel = playPauseDesc) { onTogglePlay() }
                    .testTag("radar_play_pause_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = playPauseDesc,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Scrollable Real-Time Frame Chips
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeLabels.forEachIndexed { index, label ->
                    val isSelected = index == safeFrameIndex
                    val isNowcastChip = activeFrames.getOrNull(index)?.isNowcast == true
                    val chipBg by animateColorAsState(
                        targetValue = when {
                            isSelected && isNowcastChip -> SolarGold
                            isSelected -> SleekBluePrimary
                            isNowcastChip -> SleekYellowContainer
                            else -> SleekSurfaceSecondary
                        },
                        animationSpec = tween(180),
                        label = "chipBg"
                    )
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = chipBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isSelected && isNowcastChip -> SolarGold
                                isSelected -> SleekBluePrimary
                                isNowcastChip -> SolarGold.copy(alpha = 0.4f)
                                else -> SleekBorder
                            }
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .tvFocusVisual(shape = RoundedCornerShape(percent = 50), focusBorderColor = SleekBluePrimary)
                            .semantics(mergeDescendants = true) {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .clickable { onSelectFrame(index) }
                            .testTag("radar_frame_chip_$index")
                    ) {
                        Text(
                            text = label,
                            color = when {
                                isSelected -> Color.White
                                isNowcastChip -> SleekYellowText
                                else -> SleekTextSecondary
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarHeaderTitle(
    cityName: String,
    hasLiveFrames: Boolean,
    isLoadingRadar: Boolean,
    isNowcast: Boolean
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasLiveFrames) Color(0xFF22C55E) else SolarGold)
            )
            Text(
                text = stringResource(R.string.radar_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.semantics { heading() }
            )
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = when {
                    isLoadingRadar -> SolarGold.copy(alpha = 0.2f)
                    isNowcast -> SolarGold.copy(alpha = 0.22f)
                    hasLiveFrames -> Color(0xFF16A34A).copy(alpha = 0.25f)
                    else -> Color.White.copy(alpha = 0.12f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isLoadingRadar || isNowcast -> SolarGold.copy(alpha = 0.6f)
                        hasLiveFrames -> Color(0xFF22C55E).copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.25f)
                    }
                )
            ) {
                Text(
                    text = when {
                        isLoadingRadar -> stringResource(R.string.radar_badge_syncing)
                        isNowcast -> stringResource(R.string.radar_badge_nowcast)
                        hasLiveFrames -> stringResource(R.string.radar_badge_live_feed)
                        else -> stringResource(R.string.radar_badge_observed)
                    },
                    color = when {
                        isLoadingRadar || isNowcast -> Color(0xFFFDE047)
                        hasLiveFrames -> Color(0xFF86EFAC)
                        else -> Color.White
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.radar_subtitle, cityName),
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RadarLayerBar(
    selectedLayer: Int,
    onSelectLayer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(SleekSurfaceSecondary, RoundedCornerShape(percent = 50))
            .border(1.dp, SleekBorder, RoundedCornerShape(percent = 50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RadarLayerButton(
            title = stringResource(R.string.radar_layer_precip),
            isSelected = selectedLayer == 0,
            testTag = "radar_layer_doppler"
        ) { onSelectLayer(0) }
        RadarLayerButton(
            title = stringResource(R.string.radar_layer_satellite),
            isSelected = selectedLayer == 1,
            testTag = "radar_layer_satellite"
        ) { onSelectLayer(1) }
        RadarLayerButton(
            title = stringResource(R.string.radar_layer_wind),
            isSelected = selectedLayer == 2,
            testTag = "radar_layer_wind"
        ) { onSelectLayer(2) }
        RadarLayerButton(
            title = stringResource(R.string.radar_layer_traffic),
            isSelected = selectedLayer == 3,
            testTag = "radar_layer_streets"
        ) { onSelectLayer(3) }
    }
}

@Composable
private fun MapControlFab(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    iconTint: Color = Color.White,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xDD071120),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .tvFocusVisual(shape = CircleShape, focusBorderColor = BroadcastCyan)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = if (badgeText != null) "$contentDescription ($badgeText)" else contentDescription
            }
            .clickable(onClickLabel = contentDescription) { onClick() }
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(if (badgeText != null) 16.dp else 20.dp)
            )
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    color = Color(0xFF7DD3FC),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun RadarLayerButton(
    title: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SleekBluePrimary else Color.Transparent,
        animationSpec = tween(180),
        label = "layerBg"
    )
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = bgColor,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .tvFocusVisual(shape = RoundedCornerShape(percent = 50), focusBorderColor = SleekBluePrimary)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
            }
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else SleekTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = Color.White, fontSize = 9.sp)
    }
}
