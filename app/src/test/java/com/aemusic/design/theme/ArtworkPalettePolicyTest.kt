package com.aemusic.design.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPalettePolicyTest {
    @Test fun blackAndWhiteArtwork_usesNeutralPalette() { assertTrue(isNeutralArtwork(0.03f)) }
    @Test fun colorfulArtwork_keepsArtworkHue() { assertFalse(isNeutralArtwork(0.42f)) }
    @Test fun largeArtwork_isDecodedNearTargetSize() { assertEquals(8, artworkSampleSize(3200, 3200, 320)) }
}
