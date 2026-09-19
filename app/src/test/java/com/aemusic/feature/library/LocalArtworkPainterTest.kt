package com.aemusic.feature.library

import com.aemusic.feature.common.sampleSize
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalArtworkPainterTest {
    @Test fun largeArtwork_usesPowerOfTwoDownsampling() {
        assertEquals(16, sampleSize(4096, 4096, 256))
    }

    @Test fun smallArtwork_isNotUpsampled() {
        assertEquals(1, sampleSize(128, 128, 256))
    }
}
