package com.aemusic.feature.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerOverlayPolicyTest {
    @Test fun predictiveProgressIsClamped() {
        assertEquals(0f, predictivePlayerProgress(-1f))
        assertEquals(.5f, predictivePlayerProgress(.5f))
        assertEquals(1f, predictivePlayerProgress(2f))
    }

    @Test fun invalidPredictiveProgressReturnsToExpandedState() {
        assertEquals(0f, predictivePlayerProgress(Float.NaN))
        assertEquals(0f, predictivePlayerProgress(Float.POSITIVE_INFINITY))
    }
}
