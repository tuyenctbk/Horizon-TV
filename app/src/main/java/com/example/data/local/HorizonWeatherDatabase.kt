package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.WeatherCacheDao
import com.example.data.local.entity.CurrentWeatherEntity
import com.example.data.local.entity.DailyForecastEntity
import com.example.data.local.entity.HourlyForecastEntity

@Database(
    entities = [
        CurrentWeatherEntity::class,
        DailyForecastEntity::class,
        HourlyForecastEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HorizonWeatherDatabase : RoomDatabase() {

    abstract fun weatherCacheDao(): WeatherCacheDao

    companion object {
        private const val DATABASE_NAME = "horizon_weather_cache.db"

        @Volatile
        private var INSTANCE: HorizonWeatherDatabase? = null

        fun getInstance(context: Context): HorizonWeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HorizonWeatherDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): HorizonWeatherDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                HorizonWeatherDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
