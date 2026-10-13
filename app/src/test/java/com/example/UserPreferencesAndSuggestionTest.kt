package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CityLocation
import com.example.data.model.SuggestionType
import com.example.data.model.TVScreen
import com.example.data.model.UnitSystem
import com.example.data.repository.UserPreferencesManager
import com.example.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesAndSuggestionTest {

    private lateinit var context: Context
    private lateinit var preferencesManager: UserPreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences for clean test run
        context.getSharedPreferences("horizon_tv_weather_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        preferencesManager = UserPreferencesManager(context)
    }

    @Test
    fun testInitialState() {
        assertEquals(0, preferencesManager.getLaunchCount())
        assertFalse(preferencesManager.hasUserRated())
        assertFalse(preferencesManager.isRatingDeclinedPermanently())
        assertFalse(preferencesManager.hasUserShared())
        assertFalse(preferencesManager.isShareDeclinedPermanently())
        assertEquals(0, preferencesManager.getRatingDismissCount())
        assertEquals(0, preferencesManager.getShareDismissCount())
        assertEquals(UnitSystem.METRIC, preferencesManager.getUnitSystem())
    }

    @Test
    fun testRatingSuggestionLaunchThreshold() {
        // Less than 5 launches -> must NEVER show rating prompt
        for (i in 1..4) {
            preferencesManager.incrementLaunchCount()
            assertFalse(
                "Should not show rating on launch #$i (requires at least 5)",
                preferencesManager.shouldShowRatingSuggestion()
            )
        }

        // 5th launch -> eligible for rating prompt
        preferencesManager.incrementLaunchCount()
        assertEquals(5, preferencesManager.getLaunchCount())
        assertTrue(
            "Should be eligible for rating prompt after 5 launches",
            preferencesManager.shouldShowRatingSuggestion()
        )
    }

    @Test
    fun testRatingSuggestionPermanentDeclineAndCompletion() {
        // Given 5 launches
        repeat(5) { preferencesManager.incrementLaunchCount() }
        assertTrue(preferencesManager.shouldShowRatingSuggestion())

        // User rates -> should never show again
        preferencesManager.setUserRated(true)
        assertTrue(preferencesManager.hasUserRated())
        assertFalse(preferencesManager.shouldShowRatingSuggestion())

        // Reset rating and test permanent decline
        preferencesManager.setUserRated(false)
        preferencesManager.setRatingDeclinedPermanently(true)
        assertTrue(preferencesManager.isRatingDeclinedPermanently())
        assertFalse(preferencesManager.shouldShowRatingSuggestion())
    }

    @Test
    fun testRatingSuggestionDismissCountRule() {
        repeat(5) { preferencesManager.incrementLaunchCount() }
        assertTrue(preferencesManager.shouldShowRatingSuggestion())

        // First dismissal ("Remind Me Later")
        preferencesManager.incrementRatingDismissCount()
        assertEquals(1, preferencesManager.getRatingDismissCount())
        assertFalse(preferencesManager.isRatingDeclinedPermanently())

        // Record prompt shown to simulate 14-day delay
        preferencesManager.recordRatingPromptShown()
        assertFalse("Prompt should not show immediately after being shown", preferencesManager.shouldShowRatingSuggestion())

        // Second dismissal -> Must automatically trigger permanent decline
        preferencesManager.incrementRatingDismissCount()
        assertEquals(2, preferencesManager.getRatingDismissCount())
        assertTrue("Second dismissal must permanently decline rating prompt", preferencesManager.isRatingDeclinedPermanently())
        assertFalse(preferencesManager.shouldShowRatingSuggestion())
    }

    @Test
    fun testShareSuggestionRules() {
        // Share prompt requires at least 10 launches and no active rating prompt
        repeat(5) { preferencesManager.incrementLaunchCount() }
        assertFalse(
            "Share suggestion should not show before 10 launches",
            preferencesManager.shouldShowShareSuggestion()
        )

        repeat(5) { preferencesManager.incrementLaunchCount() }
        assertEquals(10, preferencesManager.getLaunchCount())

        // Rating prompt is eligible, so share prompt must yield (never show simultaneously)
        assertTrue(preferencesManager.shouldShowRatingSuggestion())
        assertFalse("Share prompt must not show while rating prompt is pending", preferencesManager.shouldShowShareSuggestion())

        // Once rating prompt is resolved (e.g., user rated or declined), share prompt becomes eligible
        preferencesManager.setUserRated(true)
        assertFalse(preferencesManager.shouldShowRatingSuggestion())
        assertTrue("Share prompt should be eligible once rating is resolved", preferencesManager.shouldShowShareSuggestion())

        // Two dismissals must permanently decline share prompt
        preferencesManager.incrementShareDismissCount()
        assertEquals(1, preferencesManager.getShareDismissCount())
        preferencesManager.incrementShareDismissCount()
        assertEquals(2, preferencesManager.getShareDismissCount())
        assertTrue("Second dismissal must permanently decline share prompt", preferencesManager.isShareDeclinedPermanently())
        assertFalse(preferencesManager.shouldShowShareSuggestion())
    }

    @Test
    fun testUnitSystemAndDisplayPreferences() {
        assertEquals(UnitSystem.METRIC, preferencesManager.getUnitSystem())
        preferencesManager.setUnitSystem(UnitSystem.IMPERIAL)
        assertEquals(UnitSystem.IMPERIAL, preferencesManager.getUnitSystem())

        assertTrue(preferencesManager.isCrtGrainEnabled())
        preferencesManager.setCrtGrainEnabled(false)
        assertFalse(preferencesManager.isCrtGrainEnabled())

        assertFalse(preferencesManager.isVirtualRemoteVisible())
        preferencesManager.setVirtualRemoteVisible(true)
        assertTrue(preferencesManager.isVirtualRemoteVisible())
    }

    @Test
    fun testFavoriteCitiesPersistence() {
        val testCities = listOf(
            CityLocation("Tokyo", "Tokyo", "Japan", "JP", 35.6762, 139.6503, false),
            CityLocation("London", "England", "United Kingdom", "GB", 51.5074, -0.1278, false)
        )
        preferencesManager.saveFavoriteCities(testCities)
        val loaded = preferencesManager.getFavoriteCities()
        assertNotNull(loaded)
        assertEquals(2, loaded!!.size)
        assertEquals("Tokyo", loaded[0].name)
        assertEquals("London", loaded[1].name)
    }

    @Test
    fun testWeatherViewModelSuggestionHandling() {
        val viewModel = WeatherViewModel(ApplicationProvider.getApplicationContext())

        // On initial creation, active suggestion must be null (no immediate disruption)
        assertNull(viewModel.uiState.value.activeSuggestion)

        // Simulating user choosing "Later"
        viewModel.onSuggestionLater(SuggestionType.RATE)
        assertNull(viewModel.uiState.value.activeSuggestion)
        assertEquals(1, preferencesManager.getRatingDismissCount())

        // Simulating user choosing "Don't Ask Again"
        viewModel.onSuggestionNever(SuggestionType.RATE)
        assertNull(viewModel.uiState.value.activeSuggestion)
        assertTrue(preferencesManager.isRatingDeclinedPermanently())
    }
}
