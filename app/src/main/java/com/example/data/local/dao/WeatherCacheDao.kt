package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.CurrentWeatherEntity
import com.example.data.local.entity.DailyForecastEntity
import com.example.data.local.entity.HourlyForecastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {

    // Reactive Flow queries for live UI / ViewModel observation
    @Query("SELECT * FROM current_weather_cache WHERE locationKey = :locationKey LIMIT 1")
    fun observeCurrentWeather(locationKey: String): Flow<CurrentWeatherEntity?>

    @Query("SELECT * FROM current_weather_cache ORDER BY cachedAtEpochMs DESC LIMIT 1")
    fun observeLatestCurrentWeather(): Flow<CurrentWeatherEntity?>

    @Query("SELECT * FROM daily_forecast_cache WHERE locationKey = :locationKey ORDER BY dayIndex ASC")
    fun observeDailyForecast(locationKey: String): Flow<List<DailyForecastEntity>>

    @Query("SELECT * FROM hourly_forecast_cache WHERE locationKey = :locationKey ORDER BY hourIndex ASC")
    fun observeHourlyForecast(locationKey: String): Flow<List<HourlyForecastEntity>>

    @Query("SELECT COUNT(*) FROM current_weather_cache")
    fun observeCachedStationCount(): Flow<Int>

    // One-shot suspend lookups for offline fallback resolution
    @Query("SELECT * FROM current_weather_cache WHERE locationKey = :locationKey LIMIT 1")
    suspend fun getCurrentWeatherOnce(locationKey: String): CurrentWeatherEntity?

    @Query("SELECT * FROM current_weather_cache ORDER BY cachedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestCurrentWeatherOnce(): CurrentWeatherEntity?

    @Query("SELECT * FROM daily_forecast_cache WHERE locationKey = :locationKey ORDER BY dayIndex ASC")
    suspend fun getDailyForecastOnce(locationKey: String): List<DailyForecastEntity>

    @Query("SELECT * FROM hourly_forecast_cache WHERE locationKey = :locationKey ORDER BY hourIndex ASC")
    suspend fun getHourlyForecastOnce(locationKey: String): List<HourlyForecastEntity>

    // Atomic suspend write operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentWeather(entity: CurrentWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyForecast(entities: List<DailyForecastEntity>)

    @Query("DELETE FROM daily_forecast_cache WHERE locationKey = :locationKey")
    suspend fun deleteDailyForecastForLocation(locationKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyForecast(entities: List<HourlyForecastEntity>)

    @Query("DELETE FROM hourly_forecast_cache WHERE locationKey = :locationKey")
    suspend fun deleteHourlyForecastForLocation(locationKey: String)

    @Transaction
    suspend fun saveFullWeatherSnapshot(
        current: CurrentWeatherEntity,
        daily: List<DailyForecastEntity>,
        hourly: List<HourlyForecastEntity>
    ) {
        upsertCurrentWeather(current)
        if (daily.isNotEmpty()) {
            deleteDailyForecastForLocation(current.locationKey)
            insertDailyForecast(daily)
        }
        if (hourly.isNotEmpty()) {
            deleteHourlyForecastForLocation(current.locationKey)
            insertHourlyForecast(hourly)
        }
    }

    @Transaction
    suspend fun replaceDailyForecast(
        locationKey: String,
        daily: List<DailyForecastEntity>
    ) {
        if (daily.isNotEmpty()) {
            deleteDailyForecastForLocation(locationKey)
            insertDailyForecast(daily)
        }
    }

    @Query("DELETE FROM current_weather_cache")
    suspend fun clearCurrentWeatherCache()

    @Query("DELETE FROM daily_forecast_cache")
    suspend fun clearDailyForecastCache()

    @Query("DELETE FROM hourly_forecast_cache")
    suspend fun clearHourlyForecastCache()

    @Transaction
    suspend fun clearAllCache() {
        clearCurrentWeatherCache()
        clearDailyForecastCache()
        clearHourlyForecastCache()
    }
}
