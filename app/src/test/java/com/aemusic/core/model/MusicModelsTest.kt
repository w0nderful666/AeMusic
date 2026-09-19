package com.aemusic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MusicModelsTest {
    @Test(expected = IllegalArgumentException::class)
    fun trackId_rejectsBlankValues() { TrackId(" ") }

    @Test
    fun duration_clampsInvalidMediaMetadataToZero() {
        assertEquals(0, TrackDuration.ofMilliseconds(-10).milliseconds)
    }

    @Test
    fun sameRawId_fromDifferentSources_hasDifferentIdentity() {
        val local = track(MusicSourceId.Local)
        val remote = track(MusicSourceId("remote"))
        assertNotEquals(local, remote)
    }

    @Test
    fun missingArtwork_isExplicit() {
        assertSame(ArtworkRef.Missing, track(MusicSourceId.Local).artwork)
    }

    @Test
    fun track_supportsMultipleArtistsAndLongTitles() {
        val track = track(MusicSourceId.Local).copy(
            title = "A very long track title that remains domain data",
            artists = listOf(Artist("One"), Artist("Two")),
        )
        assertEquals(2, track.artists.size)
    }

    private fun track(sourceId: MusicSourceId) = Track(
        id = TrackId("42"),
        sourceId = sourceId,
        title = "Maze",
        artists = listOf(Artist("AeMusic")),
        album = Album("Preview"),
        duration = TrackDuration.ofMilliseconds(180_000),
        artwork = ArtworkRef.Missing,
        playbackRef = PlaybackReference.Local("content://track/42"),
    )
}
