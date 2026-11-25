package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.DailyForecastDay
import com.example.data.model.SevenDayForecastUiState
import com.example.data.model.UnitSystem
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Self-contained 7-Day Weather Forecast component backed by the free public Open-Meteo API.
 * Displays weekly temperature range bars, localized weather conditions, precipitation probability,
 * and expandable per-day atmospheric breakdowns.
 */
@Composable
fun SevenDayForecastComponent(
    location: CityLocation,
    unitSystem: UnitSystem,
    currentTempC: Double? = null,
    initialForecast: List<DailyForecastDay> = emptyList(),
    repository: WeatherRepository? = null,
    isOfflineMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    val resolvedRepository = remember(repository, appContext) {
        repository ?: WeatherRepository(appContext)
    }
    val coroutineScope = rememberCoroutineScope()
    var uiState by remember {
        mutableStateOf<SevenDayForecastUiState>(
            if (initialForecast.size >= 7) {
                SevenDayForecastUiState.Success(initialForecast.take(7), isCached = isOfflineMode)
            } else {
                SevenDayForecastUiState.Loading
            }
        )
    }
    var isRefreshing by remember { mutableStateOf(false) }
    var expandedIndex by remember(location.latitude, location.longitude) { mutableStateOf<Int?>(null) }

    val genericNetworkError = stringResource(R.string.error_network_generic)

    fun fetchFromApi(showFullLoading: Boolean) {
        coroutineScope.launch {
            if (showFullLoading) {
                uiState = SevenDayForecastUiState.Loading
            }
            isRefreshing = true
            try {
                val (days, fromCache) = resolvedRepository.fetchSevenDayForecastWithCache(
                    location.latitude,
                    location.longitude
                )
                uiState = if (days.isNotEmpty()) {
                    SevenDayForecastUiState.Success(days.take(7), isCached = fromCache)
                } else {
                    SevenDayForecastUiState.Error(genericNetworkError)
                }
            } catch (e: Exception) {
                if (showFullLoading || uiState !is SevenDayForecastUiState.Success) {
                    uiState = SevenDayForecastUiState.Error(e.localizedMessage ?: genericNetworkError)
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(location.latitude, location.longitude, initialForecast, isOfflineMode) {
        if (initialForecast.size >= 7) {
            uiState = SevenDayForecastUiState.Success(initialForecast.take(7), isCached = isOfflineMode)
        } else {
            uiState = SevenDayForecastUiState.Loading
            isRefreshing = true
            try {
                val (days, fromCache) = resolvedRepository.fetchSevenDayForecastWithCache(
                    location.latitude,
                    location.longitude
                )
                uiState = if (days.isNotEmpty()) {
                    SevenDayForecastUiState.Success(days.take(7), isCached = fromCache)
                } else {
                    SevenDayForecastUiState.Error(genericNetworkError)
                }
            } catch (e: Exception) {
                uiState = SevenDayForecastUiState.Error(e.localizedMessage ?: genericNetworkError)
            } finally {
                isRefreshing = false
            }
        }
    }

    val refreshRotation by rememberInfiniteTransition(label = "refreshSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshAngle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SleekSurface, RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
            .testTag("seven_day_forecast_component")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val isShowingCached = (uiState as? SevenDayForecastUiState.Success)?.isCached == true || isOfflineMode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = if (isShowingCached) SolarGold.copy(alpha = 0.18f) else SleekBlueContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isShowingCached) SolarGold else SleekBluePrimary)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(
                                    if (isShowingCached) R.string.seven_day_badge_cached
                                    else R.string.seven_day_badge
                                ),
                                color = if (isShowingCached) SolarGold else SleekOnBlueContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.seven_day_title),
                    color = SleekTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.seven_day_subtitle, location.name),
                    color = SleekTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { if (!isRefreshing) fetchFromApi(showFullLoading = uiState !is SevenDayForecastUiState.Success) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SleekSurfaceSecondary)
                    .border(1.dp, SleekBorder, CircleShape)
                    .testTag("seven_day_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.seven_day_refresh_desc),
                    tint = SleekBluePrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (isRefreshing) refreshRotation else 0f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (val state = uiState) {
            is SevenDayForecastUiState.Loading -> {
                SevenDayLoadingSkeleton()
            }

            is SevenDayForecastUiState.Error -> {
                SevenDayErrorState(
                    message = state.message,
                    onRetry = { fetchFromApi(showFullLoading = true) }
                )
            }

            is SevenDayForecastUiState.Success -> {
                val days = state.days.take(7)
                if (days.isEmpty()) {
                    SevenDayErrorState(
                        message = genericNetworkError,
                        onRetry = { fetchFromApi(showFullLoading = true) }
                    )
                } else {
                    val weekMinC = remember(days) { days.minOf { it.minTempC } }
                    val weekMaxC = remember(days) { days.maxOf { it.maxTempC } }
                    val weekSpanC = (weekMaxC - weekMinC).coerceAtLeast(1.0)
                    val rainyDaysCount = remember(days) { days.count { it.popPercent >= 35 } }

                    // Weekly Summary Strip
                    WeeklySummaryPills(
                        weekMaxC = weekMaxC,
                        weekMinC = weekMinC,
                        rainyDaysCount = rainyDaysCount,
                        unitSystem = unitSystem
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7-Day Forecast Rows
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompact = maxWidth < 500.dp

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            days.forEachIndexed { index, day ->
                                SevenDayRowItem(
                                    index = index,
                                    day = day,
                                    unitSystem = unitSystem,
                                    weekMinC = weekMinC,
                                    weekSpanC = weekSpanC,
                                    currentTempC = if (index == 0) currentTempC else null,
                                    isCompact = isCompact,
                                    isExpanded = expandedIndex == index,
                                    onToggleExpand = {
                                        expandedIndex = if (expandedIndex == index) null else index
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer Hint & API Attribution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.seven_day_tap_hint),
                            color = SleekTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.seven_day_source),
                            color = SleekBluePrimary.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryPills(
    weekMaxC: Double,
    weekMinC: Double,
    rainyDaysCount: Int,
    unitSystem: UnitSystem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(
            icon = Icons.Default.Thermostat,
            text = stringResource(R.string.seven_day_week_high, formatTemperature(weekMaxC, unitSystem)),
            containerColor = SleekYellowContainer,
            contentColor = SleekYellowText
        )
        SummaryPill(
            icon = Icons.Default.AcUnit,
            text = stringResource(R.string.seven_day_week_low, formatTemperature(weekMinC, unitSystem)),
            containerColor = SleekBlueContainer,
            contentColor = SleekOnBlueContainer
        )
        SummaryPill(
            icon = if (rainyDaysCount > 0) Icons.Default.WaterDrop else Icons.Default.WbSunny,
            text = if (rainyDaysCount > 0) {
                stringResource(R.string.seven_day_rainy_days, rainyDaysCount)
            } else {
                stringResource(R.string.seven_day_dry_week)
            },
            containerColor = if (rainyDaysCount > 0) SleekSurfaceSecondary else SleekGreenContainer,
            contentColor = if (rainyDaysCount > 0) SleekBluePrimary else SleekGreenText
        )
    }
}

@Composable
private fun SummaryPill(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        modifier = Modifier.semantics(mergeDescendants = true) { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SevenDayRowItem(
    index: Int,
    day: DailyForecastDay,
    unitSystem: UnitSystem,
    weekMinC: Double,
    weekSpanC: Double,
    currentTempC: Double?,
    isCompact: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val isToday = index == 0 || day.dayOfWeek.equals("Today", ignoreCase = true)
    val dayTitle = when {
        isToday -> stringResource(R.string.seven_day_today)
        index == 1 || day.dayOfWeek.equals("Tomorrow", ignoreCase = true) -> stringResource(R.string.seven_day_tomorrow)
        else -> day.dayOfWeek
    }
    val conditionLabel = stringResource(WeatherRepository.getWeatherDescriptionRes(day.weatherCode))
    val minLabel = formatTemperatureShort(day.minTempC, unitSystem)
    val maxLabel = formatTemperatureShort(day.maxTempC, unitSystem)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotation"
    )

    val rowBackground = when {
        isExpanded -> SleekBlueContainer.copy(alpha = 0.32f)
        isToday -> SleekSurfaceSecondary.copy(alpha = 0.85f)
        else -> SleekSurfaceDashed
    }
    val rowBorder = when {
        isExpanded -> SleekBluePrimary.copy(alpha = 0.55f)
        isToday -> SleekBluePrimary.copy(alpha = 0.25f)
        else -> SleekBorderSubtle
    }

    val iconTint = when (day.weatherCode) {
        3 -> SleekTextSecondary
        71, 73, 75, 77 -> HorizonSky
        else -> null
    }

    val expandDesc = stringResource(R.string.seven_day_expand_desc)
    val stateLabel = if (isExpanded) stringResource(R.string.lbar_collapse) else stringResource(R.string.lbar_expand)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBackground)
            .border(1.dp, rowBorder, RoundedCornerShape(16.dp))
            .tvFocusVisual(shape = RoundedCornerShape(16.dp), focusBorderColor = SleekBluePrimary, focusedScale = 1.015f)
            .semantics {
                role = Role.Button
                stateDescription = stateLabel
            }
            .clickable(onClickLabel = expandDesc) { onToggleExpand() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("seven_day_row_$index")
    ) {
        if (isCompact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day + Date + Condition
                Column(modifier = Modifier.weight(1.15f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dayTitle,
                            color = if (isToday) SleekBluePrimary else SleekTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = day.dateLabel,
                            color = SleekTextMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = conditionLabel,
                        color = SleekTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Condition Icon + Rain %
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(42.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isToday) SleekBlueContainer else SleekSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieWeatherAnimation(
                            weatherCode = day.weatherCode,
                            size = 22.dp,
                            tintFallback = iconTint
                        )
                    }
                    if (day.popPercent >= 15) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${day.popPercent}%",
                            color = SleekBluePrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Min + Proportional Range Bar + Max
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = minLabel,
                        color = SleekBluePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(30.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ThermalRangeBar(
                        minTempC = day.minTempC,
                        maxTempC = day.maxTempC,
                        weekMinC = weekMinC,
                        weekSpanC = weekSpanC,
                        currentTempC = currentTempC,
                        modifier = Modifier
                            .width(58.dp)
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = maxLabel,
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.seven_day_expand_desc),
                    tint = SleekTextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }
        } else {
            // Wide / Tablet / TV Single-Line Broadcast Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(126.dp)) {
                    Text(
                        text = dayTitle,
                        color = if (isToday) SleekBluePrimary else SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = day.dateLabel,
                        color = SleekTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isToday) SleekBlueContainer else SleekSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieWeatherAnimation(
                            weatherCode = day.weatherCode,
                            size = 22.dp,
                            tintFallback = iconTint
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = conditionLabel,
                        color = SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Precip Probability Pill
                Box(
                    modifier = Modifier.width(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.popPercent >= 15) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = SleekBlueContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = stringResource(R.string.seven_day_precip),
                                    tint = SleekBluePrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${day.popPercent}%",
                                    color = SleekOnBlueContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Temperature Range Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = minLabel,
                        color = SleekBluePrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ThermalRangeBar(
                        minTempC = day.minTempC,
                        maxTempC = day.maxTempC,
                        weekMinC = weekMinC,
                        weekSpanC = weekSpanC,
                        currentTempC = currentTempC,
                        modifier = Modifier
                            .width(112.dp)
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = maxLabel,
                        color = SleekTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.seven_day_expand_desc),
                    tint = SleekTextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
        }

        // Expandable Daily Atmospheric Breakdown
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            DailyBreakdownTray(
                day = day,
                unitSystem = unitSystem,
                isCompact = isCompact
            )
        }
    }
}

@Composable
private fun ThermalRangeBar(
    minTempC: Double,
    maxTempC: Double,
    weekMinC: Double,
    weekSpanC: Double,
    currentTempC: Double?,
    modifier: Modifier = Modifier
) {
    val rangeDesc = "${minTempC.roundToInt()}° – ${maxTempC.roundToInt()}°"
    Canvas(
        modifier = modifier.semantics { contentDescription = rangeDesc }
    ) {
        val trackHeight = size.height
        val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

        // Full weekly span track
        drawRoundRect(
            color = SleekBorder,
            topLeft = Offset.Zero,
            size = Size(size.width, trackHeight),
            cornerRadius = radius
        )

        val rawStart = ((minTempC - weekMinC) / weekSpanC).toFloat().coerceIn(0f, 1f)
        val rawEnd = ((maxTempC - weekMinC) / weekSpanC).toFloat().coerceIn(rawStart, 1f)
        val minBarWidth = (size.width * 0.14f).coerceAtLeast(trackHeight)
        val startX = rawStart * size.width
        val endX = (rawEnd * size.width).coerceAtLeast((startX + minBarWidth).coerceAtMost(size.width))
        val clampedStartX = (endX - minBarWidth).coerceAtMost(startX).coerceAtLeast(0f)
        val barWidth = (endX - clampedStartX).coerceAtLeast(trackHeight)

        // Thermal spectrum anchored across full week width so cooler days read cool and warmer days read warm
        val thermalBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF38BDF8),
                Color(0xFF22C55E),
                Color(0xFFF59E0B),
                Color(0xFFEF4444)
            ),
            startX = 0f,
            endX = size.width
        )

        drawRoundRect(
            brush = thermalBrush,
            topLeft = Offset(clampedStartX, 0f),
            size = Size(barWidth, trackHeight),
            cornerRadius = radius
        )

        // Current temperature indicator dot on Today's bar
        if (currentTempC != null) {
            val dotFrac = ((currentTempC - weekMinC) / weekSpanC).toFloat().coerceIn(0.05f, 0.95f)
            val dotCenter = Offset(dotFrac * size.width, trackHeight / 2f)
            drawCircle(
                color = Color.White,
                radius = trackHeight * 0.65f,
                center = dotCenter
            )
            drawCircle(
                color = SleekOnBlueContainer,
                radius = trackHeight * 0.42f,
                center = dotCenter
            )
        }
    }
}

@Composable
private fun DailyBreakdownTray(
    day: DailyForecastDay,
    unitSystem: UnitSystem,
    isCompact: Boolean
) {
    val feelsText = "${formatTemperatureShort(day.apparentMinTempC, unitSystem)} / ${formatTemperatureShort(day.apparentMaxTempC, unitSystem)}"
    val rainUnit = if (unitSystem == UnitSystem.METRIC) stringResource(R.string.unit_mm) else stringResource(R.string.unit_in)
    val rainVal = if (unitSystem == UnitSystem.METRIC) {
        String.format(Locale.US, "%.1f", day.rainMm)
    } else {
        String.format(Locale.US, "%.2f", day.rainMm * 0.0393701)
    }
    val precipText = "${day.popPercent}% • $rainVal $rainUnit"

    val windUnit = if (unitSystem == UnitSystem.METRIC) stringResource(R.string.unit_kmh) else stringResource(R.string.unit_mph)
    val windVal = if (unitSystem == UnitSystem.METRIC) day.maxWindKmh.roundToInt() else (day.maxWindKmh * 0.621371).roundToInt()
    val gustVal = if (unitSystem == UnitSystem.METRIC) day.maxWindGustsKmh.roundToInt() else (day.maxWindGustsKmh * 0.621371).roundToInt()
    val windText = "$windVal / $gustVal $windUnit"

    val uvText = stringResource(R.string.forecast_uv_of_11, day.maxUvIndex.roundToInt())
    val sunText = if (day.sunriseTime.isNotBlank() && day.sunsetTime.isNotBlank()) {
        "${day.sunriseTime} – ${day.sunsetTime}"
    } else {
        "--:-- – --:--"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = SleekBorder.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(10.dp))

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakdownChip(
                        icon = Icons.Default.DeviceThermostat,
                        label = stringResource(R.string.seven_day_feels_like),
                        value = feelsText,
                        accent = SolarGold,
                        modifier = Modifier.weight(1f)
                    )
                    BreakdownChip(
                        icon = Icons.Default.WaterDrop,
                        label = stringResource(R.string.seven_day_precip),
                        value = precipText,
                        accent = SleekBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakdownChip(
                        icon = Icons.Default.Air,
                        label = stringResource(R.string.seven_day_wind_gusts),
                        value = windText,
                        accent = SleekGreenText,
                        modifier = Modifier.weight(1f)
                    )
                    BreakdownChip(
                        icon = Icons.Default.WbSunny,
                        label = stringResource(R.string.seven_day_peak_uv),
                        value = uvText,
                        accent = SunsetOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                BreakdownChip(
                    icon = Icons.Default.WbTwilight,
                    label = stringResource(R.string.seven_day_sun_cycle),
                    value = sunText,
                    accent = SolarGold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BreakdownChip(
                    icon = Icons.Default.DeviceThermostat,
                    label = stringResource(R.string.seven_day_feels_like),
                    value = feelsText,
                    accent = SolarGold,
                    modifier = Modifier.weight(1f)
                )
                BreakdownChip(
                    icon = Icons.Default.WaterDrop,
                    label = stringResource(R.string.seven_day_precip),
                    value = precipText,
                    accent = SleekBluePrimary,
                    modifier = Modifier.weight(1f)
                )
                BreakdownChip(
                    icon = Icons.Default.Air,
                    label = stringResource(R.string.seven_day_wind_gusts),
                    value = windText,
                    accent = SleekGreenText,
                    modifier = Modifier.weight(1f)
                )
                BreakdownChip(
                    icon = Icons.Default.WbSunny,
                    label = stringResource(R.string.seven_day_peak_uv),
                    value = uvText,
                    accent = SunsetOrange,
                    modifier = Modifier.weight(1f)
                )
                BreakdownChip(
                    icon = Icons.Default.WbTwilight,
                    label = stringResource(R.string.seven_day_sun_cycle),
                    value = sunText,
                    accent = SolarGold,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }
    }
}

@Composable
private fun BreakdownChip(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SleekSurface, RoundedCornerShape(12.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = SleekTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            color = SleekTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SevenDayLoadingSkeleton() {
    val pulseAlpha by rememberInfiniteTransition(label = "skeletonPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val loadingDesc = stringResource(R.string.seven_day_loading)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("seven_day_loading_state"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = SleekBluePrimary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(14.dp)
                    .semantics { contentDescription = loadingDesc }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = loadingDesc,
                color = SleekTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        repeat(7) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(pulseAlpha)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SleekSurfaceSecondary)
            )
        }
    }
}

@Composable
private fun SevenDayErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SleekRedContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, SevereRed.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .testTag("seven_day_error_state")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = stringResource(R.string.error_network_generic),
                tint = SevereRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.seven_day_error, message),
                color = SleekTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("seven_day_retry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.seven_day_retry),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.seven_day_retry),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTemperature(tempC: Double, unitSystem: UnitSystem): String {
    return if (unitSystem == UnitSystem.METRIC) {
        "${tempC.roundToInt()}°C"
    } else {
        "${(tempC * 9.0 / 5.0 + 32.0).roundToInt()}°F"
    }
}

private fun formatTemperatureShort(tempC: Double, unitSystem: UnitSystem): String {
    return if (unitSystem == UnitSystem.METRIC) {
        "${tempC.roundToInt()}°"
    } else {
        "${(tempC * 9.0 / 5.0 + 32.0).roundToInt()}°"
    }
}
