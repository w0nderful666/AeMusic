package com.aemusic.feature.settings

import android.content.Context
import androidx.core.content.edit

/** Small persistence boundary for the UI preferences already exposed by the phase-2 shell. */
class AppUiPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var language: String?
        get() = preferences.getString(KEY_LANGUAGE, null)
        set(value) { preferences.edit { putString(KEY_LANGUAGE, value) } }

    var fourTabs: Boolean
        get() = preferences.getBoolean(KEY_FOUR_TABS, true)
        set(value) { preferences.edit { putBoolean(KEY_FOUR_TABS, value) } }

    var immersiveTitles: Boolean
        get() = preferences.getBoolean(KEY_IMMERSIVE_TITLES, true)
        set(value) { preferences.edit { putBoolean(KEY_IMMERSIVE_TITLES, value) } }

    var artworkColors: Boolean
        get() = preferences.getBoolean(KEY_ARTWORK_COLORS, true)
        set(value) { preferences.edit { putBoolean(KEY_ARTWORK_COLORS, value) } }

    var colorStrength: Float
        get() = preferences.getFloat(KEY_COLOR_STRENGTH, 0.72f)
        set(value) { preferences.edit { putFloat(KEY_COLOR_STRENGTH, value.coerceIn(0f, 1f)) } }

    var glassClarity: Float
        get() = preferences.getFloat(KEY_GLASS_CLARITY, 0.78f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_CLARITY, value.coerceIn(0.50f, 0.95f)) } }

    var glassBlurRadius: Float
        get() = preferences.getFloat(KEY_GLASS_BLUR_RADIUS, 24f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_BLUR_RADIUS, value.coerceIn(8f, 48f)) } }

    var glassWaterFilm: Float
        get() = preferences.getFloat(KEY_GLASS_WATER_FILM, 0.55f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_WATER_FILM, value.coerceIn(0f, 1f)) } }

    var glassRefractionStrength: Float
        get() = preferences.getFloat(KEY_GLASS_REFRACTION, 0.65f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_REFRACTION, value.coerceIn(0f, 1f)) } }

    var glassTintStrength: Float
        get() = preferences.getFloat(KEY_GLASS_TINT_STRENGTH, 0.12f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_TINT_STRENGTH, value.coerceIn(0f, 0.50f)) } }

    var glassBorderWidth: Float
        get() = preferences.getFloat(KEY_GLASS_BORDER_WIDTH, 1.2f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_BORDER_WIDTH, value.coerceIn(0.5f, 3.0f)) } }

    var glassShadowElevation: Float
        get() = preferences.getFloat(KEY_GLASS_SHADOW_ELEVATION, 6f)
        set(value) { preferences.edit { putFloat(KEY_GLASS_SHADOW_ELEVATION, value.coerceIn(0f, 16f)) } }

    var artworkBackground: Boolean
        get() = preferences.getBoolean(KEY_ARTWORK_BACKGROUND, true)
        set(value) { preferences.edit { putBoolean(KEY_ARTWORK_BACKGROUND, value) } }

    var glassEnabled: Boolean
        get() = preferences.getBoolean(KEY_GLASS_ENABLED, true)
        set(value) { preferences.edit { putBoolean(KEY_GLASS_ENABLED, value) } }

    var defaultStreamQuality: String
        get() = preferences.getString(KEY_DEFAULT_STREAM_QUALITY, "Auto") ?: "Auto"
        set(value) { preferences.edit { putString(KEY_DEFAULT_STREAM_QUALITY, value) } }

    var nextTrackPrefetch: Boolean
        get() = preferences.getBoolean(KEY_NEXT_TRACK_PREFETCH, true)
        set(value) { preferences.edit { putBoolean(KEY_NEXT_TRACK_PREFETCH, value) } }

    var bilibiliBatchCount: Int
        get() = preferences.getInt(KEY_BILIBILI_BATCH_COUNT, 15)
        set(value) { preferences.edit { putInt(KEY_BILIBILI_BATCH_COUNT, value) } }

    var preferredLyricsSource: String
        get() = preferences.getString(KEY_PREFERRED_LYRICS_SOURCE, "Auto") ?: "Auto"
        set(value) { preferences.edit { putString(KEY_PREFERRED_LYRICS_SOURCE, value) } }

    var showSourceReplacementBadge: Boolean
        get() = preferences.getBoolean(KEY_SHOW_SOURCE_BADGE, true)
        set(value) { preferences.edit { putBoolean(KEY_SHOW_SOURCE_BADGE, value) } }

    var webDavServerUrl: String
        get() = preferences.getString(KEY_WEBDAV_SERVER_URL, "") ?: ""
        set(value) { preferences.edit { putString(KEY_WEBDAV_SERVER_URL, value) } }

    var webDavUsername: String
        get() = preferences.getString(KEY_WEBDAV_USERNAME, "") ?: ""
        set(value) { preferences.edit { putString(KEY_WEBDAV_USERNAME, value) } }

    var webDavPassword: String
        get() = preferences.getString(KEY_WEBDAV_PASSWORD, "") ?: ""
        set(value) { preferences.edit { putString(KEY_WEBDAV_PASSWORD, value) } }

    var webDavPath: String
        get() = preferences.getString(KEY_WEBDAV_PATH, "/AeMusic/playlists.json") ?: "/AeMusic/playlists.json"
        set(value) { preferences.edit { putString(KEY_WEBDAV_PATH, value) } }

    var navidromeServerUrl: String
        get() = preferences.getString(KEY_NAVIDROME_SERVER_URL, "") ?: ""
        set(value) { preferences.edit { putString(KEY_NAVIDROME_SERVER_URL, value) } }

    var navidromeUsername: String
        get() = preferences.getString(KEY_NAVIDROME_USERNAME, "") ?: ""
        set(value) { preferences.edit { putString(KEY_NAVIDROME_USERNAME, value) } }

    var navidromeToken: String
        get() = preferences.getString(KEY_NAVIDROME_TOKEN, "") ?: ""
        set(value) { preferences.edit { putString(KEY_NAVIDROME_TOKEN, value) } }

    fun clearLegacyWebDavPassword() = preferences.edit { remove(KEY_WEBDAV_PASSWORD) }

    fun clearLegacyNavidromeToken() = preferences.edit { remove(KEY_NAVIDROME_TOKEN) }

    var homeShortcutPlaylists: List<String>
        get() = preferences.getString(KEY_HOME_SHORTCUT_PLAYLISTS, null)
            ?.split(",")?.filter(String::isNotBlank) ?: emptyList()
        set(value) { preferences.edit { putString(KEY_HOME_SHORTCUT_PLAYLISTS, value.joinToString(",")) } }

    private companion object {
        const val FILE_NAME = "aemusic_ui"
        const val KEY_LANGUAGE = "language"
        const val KEY_FOUR_TABS = "four_tabs"
        const val KEY_IMMERSIVE_TITLES = "immersive_titles"
        const val KEY_ARTWORK_COLORS = "artwork_colors"
        const val KEY_COLOR_STRENGTH = "color_strength"
        const val KEY_GLASS_CLARITY = "glass_clarity"
        const val KEY_GLASS_BLUR_RADIUS = "glass_blur_radius"
        const val KEY_GLASS_WATER_FILM = "glass_water_film"
        const val KEY_GLASS_REFRACTION = "glass_refraction"
        const val KEY_GLASS_TINT_STRENGTH = "glass_tint_strength"
        const val KEY_GLASS_BORDER_WIDTH = "glass_border_width"
        const val KEY_GLASS_SHADOW_ELEVATION = "glass_shadow_elevation"
        const val KEY_ARTWORK_BACKGROUND = "artwork_background"
        const val KEY_GLASS_ENABLED = "glass_enabled"
        const val KEY_DEFAULT_STREAM_QUALITY = "default_stream_quality"
        const val KEY_NEXT_TRACK_PREFETCH = "next_track_prefetch"
        const val KEY_BILIBILI_BATCH_COUNT = "bilibili_batch_count"
        const val KEY_PREFERRED_LYRICS_SOURCE = "preferred_lyrics_source"
        const val KEY_SHOW_SOURCE_BADGE = "show_source_replacement_badge"
        const val KEY_WEBDAV_SERVER_URL = "webdav_server_url"
        const val KEY_WEBDAV_USERNAME = "webdav_username"
        const val KEY_WEBDAV_PASSWORD = "webdav_password"
        const val KEY_WEBDAV_PATH = "webdav_path"
        const val KEY_NAVIDROME_SERVER_URL = "navidrome_server_url"
        const val KEY_NAVIDROME_USERNAME = "navidrome_username"
        const val KEY_NAVIDROME_TOKEN = "navidrome_token"
        const val KEY_HOME_SHORTCUT_PLAYLISTS = "home_shortcut_playlists"
    }
}
