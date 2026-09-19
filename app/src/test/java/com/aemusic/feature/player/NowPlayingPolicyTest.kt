package com.aemusic.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingPolicyTest {
    @Test fun seekFractionRejectsUnknownDurationAndInvalidInput() {
        assertNull(seekPositionFromFraction(.5f, 0))
        assertNull(seekPositionFromFraction(Float.NaN, 100_000))
    }

    @Test fun seekFractionClampsToMediaBounds() {
        assertEquals(0L, seekPositionFromFraction(-.2f, 100_000))
        assertEquals(50_000L, seekPositionFromFraction(.5f, 100_000))
        assertEquals(100_000L, seekPositionFromFraction(1.2f, 100_000))
    }
}
