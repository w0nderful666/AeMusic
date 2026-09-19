package com.aemusic.feature.library

import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.MusicSourceId
import com.aemusic.core.model.PlaybackReference
import com.aemusic.core.model.Track
import com.aemusic.core.model.TrackDuration
import com.aemusic.core.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryIndexTest {
    @Test fun latinTitles_useUppercaseSection() { assertEquals('G', sectionFor(track("gee"))) }
    @Test fun nonLatinAndSymbols_useFallbackSection() { assertEquals('#', sectionFor(track("少女时代"))); assertEquals('#', sectionFor(track("2026"))) }

    private fun track(title: String) = Track(TrackId(title), MusicSourceId.Local, title, emptyList(), null, TrackDuration.Zero, ArtworkRef.Missing, PlaybackReference.Local("content://$title"))
}
