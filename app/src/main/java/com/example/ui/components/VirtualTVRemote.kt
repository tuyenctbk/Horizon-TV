package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.model.TVScreen
import com.example.ui.theme.*

@Composable
fun VirtualTVRemote(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (TVScreen) -> Unit,
    onDirectionClick: (String) -> Unit
) {
    if (!isVisible) return

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(260.dp)
                .background(SleekSurface, RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Remote Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekBluePrimary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.remote_title),
                        color = SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remote_close), tint = SleekTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D-PAD Controller
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(SleekSurfaceSecondary)
                    .border(1.dp, SleekBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // UP
                RemoteDirectionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    description = stringResource(R.string.remote_up),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    onClick = { onDirectionClick("UP") }
                )
                // DOWN
                RemoteDirectionButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    description = stringResource(R.string.remote_down),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    onClick = { onDirectionClick("DOWN") }
                )
                // LEFT
                RemoteDirectionButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    description = stringResource(R.string.remote_left),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                    onClick = { onDirectionClick("LEFT") }
                )
                // RIGHT
                RemoteDirectionButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    description = stringResource(R.string.remote_right),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    onClick = { onDirectionClick("RIGHT") }
                )
                // OK / SELECT
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SleekBluePrimary)
                        .clickable { onDirectionClick("OK") }
                        .testTag("remote_ok_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.remote_ok),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fast Television Shortcuts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RemoteQuickAction(
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.remote_home),
                    onClick = {
                        onNavigate(TVScreen.HOME)
                        onDismiss()
                    }
                )
                RemoteQuickAction(
                    icon = Icons.Default.Tv,
                    label = stringResource(R.string.remote_live_tv),
                    onClick = {
                        onNavigate(TVScreen.LIVE_TV)
                        onDismiss()
                    }
                )
                RemoteQuickAction(
                    icon = Icons.Default.Radar,
                    label = stringResource(R.string.remote_radar),
                    onClick = {
                        onNavigate(TVScreen.RADAR)
                        onDismiss()
                    }
                )
                RemoteQuickAction(
                    icon = Icons.Default.Search,
                    label = stringResource(R.string.remote_search),
                    onClick = {
                        onNavigate(TVScreen.SEARCH)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun RemoteDirectionButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = SleekTextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun RemoteQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = SleekSurfaceSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = SleekBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
