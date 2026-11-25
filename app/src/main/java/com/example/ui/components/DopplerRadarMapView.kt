package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.RadarTimelineFrame
import com.example.data.model.UnitSystem
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.HorizonCyan
import com.example.ui.theme.SolarGold
import kotlin.math.*

/**
 * Pure Web Mercator (EPSG:3857) projection utilities for Slippy Map tiles (256px base tile size).
 */
object WebMercatorProjection {
    private const val EARTH_RADIUS_METERS = 6378137.0
    private const val MAX_LAT = 85.05112878

    fun clampLat(lat: Double): Double = lat.coerceIn(-MAX_LAT, MAX_LAT)

    fun wrapLon(lon: Double): Double {
        var wrapped = ((lon + 180.0) % 360.0)
        if (wrapped < 0.0) wrapped += 360.0
        return wrapped - 180.0
    }

    /** Returns fractional tile coordinates (x, y) at zoom level z. */
    fun latLonToTileDouble(lat: Double, lon: Double, zoom: Double): Pair<Double, Double> {
        val n = 2.0.pow(zoom)
        val clampedLat = clampLat(lat)
        val latRad = Math.toRadians(clampedLat)
        val x = (wrapLon(lon) + 180.0) / 360.0 * n
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n
        return Pair(x, y)
    }

    /** Converts fractional tile coordinates (x, y) back to geographic (lat, lon). */
    fun tileDoubleToLatLon(tileX: Double, tileY: Double, zoom: Double): Pair<Double, Double> {
        val n = 2.0.pow(zoom)
        val lon = wrapLon(tileX / n * 360.0 - 180.0)
        val latRad = atan(sinh(PI * (1.0 - 2.0 * tileY / n)))
        val lat = clampLat(Math.toDegrees(latRad))
        return Pair(lat, lon)
    }

    /** Real-world ground resolution in meters per CSS tile pixel at given latitude and zoom. */
    fun metersPerTilePixel(lat: Double, zoom: Double, tileSizePx: Float): Double {
        val latRad = Math.toRadians(clampLat(lat))
        val worldCircumference = 2.0 * PI * EARTH_RADIUS_METERS
        return (worldCircumference * cos(latRad)) / (tileSizePx * 2.0.pow(zoom))
    }

    /** Great-circle Haversine distance in kilometers between two geographic points. */
    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2.0 * 6371.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}

data class InspectedMapPoint(
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double
)

private data class VisibleTileSpec(
    val tileX: Int,
    val tileY: Int,
    val wrappedX: Int,
    val leftPx: Float,
    val topPx: Float
)

/**
 * Interactive real-time Web Mercator Doppler Radar & Satellite Map Component.
 * Streams live CartoDB basemap tiles layered with live RainViewer NEXRAD reflectivity
 * and infrared cloud-top tiles, plus real-time Open-Meteo wind vector streamlines.
 */
@Composable
fun DopplerRadarMapView(
    stationLocation: CityLocation,
    currentWeather: CurrentWeather?,
    unitSystem: UnitSystem,
    selectedLayer: Int, // 0: Doppler Dark, 1: Satellite IR, 2: Wind Flow, 3: Streets + Doppler
    radarHost: String?,
    activeFrame: RadarTimelineFrame?,
    radarOpacity: Float,
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    onMapTransform: (newLat: Double, newLon: Double, newZoom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var inspectedPoint by remember(stationLocation.latitude, stationLocation.longitude) {
        mutableStateOf<InspectedMapPoint?>(null)
    }

    // Smooth infinite animations for radar sweep beam, station pulse, and wind streamlines
    val infiniteTransition = rememberInfiniteTransition(label = "dopplerMapEffects")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beaconPulse"
    )
    val windPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "windPhase"
    )

    val isLightBasemap = selectedLayer == 3
    val mapBackdropColor = if (isLightBasemap) Color(0xFFE5EDF5) else Color(0xFF071120)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(mapBackdropColor)
            .testTag("doppler_radar_map_canvas")
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val viewportHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // Integer zoom for tile fetching + smooth fractional scale factor
        val clampedZoom = zoomLevel.coerceIn(3.0f, 11.0f)
        val tileZoom = floor(clampedZoom).toInt().coerceIn(3, 11)
        val zoomScaleFactor = 2.0.pow((clampedZoom - tileZoom).toDouble()).toFloat()
        val baseTileSizePx = with(density) { 256.dp.toPx() }
        val scaledTileSizePx = baseTileSizePx * zoomScaleFactor
        val maxTileIndex = 1 shl tileZoom

        val (centerTileX, centerTileY) = remember(centerLat, centerLon, tileZoom) {
            WebMercatorProjection.latLonToTileDouble(centerLat, centerLon, tileZoom.toDouble())
        }

        // Compute station screen coordinates
        val (stationTileX, stationTileY) = remember(stationLocation.latitude, stationLocation.longitude, tileZoom) {
            WebMercatorProjection.latLonToTileDouble(
                stationLocation.latitude,
                stationLocation.longitude,
                tileZoom.toDouble()
            )
        }
        val stationScreenX = viewportWidthPx / 2f + ((stationTileX - centerTileX) * scaledTileSizePx).toFloat()
        val stationScreenY = viewportHeightPx / 2f + ((stationTileY - centerTileY) * scaledTileSizePx).toFloat()

        // Compute visible tiles covering viewport (+1 tile margin for smooth panning)
        val visibleTiles = remember(
            centerTileX,
            centerTileY,
            tileZoom,
            viewportWidthPx,
            viewportHeightPx,
            scaledTileSizePx
        ) {
            val halfCols = ceil((viewportWidthPx / 2f) / scaledTileSizePx).toInt() + 1
            val halfRows = ceil((viewportHeightPx / 2f) / scaledTileSizePx).toInt() + 1
            val startX = floor(centerTileX).toInt() - halfCols
            val endX = floor(centerTileX).toInt() + halfCols
            val startY = (floor(centerTileY).toInt() - halfRows).coerceAtLeast(0)
            val endY = (floor(centerTileY).toInt() + halfRows).coerceAtMost(maxTileIndex - 1)

            buildList {
                for (ty in startY..endY) {
                    for (tx in startX..endX) {
                        val wrappedX = ((tx % maxTileIndex) + maxTileIndex) % maxTileIndex
                        val left = viewportWidthPx / 2f + ((tx - centerTileX) * scaledTileSizePx).toFloat()
                        val top = viewportHeightPx / 2f + ((ty - centerTileY) * scaledTileSizePx).toFloat()
                        add(VisibleTileSpec(tx, ty, wrappedX, left, top))
                    }
                }
            }
        }

        val currentCenterLat by rememberUpdatedState(centerLat)
        val currentCenterLon by rememberUpdatedState(centerLon)
        val currentZoom by rememberUpdatedState(clampedZoom)
        val currentTileZoom by rememberUpdatedState(tileZoom)
        val currentScaledTilePx by rememberUpdatedState(scaledTileSizePx)
        val currentOnTransform by rememberUpdatedState(onMapTransform)

        val mapAccessibilityLabel = "${stringResource(R.string.radar_title)}: ${stringResource(R.string.radar_subtitle, stationLocation.name)}"
        // Interactive gesture layer (Pan + Pinch Zoom + Tap to inspect coordinates)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = mapAccessibilityLabel
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextZoom = (currentZoom + ln(zoom.coerceIn(0.5f, 2.0f)) / ln(2f))
                            .coerceIn(3.0f, 11.0f)
                        val (cx, cy) = WebMercatorProjection.latLonToTileDouble(
                            currentCenterLat,
                            currentCenterLon,
                            currentTileZoom.toDouble()
                        )
                        val shiftedTileX = cx - (pan.x / currentScaledTilePx)
                        val shiftedTileY = cy - (pan.y / currentScaledTilePx)
                        val (newLat, newLon) = WebMercatorProjection.tileDoubleToLatLon(
                            shiftedTileX,
                            shiftedTileY,
                            currentTileZoom.toDouble()
                        )
                        currentOnTransform(newLat, newLon, nextZoom)
                    }
                }
                .pointerInput(stationLocation.latitude, stationLocation.longitude) {
                    detectTapGestures { tapOffset ->
                        val (cx, cy) = WebMercatorProjection.latLonToTileDouble(
                            currentCenterLat,
                            currentCenterLon,
                            currentTileZoom.toDouble()
                        )
                        val tapTileX = cx + ((tapOffset.x - viewportWidthPx / 2f) / currentScaledTilePx)
                        val tapTileY = cy + ((tapOffset.y - viewportHeightPx / 2f) / currentScaledTilePx)
                        val (lat, lon) = WebMercatorProjection.tileDoubleToLatLon(
                            tapTileX,
                            tapTileY,
                            currentTileZoom.toDouble()
                        )
                        val distKm = WebMercatorProjection.haversineDistanceKm(
                            stationLocation.latitude,
                            stationLocation.longitude,
                            lat,
                            lon
                        )
                        inspectedPoint = InspectedMapPoint(lat, lon, distKm)
                    }
                }
        ) {
            val tileSizeDp = with(density) { ceil(scaledTileSizePx + 1f).toDp() }
            val subdomains = remember { listOf("a", "b", "c", "d") }

            // 1. BASEMAP TILE LAYER (CartoDB Dark Matter or CartoDB Voyager Terrain/Streets)
            visibleTiles.forEach { tile ->
                val sub = subdomains[(tile.wrappedX + tile.tileY) and 3]
                val baseUrl = if (isLightBasemap) {
                    "https://$sub.basemaps.cartocdn.com/rastertiles/voyager/$tileZoom/${tile.wrappedX}/${tile.tileY}@2x.png"
                } else {
                    "https://$sub.basemaps.cartocdn.com/dark_nolabels/$tileZoom/${tile.wrappedX}/${tile.tileY}@2x.png"
                }

                key("base_${isLightBasemap}_${tileZoom}_${tile.tileX}_${tile.tileY}") {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(baseUrl)
                            .addHeader("User-Agent", "HorizonTVWeather/${com.example.BuildConfig.VERSION_NAME} (Android)")
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .crossfade(160)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .offset { IntOffset(tile.leftPx.roundToInt(), tile.topPx.roundToInt()) }
                            .size(tileSizeDp)
                            .clearAndSetSemantics { }
                    )
                }
            }

            // 2. LIVE RAINVIEWER DOPPLER RADAR / SATELLITE IR TILE OVERLAY
            val showRadarTiles = (selectedLayer == 0 || selectedLayer == 1 || selectedLayer == 3) &&
                !radarHost.isNullOrBlank() && activeFrame != null && activeFrame.tilePath.isNotBlank()

            if (showRadarTiles && activeFrame != null && radarHost != null) {
                // RainViewer free public radar tiles support up to zoom 7 (higher zooms upscale seamlessly via Mercator tile math)
                val rvZoom = tileZoom.coerceAtMost(7)
                val rvScale = 2.0.pow((clampedZoom - rvZoom).toDouble()).toFloat()
                val rvTilePx = baseTileSizePx * rvScale
                val rvMaxIndex = 1 shl rvZoom
                val (rvCenterX, rvCenterY) = WebMercatorProjection.latLonToTileDouble(
                    centerLat,
                    centerLon,
                    rvZoom.toDouble()
                )
                val rvHalfCols = ceil((viewportWidthPx / 2f) / rvTilePx).toInt() + 1
                val rvHalfRows = ceil((viewportHeightPx / 2f) / rvTilePx).toInt() + 1
                val rvStartX = floor(rvCenterX).toInt() - rvHalfCols
                val rvEndX = floor(rvCenterX).toInt() + rvHalfCols
                val rvStartY = (floor(rvCenterY).toInt() - rvHalfRows).coerceAtLeast(0)
                val rvEndY = (floor(rvCenterY).toInt() + rvHalfRows).coerceAtMost(rvMaxIndex - 1)
                val rvTileSizeDp = with(density) { ceil(rvTilePx + 1f).toDp() }

                // Color scheme 6 = NEXRAD Level-III (Green -> Yellow -> Orange -> Red -> Purple), 0 = IR Blue/White
                val colorScheme = if (activeFrame.isSatellite || selectedLayer == 1) "0/0_0" else "6/1_1"

                for (ty in rvStartY..rvEndY) {
                    for (tx in rvStartX..rvEndX) {
                        val wrappedX = ((tx % rvMaxIndex) + rvMaxIndex) % rvMaxIndex
                        val leftPx = viewportWidthPx / 2f + ((tx - rvCenterX) * rvTilePx).toFloat()
                        val topPx = viewportHeightPx / 2f + ((ty - rvCenterY) * rvTilePx).toFloat()
                        val radarUrl = "$radarHost${activeFrame.tilePath}/256/$rvZoom/$wrappedX/$ty/$colorScheme.png"

                        key("rv_${activeFrame.tilePath}_${rvZoom}_${tx}_${ty}") {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(radarUrl)
                                    .addHeader("User-Agent", "HorizonTVWeather/${com.example.BuildConfig.VERSION_NAME} (Android)")
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                                    .size(rvTileSizeDp)
                                    .alpha(radarOpacity)
                                    .clearAndSetSemantics { }
                            )
                        }
                    }
                }
            }

            // 3. CRISP GEOGRAPHIC LABELS OVERLAY (keeps city & highway names readable on dark maps over radar!)
            if (!isLightBasemap) {
                visibleTiles.forEach { tile ->
                    val sub = subdomains[(tile.wrappedX + tile.tileY) and 3]
                    val labelsUrl = "https://$sub.basemaps.cartocdn.com/dark_only_labels/$tileZoom/${tile.wrappedX}/${tile.tileY}@2x.png"

                    key("lbl_${tileZoom}_${tile.tileX}_${tile.tileY}") {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(labelsUrl)
                                .addHeader("User-Agent", "HorizonTVWeather/${com.example.BuildConfig.VERSION_NAME} (Android)")
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .crossfade(120)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                .offset { IntOffset(tile.leftPx.roundToInt(), tile.topPx.roundToInt()) }
                                .size(tileSizeDp)
                                .alpha(0.92f)
                                .clearAndSetSemantics { }
                        )
                    }
                }
            }

            // 4. REAL-TIME METEOROLOGICAL HUD CANVAS (Range Rings, Dual-Pol Sweep, Wind Flow, Station Beacon)
            val hudCanvasDesc = "${stationLocation.name} • ${stringResource(R.string.radar_pin_coords, centerLat, centerLon)}"
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = hudCanvasDesc }
            ) {
                val w = size.width
                val h = size.height
                val sc = Offset(stationScreenX, stationScreenY)

                val metersPerPx = WebMercatorProjection.metersPerTilePixel(
                    stationLocation.latitude,
                    clampedZoom.toDouble(),
                    baseTileSizePx
                )
                val ringColor = if (isLightBasemap) Color(0xFF0B57D0) else Color(0xFF38BDF8)

                // Distance-calibrated Doppler range rings (50 km, 100 km, 200 km)
                val ringDistancesMeters = listOf(50_000.0, 100_000.0, 200_000.0)
                val maxRingRadius = (200_000.0 / metersPerPx).toFloat().coerceIn(80f, maxOf(w, h) * 1.4f)

                ringDistancesMeters.forEach { distM ->
                    val radiusPx = (distM / metersPerPx).toFloat()
                    if (radiusPx in 16f..(maxOf(w, h) * 1.5f)) {
                        drawCircle(
                            color = ringColor.copy(alpha = if (isLightBasemap) 0.30f else 0.28f),
                            radius = radiusPx,
                            center = sc,
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )
                    }
                }

                // Subtle Dual-Pol Radar Sweep Beam for Doppler & Streets+Radar modes
                if (selectedLayer == 0 || selectedLayer == 3) {
                    val beamRadius = maxRingRadius.coerceAtMost(minOf(w, h) * 0.55f)
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = sc.x + (beamRadius * cos(rad)).toFloat()
                    val endY = sc.y + (beamRadius * sin(rad)).toFloat()

                    // Sweep wedge tail
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(ringColor.copy(alpha = 0.22f), Color.Transparent),
                            center = sc,
                            radius = beamRadius.coerceAtLeast(1f)
                        ),
                        startAngle = sweepAngle - 36f,
                        sweepAngle = 36f,
                        useCenter = true,
                        topLeft = Offset(sc.x - beamRadius, sc.y - beamRadius),
                        size = androidx.compose.ui.geometry.Size(beamRadius * 2f, beamRadius * 2f)
                    )
                    // Leading sweep pulse line
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(ringColor.copy(alpha = 0.85f), Color.Transparent),
                            start = sc,
                            end = Offset(endX, endY)
                        ),
                        start = sc,
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Live Wind Streamlines Layer (Layer 2) driven by actual Open-Meteo wind speed & direction
                if (selectedLayer == 2) {
                    val windFromDeg = currentWeather?.windDirectionDeg ?: 225
                    val windToDeg = (windFromDeg + 180) % 360
                    val windKmh = currentWeather?.windSpeedKmh ?: 18.0
                    // Meteorological degrees: 0 = North (-Y), 90 = East (+X)
                    val flowAngleRad = Math.toRadians((windToDeg - 90).toDouble())
                    val dx = cos(flowAngleRad).toFloat()
                    val dy = sin(flowAngleRad).toFloat()
                    val streamLen = (26.dp.toPx() + (windKmh.toFloat().coerceIn(5f, 80f) * 0.5f))

                    val cols = 7
                    val rows = 6
                    val cellW = w / cols
                    val cellH = h / rows
                    for (r in 0..rows) {
                        for (c in 0..cols) {
                            val stagger = ((r * 37 + c * 53) % 100) / 100f
                            val phase = (windPhase + stagger) % 1f
                            val alpha = sin(phase * PI).toFloat().coerceIn(0f, 1f) * 0.82f
                            val travel = (phase - 0.5f) * streamLen * 1.4f
                            val sx = (c + 0.5f) * cellW + dx * travel
                            val sy = (r + 0.5f) * cellH + dy * travel
                            val ex = sx + dx * streamLen
                            val ey = sy + dy * streamLen

                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF38BDF8).copy(alpha = alpha),
                                        Color(0xFF7DD3FC).copy(alpha = alpha)
                                    ),
                                    start = Offset(sx, sy),
                                    end = Offset(ex, ey)
                                ),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color(0xFFBAE6FD).copy(alpha = alpha),
                                radius = 2.5.dp.toPx(),
                                center = Offset(ex, ey)
                            )
                        }
                    }
                }

                // Inspected Tap Crosshair Pin
                inspectedPoint?.let { pt ->
                    val (ptTileX, ptTileY) = WebMercatorProjection.latLonToTileDouble(
                        pt.latitude,
                        pt.longitude,
                        tileZoom.toDouble()
                    )
                    val px = viewportWidthPx / 2f + ((ptTileX - centerTileX) * scaledTileSizePx).toFloat()
                    val py = viewportHeightPx / 2f + ((ptTileY - centerTileY) * scaledTileSizePx).toFloat()
                    drawCircle(color = SolarGold.copy(alpha = 0.25f), radius = 16.dp.toPx(), center = Offset(px, py))
                    drawCircle(
                        color = SolarGold,
                        radius = 7.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(px, py))
                }

                // Active Station Beacon (pulsing concentric radar ring + solid pin)
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = (1f - beaconPulse) * 0.55f),
                    radius = (8.dp.toPx() + beaconPulse * 20.dp.toPx()),
                    center = sc,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(color = Color(0xFF0EA5E9), radius = 7.dp.toPx(), center = sc)
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = sc)
            }

            // 5. STATION CITY LABEL PIN (Correct pixel-based IntOffset — stays locked to station!)
            if (stationScreenX in -80f..(viewportWidthPx + 80f) && stationScreenY in -40f..(viewportHeightPx + 40f)) {
                val labelYOffsetPx = with(density) { 14.dp.toPx() }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (stationScreenX - 48f).roundToInt(),
                                y = (stationScreenY + labelYOffsetPx).roundToInt()
                            )
                        }
                        .background(Color(0xDD0B1324), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stationLocation.name.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 6. WIND TELEMETRY CHIP (when Layer 2 Wind Flow is active)
            if (selectedLayer == 2 && currentWeather != null) {
                val windUnit = if (unitSystem == UnitSystem.METRIC) {
                    stringResource(R.string.unit_kmh)
                } else {
                    stringResource(R.string.unit_mph)
                }
                val windSpeedVal = if (unitSystem == UnitSystem.METRIC) {
                    currentWeather.windSpeedKmh.roundToInt()
                } else {
                    (currentWeather.windSpeedKmh * 0.621371).roundToInt()
                }
                val compass = WeatherRepository.getWindCompass(currentWeather.windDirectionDeg)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xDD0B1324),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = stringResource(R.string.radar_layer_wind),
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.radar_wind_telemetry, "$windSpeedVal $windUnit", compass),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 7. INSPECTED COORDINATES CALLOUT CARD (appears when user taps anywhere on the map)
            AnimatedVisibility(
                visible = inspectedPoint != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                inspectedPoint?.let { pt ->
                    val distStr = if (unitSystem == UnitSystem.METRIC) {
                        "${pt.distanceKm.roundToInt()} km"
                    } else {
                        "${(pt.distanceKm * 0.621371).roundToInt()} mi"
                    }
                    val closeDesc = stringResource(R.string.common_close)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xEE0B1324),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SolarGold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("radar_inspected_point_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.radar_pin_coords, pt.latitude, pt.longitude),
                                    color = SolarGold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.radar_pin_distance, distStr, stationLocation.name),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = closeDesc
                                    }
                                    .clickable(onClickLabel = closeDesc) { inspectedPoint = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = closeDesc,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 8. MAP ATTRIBUTION BADGE
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color(0xAA071120), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.radar_map_attribution),
                    color = Color(0xFFCBD5E1),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
