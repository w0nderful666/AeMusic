package com.aemusic.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamRequestHeadersTest {
    @Test fun headersAreIsolatedByStableMediaKey() {
        StreamRequestHeaders.put("bilibili:BV-one", mapOf("Referer" to "https://www.bilibili.com/video/BV-one/"))
        StreamRequestHeaders.put("bilibili:BV-two", mapOf("Referer" to "https://www.bilibili.com/video/BV-two/"))

        assertEquals("https://www.bilibili.com/video/BV-one/", StreamRequestHeaders.get("bilibili:BV-one")["Referer"])
        assertEquals("https://www.bilibili.com/video/BV-two/", StreamRequestHeaders.get("bilibili:BV-two")["Referer"])
        assertTrue(StreamRequestHeaders.get("missing").isEmpty())
    }

    @Test fun headersCanBeResolvedByUri() {
        val streamUrl = "https://xy123x.mcdn.bilivideo.cn:8082/v1/resource/audio.m4s?token=xyz"
        StreamRequestHeaders.put(streamUrl, mapOf("Referer" to "https://www.bilibili.com/video/BV12345"))

        assertEquals("https://www.bilibili.com/video/BV12345", StreamRequestHeaders.getForUrl(streamUrl)["Referer"])
    }

    @Test fun hostFallbackAppliesForBilibiliCdnWhenNotRegistered() {
        val unknownBiliUrl = "https://upos-sz-mirrorcos.bilivideo.com/upgcxcode/12/34/audio.m4s"
        val headers = StreamRequestHeaders.getForUrl(unknownBiliUrl)

        assertEquals("https://www.bilibili.com/", headers["Referer"])
        assertTrue(headers["User-Agent"]?.contains("Chrome") == true)
    }
}
