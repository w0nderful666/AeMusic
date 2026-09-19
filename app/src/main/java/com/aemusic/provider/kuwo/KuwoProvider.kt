package com.aemusic.provider.kuwo

import com.aemusic.core.model.Album
import com.aemusic.core.model.Artist
import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.MusicSourceId
import com.aemusic.core.model.PlaybackReference
import com.aemusic.core.model.Track
import com.aemusic.core.model.TrackDuration
import com.aemusic.core.model.TrackId
import com.aemusic.core.network.AeNetworkClient
import com.aemusic.provider.ProviderCapabilities
import com.aemusic.provider.ProviderDescriptor
import com.aemusic.provider.ProviderFailure
import com.aemusic.provider.ProviderResult
import com.aemusic.provider.ResolvedStream
import com.aemusic.provider.SearchProvider
import com.aemusic.provider.StreamProvider
import com.aemusic.provider.StreamQuality
import com.aemusic.provider.arr
import com.aemusic.provider.long
import com.aemusic.provider.obj
import com.aemusic.provider.recognizeTrackIdentity
import com.aemusic.provider.text
import kotlinx.coroutines.CancellationException
import java.net.URLEncoder

class KuwoProvider(private val http: AeNetworkClient) : SearchProvider, StreamProvider {
    override val descriptor = ProviderDescriptor(SOURCE, "酷我音乐", ProviderCapabilities(true, true, false))

    override suspend fun search(query: String, limit: Int): ProviderResult<List<Track>> = guarded {
        if (query.isBlank()) return@guarded ProviderResult.Success(emptyList())
        val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val raw = http.getText(
            "http://search.kuwo.cn/r.s?all=$encoded&ft=music&itemset=web_2013&client=kt&pn=0&rn=${limit.coerceIn(1, 30)}&rformat=json&encoding=utf8",
            DESKTOP_HEADERS,
        ).replace('’', '\'').replace("'", "\"")
        val root = http.json.parseToJsonElement(raw).obj()
        val tracks = root?.arr("abslist").orEmpty().mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val rid = item.text("MUSICRID").removePrefix("MUSIC_").trim().takeIf(String::isNotBlank) ?: return@mapNotNull null
            val rawTitle = item.text("SONGNAME").replace("&nbsp;", " ").ifBlank { return@mapNotNull null }
            val rawArtist = item.text("ARTIST").replace("&nbsp;", " ")
            val identity = recognizeTrackIdentity(rawTitle, rawArtist)
            val album = item.text("ALBUM").replace("&nbsp;", " ").takeIf(String::isNotBlank)
            val artwork = cover(item.text("web_albumpic_short"), item.text("web_artistpic_short"), rid)
            Track(
                TrackId(rid), SOURCE, identity.title.ifBlank { rawTitle },
                listOfNotNull(identity.artist.takeIf(String::isNotBlank)?.let(::Artist)),
                album?.let(::Album), TrackDuration.ofMilliseconds(item.long("DURATION") * 1_000),
                artwork?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing,
                PlaybackReference.Provider(SOURCE, rid),
            )
        }
        ProviderResult.Success(tracks)
    }

    override suspend fun resolve(reference: PlaybackReference.Provider, quality: StreamQuality): ProviderResult<ResolvedStream> = guarded {
        val rid = reference.mediaId.removePrefix("MUSIC_").trim()
        val raw = http.getText(
            "http://antiserver.kuwo.cn/anti.s?type=convert_url3&rid=${URLEncoder.encode(rid, Charsets.UTF_8.name())}&format=mp3",
            DESKTOP_HEADERS + ("Referer" to "https://kuwo.cn/"),
        )
        val streamUrl = runCatching { http.json.parseToJsonElement(raw).obj()?.text("url") }.getOrNull()
            ?.takeIf(String::isNotBlank) ?: raw.trim().takeIf { it.startsWith("http") }
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("Kuwo did not return a playable URL"))
        val headers = DESKTOP_HEADERS + ("Referer" to "https://kuwo.cn/") + ("Accept-Encoding" to "identity")
        val probe = try {
            http.probeMedia(streamUrl, headers)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val finalUri = if (probe?.successful == true && isLikelyMedia(probe.contentType, probe.finalUrl)) probe.finalUrl else streamUrl
        ProviderResult.Success(
            ResolvedStream(
                uri = finalUri,
                headers = headers,
                expiresAtEpochMs = System.currentTimeMillis() + 60 * 60_000,
                bitrate = if (quality == StreamQuality.Standard) 128_000 else 320_000,
                mimeType = probe?.contentType ?: "audio/mpeg",
                codec = "mp3",
                qualityLabel = if (quality == StreamQuality.Standard) "standard" else "320k HQ",
            ),
        )
    }

    private fun cover(album: String, artist: String, rid: String): String? = when {
        album.isNotBlank() -> "https://img1.kuwo.cn/star/albumcover/300/${album.removePrefix("120/").removePrefix("70/")}"
        artist.isNotBlank() -> "https://img1.kuwo.cn/star/starheads/300/${artist.removePrefix("120/").removePrefix("70/")}"
        rid.isNotBlank() -> "http://artistpicserver.kuwo.cn/pic.web?corp=kuwo&type=rid_pic&pictype=url&size=300&rid=$rid"
        else -> null
    }

    companion object {
        val SOURCE = MusicSourceId("kuwo")
        val DESKTOP_HEADERS = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    }
}

private fun isLikelyMedia(type: String?, url: String): Boolean {
    val normalized = type.orEmpty().lowercase()
    if (normalized.startsWith("audio/") || normalized.contains("octet-stream")) return true
    if (normalized.contains("html") || normalized.contains("json") || normalized.startsWith("text/")) return false
    return url.substringBefore('?').lowercase().endsWith(".mp3")
}

private suspend fun <T> guarded(block: suspend () -> ProviderResult<T>): ProviderResult<T> = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    ProviderResult.Failure(ProviderFailure.Network(error.message))
}
