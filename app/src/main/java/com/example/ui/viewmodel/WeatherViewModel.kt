package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.FullWeatherPackage
import com.example.data.repository.UserPreferencesManager
import com.example.data.repository.WeatherRepository
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
    val currentLocation: CityLocation = CityLocation(
        name = "New York",
        region = "New York",
        country = "United States",
        countryCode = "US",
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
    val isRadarPlaying: Boolean = true,
    val radarFrameIndex: Int = 3, // -30m to +30m (index 0 to 6, 3 = Now)
    val radarSelectedLayer: Int = 0, // 0: Precipitation, 1: Traffic, 2: IR Satellite, 3: Wind Streamlines
    val searchQuery: String = "",
    val searchResults: List<CityLocation> = emptyList(),
    val isSearching: Boolean = false
)

class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: WeatherRepository = WeatherRepository()
    private val preferencesManager = UserPreferencesManager(application)

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            unitSystem = preferencesManager.getUnitSystem(),
            isCrtGrainEnabled = preferencesManager.isCrtGrainEnabled(),
            isVirtualRemoteVisible = preferencesManager.isVirtualRemoteVisible(),
            isLiveAudioPlaying = preferencesManager.isLiveAudioPlaying(),
            isLBarVisible = preferencesManager.isLBarVisible(),
            favoriteCities = preferencesManager.getFavoriteCities() ?: repository.getDefaultFavoriteCities(),
            vodStories = repository.getSampleVodStories()
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var radarTimerJob: Job? = null

    init {
        // Initial location detection and load
        detectAndLoadInitialLocation()
        startEmergencyPolling()
        startRadarLoop()
    }

    private fun detectAndLoadInitialLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val detected = repository.detectRealLocation()
            _uiState.update { it.copy(currentLocation = detected) }
            loadWeatherForLocation(detected)
        }
    }

    fun loadWeatherForLocation(location: CityLocation) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, currentLocation = location) }
            try {
                val (current, fullPkg) = repository.fetchRealWeatherData(location.latitude, location.longitude)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentWeather = current,
                        weatherPackage = fullPkg,
                        activeAlertModal = fullPkg.alerts.firstOrNull() // Show prominent emergency modal if alert active
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to synchronize meteorological feeds: ${e.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun setScreen(screen: TVScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setUnitSystem(unit: UnitSystem) {
        preferencesManager.setUnitSystem(unit)
        _uiState.update { it.copy(unitSystem = unit) }
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

    fun dismissAlertModal() {
        _uiState.update { it.copy(activeAlertModal = null) }
    }

    fun playVodStory(story: VodStory?) {
        _uiState.update { it.copy(activeVodStory = story) }
    }

    fun toggleRadarPlayback() {
        _uiState.update { it.copy(isRadarPlaying = !it.isRadarPlaying) }
    }

    fun setRadarFrameIndex(index: Int) {
        _uiState.update { it.copy(radarFrameIndex = index.coerceIn(0, 6)) }
    }

    fun setRadarLayer(layerIndex: Int) {
        _uiState.update { it.copy(radarSelectedLayer = layerIndex.coerceIn(0, 3)) }
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

    fun toggleFavorite(city: CityLocation) {
        if (_uiState.value.favoriteCities.any { it.name.equals(city.name, ignoreCase = true) }) {
            removeFavorite(city)
        } else {
            addFavorite(city)
        }
    }

    fun searchCities(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
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
                    val (current, fullPkg) = repository.fetchRealWeatherData(loc.latitude, loc.longitude)
                    _uiState.update {
                        it.copy(
                            currentWeather = current,
                            weatherPackage = fullPkg
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun startRadarLoop() {
        radarTimerJob?.cancel()
        radarTimerJob = viewModelScope.launch {
            while (true) {
                delay(1200)
                if (_uiState.value.isRadarPlaying) {
                    _uiState.update {
                        it.copy(radarFrameIndex = (it.radarFrameIndex + 1) % 7)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        radarTimerJob?.cancel()
    }
}
