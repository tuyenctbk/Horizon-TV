#!/usr/bin/env python3
import os
import xml.sax.saxutils as saxutils

# Base English key-value pairs parsed from strings.xml
base_keys = [
    "app_name", "common_warning", "common_refresh", "common_search", "common_clear", "common_city",
    "common_favorite", "common_close", "common_play_pause", "common_live", "common_active",
    "unit_kmh", "unit_mph", "nav_home", "nav_live_tv", "nav_forecast", "nav_radar", "nav_search",
    "nav_vod", "nav_settings", "brand_title", "brand_subtitle", "badge_live_24_7",
    "header_telemetry_status", "header_remote", "header_active_alert", "lbar_badge_live",
    "lbar_title", "lbar_ch", "lbar_channel_tag", "lbar_collapse", "lbar_expand", "lbar_feels_like",
    "lbar_wind", "lbar_humidity", "lbar_barometer", "lbar_dew_point", "lbar_aqi", "lbar_uv_index",
    "lbar_visibility", "lbar_diurnal_outlook", "lbar_dispatch_title", "lbar_horizon_dispatch",
    "lbar_default_ticker", "lbar_rain_suffix", "emergency_broadcast", "emergency_exp",
    "emergency_safety_protocol", "emergency_dismiss", "emergency_bulletin", "emergency_telemetry_title",
    "emergency_safety_title", "emergency_effective", "emergency_expires", "emergency_acknowledge",
    "alert_warning", "alert_bulletin", "alert_meteorological_telemetry", "alert_civil_defense",
    "alert_effective", "alert_acknowledge_advisory", "alert_test_title", "alert_test_summary",
    "alert_test_instructions", "pip_live", "pip_mute", "pip_expand", "pip_close", "remote_title",
    "remote_close", "remote_up", "remote_down", "remote_left", "remote_right", "remote_dpad_up",
    "remote_dpad_down", "remote_dpad_left", "remote_dpad_right", "remote_ok", "remote_home",
    "remote_live_tv", "remote_radar", "remote_search", "remote_quick_channels", "remote_instructions",
    "home_live_conditions", "home_last_updated", "home_wind_gusts", "home_uv_index_label",
    "home_active_alert_banner_prefix", "home_hourly_synoptic_strip", "home_saved_stations",
    "home_manage_stations", "home_vod_featured", "home_view_all_vod", "home_view_protocol",
    "home_card_radar_title", "home_card_radar_desc", "home_badge_layers", "home_card_forecast_title",
    "home_card_forecast_desc", "home_badge_days", "home_card_vod_title", "home_card_vod_desc",
    "home_badge_vod", "home_carousel_title", "home_carousel_switch", "home_featured_stories",
    "home_all_stories", "home_live_badge", "home_studio_center", "home_meteorologist_duty",
    "home_continuous_broadcast", "home_switch_broadcast", "home_presented_by", "home_news_ticker_prefix",
    "home_local_microclimate", "home_now_badge", "home_feels_like", "home_wind_label",
    "home_humidity_label", "home_aqi_label", "home_diurnal_breakdown", "home_rain_chance",
    "home_wind", "home_humidity", "home_barometer", "home_dew_point", "home_air_quality", "home_audio",
    "forecast_title", "forecast_subtitle", "forecast_tab_five_day", "forecast_tab_hourly",
    "forecast_tab_astronomy", "forecast_tab_telemetry", "forecast_tab_extended",
    "forecast_fourteen_day_header", "forecast_meteorological_outlook", "forecast_numerical_prediction",
    "forecast_tab_5day", "forecast_tab_36hour", "forecast_tab_24hour", "forecast_tab_14day",
    "forecast_rain_risk", "forecast_atmospheric_deepdive", "forecast_relative_humidity",
    "forecast_dew_point", "forecast_surface_pressure", "forecast_high_pressure", "forecast_low_pressure",
    "forecast_uv_index", "forecast_uv_of_11", "forecast_uv_very_high", "forecast_uv_high",
    "forecast_uv_moderate", "forecast_uv_low", "forecast_wind_gust_vector", "forecast_peak_gusts",
    "forecast_atmospheric_visibility", "forecast_clear_horizon", "forecast_reduced_visibility",
    "forecast_cloud_cover", "forecast_sky_observation", "forecast_granular_table", "forecast_hum",
    "forecast_14day_extended", "forecast_rain", "radar_title", "radar_subtitle", "radar_layer_precip",
    "radar_layer_traffic", "radar_layer_satellite", "radar_layer_wind", "radar_legend_precip_title",
    "radar_legend_traffic_title", "radar_legend_satellite_title", "radar_legend_wind_title",
    "radar_legend_precip_light", "radar_legend_precip_mod", "radar_legend_precip_heavy",
    "radar_legend_precip_hail", "radar_legend_traffic_fast", "radar_legend_traffic_mod",
    "radar_legend_traffic_slow", "radar_legend_sat_cirrus", "radar_legend_sat_frontal",
    "radar_legend_sat_core", "radar_legend_wind_vector", "radar_zoom_in", "radar_zoom_out",
    "radar_recenter", "livetv_next_periods", "livetv_live_broadcast", "livetv_doppler_specs",
    "livetv_audio_on", "livetv_muted", "livetv_pip_mode", "livetv_presenter_name",
    "livetv_presenter_subtitle", "search_header_title", "search_header_subtitle", "search_placeholder",
    "search_gps_button", "search_results_title", "search_no_results", "search_saved_title",
    "search_saved_subtitle", "search_active_badge", "settings_header_title", "settings_header_subtitle",
    "settings_cat_display", "settings_crt_title", "settings_crt_subtitle", "settings_remote_title",
    "settings_remote_subtitle", "settings_cat_units", "settings_units_title", "settings_units_subtitle",
    "settings_saved_locally", "settings_celsius_title", "settings_celsius_subtitle",
    "settings_fahrenheit_title", "settings_fahrenheit_subtitle", "settings_cat_audio",
    "settings_audio_title", "settings_audio_subtitle", "settings_cat_civil", "settings_ebs_title",
    "settings_ebs_subtitle", "settings_ebs_btn", "settings_cat_system", "settings_sys_platform",
    "settings_sys_provider", "settings_sys_radar", "settings_sys_astronomy", "settings_sys_interval",
    "vod_title", "vod_subtitle", "vod_badge_4k", "vod_quality_badge", "vod_more_category",
    "vod_more_in_category", "vod_streaming_status", "vod_streaming_feed", "vod_paused_status",
    "vod_paused", "vod_produced_by", "vod_published", "vod_cat_all", "vod_cat_severe",
    "vod_cat_forecasts", "vod_cat_science", "vod_cat_earth_sci", "vod_cat_climate", "astro_title",
    "astro_synodic", "astro_sunrise", "astro_sunset", "astro_daylight_progress", "astro_night_horizon",
    "astro_illumination", "astro_days_to_full", "telemetry_header_title", "telemetry_live_status",
    "telemetry_paused_status", "telemetry_btn_pause", "telemetry_btn_resume", "telemetry_temperature",
    "telemetry_humidity", "telemetry_barometer", "telemetry_temp_chilly", "telemetry_temp_mild",
    "telemetry_temp_warm", "telemetry_temp_heat", "telemetry_hum_dry", "telemetry_hum_ideal",
    "telemetry_hum_humid", "telemetry_hum_high", "telemetry_press_low", "telemetry_press_normal",
    "telemetry_press_high", "recharts_badge", "recharts_title", "recharts_subtitle", "recharts_tab_all",
    "recharts_tab_temp", "recharts_tab_rain", "recharts_today", "recharts_legend_high",
    "recharts_legend_low", "recharts_legend_pop", "recharts_hint", "trends_title", "trends_temp_legend",
    "trends_rain_legend", "loading_sync", "loading_connecting", "error_signal_interrupted",
    "error_reconnect_button"
]

def escape_xml(text):
    text = text.replace("&", "&amp;")
    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    text = text.replace('"', '\\"')
    text = text.replace("'", "\\'")
    return text

print(f"Total base keys: {len(base_keys)}")
