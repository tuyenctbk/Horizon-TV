package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UnitSystem
import com.example.ui.theme.HorizonCyan
import com.example.ui.theme.RadarGreen
import com.example.ui.theme.SevereRed
import com.example.ui.theme.SleekBlueContainer
import com.example.ui.theme.SleekBluePrimary
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekOnBlueContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekSurfaceSecondary
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextPrimary
import com.example.ui.theme.SleekTextSecondary
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SunsetOrange
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingSlideSpec(
    val index: Int,
    val title: String,
    val description: String,
    val highlight1: String,
    val highlight2: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun BroadcastOnboardingModal(
    isVisible: Boolean,
    unitSystem: UnitSystem,
    isCrtGrainEnabled: Boolean,
    onSelectUnitSystem: (UnitSystem) -> Unit,
    onToggleCrtGrain: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(240)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180))
    ) {
        val slides = listOf(
            OnboardingSlideSpec(
                index = 0,
                title = stringResource(R.string.onboarding_slide1_title),
                description = stringResource(R.string.onboarding_slide1_desc),
                highlight1 = stringResource(R.string.onboarding_slide1_highlight1),
                highlight2 = stringResource(R.string.onboarding_slide1_highlight2),
                icon = Icons.Default.LiveTv,
                accentColor = HorizonCyan
            ),
            OnboardingSlideSpec(
                index = 1,
                title = stringResource(R.string.onboarding_slide2_title),
                description = stringResource(R.string.onboarding_slide2_desc),
                highlight1 = stringResource(R.string.onboarding_slide2_highlight1),
                highlight2 = stringResource(R.string.onboarding_slide2_highlight2),
                icon = Icons.Default.Radar,
                accentColor = RadarGreen
            ),
            OnboardingSlideSpec(
                index = 2,
                title = stringResource(R.string.onboarding_slide3_title),
                description = stringResource(R.string.onboarding_slide3_desc),
                highlight1 = stringResource(R.string.onboarding_slide3_highlight1),
                highlight2 = stringResource(R.string.onboarding_slide3_highlight2),
                icon = Icons.Default.AutoAwesome,
                accentColor = SolarGold
            ),
            OnboardingSlideSpec(
                index = 3,
                title = stringResource(R.string.onboarding_slide4_title),
                description = stringResource(R.string.onboarding_slide4_desc),
                highlight1 = stringResource(R.string.onboarding_slide4_highlight1),
                highlight2 = stringResource(R.string.onboarding_slide4_highlight2),
                icon = Icons.Default.NotificationsActive,
                accentColor = SunsetOrange
            )
        )

        var currentStep by remember { mutableIntStateOf(0) }
        val activeSlide = slides[currentStep.coerceIn(0, slides.lastIndex)]
        val stepIndicatorText = stringResource(R.string.onboarding_step_indicator, currentStep + 1, slides.size)
        val skipLabel = stringResource(R.string.onboarding_skip)
        val backLabel = stringResource(R.string.onboarding_back)
        val nextLabel = stringResource(R.string.onboarding_next)
        val finishLabel = stringResource(R.string.onboarding_finish)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xD9020617))
                .clickable(enabled = true, onClick = {})
                .testTag("broadcast_onboarding_modal"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurface,
                border = BorderStroke(1.5.dp, activeSlide.accentColor.copy(alpha = 0.55f)),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 680.dp)
                    .padding(vertical = 16.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp)
                ) {
                    // Header bar: Studio badge + Step indicator + Skip button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = activeSlide.accentColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, activeSlide.accentColor.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(activeSlide.accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.onboarding_badge),
                                        color = activeSlide.accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stepIndicatorText,
                                color = SleekTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("onboarding_skip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = skipLabel,
                                tint = SleekTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Studio Showcase Hero Graphic
                    OnboardingStudioIllustration(
                        stepIndex = currentStep,
                        accentColor = activeSlide.accentColor,
                        contentDescription = activeSlide.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Animated Slide Content
                    AnimatedContent(
                        targetState = activeSlide,
                        transitionSpec = {
                            if (targetState.index >= initialState.index) {
                                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 }) togetherWith
                                    (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 6 })
                            } else {
                                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 6 }) togetherWith
                                    (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 6 })
                            }
                        },
                        label = "onboardingSlideContent"
                    ) { slide ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(slide.accentColor.copy(alpha = 0.16f))
                                        .border(1.dp, slide.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = slide.icon,
                                        contentDescription = slide.title,
                                        tint = slide.accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = slide.title,
                                    color = SleekTextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.3).sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { heading() }
                                        .testTag("onboarding_slide_title")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = slide.description,
                                color = SleekTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.testTag("onboarding_slide_description")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Feature Highlight Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OnboardingHighlightPill(
                                    text = slide.highlight1,
                                    accentColor = slide.accentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                OnboardingHighlightPill(
                                    text = slide.highlight2,
                                    accentColor = slide.accentColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Interactive Studio Quick Setup on the final slide
                            if (slide.index == 3) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OnboardingQuickStudioSetup(
                                    unitSystem = unitSystem,
                                    isCrtGrainEnabled = isCrtGrainEnabled,
                                    onSelectUnitSystem = onSelectUnitSystem,
                                    onToggleCrtGrain = onToggleCrtGrain
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Navigation Row: Step Dots + Back / Next / Enter Studio Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Step Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            slides.forEach { slide ->
                                val isCurrent = slide.index == currentStep
                                val stepDesc = stringResource(
                                    R.string.onboarding_step_indicator,
                                    slide.index + 1,
                                    slides.size
                                ) + ": " + slide.title
                                val dotWidth by animateDpAsState(
                                    targetValue = if (isCurrent) 24.dp else 8.dp,
                                    animationSpec = tween(220),
                                    label = "onboardingDotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .tvFocusVisual(shape = CircleShape, focusBorderColor = activeSlide.accentColor)
                                        .semantics {
                                            role = Role.Tab
                                            selected = isCurrent
                                            contentDescription = stepDesc
                                        }
                                        .clickable { currentStep = slide.index }
                                        .testTag("onboarding_step_dot_${slide.index}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .width(dotWidth)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(
                                                if (isCurrent) activeSlide.accentColor
                                                else SleekTextMuted.copy(alpha = 0.45f)
                                            )
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentStep > 0) {
                                OutlinedButton(
                                    onClick = { currentStep = (currentStep - 1).coerceAtLeast(0) },
                                    shape = RoundedCornerShape(percent = 50),
                                    border = BorderStroke(1.dp, SleekBorder),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("onboarding_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = backLabel,
                                        tint = SleekTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = backLabel,
                                        color = SleekTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onComplete,
                                    shape = RoundedCornerShape(percent = 50),
                                    border = BorderStroke(1.dp, SleekBorder),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("onboarding_skip_text_button")
                                ) {
                                    Text(
                                        text = skipLabel,
                                        color = SleekTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (currentStep < slides.lastIndex) {
                                Button(
                                    onClick = { currentStep = (currentStep + 1).coerceAtMost(slides.lastIndex) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                                    shape = RoundedCornerShape(percent = 50),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("onboarding_next_button")
                                ) {
                                    Text(
                                        text = nextLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = nextLabel,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onComplete,
                                    colors = ButtonDefaults.buttonColors(containerColor = RadarGreen),
                                    shape = RoundedCornerShape(percent = 50),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("onboarding_finish_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = finishLabel,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = finishLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHighlightPill(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SleekSurface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = text,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = SleekTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun OnboardingQuickStudioSetup(
    unitSystem: UnitSystem,
    isCrtGrainEnabled: Boolean,
    onSelectUnitSystem: (UnitSystem) -> Unit,
    onToggleCrtGrain: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SleekSurface,
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_quick_setup_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Unit system quick selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isMetric = unitSystem == UnitSystem.METRIC
                val celsiusLabel = stringResource(R.string.settings_celsius_title)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMetric) SleekBlueContainer else SleekSurfaceSecondary,
                    border = BorderStroke(1.dp, if (isMetric) SleekBluePrimary else SleekBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .semantics(mergeDescendants = true) {
                            role = Role.RadioButton
                            selected = isMetric
                        }
                        .clickable { onSelectUnitSystem(UnitSystem.METRIC) }
                        .testTag("onboarding_unit_metric_button")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = celsiusLabel,
                            color = if (isMetric) SleekOnBlueContainer else SleekTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isImperial = unitSystem == UnitSystem.IMPERIAL
                val fahrenheitLabel = stringResource(R.string.settings_fahrenheit_title)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isImperial) SleekBlueContainer else SleekSurfaceSecondary,
                    border = BorderStroke(1.dp, if (isImperial) SleekBluePrimary else SleekBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .semantics(mergeDescendants = true) {
                            role = Role.RadioButton
                            selected = isImperial
                        }
                        .clickable { onSelectUnitSystem(UnitSystem.IMPERIAL) }
                        .testTag("onboarding_unit_imperial_button")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = fahrenheitLabel,
                            color = if (isImperial) SleekOnBlueContainer else SleekTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CRT Broadcast Scanlines quick toggle
            val crtTitle = stringResource(R.string.settings_crt_title)
            val stateDesc = stringResource(if (isCrtGrainEnabled) R.string.common_active else R.string.livetv_muted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .semantics(mergeDescendants = true) {
                        role = Role.Switch
                        stateDescription = stateDesc
                    }
                    .clickable { onToggleCrtGrain() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("onboarding_crt_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = crtTitle,
                        tint = HorizonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = crtTitle,
                        color = SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = isCrtGrainEnabled,
                    onCheckedChange = { onToggleCrtGrain() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SleekBluePrimary,
                        checkedTrackColor = SleekBlueContainer
                    ),
                    modifier = Modifier.semantics { contentDescription = crtTitle }
                )
            }
        }
    }
}

@Composable
private fun OnboardingStudioIllustration(
    stepIndex: Int,
    accentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboardingCanvasAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboardingPhase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingPulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF04091A),
                        Color(0xFF0A1936),
                        Color(0xFF050D20)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { this.contentDescription = contentDescription }
        ) {
            val w = size.width
            val h = size.height

            // Subtle studio telemetry grid
            val cols = 8
            val rows = 4
            for (c in 1 until cols) {
                val x = w * c / cols
                drawLine(
                    color = accentColor.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }
            for (r in 1 until rows) {
                val y = h * r / rows
                drawLine(
                    color = accentColor.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            when (stepIndex) {
                0 -> {
                    // Slide 1: 24/7 Broadcast L-Bar + Live Stage + Ticker illustration
                    val pad = 16.dp.toPx()
                    val lBarWidth = w * 0.24f
                    val tickerHeight = h * 0.22f
                    // Left L-Bar panel
                    drawRoundRect(
                        color = HorizonCyan.copy(alpha = 0.16f),
                        topLeft = Offset(pad, pad),
                        size = Size(lBarWidth, h - pad * 2 - tickerHeight - 4.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                    // Bottom L-Bar ticker strip
                    drawRoundRect(
                        color = SevereRed.copy(alpha = 0.28f),
                        topLeft = Offset(pad, h - pad - tickerHeight),
                        size = Size(w - pad * 2, tickerHeight),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    // Main Live TV Stage with radar sweep
                    val stageX = pad + lBarWidth + 8.dp.toPx()
                    val stageW = w - stageX - pad
                    val stageH = h - pad * 2 - tickerHeight - 4.dp.toPx()
                    drawRoundRect(
                        color = Color(0xFF08142C),
                        topLeft = Offset(stageX, pad),
                        size = Size(stageW, stageH),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    val cx = stageX + stageW / 2f
                    val cy = pad + stageH / 2f
                    val radius = minOf(stageW, stageH) * 0.36f
                    drawCircle(
                        color = HorizonCyan.copy(alpha = 0.3f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                    val rad = Math.toRadians(phase.toDouble())
                    drawLine(
                        color = HorizonCyan,
                        start = Offset(cx, cy),
                        end = Offset(
                            cx + (radius * cos(rad)).toFloat(),
                            cy + (radius * sin(rad)).toFloat()
                        ),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                1 -> {
                    // Slide 2: 7-Day Thermal Range Bars & Doppler Radar Sweep
                    val barCount = 7
                    val spacing = w / (barCount + 2)
                    val barHeightMax = h * 0.52f
                    for (i in 0 until barCount) {
                        val x = spacing * (i + 1.5f)
                        val topY = h * 0.22f + sin(i * 0.8f) * 12.dp.toPx()
                        val botY = topY + barHeightMax * (0.55f + (i % 3) * 0.14f)
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(SolarGold, HorizonCyan),
                                startY = topY,
                                endY = botY
                            ),
                            start = Offset(x, topY),
                            end = Offset(x, botY),
                            strokeWidth = 10.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = RadarGreen.copy(alpha = pulse),
                            radius = 4.dp.toPx(),
                            center = Offset(x, topY)
                        )
                    }
                }

                2 -> {
                    // Slide 3: AI Meteorologist Desk Waveform & Synoptic Nodes
                    val path = Path()
                    val midY = h / 2f
                    val steps = 48
                    for (i in 0..steps) {
                        val x = w * i / steps
                        val angle = Math.toRadians((phase + i * 12).toDouble())
                        val amp = (h * 0.24f) * sin(i * Math.PI / steps).toFloat()
                        val y = midY + (sin(angle).toFloat() * amp)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(HorizonCyan, SolarGold, RadarGreen)
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = SolarGold.copy(alpha = pulse),
                        radius = 16.dp.toPx(),
                        center = Offset(w / 2f, midY)
                    )
                }

                else -> {
                    // Slide 4: Severe Weather Alert Beacon & WorkManager Pulse Rings
                    val cx = w / 2f
                    val cy = h / 2f
                    val maxR = minOf(w, h) * 0.42f
                    listOf(0.35f, 0.65f, 0.95f).forEach { frac ->
                        drawCircle(
                            color = SunsetOrange.copy(alpha = (1f - frac * 0.6f) * pulse),
                            radius = maxR * frac,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = SevereRed,
                        radius = 14.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}
