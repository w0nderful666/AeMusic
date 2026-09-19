package com.aemusic.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackIdentityTest {
    @Test fun recognizesBookMarkedBilibiliTitle() {
        val result = recognizeTrackIdentity("【Hi-Res】周杰伦《晴天》完整版", "上传者")
        assertEquals("晴天", result.title)
        assertEquals("周杰伦", result.artist)
        assertTrue(result.changed)
    }

    @Test fun stripsTrackNumberAndVersionNoise() {
        val result = recognizeTrackIdentity("01 - 夜曲 - 周杰伦【无损】")
        assertEquals("夜曲", result.title)
        assertEquals("周杰伦", result.artist)
    }
}
