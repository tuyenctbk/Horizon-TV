package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TVScreen
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()

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
                                // TV Top Header Bar
                                TVHeader(
                                    currentScreen = uiState.currentScreen,
                                    currentLocation = uiState.currentLocation,
                                    unitSystem = uiState.unitSystem,
                                    hasAlerts = uiState.weatherPackage?.alerts?.isNotEmpty() == true,
                                    onNavigate = { viewModel.setScreen(it) },
                                    onToggleUnits = { viewModel.toggleUnitSystem() },
                                    onOpenRemote = { viewModel.toggleVirtualRemote() },
                                    onOpenAlerts = {
                                        uiState.weatherPackage?.alerts?.firstOrNull()?.let {
                                            viewModel.openAlertModal(it)
                                        }
                                    }
                                )

                                // Reactive Emergency Weather Alert Banner (Stream Event Monitor)
                                EmergencyBroadcastBanner(
                                    alert = uiState.weatherPackage?.alerts?.firstOrNull(),
                                    onOpenModal = { viewModel.openAlertModal(it) },
                                    onDismiss = { viewModel.dismissAlertModal() }
                                )

                                // Main Content Area
                                Box(modifier = Modifier.weight(1f)) {
                                    if (uiState.isLoading && uiState.currentWeather == null) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = HorizonCyan, strokeWidth = 3.dp)
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Text(
                                                    text = stringResource(R.string.loading_sync),
                                                    color = HorizonCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = stringResource(R.string.loading_connecting),
                                                    color = TextSecondarySilver,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    } else if (uiState.errorMessage != null && uiState.currentWeather == null) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = stringResource(R.string.common_warning),
                                                    tint = SevereRed,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = stringResource(R.string.error_signal_interrupted),
                                                    color = SleekTextPrimary,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = uiState.errorMessage ?: stringResource(R.string.common_warning),
                                                    color = SleekTextSecondary,
                                                    fontSize = 13.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = { viewModel.loadWeatherForLocation(uiState.currentLocation) },
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh), tint = Color.White)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(stringResource(R.string.error_reconnect_button), color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        PersistentLBarLayout(
                                            currentLocation = uiState.currentLocation,
                                            currentWeather = uiState.currentWeather,
                                            weatherPackage = uiState.weatherPackage,
                                            unitSystem = uiState.unitSystem,
                                            isLBarVisible = uiState.isLBarVisible,
                                            onToggleLBar = { viewModel.toggleLBar() }
                                        ) {
                                            when (uiState.currentScreen) {
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
                                                        onOpenAlertModal = { viewModel.openAlertModal(it) }
                                                    )
                                                }
                                                TVScreen.LIVE_TV -> {
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
                                                }
                                                TVScreen.FORECAST -> {
                                                    ForecastScreen(
                                                        currentLocation = uiState.currentLocation,
                                                        currentWeather = uiState.currentWeather,
                                                        weatherPackage = uiState.weatherPackage,
                                                        unitSystem = uiState.unitSystem
                                                    )
                                                }
                                                TVScreen.RADAR -> {
                                                    InteractiveRadarScreen(
                                                        currentLocation = uiState.currentLocation,
                                                        selectedLayer = uiState.radarSelectedLayer,
                                                        isPlaying = uiState.isRadarPlaying,
                                                        frameIndex = uiState.radarFrameIndex,
                                                        onSelectLayer = { viewModel.setRadarLayer(it) },
                                                        onTogglePlay = { viewModel.toggleRadarPlayback() },
                                                        onSelectFrame = { viewModel.setRadarFrameIndex(it) }
                                                    )
                                                }
                                                TVScreen.VOD -> {
                                                    VideoOnDemandScreen(
                                                        stories = uiState.vodStories,
                                                        activePlayingStory = uiState.activeVodStory,
                                                        onPlayStory = { viewModel.playVodStory(it) }
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
                                                            if (uiState.favoriteCities.any { it.name.equals(city.name, ignoreCase = true) }) {
                                                                viewModel.removeFavorite(city)
                                                            } else {
                                                                viewModel.addCurrentToFavorites()
                                                            }
                                                        }
                                                    )
                                                }
                                                TVScreen.SETTINGS -> {
                                                    SettingsScreen(
                                                        unitSystem = uiState.unitSystem,
                                                        isCrtGrainEnabled = uiState.isCrtGrainEnabled,
                                                        isVirtualRemoteVisible = uiState.isVirtualRemoteVisible,
                                                        isLiveAudioPlaying = uiState.isLiveAudioPlaying,
                                                        onToggleUnits = { viewModel.toggleUnitSystem() },
                                                        onSelectUnitSystem = { viewModel.setUnitSystem(it) },
                                                        onToggleCrtGrain = { viewModel.toggleCrtGrain() },
                                                        onToggleVirtualRemote = { viewModel.toggleVirtualRemote() },
                                                        onToggleLiveAudio = { viewModel.toggleLiveAudio() },
                                                        onTriggerTestAlert = { viewModel.openAlertModal(it) }
                                                    )
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
                                        "RIGHT" -> {
                                            val screens = TVScreen.values()
                                            val nextIndex = (uiState.currentScreen.ordinal + 1) % screens.size
                                            viewModel.setScreen(screens[nextIndex])
                                        }
                                        "LEFT" -> {
                                            val screens = TVScreen.values()
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

                            // CRT Scanlines & Broadcast Vignette Overlay
                            FilmGrainVignette(isEnabled = uiState.isCrtGrainEnabled)
                        }
                    }
                }
            }
        }
    }

    // Hardware Keyboard / TV Remote Event Dispatcher
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val current = viewModel.uiState.value.currentScreen
                val screens = TVScreen.values()
                viewModel.setScreen(screens[(current.ordinal + 1) % screens.size])
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                val current = viewModel.uiState.value.currentScreen
                val screens = TVScreen.values()
                viewModel.setScreen(screens[(current.ordinal - 1 + screens.size) % screens.size])
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (viewModel.uiState.value.currentScreen == TVScreen.RADAR) {
                    viewModel.toggleRadarPlayback()
                }
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (viewModel.uiState.value.activeAlertModal != null) {
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
