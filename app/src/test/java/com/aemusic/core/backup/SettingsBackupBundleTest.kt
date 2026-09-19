package com.aemusic.core.backup

import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.core.database.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupBundleTest {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun backupBundle_encodesAndDecodesCorrectly() {
        val bundle = SettingsBackupBundle(
            formatVersion = 1,
            exportedAtEpochMs = 1726700000000L,
            appVersion = "2.3.2",
            preferences = UiPreferencesBackup(
                language = "zh",
                fourTabs = false,
                glassEnabled = true,
                glassBorderWidth = 1.8f,
                glassShadowElevation = 8f,
            ),
            trackedPlaylists = listOf(
                TrackedPlaylist("BV123456", "bilibili", "测试歌单", null, 12, "Up主", 1726700000000L),
            ),
            favorites = listOf(
                FavoriteTrackEntity("netease", "1001", 1726700000000L),
            ),
            playlists = listOf(
                LocalPlaylistEntity("pl_1", "我的最爱", 1726700000000L, 1726700000000L),
            ),
            playlistTracks = listOf(
                LocalPlaylistTrackEntity("pl_1", "netease", "1001", 1726700000000L),
            ),
            snapshots = listOf(
                TrackSnapshotEntity(
                    sourceId = "netease",
                    trackId = "1001",
                    title = "七里香",
                    artists = "周杰伦",
                    albumTitle = "七里香",
                    durationMs = 299_000L,
                    artworkKey = "http://artwork/1001",
                    playbackKind = "provider",
                    playbackValue = "1001",
                    playbackPartId = null,
                    updatedAtEpochMs = 1726700000000L,
                ),
            ),
            lyricBindings = listOf(
                LyricBindingEntity(
                    sourceId = "netease",
                    trackId = "1001",
                    provider = "netease",
                    candidateId = "1001",
                    matchedTitle = "七里香",
                    matchedArtist = "周杰伦",
                    lyrics = "[00:00.00]窗外的麻雀",
                    updatedAtEpochMs = 1726700000000L,
                ),
            ),
        )

        val encoded = json.encodeToString(bundle)
        assertTrue(encoded.contains("\"formatVersion\": 1"))
        assertTrue(encoded.contains("\"七里香\""))
        assertTrue(encoded.contains("\"BV123456\""))

        val decoded = json.decodeFromString<SettingsBackupBundle>(encoded)
        assertEquals(1, decoded.formatVersion)
        assertEquals(1, decoded.trackedPlaylists.size)
        assertEquals("BV123456", decoded.trackedPlaylists.first().id)
        assertEquals(1, decoded.favorites.size)
        assertEquals("1001", decoded.favorites.first().trackId)
        assertEquals(1, decoded.playlists.size)
        assertEquals("我的最爱", decoded.playlists.first().name)
        assertEquals(1, decoded.snapshots.size)
        assertEquals("七里香", decoded.snapshots.first().title)
        assertEquals(1, decoded.lyricBindings.size)
        assertEquals("[00:00.00]窗外的麻雀", decoded.lyricBindings.first().lyrics)
        assertEquals(1.8f, decoded.preferences.glassBorderWidth, 0.01f)
        assertEquals(8f, decoded.preferences.glassShadowElevation, 0.01f)
    }
}
