package com.aemusic.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistPolicyTest {
    @Test fun playlistNameIsTrimmedAndLimited() {
        assertEquals("My playlist", normalizePlaylistName("  My playlist  "))
        assertEquals(80, normalizePlaylistName("x".repeat(100)).length)
    }

    @Test fun blankPlaylistNameGetsStableFallback() {
        assertEquals("New playlist", normalizePlaylistName("   "))
    }

    @Test fun latestMembershipIsTheCoverCandidate() {
        val newest = TrackKey("local", "new")
        val playlist = LocalPlaylist("id", "Mix", listOf(newest, TrackKey("local", "old")))
        assertEquals(newest, playlist.coverKey)
    }
}
