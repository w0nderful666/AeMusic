package com.aemusic.provider

import com.aemusic.core.model.*
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.bilibili.parseAudioCandidates
import com.aemusic.provider.netease.NeteaseProvider
import com.aemusic.provider.netease.isLikelyAudioProbe
import com.aemusic.provider.netease.isNeteasePreview
import com.aemusic.playback.playbackCacheKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

class ProviderPolicyTest {
    @Test fun bilibiliFavoritePageSizeRespectsUpstreamLimit() {
        assertEquals(1, com.aemusic.provider.bilibili.favoritePageSize(0))
        assertEquals(20, com.aemusic.provider.bilibili.favoritePageSize(30))
        assertEquals(12, com.aemusic.provider.bilibili.favoritePageSize(12))
    }
    @Test fun bilibiliCdnOrder_prioritizesOfficialAndDeprioritizesPcdn() {
        val ordered = BilibiliProvider.sortCdnCandidates(listOf(
            "https://x.mcdn.bilivideo.cn:8082/audio.m4s",
            "https://edge.mountaintoys.cn/audio.m4s",
            "https://cn-hbwh-01.bilivideo.com/audio.m4s",
        ))
        assertEquals("https://cn-hbwh-01.bilivideo.com/audio.m4s", ordered.first())
        assertTrue(ordered.last().contains("mcdn"))
    }

    @Test fun neteaseLogin_requiresDurableSessionCookie() {
        assertTrue(NeteaseProvider.hasLoginCookie("foo=1; MUSIC_U=token"))
        assertFalse(NeteaseProvider.hasLoginCookie("MUSIC_A=anonymous"))
        assertTrue(NeteaseProvider.hasAnonymousCookie("foo=1; MUSIC_A=anonymous"))
        assertFalse(NeteaseProvider.hasLoginCookie("__csrf=token"))
    }

    @Test fun resolver_routesStableReferenceToMatchingProvider() = runBlocking {
        val source = MusicSourceId("fixture")
        val provider = object : StreamProvider {
            override val descriptor = ProviderDescriptor(source, "Fixture", ProviderCapabilities(false, true, false))
            override suspend fun resolve(reference: PlaybackReference.Provider, quality: StreamQuality) = ProviderResult.Success(ResolvedStream("https://example.test/${reference.mediaId}"))
        }
        val result = PlaybackResolver(ProviderRegistry(listOf(provider))).resolve(PlaybackReference.Provider(source, "42"))
        assertEquals("https://example.test/42", (result as ProviderResult.Success).value.uri)
    }

    @Test fun resolver_doesNotNeedProviderForLocalContent() = runBlocking {
        val result = PlaybackResolver(ProviderRegistry(emptyList())).resolve(PlaybackReference.Local("content://audio/42"))
        assertEquals("content://audio/42", (result as ProviderResult.Success).value.uri)
    }

    @Test fun bilibiliCandidates_keepEveryCdnAndPreferCompatibleAudio() {
        val dash = buildJsonObject {
            put("audio", buildJsonArray {
                add(buildJsonObject {
                    put("baseUrl", "https://primary.test/audio.m4s")
                    put("backupUrl", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("https://backup.test/audio.m4s")) })
                    put("bandwidth", 192_000)
                    put("mimeType", "audio/mp4")
                    put("codecs", "mp4a.40.2")
                })
                add(buildJsonObject {
                    put("baseUrl", "https://opus.test/audio.m4s")
                    put("bandwidth", 256_000)
                    put("mimeType", "audio/webm")
                    put("codecs", "opus")
                })
            })
        }
        val candidates = parseAudioCandidates(dash)
        assertEquals(2, candidates.size)
        assertEquals(2, candidates.first().urls.size)
        assertEquals("mp4a.40.2", orderStreamCandidates(candidates, StreamQuality.High).first().codec)
    }

    @Test fun neteaseProbe_rejectsWebPagesAndAcceptsAudio() {
        assertTrue(isLikelyAudioProbe("audio/mpeg", "https://cdn.test/value"))
        assertTrue(isLikelyAudioProbe(null, "https://cdn.test/value.mp3?token=x"))
        assertFalse(isLikelyAudioProbe("text/html", "https://music.163.com/song/1"))
        assertFalse(isLikelyAudioProbe("application/json", "https://music.163.com/api/1"))
    }

    @Test fun jsonAccess_toleratesObjectWherePrimitiveWasExpected() {
        val response = buildJsonObject {
            put("freeTrialPrivilege", buildJsonObject { put("resConsumable", true) })
        }
        assertEquals(0L, response.long("freeTrialPrivilege"))
        assertEquals("", response.text("freeTrialPrivilege"))
        assertFalse(response.bool("freeTrialPrivilege"))
        assertFalse(isNeteasePreview(response))
    }

    @Test fun playbackCacheKey_separatesDifferentDashRepresentations() {
        val track = Track(
            TrackId("BV-test"), MusicSourceId("bilibili"), "Fixture", emptyList(), null,
            TrackDuration.ofMilliseconds(60_000), ArtworkRef.Missing,
            PlaybackReference.Provider(MusicSourceId("bilibili"), "BV-test"),
        )
        val aac = ResolvedStream("https://cdn.test/a", bitrate = 192_000, mimeType = "audio/mp4", codec = "mp4a.40.2")
        val opus = ResolvedStream("https://cdn.test/b", bitrate = 192_000, mimeType = "audio/webm", codec = "opus")
        assertNotEquals(playbackCacheKey(track, aac), playbackCacheKey(track, opus))
        assertEquals(playbackCacheKey(track, aac), playbackCacheKey(track, aac.copy(uri = "https://backup.test/a")))
    }

    @Test fun bilibili_effectiveCookie_preservesAccountCookieAndEnsuresBuvid3() {
        val http = com.aemusic.core.network.AeNetworkClient()
        var savedAnon: String? = null
        val provider = BilibiliProvider(
            http = http,
            accountCookie = { "SESSDATA=user_token; bili_jct=csrf_token" },
            anonymousCookie = { "buvid3=test_buvid3_infoc; b_nut=123" },
            saveAnonymousCookie = { savedAnon = it },
        )
        val cookie = provider.effectiveCookie()
        assertTrue(cookie.contains("SESSDATA=user_token"))
        assertTrue(cookie.contains("bili_jct=csrf_token"))
        assertTrue(cookie.contains("buvid3=test_buvid3_infoc"))
        assertTrue(cookie.contains("CURRENT_FNVAL=4048"))
    }

    @Test fun bilibili_effectiveCookie_doesNotDuplicateBuvid3IfAlreadyPresent() {
        val http = com.aemusic.core.network.AeNetworkClient()
        val provider = BilibiliProvider(
            http = http,
            accountCookie = { "SESSDATA=token; buvid3=existing_buvid3; CURRENT_FNVAL=4048" },
            anonymousCookie = { "buvid3=other" },
            saveAnonymousCookie = { },
        )
        val cookie = provider.effectiveCookie()
        assertEquals("SESSDATA=token; buvid3=existing_buvid3; CURRENT_FNVAL=4048", cookie)
    }

    @Test fun trackedPlaylist_updateMetadata_preservesIdAndUpdatesFields() {
        val original = com.aemusic.core.data.TrackedPlaylist(
            id = "12345",
            source = "bilibili",
            title = "Old Title",
            coverUrl = null,
            trackCount = 0,
        )
        val updated = original.copy(
            coverUrl = "https://example.test/cover.jpg",
            trackCount = 10,
        )
        assertEquals("12345", updated.id)
        assertEquals("https://example.test/cover.jpg", updated.coverUrl)
        assertEquals(10, updated.trackCount)
    }
}
