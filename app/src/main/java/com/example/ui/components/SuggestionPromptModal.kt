package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.SuggestionType
import com.example.ui.theme.*

@Composable
fun SuggestionPromptModal(
    suggestionType: SuggestionType?,
    onPositiveAction: (SuggestionType) -> Unit,
    onLaterAction: (SuggestionType) -> Unit,
    onNeverAction: (SuggestionType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestionType == null) return

    val context = LocalContext.current

    Dialog(
        onDismissRequest = { onLaterAction(suggestionType) },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SleekSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (suggestionType == SuggestionType.RATE) SolarGold.copy(alpha = 0.5f) else SleekBluePrimary.copy(alpha = 0.5f)),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag(if (suggestionType == SuggestionType.RATE) "rating_suggestion_modal" else "share_suggestion_modal")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (suggestionType == SuggestionType.RATE) SolarGold.copy(alpha = 0.15f)
                                else SleekBluePrimary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (suggestionType == SuggestionType.RATE) Icons.Default.Star else Icons.Default.Share,
                            contentDescription = null,
                            tint = if (suggestionType == SuggestionType.RATE) SolarGold else SleekBluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { onLaterAction(suggestionType) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Five Star indicator for rating
                if (suggestionType == SuggestionType.RATE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SolarGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Title & Subtitle
                Text(
                    text = stringResource(
                        if (suggestionType == SuggestionType.RATE) R.string.suggestion_rate_title
                        else R.string.suggestion_share_title
                    ),
                    color = SleekTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(
                        if (suggestionType == SuggestionType.RATE) R.string.suggestion_rate_desc
                        else R.string.suggestion_share_desc
                    ),
                    color = SleekTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                val shareText = stringResource(R.string.suggestion_share_text)

                // Primary Action Button
                Button(
                    onClick = {
                        if (suggestionType == SuggestionType.RATE) {
                            launchPlayStore(context)
                        } else {
                            launchShareSheet(context, shareText)
                        }
                        onPositiveAction(suggestionType)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (suggestionType == SuggestionType.RATE) SolarGold else SleekBluePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(if (suggestionType == SuggestionType.RATE) "rate_positive_button" else "share_positive_button")
                ) {
                    Text(
                        text = stringResource(
                            if (suggestionType == SuggestionType.RATE) R.string.suggestion_rate_positive
                            else R.string.suggestion_share_positive
                        ),
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary "Remind Me Later" Button
                OutlinedButton(
                    onClick = { onLaterAction(suggestionType) },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("suggestion_later_button")
                ) {
                    Text(
                        text = stringResource(
                            if (suggestionType == SuggestionType.RATE) R.string.suggestion_rate_later
                            else R.string.suggestion_share_later
                        ),
                        color = SleekTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tertiary "Don't Ask Again" Button (Never bother repeatedly)
                TextButton(
                    onClick = { onNeverAction(suggestionType) },
                    modifier = Modifier.testTag("suggestion_never_button")
                ) {
                    Text(
                        text = stringResource(
                            if (suggestionType == SuggestionType.RATE) R.string.suggestion_rate_never
                            else R.string.suggestion_share_never
                        ),
                        color = SleekTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun launchPlayStore(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}

private fun launchShareSheet(context: Context, text: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "Share Horizon TV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
