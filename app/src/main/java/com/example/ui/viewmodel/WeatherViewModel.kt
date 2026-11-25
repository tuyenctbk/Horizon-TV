package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.*
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.UserPreferencesManager
import com.example.data.repository.WeatherRepository
import com.example.data.worker.SevereWeatherAlertNotificationHelper
import com.example.data.worker.WeatherAlertWorkScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    @StringRes val errorResId: Int? = null,
    val errorDetail: String? = null,
    val currentLocation: CityLocation = CityLocation(
        name = "",
        region = "",
        country = "",
        countryCode = "",
        latitude = 40.7128,
        longitude = -74.0060,
        isGpsDetected = false
    ),
    val currentWeather: CurrentWeather? = null,
    val weatherPackage: FullWeatherPackage? = null,
    val favoriteCities: List<CityLocation> = emptyList(),
    val currentScreen: TVScreen = TVScreen.HOME,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val isCrtGrainEnabled: Boolean = true,
    val isVirtualRemoteVisible: Boolean = false,
    val isPipActive: Boolean = false,
    val isPipMuted: Boolean = false,
    val isLiveAudioPlaying: Boolean = false,
    val isLBarVisible: Boolean = false,
    val activeAlertModal: SevereWeatherAlert? = null,
    val activeVodStory: VodStory? = null,
    val vodStories: List<VodStory> = emptyList(),
    val broadcastSchedule: List<BroadcastScheduleSlot> = emptyList(),
    val remindedScheduleIds: Set<String> = emptySet(),
    val isOfflineCacheMode: Boolean = false,
    val lastSyncedAtMs: Long? = null,
    val cachedStationCount: Int = 0,
    val isRadarPlaying: Boolean = true,
    val radarFrameIndex: Int = 0,
    val radarSelectedLayer: Int = 0, // 0: Precipitation, 1: Satellite IR, 2: Wind Streamlines, 3: Streets & Radar
    val radarPackage: LiveRadarPackage? = null,
    val isLoadingRadar: Boolean = false,
    val radarOpacity: Float = 0.78f,
    val searchQuery: String = "",
    val searchResults: List<CityLocation> = emptyList(),
    val isSearching: Boolean = false,
    val dismissedAlertIds: Set<String> = emptySet(),
    val activeSuggestion: SuggestionType? = null,
    val isBackgroundAlertsEnabled: Boolean = true,
    val backgroundSyncIntervalMinutes: Int = 30,
    val lastBackgroundSyncSummary: String = "",
    val isOnboardingVisible: Boolean = false
)

class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: WeatherRepository = WeatherRepository(application)
    private val preferencesManager = UserPreferencesManager(application)
    private var searchJob: Job? = null
    private var loadWeatherJob: Job? = null

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            currentLocation = preferencesManager.getLastSelectedLocation() ?: repository.defaultLocation,
            unitSystem = preferencesManager.getUnitSystem(),
            isCrtGrainEnabled = preferencesManager.isCrtGrainEnabled(),
            isVirtualRemoteVisible = preferencesManager.isVirtualRemoteVisible(),
            isLiveAudioPlaying = preferencesManager.isLiveAudioPlaying(),
            isLBarVisible = preferencesManager.isLBarVisible(),
            favoriteCities = preferencesManager.getFavoriteCities() ?: repository.getDefaultFavoriteCities(),
            remindedScheduleIds = preferencesManager.getRemindedScheduleIds(),
            vodStories = emptyList(),
            isBackgroundAlertsEnabled = preferencesManager.isBackgroundAlertsEnabled(),
            backgroundSyncIntervalMinutes = preferencesManager.getBackgroundSyncIntervalMinutes(),
            lastBackgroundSyncSummary = preferencesManager.getLastBackgroundSyncSummary(),
            isOnboardingVisible = !preferencesManager.hasCompletedOnboarding()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var radarTimerJob: Job? = null
    private var hasShownSuggestionInSession: Boolean = false

    init {
        // Track app launch for non-intrusive engagement rules
        preferencesManager.incrementLaunchCount()
        // Initialize high-priority severe weather notification channel & WorkManager schedule
        SevereWeatherAlertNotificationHelper.ensureNotificationChannel(application)
        WeatherAlertWorkScheduler.syncPeriodicSchedule(application, replaceExisting = false)
        // Observe local Room cached station count reactively
        viewModelScope.launch {
            repository.observeCachedStationCount().collect { count ->
                _uiState.update { it.copy(cachedStationCount = count) }
            }
        }
        // Initial location detection and load
        detectAndLoadInitialLocation()
        refreshRadarMetadata()
        startEmergencyPolling()
        startRadarLoop()
        scheduleDelayedEngagementCheck()
    }

    fun refreshRadarMetadata() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRadar = true) }
            val radarPkg = repository.fetchLiveRadarMetadata()
            _uiState.update { state ->
                val defaultIdx = if (radarPkg != null && radarPkg.precipitationFrames.isNotEmpty()) {
                    // Start at the latest observed past frame ("NOW")
                    radarPkg.precipitationFrames.indexOfLast { !it.isNowcast }.coerceAtLeast(0)
                } else {
                    0
                }
                state.copy(
                    radarPackage = radarPkg ?: state.radarPackage,
                    isLoadingRadar = false,
                    radarFrameIndex = defaultIdx
                )
            }
        }
    }

    private fun scheduleDelayedEngagementCheck() {
        viewModelScope.launch {
            delay(40_000) // Gentle 40-second delay to ensure user is comfortable before evaluating non-intrusive suggestions
            checkAndShowEngagementSuggestion()
        }
    }

    private fun detectAndLoadInitialLocation() {
        viewModelScope.launch {
            val saved = preferencesManager.getLastSelectedLocation()
            if (saved != null) {
                _uiState.update { it.copy(currentLocation = saved) }
                loadWeatherForLocation(saved)
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val detected = repository.detectRealLocation()
                _uiState.update { it.copy(currentLocation = detected) }
                loadWeatherForLocation(detected)
            }
        }
    }

    fun loadWeatherForLocation(location: CityLocation) {
        preferencesManager.saveLastSelectedLocation(location)
        loadWeatherJob?.cancel()
        loadWeatherJob = viewModelScope.launch {
            // Hydrate immediately from Room cache for this location if available so cold starts are instant
            val preCached = repository.loadCachedWeatherSnapshot(
                location = location,
                fallbackToLatestAnyLocation = false
            )
            if (preCached != null) {
                val unit = _uiState.value.unitSystem
                val cachedVods = repository.buildLiveVodDispatches(
                    location = preCached.location,
                    current = preCached.currentWeather,
                    pkg = preCached.weatherPackage,
                    unitSystem = unit
                )
                val cachedSchedule = repository.buildLiveBroadcastSchedule(
                    location = preCached.location,
                    current = preCached.currentWeather,
                    pkg = preCached.weatherPackage,
                    vodStories = cachedVods,
                    unitSystem = unit
                )
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        errorMessage = null,
                        errorResId = null,
                        errorDetail = null,
                        currentLocation = location,
                        currentWeather = preCached.currentWeather,
                        weatherPackage = preCached.weatherPackage,
                        vodStories = cachedVods,
                        broadcastSchedule = cachedSchedule,
                        activeAlertModal = null,
                        lastSyncedAtMs = preCached.cachedAtEpochMs
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        errorMessage = null,
                        errorResId = null,
                        errorDetail = null,
                        currentLocation = location,
                        activeAlertModal = null
                    )
                }
            }

            try {
                val (current, fullPkg) = repository.fetchRealWeatherData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    location = location
                )
                val unit = _uiState.value.unitSystem
                // Only auto-open blocking full-screen modal for EMERGENCY severity;
                // WARNING/WATCH/ADVISORY remain prominently actionable in the top alert banner & L-Bar ticker.
                val urgentEmergencyModal = fullPkg.alerts.firstOrNull {
                    it.severity == AlertSeverity.EMERGENCY && it.id !in _uiState.value.dismissedAlertIds
                }
                val liveVods = repository.buildLiveVodDispatches(
                    location = location,
                    current = current,
                    pkg = fullPkg,
                    unitSystem = unit
                )
                val liveSchedule = repository.buildLiveBroadcastSchedule(
                    location = location,
                    current = current,
                    pkg = fullPkg,
                    vodStories = liveVods,
                    unitSystem = unit
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentLocation = location,
                        currentWeather = current,
                        weatherPackage = fullPkg,
                        vodStories = liveVods,
                        broadcastSchedule = liveSchedule,
                        activeAlertModal = urgentEmergencyModal,
                        isOfflineCacheMode = false,
                        lastSyncedAtMs = System.currentTimeMillis(),
                        errorMessage = null,
                        errorResId = null,
                        errorDetail = null
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Network outage: restore last known snapshot from local Room database
                val fallbackSnapshot = preCached ?: repository.loadCachedWeatherSnapshot(
                    location = location,
                    fallbackToLatestAnyLocation = true
                )
                if (fallbackSnapshot != null) {
                    val unit = _uiState.value.unitSystem
                    val cachedVods = repository.buildLiveVodDispatches(
                        location = fallbackSnapshot.location,
                        current = fallbackSnapshot.currentWeather,
                        pkg = fallbackSnapshot.weatherPackage,
                        unitSystem = unit
                    )
                    val cachedSchedule = repository.buildLiveBroadcastSchedule(
                        location = fallbackSnapshot.location,
                        current = fallbackSnapshot.currentWeather,
                        pkg = fallbackSnapshot.weatherPackage,
                        vodStories = cachedVods,
                        unitSystem = unit
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentLocation = fallbackSnapshot.location,
                            currentWeather = fallbackSnapshot.currentWeather,
                            weatherPackage = fallbackSnapshot.weatherPackage,
                            vodStories = cachedVods,
                            broadcastSchedule = cachedSchedule,
                            isOfflineCacheMode = true,
                            lastSyncedAtMs = fallbackSnapshot.cachedAtEpochMs,
                            errorMessage = null,
                            errorResId = null,
                            errorDetail = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineCacheMode = true,
                            errorMessage = null,
                            errorResId = R.string.error_sync_failed,
                            errorDetail = e.localizedMessage
                        )
                    }
                }
            }
        }
    }

    fun toggleScheduleReminder(slotId: String) {
        _uiState.update { state ->
            val updated = if (slotId in state.remindedScheduleIds) {
                state.remindedScheduleIds - slotId
            } else {
                state.remindedScheduleIds + slotId
            }
            preferencesManager.saveRemindedScheduleIds(updated)
            state.copy(remindedScheduleIds = updated)
        }
    }

    fun clearLocalWeatherCache() {
        viewModelScope.launch {
            repository.clearWeatherCache()
        }
    }

    private fun checkAndShowEngagementSuggestion() {
        if (hasShownSuggestionInSession) return
        if (_uiState.value.isOnboardingVisible) return
        if (_uiState.value.activeSuggestion != null) return
        if (_uiState.value.activeAlertModal != null) return
        if (_uiState.value.activeVodStory != null) return
        if (_uiState.value.currentScreen == TVScreen.SEARCH) return
        if (preferencesManager.shouldShowRatingSuggestion()) {
            hasShownSuggestionInSession = true
            preferencesManager.recordRatingPromptShown()
            _uiState.update { it.copy(activeSuggestion = SuggestionType.RATE) }
        } else if (preferencesManager.shouldShowShareSuggestion()) {
            hasShownSuggestionInSession = true
            preferencesManager.recordSharePromptShown()
            _uiState.update { it.copy(activeSuggestion = SuggestionType.SHARE) }
        }
    }

    fun completeOnboarding() {
        preferencesManager.setOnboardingCompleted(true)
        _uiState.update { it.copy(isOnboardingVisible = false) }
    }

    fun openOnboardingTour() {
        _uiState.update { it.copy(isOnboardingVisible = true) }
    }

    fun dismissSuggestion(permanently: Boolean = false) {
        val current = _uiState.value.activeSuggestion ?: return
        if (permanently) {
            onSuggestionNever(current)
        } else {
            onSuggestionLater(current)
        }
    }

    fun onSuggestionPositive(type: SuggestionType) {
        if (type == SuggestionType.RATE) {
            preferencesManager.setUserRated(true)
        } else {
            preferencesManager.setUserShared(true)
        }
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    fun onSuggestionLater(type: SuggestionType) {
        if (type == SuggestionType.RATE) {
            preferencesManager.incrementRatingDismissCount()
        } else {
            preferencesManager.incrementShareDismissCount()
        }
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    fun onSuggestionNever(type: SuggestionType) {
        if (type == SuggestionType.RATE) {
            preferencesManager.setRatingDeclinedPermanently(true)
        } else {
            preferencesManager.setShareDeclinedPermanently(true)
        }
        _uiState.update { it.copy(activeSuggestion = null) }
    }

    fun setScreen(screen: TVScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setUnitSystem(unit: UnitSystem) {
        preferencesManager.setUnitSystem(unit)
        _uiState.update { state ->
            val current = state.currentWeather
            val pkg = state.weatherPackage
            val updatedVods = if (current != null && pkg != null) {
                repository.buildLiveVodDispatches(state.currentLocation, current, pkg, unit)
            } else {
                state.vodStories
            }
            val updatedSchedule = if (current != null && pkg != null) {
                repository.buildLiveBroadcastSchedule(state.currentLocation, current, pkg, updatedVods, unit)
            } else {
                state.broadcastSchedule
            }
            state.copy(
                unitSystem = unit,
                vodStories = updatedVods,
                broadcastSchedule = updatedSchedule
            )
        }
    }

    fun toggleUnitSystem() {
        val newUnit = if (_uiState.value.unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
        setUnitSystem(newUnit)
    }

    fun toggleCrtGrain() {
        val newVal = !_uiState.value.isCrtGrainEnabled
        preferencesManager.setCrtGrainEnabled(newVal)
        _uiState.update { it.copy(isCrtGrainEnabled = newVal) }
    }

    fun toggleVirtualRemote() {
        val newVal = !_uiState.value.isVirtualRemoteVisible
        preferencesManager.setVirtualRemoteVisible(newVal)
        _uiState.update { it.copy(isVirtualRemoteVisible = newVal) }
    }

    fun toggleLBar() {
        val newVal = !_uiState.value.isLBarVisible
        preferencesManager.setLBarVisible(newVal)
        _uiState.update { it.copy(isLBarVisible = newVal) }
    }

    fun togglePip() {
        _uiState.update { it.copy(isPipActive = !it.isPipActive) }
    }

    fun togglePipMute() {
        _uiState.update { it.copy(isPipMuted = !it.isPipMuted) }
    }

    fun toggleLiveAudio() {
        val newVal = !_uiState.value.isLiveAudioPlaying
        preferencesManager.setLiveAudioPlaying(newVal)
        _uiState.update { it.copy(isLiveAudioPlaying = newVal) }
    }

    fun openAlertModal(alert: SevereWeatherAlert) {
        _uiState.update { it.copy(activeAlertModal = alert) }
    }

    fun toggleBackgroundAlerts() {
        val next = !_uiState.value.isBackgroundAlertsEnabled
        preferencesManager.setBackgroundAlertsEnabled(next)
        _uiState.update { it.copy(isBackgroundAlertsEnabled = next) }
        WeatherAlertWorkScheduler.syncPeriodicSchedule(getApplication(), replaceExisting = true)
    }

    fun setBackgroundSyncInterval(minutes: Int) {
        val clamped = minutes.coerceAtLeast(15)
        preferencesManager.setBackgroundSyncIntervalMinutes(clamped)
        _uiState.update { it.copy(backgroundSyncIntervalMinutes = clamped) }
        WeatherAlertWorkScheduler.syncPeriodicSchedule(getApplication(), replaceExisting = true)
    }

    fun triggerImmediateBackgroundWeatherCheck() {
        WeatherAlertWorkScheduler.triggerImmediateBackgroundCheck(getApplication(), forceNotify = true)
        viewModelScope.launch {
            delay(1500)
            refreshBackgroundSyncStatus()
        }
    }

    fun triggerTestSeverePushNotification(alert: SevereWeatherAlert) {
        SevereWeatherAlertNotificationHelper.postSevereWeatherNotification(
            context = getApplication(),
            locationName = _uiState.value.currentLocation.name,
            alert = alert
        )
        openAlertModal(alert)
    }

    fun refreshBackgroundSyncStatus() {
        _uiState.update {
            it.copy(
                isBackgroundAlertsEnabled = preferencesManager.isBackgroundAlertsEnabled(),
                backgroundSyncIntervalMinutes = preferencesManager.getBackgroundSyncIntervalMinutes(),
                lastBackgroundSyncSummary = preferencesManager.getLastBackgroundSyncSummary()
            )
        }
    }

    fun dismissAlert(alertId: String) {
        _uiState.update {
            it.copy(
                dismissedAlertIds = it.dismissedAlertIds + alertId,
                activeAlertModal = if (it.activeAlertModal?.id == alertId) null else it.activeAlertModal
            )
        }
    }

    fun dismissAlertModal() {
        val alert = _uiState.value.activeAlertModal
        _uiState.update {
            it.copy(
                activeAlertModal = null,
                dismissedAlertIds = if (alert != null) it.dismissedAlertIds + alert.id else it.dismissedAlertIds
            )
        }
    }

    fun playVodStory(story: VodStory?) {
        _uiState.update { it.copy(activeVodStory = story) }
    }

    fun toggleRadarPlayback() {
        _uiState.update { it.copy(isRadarPlaying = !it.isRadarPlaying) }
    }

    fun setRadarFrameIndex(index: Int) {
        val count = activeRadarFrameCount(_uiState.value)
        _uiState.update { it.copy(radarFrameIndex = index.coerceIn(0, (count - 1).coerceAtLeast(0))) }
    }

    fun setRadarLayer(layerIndex: Int) {
        val clampedLayer = layerIndex.coerceIn(0, 3)
        _uiState.update { state ->
            val updatedState = state.copy(radarSelectedLayer = clampedLayer)
            val count = activeRadarFrameCount(updatedState)
            val safeIdx = if (count > 0) state.radarFrameIndex.coerceIn(0, count - 1) else 0
            updatedState.copy(radarFrameIndex = safeIdx)
        }
    }

    fun cycleRadarOpacity() {
        _uiState.update { state ->
            val nextOpacity = when {
                state.radarOpacity < 0.65f -> 0.78f
                state.radarOpacity < 0.88f -> 0.94f
                else -> 0.52f
            }
            state.copy(radarOpacity = nextOpacity)
        }
    }

    private fun activeRadarFrameCount(state: WeatherUiState): Int {
        val pkg = state.radarPackage ?: return 7
        return when (state.radarSelectedLayer) {
            1 -> pkg.satelliteFrames.size.coerceAtLeast(1)
            else -> pkg.precipitationFrames.size.coerceAtLeast(1)
        }
    }

    fun addCurrentToFavorites() {
        val current = _uiState.value.currentLocation
        addFavorite(current)
    }

    fun addFavorite(city: CityLocation) {
        if (_uiState.value.favoriteCities.none { it.name.equals(city.name, ignoreCase = true) }) {
            val updated = _uiState.value.favoriteCities + city
            preferencesManager.saveFavoriteCities(updated)
            _uiState.update {
                it.copy(favoriteCities = updated)
            }
        }
    }

    fun removeFavorite(city: CityLocation) {
        val updated = _uiState.value.favoriteCities.filter { fav -> !fav.name.equals(city.name, ignoreCase = true) }
        preferencesManager.saveFavoriteCities(updated)
        _uiState.update {
            it.copy(favoriteCities = updated)
        }
    }

    fun clearAllFavorites() {
        preferencesManager.saveFavoriteCities(emptyList())
        _uiState.update {
            it.copy(favoriteCities = emptyList())
        }
    }

    fun toggleFavorite(city: CityLocation) {
        if (_uiState.value.favoriteCities.any { it.name.equals(city.name, ignoreCase = true) }) {
            removeFavorite(city)
        } else {
            addFavorite(city)
        }
    }

    fun searchCities(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(250) // 250ms debounce to prevent racing queries
            val results = repository.searchCities(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun autoDetectGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentScreen = TVScreen.HOME) }
            val loc = repository.detectRealLocation()
            loadWeatherForLocation(loc)
        }
    }

    private fun startEmergencyPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(45_000) // 45 seconds polling loop
                val loc = _uiState.value.currentLocation
                try {
                    val (current, fullPkg) = repository.fetchRealWeatherData(loc.latitude, loc.longitude, location = loc)
                    // Discard stale poll if the user switched cities while request was in-flight
                    if (_uiState.value.currentLocation != loc) continue
                    val unit = _uiState.value.unitSystem
                    val liveVods = repository.buildLiveVodDispatches(loc, current, fullPkg, unit)
                    val liveSchedule = repository.buildLiveBroadcastSchedule(loc, current, fullPkg, liveVods, unit)
                    _uiState.update {
                        it.copy(
                            currentWeather = current,
                            weatherPackage = fullPkg,
                            vodStories = liveVods,
                            broadcastSchedule = liveSchedule,
                            isOfflineCacheMode = false,
                            lastSyncedAtMs = System.currentTimeMillis()
                        )
                    }
                } catch (_: Exception) {
                    if (_uiState.value.currentLocation == loc && _uiState.value.currentWeather != null) {
                        _uiState.update { it.copy(isOfflineCacheMode = true) }
                    }
                }
            }
        }
    }

    fun cycleFavoriteCity(forward: Boolean) {
        val favorites = _uiState.value.favoriteCities
        if (favorites.isEmpty()) return
        val currentIdx = favorites.indexOfFirst {
            it.name.equals(_uiState.value.currentLocation.name, ignoreCase = true)
        }
        val nextIdx = if (currentIdx == -1) {
            0
        } else if (forward) {
            (currentIdx + 1) % favorites.size
        } else {
            (currentIdx - 1 + favorites.size) % favorites.size
        }
        loadWeatherForLocation(favorites[nextIdx])
    }

    private fun startRadarLoop() {
        radarTimerJob?.cancel()
        radarTimerJob = viewModelScope.launch {
            while (true) {
                delay(1100)
                val state = _uiState.value
                if (state.isRadarPlaying && state.currentScreen == TVScreen.RADAR) {
                    val count = activeRadarFrameCount(state).coerceAtLeast(1)
                    _uiState.update {
                        it.copy(radarFrameIndex = (it.radarFrameIndex + 1) % count)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadWeatherJob?.cancel()
        searchJob?.cancel()
        pollingJob?.cancel()
        radarTimerJob?.cancel()
    }
}
