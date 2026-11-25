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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SolarGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflineCacheBanner(
    visible: Boolean,
    locationName: String,
    lastSyncedAtMs: Long?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedSyncTime = remember(lastSyncedAtMs) {
        if (lastSyncedAtMs != null && lastSyncedAtMs > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSyncedAtMs))
        } else {
            "--:--"
        }
    }

    val titleText = stringResource(R.string.offline_cache_banner_title)
    val subtitleText = stringResource(R.string.offline_cache_banner_subtitle, locationName, formattedSyncTime)
    val retryText = stringResource(R.string.offline_cache_retry)

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xFF2B1F06),
            border = BorderStroke(1.dp, SolarGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_cache_banner")
                .semantics(mergeDescendants = false) {
                    liveRegion = LiveRegionMode.Polite
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$titleText. $subtitleText"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = titleText,
                            color = SolarGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = subtitleText,
                            color = Color(0xFFFEF3C7),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(percent = 50),
                    color = SolarGold,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("offline_cache_retry_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = retryText,
                            tint = Color(0xFF1F1604),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = retryText,
                            color = Color(0xFF1F1604),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
