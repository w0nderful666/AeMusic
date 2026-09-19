package com.aemusic.core.backup

import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.core.data.TrackedPlaylistStore
import com.aemusic.core.database.FavoriteTrackEntity
import com.aemusic.core.database.LibraryDataRepository
import com.aemusic.core.database.LocalPlaylistEntity
import com.aemusic.core.database.LocalPlaylistTrackEntity
import com.aemusic.core.database.LyricBindingEntity
import com.aemusic.core.database.TrackSnapshotEntity
import com.aemusic.feature.settings.AppUiPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UiPreferencesBackup(
    val language: String? = null,
    val fourTabs: Boolean = true,
    val immersiveTitles: Boolean = true,
    val artworkColors: Boolean = true,
    val colorStrength: Float = 0.72f,
    val glassEnabled: Boolean = true,
    val glassClarity: Float = 0.78f,
    val glassBlurRadius: Float = 24f,
    val glassWaterFilm: Float = 0.55f,
    val glassRefractionStrength: Float = 0.65f,
    val glassTintStrength: Float = 0.12f,
    val glassBorderWidth: Float = 1.2f,
    val glassShadowElevation: Float = 6f,
    val artworkBackground: Boolean = true,
    val defaultStreamQuality: String = "Auto",
    val nextTrackPrefetch: Boolean = true,
    val bilibiliBatchCount: Int = 15,
    val preferredLyricsSource: String = "Auto",
    val showSourceReplacementBadge: Boolean = true,
    val webDavServerUrl: String = "",
    val webDavUsername: String = "",
    val webDavPath: String = "/AeMusic/playlists.json",
    val navidromeServerUrl: String = "",
    val navidromeUsername: String = "",
)

@Serializable
data class SettingsBackupBundle(
    val formatVersion: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val appVersion: String = "2.3.2",
    val preferences: UiPreferencesBackup = UiPreferencesBackup(),
    val trackedPlaylists: List<TrackedPlaylist> = emptyList(),
    val favorites: List<FavoriteTrackEntity> = emptyList(),
    val playlists: List<LocalPlaylistEntity> = emptyList(),
    val playlistTracks: List<LocalPlaylistTrackEntity> = emptyList(),
    val snapshots: List<TrackSnapshotEntity> = emptyList(),
    val lyricBindings: List<LyricBindingEntity> = emptyList(),
)

data class ImportSummary(
    val favoritesCount: Int,
    val playlistsCount: Int,
    val trackedPlaylistsCount: Int,
    val lyricBindingsCount: Int,
)

class SettingsBackupManager(
    private val preferences: AppUiPreferences,
    private val trackedPlaylistStore: TrackedPlaylistStore,
    private val libraryDataRepository: LibraryDataRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val prefBackup = UiPreferencesBackup(
            language = preferences.language,
            fourTabs = preferences.fourTabs,
            immersiveTitles = preferences.immersiveTitles,
            artworkColors = preferences.artworkColors,
            colorStrength = preferences.colorStrength,
            glassEnabled = preferences.glassEnabled,
            glassClarity = preferences.glassClarity,
            glassBlurRadius = preferences.glassBlurRadius,
            glassWaterFilm = preferences.glassWaterFilm,
            glassRefractionStrength = preferences.glassRefractionStrength,
            glassTintStrength = preferences.glassTintStrength,
            glassBorderWidth = preferences.glassBorderWidth,
            glassShadowElevation = preferences.glassShadowElevation,
            artworkBackground = preferences.artworkBackground,
            defaultStreamQuality = preferences.defaultStreamQuality,
            nextTrackPrefetch = preferences.nextTrackPrefetch,
            bilibiliBatchCount = preferences.bilibiliBatchCount,
            preferredLyricsSource = preferences.preferredLyricsSource,
            showSourceReplacementBadge = preferences.showSourceReplacementBadge,
            webDavServerUrl = preferences.webDavServerUrl,
            webDavUsername = preferences.webDavUsername,
            webDavPath = preferences.webDavPath,
            navidromeServerUrl = preferences.navidromeServerUrl,
            navidromeUsername = preferences.navidromeUsername,
        )

        val bundle = SettingsBackupBundle(
            formatVersion = 1,
            exportedAtEpochMs = System.currentTimeMillis(),
            preferences = prefBackup,
            trackedPlaylists = trackedPlaylistStore.playlists.value,
            favorites = libraryDataRepository.allFavorites(),
            playlists = libraryDataRepository.allPlaylists(),
            playlistTracks = libraryDataRepository.allPlaylistTracks(),
            snapshots = libraryDataRepository.allTrackSnapshots(),
            lyricBindings = libraryDataRepository.allLyricBindings(),
        )

        json.encodeToString(bundle)
    }

    suspend fun importBackupJson(jsonString: String): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = json.decodeFromString<SettingsBackupBundle>(jsonString)
            if (bundle.formatVersion > 1) {
                error("Unsupported backup format version: ${bundle.formatVersion}")
            }

            val p = bundle.preferences
            p.language?.let { preferences.language = it }
            preferences.fourTabs = p.fourTabs
            preferences.immersiveTitles = p.immersiveTitles
            preferences.artworkColors = p.artworkColors
            preferences.colorStrength = p.colorStrength
            preferences.glassEnabled = p.glassEnabled
            preferences.glassClarity = p.glassClarity
            preferences.glassBlurRadius = p.glassBlurRadius
            preferences.glassWaterFilm = p.glassWaterFilm
            preferences.glassRefractionStrength = p.glassRefractionStrength
            preferences.glassTintStrength = p.glassTintStrength
            preferences.glassBorderWidth = p.glassBorderWidth
            preferences.glassShadowElevation = p.glassShadowElevation
            preferences.artworkBackground = p.artworkBackground
            preferences.defaultStreamQuality = p.defaultStreamQuality
            preferences.nextTrackPrefetch = p.nextTrackPrefetch
            preferences.bilibiliBatchCount = p.bilibiliBatchCount
            preferences.preferredLyricsSource = p.preferredLyricsSource
            preferences.showSourceReplacementBadge = p.showSourceReplacementBadge
            if (p.webDavServerUrl.isNotBlank()) preferences.webDavServerUrl = p.webDavServerUrl
            if (p.webDavUsername.isNotBlank()) preferences.webDavUsername = p.webDavUsername
            if (p.webDavPath.isNotBlank()) preferences.webDavPath = p.webDavPath
            if (p.navidromeServerUrl.isNotBlank()) preferences.navidromeServerUrl = p.navidromeServerUrl
            if (p.navidromeUsername.isNotBlank()) preferences.navidromeUsername = p.navidromeUsername

            if (bundle.trackedPlaylists.isNotEmpty()) {
                trackedPlaylistStore.mergePlaylists(bundle.trackedPlaylists)
            }

            libraryDataRepository.restoreBackup(
                snapshots = bundle.snapshots,
                favorites = bundle.favorites,
                playlists = bundle.playlists,
                playlistTracks = bundle.playlistTracks,
                lyricBindings = bundle.lyricBindings,
            )

            ImportSummary(
                favoritesCount = bundle.favorites.size,
                playlistsCount = bundle.playlists.size,
                trackedPlaylistsCount = bundle.trackedPlaylists.size,
                lyricBindingsCount = bundle.lyricBindings.size,
            )
        }
    }
}
