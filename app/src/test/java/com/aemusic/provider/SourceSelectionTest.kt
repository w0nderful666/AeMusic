package com.aemusic.provider

import com.aemusic.core.model.*
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSelectionTest {
    @Test fun exactArtistTitleAndDurationAreHighConfidence() {
        val expected = track("晴天", "周杰伦", 269_000)
        val candidate = track("晴天 Official Audio", "周杰伦", 270_000)
        assertTrue(sourceMatchScore(expected, candidate) >= SourceSelectionEngine.HIGH_CONFIDENCE_SCORE)
    }

    @Test fun liveCoverWithWrongDurationCannotAutoMatch() {
        val expected = track("晴天", "周杰伦", 269_000)
        val candidate = track("晴天 Live 翻唱", "其他歌手", 330_000)
        assertTrue(sourceMatchScore(expected, candidate) < SourceSelectionEngine.HIGH_CONFIDENCE_SCORE)
    }

    private fun track(title: String, artist: String, duration: Long) = Track(
        TrackId("$title:$artist:$duration"), MusicSourceId("fixture"), title, listOf(Artist(artist)), null,
        TrackDuration.ofMilliseconds(duration), ArtworkRef.Missing, PlaybackReference.Provider(MusicSourceId("fixture"), title),
    )
}
