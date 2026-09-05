package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.RealTimeTelemetryVisualizer
import com.example.ui.components.RechartsFiveDayForecastView
import com.example.ui.components.TVHeader
import com.example.ui.components.WeatherParticleCanvas
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val mockLocation = CityLocation("New York", "NY", "USA", "US", 40.7128, -74.0060, true)

  private val mockWeather = CurrentWeather(
    tempC = 22.5,
    feelsLikeC = 23.0,
    weatherCode = 1,
    conditionText = "Mainly Clear",
    isDay = true,
    humidityPercent = 55,
    pressureHpa = 1014.2,
    windSpeedKmh = 12.0,
    windDirectionDeg = 180,
    windGustsKmh = 18.0,
    uvIndex = 4.2,
    cloudCoverPercent = 15,
    dewPointC = 12.0,
    visibilityKm = 10.0,
    aqi = 28,
    aqiStatus = "Good",
    pm25 = 6.0,
    pm10 = 12.0,
    sunriseTime = "06:15",
    sunsetTime = "19:45"
  )

  private val mockFiveDays = listOf(
    DailyForecastDay("Today", "Today", 24.0, 16.0, 0, "Clear Sky", 0.0, 5, 5.2, 14.0),
    DailyForecastDay("Thu Sep 06", "Thursday", 22.0, 15.0, 2, "Partly Cloudy", 0.2, 20, 4.8, 16.0),
    DailyForecastDay("Fri Sep 07", "Friday", 19.0, 14.0, 61, "Light Rain", 4.5, 75, 2.5, 22.0),
    DailyForecastDay("Sat Sep 08", "Saturday", 21.0, 13.0, 3, "Overcast", 0.0, 15, 3.8, 12.0),
    DailyForecastDay("Sun Sep 09", "Sunday", 25.0, 17.0, 1, "Mainly Sunny", 0.0, 10, 6.0, 10.0)
  )

  @Test
  fun tv_header_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        TVHeader(
          currentScreen = TVScreen.HOME,
          currentLocation = mockLocation,
          unitSystem = UnitSystem.METRIC,
          hasAlerts = false,
          onNavigate = {},
          onToggleUnits = {},
          onOpenRemote = {},
          onOpenAlerts = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tv_header.png")
  }

  @Test
  fun telemetry_visualizer_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        RealTimeTelemetryVisualizer(
          currentWeather = mockWeather,
          unitSystem = UnitSystem.METRIC
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/telemetry_visualizer.png")
  }

  @Test
  fun recharts_five_day_forecast_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        RechartsFiveDayForecastView(
          dailyForecast = mockFiveDays,
          unitSystem = UnitSystem.METRIC
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/recharts_five_day_forecast.png")
  }

  @Test
  fun settings_screen_unit_toggle_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.SettingsScreen(
          unitSystem = UnitSystem.METRIC,
          isCrtGrainEnabled = true,
          isVirtualRemoteVisible = false,
          isLiveAudioPlaying = false,
          onToggleUnits = {},
          onToggleCrtGrain = {},
          onToggleVirtualRemote = {},
          onToggleLiveAudio = {},
          onTriggerTestAlert = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/settings_screen_units.png")
  }
}

