package com.aemusic.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackedPlaylistPolicyTest {
    private val playlists = listOf(
        TrackedPlaylist("a", "netease", "A"),
        TrackedPlaylist("b", "bilibili", "B"),
        TrackedPlaylist("c", "netease", "C"),
    )

    @Test fun movesPlaylistWithinBounds() {
        assertEquals(listOf("b", "a", "c"), reorderTracked(playlists, "a", 1).map { it.id })
        assertEquals(listOf("a", "c", "b"), reorderTracked(playlists, "c", -1).map { it.id })
    }

    @Test fun boundaryAndUnknownMovesAreNoOps() {
        assertEquals(playlists, reorderTracked(playlists, "a", -1))
        assertEquals(playlists, reorderTracked(playlists, "missing", 1))
    }
}
