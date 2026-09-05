package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.CityLocation
import com.example.data.model.TVScreen
import com.example.data.model.UnitSystem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TVHeader(
    currentScreen: TVScreen,
    currentLocation: CityLocation,
    unitSystem: UnitSystem,
    hasAlerts: Boolean,
    onNavigate: (TVScreen) -> Unit,
    onToggleUnits: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
        while (true) {
            val now = Date()
            currentTimeString = timeFmt.format(now)
            currentDateString = dateFmt.format(now)
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(SleekSurface)
            .border(width = 1.dp, color = SleekBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Broadcast Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Station Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = "Horizon",
                        tint = SleekOnBlueContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.brand_title),
                            color = SleekTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = " " + stringResource(R.string.brand_subtitle),
                            color = SleekBluePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SleekGreenContainer
                        ) {
                            Text(
                                text = stringResource(R.string.badge_live_24_7),
                                color = SleekGreenText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.header_telemetry_status),
                        color = SleekTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Location Tag & Alert Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = hasAlerts) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SleekRedContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SleekRedText.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable { onOpenAlerts() }
                            .padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = stringResource(R.string.header_active_alert),
                                tint = SleekRedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.header_active_alert),
                                color = SleekRedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Current City",
                    tint = SleekBluePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${currentLocation.name.uppercase()}${if (currentLocation.region.isNotBlank()) ", ${currentLocation.region.uppercase()}" else ""}",
                    color = SleekTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (currentLocation.isGpsDetected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SleekGreenContainer
                    ) {
                        Text(
                            text = "GPS",
                            color = SleekGreenText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Digital Clock
                Text(
                    text = currentTimeString,
                    color = SleekBluePrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentDateString,
                    color = SleekTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TV Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TVNavButton(
                    title = stringResource(R.string.nav_home),
                    icon = Icons.Default.Home,
                    isSelected = currentScreen == TVScreen.HOME,
                    onClick = { onNavigate(TVScreen.HOME) }
                )
                TVNavButton(
                    title = stringResource(R.string.nav_forecast),
                    icon = Icons.Default.Timeline,
                    isSelected = currentScreen == TVScreen.FORECAST,
                    onClick = { onNavigate(TVScreen.FORECAST) }
                )
                TVNavButton(
                    title = stringResource(R.string.nav_live_tv),
                    icon = Icons.Default.Tv,
                    isSelected = currentScreen == TVScreen.LIVE_TV,
                    onClick = { onNavigate(TVScreen.LIVE_TV) }
                )
                TVNavButton(
                    title = stringResource(R.string.nav_radar),
                    icon = Icons.Default.Radar,
                    isSelected = currentScreen == TVScreen.RADAR,
                    onClick = { onNavigate(TVScreen.RADAR) }
                )
                TVNavButton(
                    title = stringResource(R.string.nav_vod),
                    icon = Icons.Default.VideoLibrary,
                    isSelected = currentScreen == TVScreen.VOD,
                    onClick = { onNavigate(TVScreen.VOD) }
                )
                TVNavButton(
                    title = stringResource(R.string.nav_search),
                    icon = Icons.Default.Search,
                    isSelected = currentScreen == TVScreen.SEARCH,
                    onClick = { onNavigate(TVScreen.SEARCH) }
                )
                TVNavButton(
                    title = stringResource(R.string.nav_settings),
                    icon = Icons.Default.Settings,
                    isSelected = currentScreen == TVScreen.SETTINGS,
                    onClick = { onNavigate(TVScreen.SETTINGS) }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Quick Control Shortcuts
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Unit Switcher
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = SleekSurfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .clickable { onToggleUnits() }
                        .testTag("unit_toggle_button")
                ) {
                    Text(
                        text = if (unitSystem == UnitSystem.METRIC) "°C / km/h" else "°F / mph",
                        color = SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Virtual TV Remote Shortcut
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = SleekBluePrimary,
                    modifier = Modifier
                        .clickable { onOpenRemote() }
                        .testTag("open_remote_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.SettingsRemote,
                            contentDescription = stringResource(R.string.header_remote),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.header_remote).uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TVNavButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) SleekBlueContainer else SleekSurfaceSecondary
    val textColor = if (isSelected) SleekOnBlueContainer else SleekTextSecondary
    val border = if (isSelected) SleekBluePrimary.copy(alpha = 0.25f) else SleekBorder.copy(alpha = 0.6f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("nav_btn_$title")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}
