package com.aemusic.provider

import com.aemusic.provider.lyrics.parseTimedLyrics
import com.aemusic.provider.lyrics.lyricsMatchScore
import com.aemusic.core.model.*
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsParserTest {
    @Test
    fun parsesFractionPrecisionAndSortsLines() {
        val result = parseTimedLyrics("[01:02.3]Later\n[00:01.25]First\n[00:03.125]Third")

        assertEquals(listOf(1_250L, 3_125L, 62_300L), result.map { it.timeMs })
        assertEquals(listOf("First", "Third", "Later"), result.map { it.text })
    }

    @Test
    fun supportsMultipleTimestampsAndIgnoresMetadata() {
        val result = parseTimedLyrics("[ar:Artist]\n[00:04.00][00:08.00]Chorus")

        assertEquals(listOf(4_000L, 8_000L), result.map { it.timeMs })
        assertEquals(listOf("Chorus", "Chorus"), result.map { it.text })
    }

    @Test
    fun exactMetadataAndDurationOutrankWrongVersion() {
        val track = sampleTrack("Night Drive", 180_000)
        val exact = lyricsMatchScore(track, "Night Drive (Official Audio)", listOf("Ae Artist"), "Ae Album", 181_000)
        val live = lyricsMatchScore(track, "Night Drive Live", listOf("Ae Artist"), "Ae Album", 240_000)

        assertTrue(exact >= 85)
        assertTrue(exact > live)
    }

    @Test
    fun titleOnlyCandidateCannotBecomeAutomaticMatch() {
        val track = sampleTrack("Same Name", 200_000)
        val score = lyricsMatchScore(track, "Same Name", listOf("Another Artist"), null, null)

        assertTrue(score < 85)
    }

    private fun sampleTrack(title: String, durationMs: Long) = Track(
        TrackId("track"), MusicSourceId("test"), title, listOf(Artist("Ae Artist")), Album("Ae Album"),
        TrackDuration.ofMilliseconds(durationMs), ArtworkRef.Missing,
        PlaybackReference.Provider(MusicSourceId("test"), "track"),
    )
}
