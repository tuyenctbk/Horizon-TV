package com.example

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.configureAppCheck
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.SuggestionType
import com.example.data.model.TVScreen
import com.example.data.worker.SevereWeatherAlertNotificationHelper
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiMeteorologistViewModel
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()
    private val aiViewModel: AiMeteorologistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.defaultFocusHighlightEnabled = true
        }

        handleSevereAlertNotificationIntent(intent)

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("horizon_tv_main_scaffold"),
                    containerColor = TVMidnightNavy,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    val weatherCode = uiState.currentWeather?.weatherCode ?: 0
                    val isDay = uiState.currentWeather?.isDay ?: true

                    AtmosphericBackground(
                        weatherCode = weatherCode,
                        isDay = isDay,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                val activeAlert = uiState.weatherPackage?.alerts?.firstOrNull { it.id !in uiState.dismissedAlertIds }

                                // TV Top Header Bar
                                TVHeader(
                                    currentScreen = uiState.currentScreen,
                                    currentLocation = uiState.currentLocation,
                                    unitSystem = uiState.unitSystem,
                                    hasAlerts = activeAlert != null,
                                    onNavigate = { viewModel.setScreen(it) },
                                    onToggleUnits = { viewModel.toggleUnitSystem() },
                                    onOpenRemote = { viewModel.toggleVirtualRemote() },
                                    onOpenAlerts = {
                                        activeAlert?.let { viewModel.openAlertModal(it) }
                                    }
                                )

                                // Reactive Emergency Weather Alert Banner (Stream Event Monitor)
                                EmergencyBroadcastBanner(
                                    alert = activeAlert,
                                    onOpenModal = { viewModel.openAlertModal(it) },
                                    onDismiss = { activeAlert?.let { viewModel.dismissAlert(it.id) } }
                                )

                                // Offline Room Cache Resilience Banner (when showing last known state during outage)
                                OfflineCacheBanner(
                                    visible = uiState.isOfflineCacheMode && uiState.currentWeather != null,
                                    locationName = uiState.currentLocation.name,
                                    lastSyncedAtMs = uiState.lastSyncedAtMs,
                                    onRetry = { viewModel.loadWeatherForLocation(uiState.currentLocation) }
                                )

                                // Main Content Area
                                Box(modifier = Modifier.weight(1f)) {
                                    val displayError = when {
                                        uiState.errorResId != null -> {
                                            val detail = uiState.errorDetail ?: stringResource(R.string.error_network_generic)
                                            stringResource(uiState.errorResId!!, detail)
                                        }
                                        uiState.errorMessage != null -> uiState.errorMessage
                                        else -> null
                                    }

                                    if (uiState.isLoading && uiState.currentWeather == null) {
                                        BroadcastSplashScreen(isLoading = true)
                                    } else if (displayError != null && uiState.currentWeather == null) {
                                        BroadcastSplashScreen(
                                            isLoading = false,
                                            errorMessage = displayError,
                                            onRetry = { viewModel.loadWeatherForLocation(uiState.currentLocation) }
                                        )
                                    } else if (uiState.currentScreen == TVScreen.LIVE_TV) {
                                        // Live TV has its own native integrated full-screen broadcast layout (Stage + L-Bar + Ticker)
                                        LiveTVBroadcastScreen(
                                            currentLocation = uiState.currentLocation,
                                            currentWeather = uiState.currentWeather,
                                            weatherPackage = uiState.weatherPackage,
                                            unitSystem = uiState.unitSystem,
                                            isAudioPlaying = uiState.isLiveAudioPlaying,
                                            onToggleAudio = { viewModel.toggleLiveAudio() },
                                            onTogglePip = { viewModel.togglePip() },
                                            onOpenAlertModal = { viewModel.openAlertModal(it) }
                                        )
                                    } else {
                                        PersistentLBarLayout(
                                            currentLocation = uiState.currentLocation,
                                            currentWeather = uiState.currentWeather,
                                            weatherPackage = uiState.weatherPackage,
                                            unitSystem = uiState.unitSystem,
                                            isLBarVisible = uiState.isLBarVisible,
                                            onToggleLBar = { viewModel.toggleLBar() }
                                        ) {
                                            AnimatedContent(
                                                targetState = uiState.currentScreen,
                                                transitionSpec = {
                                                    (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 28 }) togetherWith
                                                        fadeOut(tween(170))
                                                },
                                                label = "screenTransition"
                                            ) { targetScreen ->
                                                when (targetScreen) {
                                                    TVScreen.HOME -> {
                                                        HomeDashboardScreen(
                                                            currentLocation = uiState.currentLocation,
                                                            currentWeather = uiState.currentWeather,
                                                            weatherPackage = uiState.weatherPackage,
                                                            favoriteCities = uiState.favoriteCities,
                                                            unitSystem = uiState.unitSystem,
                                                            vodStories = uiState.vodStories,
                                                            onSelectCity = { viewModel.loadWeatherForLocation(it) },
                                                            onNavigate = { viewModel.setScreen(it) },
                                                            onPlayVodStory = { viewModel.playVodStory(it) },
                                                            onOpenAlertModal = { viewModel.openAlertModal(it) },
                                                            onRemoveCity = { viewModel.removeFavorite(it) },
                                                            onDismissAlert = { viewModel.dismissAlert(it.id) },
                                                            isOfflineMode = uiState.isOfflineCacheMode
                                                        )
                                                    }
                                                    TVScreen.LIVE_TV -> {
                                                        // Handled above
                                                    }
                                                    TVScreen.FORECAST -> {
                                                        ForecastScreen(
                                                            currentLocation = uiState.currentLocation,
                                                            currentWeather = uiState.currentWeather,
                                                            weatherPackage = uiState.weatherPackage,
                                                            unitSystem = uiState.unitSystem,
                                                            isOfflineMode = uiState.isOfflineCacheMode
                                                        )
                                                    }
                                                    TVScreen.AI_DESK -> {
                                                        AiMeteorologistScreen(
                                                            currentLocation = uiState.currentLocation,
                                                            currentWeather = uiState.currentWeather,
                                                            weatherPackage = uiState.weatherPackage,
                                                            unitSystem = uiState.unitSystem,
                                                            aiViewModel = aiViewModel
                                                        )
                                                    }
                                                    TVScreen.RADAR -> {
                                                        InteractiveRadarScreen(
                                                            currentLocation = uiState.currentLocation,
                                                            currentWeather = uiState.currentWeather,
                                                            unitSystem = uiState.unitSystem,
                                                            selectedLayer = uiState.radarSelectedLayer,
                                                            isPlaying = uiState.isRadarPlaying,
                                                            frameIndex = uiState.radarFrameIndex,
                                                            radarPackage = uiState.radarPackage,
                                                            isLoadingRadar = uiState.isLoadingRadar,
                                                            radarOpacity = uiState.radarOpacity,
                                                            onSelectLayer = { viewModel.setRadarLayer(it) },
                                                            onTogglePlay = { viewModel.toggleRadarPlayback() },
                                                            onSelectFrame = { viewModel.setRadarFrameIndex(it) },
                                                            onCycleOpacity = { viewModel.cycleRadarOpacity() },
                                                            onRefreshRadar = { viewModel.refreshRadarMetadata() }
                                                        )
                                                    }
                                                    TVScreen.VOD -> {
                                                        VideoOnDemandScreen(
                                                            stories = uiState.vodStories,
                                                            activePlayingStory = uiState.activeVodStory,
                                                            onPlayStory = { viewModel.playVodStory(it) },
                                                            onOpenSchedule = { viewModel.setScreen(TVScreen.SCHEDULE) }
                                                        )
                                                    }
                                                    TVScreen.SCHEDULE -> {
                                                        BroadcastScheduleScreen(
                                                            currentLocation = uiState.currentLocation,
                                                            scheduleSlots = uiState.broadcastSchedule,
                                                            remindedSlotIds = uiState.remindedScheduleIds,
                                                            activePlayingStory = uiState.activeVodStory,
                                                            allVodStories = uiState.vodStories,
                                                            isOfflineCacheMode = uiState.isOfflineCacheMode,
                                                            onPlayStory = { viewModel.playVodStory(it) },
                                                            onToggleReminder = { viewModel.toggleScheduleReminder(it) },
                                                            onRefresh = { viewModel.loadWeatherForLocation(uiState.currentLocation) }
                                                        )
                                                    }
                                                    TVScreen.SEARCH -> {
                                                        CitySearchScreen(
                                                            searchQuery = uiState.searchQuery,
                                                            searchResults = uiState.searchResults,
                                                            isSearching = uiState.isSearching,
                                                            favoriteCities = uiState.favoriteCities,
                                                            currentLocation = uiState.currentLocation,
                                                            onQueryChange = { viewModel.searchCities(it) },
                                                            onSelectCity = {
                                                                viewModel.loadWeatherForLocation(it)
                                                                viewModel.setScreen(TVScreen.HOME)
                                                            },
                                                            onAutoDetectGps = { viewModel.autoDetectGpsLocation() },
                                                            onToggleFavorite = { city ->
                                                                viewModel.toggleFavorite(city)
                                                            },
                                                            onRemoveFavorite = { city ->
                                                                viewModel.removeFavorite(city)
                                                            }
                                                        )
                                                    }
                                                    TVScreen.SETTINGS -> {
                                                        SettingsScreen(
                                                            unitSystem = uiState.unitSystem,
                                                            isCrtGrainEnabled = uiState.isCrtGrainEnabled,
                                                            isVirtualRemoteVisible = uiState.isVirtualRemoteVisible,
                                                            isLiveAudioPlaying = uiState.isLiveAudioPlaying,
                                                            aiQuota = aiUiState.quota,
                                                            aiResetCountdown = aiViewModel.formatResetCountdown(aiUiState.quota.millisUntilReset),
                                                            currentByokKey = aiViewModel.getCurrentByokKey(),
                                                            onToggleUnits = { viewModel.toggleUnitSystem() },
                                                            onSelectUnitSystem = { viewModel.setUnitSystem(it) },
                                                            onToggleCrtGrain = { viewModel.toggleCrtGrain() },
                                                            onToggleVirtualRemote = { viewModel.toggleVirtualRemote() },
                                                            onToggleLiveAudio = { viewModel.toggleLiveAudio() },
                                                            onToggleAiEcoLite = { aiViewModel.toggleEcoLiteMode() },
                                                            onClaimAiGraceCredits = { aiViewModel.claimDailyGraceCredits() },
                                                            onSaveByokKey = { aiViewModel.saveByokKey(it) },
                                                            onClearByokKey = { aiViewModel.clearByokKey() },
                                                            isBackgroundAlertsEnabled = uiState.isBackgroundAlertsEnabled,
                                                            backgroundSyncIntervalMinutes = uiState.backgroundSyncIntervalMinutes,
                                                            lastBackgroundSyncSummary = uiState.lastBackgroundSyncSummary,
                                                            onToggleBackgroundAlerts = { viewModel.toggleBackgroundAlerts() },
                                                            onSelectBackgroundInterval = { viewModel.setBackgroundSyncInterval(it) },
                                                            onSyncBackgroundNow = { viewModel.triggerImmediateBackgroundWeatherCheck() },
                                                            onTestPushNotification = { viewModel.triggerTestSeverePushNotification(it) },
                                                            onTriggerTestAlert = { viewModel.openAlertModal(it) },
                                                            onRateApp = { viewModel.onSuggestionPositive(SuggestionType.RATE) },
                                                            onShareApp = { viewModel.onSuggestionPositive(SuggestionType.SHARE) },
                                                            onOpenOnboardingTour = { viewModel.openOnboardingTour() },
                                                            cachedStationCount = uiState.cachedStationCount,
                                                            lastSyncedAtMs = uiState.lastSyncedAtMs,
                                                            onClearWeatherCache = { viewModel.clearLocalWeatherCache() }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Floating Picture-in-Picture Broadcast Player (shown when active and not already on Live TV)
                            if (uiState.currentScreen != TVScreen.LIVE_TV) {
                                FloatingPiP(
                                    isActive = uiState.isPipActive,
                                    isMuted = uiState.isPipMuted,
                                    onToggleMute = { viewModel.togglePipMute() },
                                    onExpand = {
                                        viewModel.togglePip()
                                        viewModel.setScreen(TVScreen.LIVE_TV)
                                    },
                                    onClose = { viewModel.togglePip() },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(20.dp)
                                )
                            }

                            // Virtual TV Remote Overlay
                            VirtualTVRemote(
                                isVisible = uiState.isVirtualRemoteVisible,
                                onDismiss = { viewModel.toggleVirtualRemote() },
                                onNavigate = { viewModel.setScreen(it) },
                                onDirectionClick = { direction ->
                                    // Handle D-pad directional clicks
                                    when (direction) {
                                        "RIGHT", "DOWN" -> {
                                            val screens = TVScreen.entries
                                            val nextIndex = (uiState.currentScreen.ordinal + 1) % screens.size
                                            viewModel.setScreen(screens[nextIndex])
                                        }
                                        "LEFT", "UP" -> {
                                            val screens = TVScreen.entries
                                            val prevIndex = (uiState.currentScreen.ordinal - 1 + screens.size) % screens.size
                                            viewModel.setScreen(screens[prevIndex])
                                        }
                                        "OK" -> {
                                            // Trigger action on current screen
                                            if (uiState.currentScreen == TVScreen.RADAR) {
                                                viewModel.toggleRadarPlayback()
                                            }
                                        }
                                    }
                                }
                            )

                            // Weather Alert Emergency Modal
                            WeatherAlertModal(
                                alert = uiState.activeAlertModal,
                                onDismiss = { viewModel.dismissAlertModal() }
                            )

                            // Rating & Share Engagement Suggestion Modal (Non-intrusive)
                            SuggestionPromptModal(
                                suggestionType = uiState.activeSuggestion,
                                onPositiveAction = { viewModel.onSuggestionPositive(it) },
                                onLaterAction = { viewModel.onSuggestionLater(it) },
                                onNeverAction = { viewModel.onSuggestionNever(it) }
                            )

                            // Interactive Broadcast Studio Onboarding Tour Modal
                            BroadcastOnboardingModal(
                                isVisible = uiState.isOnboardingVisible,
                                unitSystem = uiState.unitSystem,
                                isCrtGrainEnabled = uiState.isCrtGrainEnabled,
                                onSelectUnitSystem = { viewModel.setUnitSystem(it) },
                                onToggleCrtGrain = { viewModel.toggleCrtGrain() },
                                onComplete = { viewModel.completeOnboarding() }
                            )

                            // CRT Scanlines & Broadcast Vignette Overlay
                            FilmGrainVignette(isEnabled = uiState.isCrtGrainEnabled)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleSevereAlertNotificationIntent(intent)
    }

    private fun handleSevereAlertNotificationIntent(intent: android.content.Intent?) {
        val alertId = intent?.getStringExtra(SevereWeatherAlertNotificationHelper.EXTRA_ALERT_ID)
            ?: return
        val title = intent.getStringExtra(SevereWeatherAlertNotificationHelper.EXTRA_ALERT_TITLE)
            ?.takeIf { it.isNotBlank() } ?: return
        val summary = intent.getStringExtra(SevereWeatherAlertNotificationHelper.EXTRA_ALERT_SUMMARY).orEmpty()
        val instructions = intent.getStringExtra(SevereWeatherAlertNotificationHelper.EXTRA_ALERT_INSTRUCTIONS).orEmpty()
        val severityStr = intent.getStringExtra(SevereWeatherAlertNotificationHelper.EXTRA_ALERT_SEVERITY)
        val severity = try {
            AlertSeverity.valueOf(severityStr ?: AlertSeverity.WARNING.name)
        } catch (_: Exception) {
            AlertSeverity.WARNING
        }

        viewModel.openAlertModal(
            SevereWeatherAlert(
                id = alertId,
                severity = severity,
                title = title,
                summary = summary,
                safetyInstructions = instructions,
                effectiveTime = getString(R.string.alert_effective_now),
                expiresTime = getString(R.string.alert_expires_conditions_improve)
            )
        )
    }

    // Hardware Keyboard / TV Remote Event Dispatcher
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                val current = viewModel.uiState.value.currentScreen
                val screens = TVScreen.entries
                viewModel.setScreen(screens[(current.ordinal + 1) % screens.size])
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                val current = viewModel.uiState.value.currentScreen
                val screens = TVScreen.entries
                viewModel.setScreen(screens[(current.ordinal - 1 + screens.size) % screens.size])
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (viewModel.uiState.value.currentScreen == TVScreen.RADAR) {
                    viewModel.toggleRadarPlayback()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (viewModel.uiState.value.isOnboardingVisible) {
                    viewModel.completeOnboarding()
                    true
                } else if (viewModel.uiState.value.activeSuggestion != null) {
                    viewModel.dismissSuggestion(false)
                    true
                } else if (viewModel.uiState.value.activeAlertModal != null) {
                    viewModel.dismissAlertModal()
                    true
                } else if (viewModel.uiState.value.activeVodStory != null) {
                    viewModel.playVodStory(null)
                    true
                } else if (viewModel.uiState.value.isVirtualRemoteVisible) {
                    viewModel.toggleVirtualRemote()
                    true
                } else if (viewModel.uiState.value.currentScreen != TVScreen.HOME) {
                    viewModel.setScreen(TVScreen.HOME)
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
