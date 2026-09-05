package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CityLocation
import com.example.data.model.UnitSystem
import org.json.JSONArray
import org.json.JSONObject

class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("horizon_tv_weather_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_UNIT_SYSTEM = "key_unit_system"
        private const val KEY_CRT_GRAIN = "key_crt_grain"
        private const val KEY_VIRTUAL_REMOTE = "key_virtual_remote"
        private const val KEY_LIVE_AUDIO = "key_live_audio"
        private const val KEY_LBAR_VISIBLE = "key_lbar_visible"
        private const val KEY_FAVORITE_CITIES = "key_favorite_cities"
    }

    fun getUnitSystem(): UnitSystem {
        val saved = prefs.getString(KEY_UNIT_SYSTEM, UnitSystem.METRIC.name)
        return try {
            UnitSystem.valueOf(saved ?: UnitSystem.METRIC.name)
        } catch (_: Exception) {
            UnitSystem.METRIC
        }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        prefs.edit().putString(KEY_UNIT_SYSTEM, unitSystem.name).apply()
    }

    fun isCrtGrainEnabled(): Boolean = prefs.getBoolean(KEY_CRT_GRAIN, true)
    fun setCrtGrainEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CRT_GRAIN, enabled).apply()

    fun isVirtualRemoteVisible(): Boolean = prefs.getBoolean(KEY_VIRTUAL_REMOTE, false)
    fun setVirtualRemoteVisible(visible: Boolean) = prefs.edit().putBoolean(KEY_VIRTUAL_REMOTE, visible).apply()

    fun isLiveAudioPlaying(): Boolean = prefs.getBoolean(KEY_LIVE_AUDIO, false)
    fun setLiveAudioPlaying(playing: Boolean) = prefs.edit().putBoolean(KEY_LIVE_AUDIO, playing).apply()

    fun isLBarVisible(): Boolean = prefs.getBoolean(KEY_LBAR_VISIBLE, false)
    fun setLBarVisible(visible: Boolean) = prefs.edit().putBoolean(KEY_LBAR_VISIBLE, visible).apply()

    fun getFavoriteCities(): List<CityLocation>? {
        val raw = prefs.getString(KEY_FAVORITE_CITIES, null) ?: return null
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CityLocation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CityLocation(
                        name = obj.getString("name"),
                        region = obj.optString("region", ""),
                        country = obj.optString("country", ""),
                        countryCode = obj.optString("countryCode", ""),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        isGpsDetected = obj.optBoolean("isGpsDetected", false)
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveFavoriteCities(cities: List<CityLocation>) {
        try {
            val array = JSONArray()
            for (city in cities) {
                val obj = JSONObject().apply {
                    put("name", city.name)
                    put("region", city.region)
                    put("country", city.country)
                    put("countryCode", city.countryCode)
                    put("latitude", city.latitude)
                    put("longitude", city.longitude)
                    put("isGpsDetected", city.isGpsDetected)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_FAVORITE_CITIES, array.toString()).apply()
        } catch (_: Exception) {
        }
    }
}
