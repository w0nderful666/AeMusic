package com.aemusic.provider.lyrics

import android.content.Context
import androidx.core.content.edit
import com.aemusic.core.database.LibraryDataRepository
import com.aemusic.core.database.LyricBindingEntity
import com.aemusic.core.model.Track
import com.aemusic.core.network.AeNetworkClient
import com.aemusic.provider.netease.NeteaseProvider
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.kuwo.KuwoProvider
import com.aemusic.provider.obj
import com.aemusic.provider.arr
import com.aemusic.provider.long
import com.aemusic.provider.text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URLEncoder
import java.util.Base64

enum class LyricsSource { Auto, Netease, BilibiliCc, Lrclib, Kugou, Kuwo }
data class LyricsCandidate(
    val id: String,
    val title: String,
    val artist: String,
    val text: String,
    val source: LyricsSource,
    val album: String? = null,
    val durationMs: Long? = null,
    val matchScore: Int = 0,
)
data class TimedLyricLine(val timeMs: Long, val text: String)

class LyricsRepository(
    context: Context,
    private val http: AeNetworkClient,
    private val netease: NeteaseProvider,
    private val bilibili: BilibiliProvider,
    private val kuwo: KuwoProvider,
    private val library: LibraryDataRepository,
) {
    private val preferences = context.getSharedPreferences("lyrics_preferences", Context.MODE_PRIVATE)
    var preferredSource: LyricsSource
        get() = runCatching { LyricsSource.valueOf(preferences.getString("source", LyricsSource.Auto.name).orEmpty()) }.getOrDefault(LyricsSource.Auto)
        set(value) = preferences.edit { putString("source", value.name) }

    suspend fun saved(track: Track): LyricsCandidate? = library.lyricBinding(track)?.let { row ->
        LyricsCandidate(row.candidateId, row.matchedTitle, row.matchedArtist, row.lyrics, runCatching { LyricsSource.valueOf(row.provider) }.getOrDefault(LyricsSource.Auto))
    }

    suspend fun select(track: Track, candidate: LyricsCandidate) {
        library.saveLyricBinding(
            track,
            LyricBindingEntity(track.sourceId.value, track.id.value, candidate.source.name, candidate.id, candidate.title, candidate.artist, candidate.text.take(MAX_LYRICS_CHARS), System.currentTimeMillis()),
        )
    }

    suspend fun search(track: Track, query: String, source: LyricsSource): List<LyricsCandidate> {
        val term = query.trim().ifBlank { track.title }
        val artist = track.artists.firstOrNull()?.name.orEmpty()
        val sources = if (source == LyricsSource.Auto) {
            buildList {
                when (track.sourceId) {
                    NeteaseProvider.SOURCE -> add(LyricsSource.Netease)
                    BilibiliProvider.SOURCE -> add(LyricsSource.BilibiliCc)
                    KuwoProvider.SOURCE -> add(LyricsSource.Kuwo)
                }
                add(LyricsSource.Lrclib)
                add(LyricsSource.Kugou)
                if (track.sourceId != KuwoProvider.SOURCE) add(LyricsSource.Kuwo)
            }
        } else listOf(source)
        return sources.flatMap { selected ->
            try {
                when (selected) {
                    LyricsSource.Lrclib -> searchLrclib(track, term, artist)
                    LyricsSource.Netease -> searchNetease(track, term)
                    LyricsSource.BilibiliCc -> searchBilibiliCc(track)
                    LyricsSource.Kugou -> searchKugou(track, term, artist)
                    LyricsSource.Kuwo -> searchKuwo(track, term, artist)
                    LyricsSource.Auto -> emptyList()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }.distinctBy { it.text }.sortedByDescending(LyricsCandidate::matchScore).take(MAX_RESULTS)
    }

    private suspend fun searchLrclib(track: Track, title: String, artist: String): List<LyricsCandidate> {
        val exact = if (artist.isNotBlank() && track.duration.milliseconds > 0) lyricsNetworkAttempt {
            val exactUrl = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .apply {
                    track.album?.title?.let { addQueryParameter("album_name", it) }
                    addQueryParameter("duration", (track.duration.milliseconds / 1_000).toString())
                }.build().toString()
            parseLrclib(http.getJson(exactUrl, LRCLIB_HEADERS) as? JsonObject, track)
        } else null
        if (exact != null && exact.matchScore >= AUTO_MATCH_SCORE) return listOf(exact)
        val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .apply { if (artist.isNotBlank()) addQueryParameter("artist_name", artist) }
            .build().toString()
        val response = http.getJson(url, LRCLIB_HEADERS)
        return withContext(Dispatchers.Default) {
            (listOfNotNull(exact) + response.jsonArray.asSequence().take(20).mapNotNull { parseLrclib(it as? JsonObject, track) })
                .distinctBy(LyricsCandidate::id)
                .filter { it.matchScore >= MANUAL_MATCH_SCORE }
                .sortedByDescending(LyricsCandidate::matchScore)
                .take(MAX_RESULTS)
        }
    }

    private suspend fun searchNetease(track: Track, query: String): List<LyricsCandidate> {
        val tracks = if (track.sourceId == NeteaseProvider.SOURCE && query.equals(track.title, true)) listOf(track) else netease.searchTracks(query, MAX_RESULTS)
        return tracks.mapNotNull { match ->
            netease.lyrics(match.id.value)?.takeIf(String::isNotBlank)?.let {
                LyricsCandidate(
                    match.id.value,
                    match.title,
                    match.artists.joinToString { artist -> artist.name },
                    it.take(MAX_LYRICS_CHARS),
                    LyricsSource.Netease,
                    album = match.album?.title,
                    durationMs = match.duration.milliseconds,
                    matchScore = lyricsMatchScore(track, match.title, match.artists.map { artist -> artist.name }, match.album?.title, match.duration.milliseconds),
                )
            }
        }
    }

    private suspend fun searchBilibiliCc(track: Track): List<LyricsCandidate> {
        val reference = track.playbackRef as? com.aemusic.core.model.PlaybackReference.Provider ?: return emptyList()
        if (reference.sourceId != BilibiliProvider.SOURCE) return emptyList()
        val collection = (bilibili.collection(reference.mediaId) as? com.aemusic.provider.ProviderResult.Success)?.value ?: return emptyList()
        val cid = reference.partId ?: collection.parts.firstOrNull()?.cid ?: return emptyList()
        val root = http.getJson(
            "https://api.bilibili.com/x/player/v2?bvid=${encode(reference.mediaId)}&cid=${encode(cid)}",
            mapOf("User-Agent" to DESKTOP_UA, "Referer" to "https://www.bilibili.com/video/${reference.mediaId}"),
        ).obj()
        val subtitle = root?.obj("data")?.obj("subtitle")?.arr("subtitles")?.firstOrNull()?.obj() ?: return emptyList()
        val url = subtitle.text("subtitle_url").let { if (it.startsWith("//")) "https:$it" else it }
        val body = http.getJson(url, mapOf("User-Agent" to DESKTOP_UA)).obj()?.arr("body").orEmpty()
        val lines = body.mapNotNull { item ->
            val row = item.obj() ?: return@mapNotNull null
            val seconds = (row["from"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val text = row.text("content").takeIf(String::isNotBlank) ?: return@mapNotNull null
            "${lrcTimestamp((seconds * 1_000).toLong())}$text"
        }
        if (lines.isEmpty()) return emptyList()
        return listOf(LyricsCandidate("bilibili:${reference.mediaId}:$cid", track.title, track.artists.joinToString { it.name }, lines.joinToString("\n"), LyricsSource.BilibiliCc, track.album?.title, track.duration.milliseconds, 100))
    }

    private suspend fun searchKugou(track: Track, title: String, artist: String): List<LyricsCandidate> {
        val keyword = listOf(title, artist).filter(String::isNotBlank).joinToString(" ")
        val root = http.getJson(
            "http://krcs.kugou.com/search?ver=1&man=yes&client=mobi&keyword=${encode(keyword)}",
            mapOf("User-Agent" to DESKTOP_UA),
        ).obj()
        return root?.arr("candidates").orEmpty().take(5).mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val id = item.text("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val accessKey = item.text("accesskey").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val download = http.getJson(
                "http://lyrics.kugou.com/download?ver=1&client=pc&id=${encode(id)}&accesskey=${encode(accessKey)}&fmt=lrc&charset=utf8",
                mapOf("User-Agent" to DESKTOP_UA),
            ).obj()
            val text = runCatching { Base64.getDecoder().decode(download?.text("content")).toString(Charsets.UTF_8) }.getOrNull()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val candidateTitle = item.text("song").ifBlank { title }
            val candidateArtist = item.text("singer")
            val duration = item.long("duration").takeIf { it > 0 }
            val durationMs = duration?.let { if (it < 10_000) it * 1_000 else it }
            LyricsCandidate("kugou:$id:$accessKey", candidateTitle, candidateArtist, text.take(MAX_LYRICS_CHARS), LyricsSource.Kugou, durationMs = durationMs, matchScore = lyricsMatchScore(track, candidateTitle, listOf(candidateArtist), null, durationMs))
        }
    }

    private suspend fun searchKuwo(track: Track, title: String, artist: String): List<LyricsCandidate> {
        val matches = when (val result = kuwo.search(listOf(title, artist).filter(String::isNotBlank).joinToString(" "), 3)) {
            is com.aemusic.provider.ProviderResult.Success -> result.value
            is com.aemusic.provider.ProviderResult.Failure -> emptyList()
        }
        return matches.mapNotNull { match ->
            val root = http.getJson("http://m.kuwo.cn/newh5/singles/songinfoandlrc?musicId=${encode(match.id.value)}", mapOf("User-Agent" to DESKTOP_UA)).obj()
            val rows = root?.obj("data")?.arr("lrclist").orEmpty()
            val text = rows.mapNotNull { element ->
                val row = element.obj() ?: return@mapNotNull null
                val seconds = (row["time"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
                row.text("lineLyric").takeIf(String::isNotBlank)?.let { "${lrcTimestamp((seconds * 1_000).toLong())}$it" }
            }.joinToString("\n").takeIf(String::isNotBlank) ?: return@mapNotNull null
            LyricsCandidate(match.id.value, match.title, match.artists.joinToString { it.name }, text.take(MAX_LYRICS_CHARS), LyricsSource.Kuwo, match.album?.title, match.duration.milliseconds, lyricsMatchScore(track, match.title, match.artists.map { it.name }, match.album?.title, match.duration.milliseconds))
        }
    }

    private fun parseLrclib(item: JsonObject?, track: Track): LyricsCandidate? {
        item ?: return null
        val lyrics = (item["syncedLyrics"] as? JsonPrimitive)?.contentOrNull
            ?: (item["plainLyrics"] as? JsonPrimitive)?.contentOrNull
        if (lyrics.isNullOrBlank()) return null
        val title = item.text("trackName")
        val artist = item.text("artistName")
        val album = item.text("albumName").takeIf(String::isNotBlank)
        val durationMs = (item["duration"] as? JsonPrimitive)?.doubleOrNull?.times(1_000)?.toLong()
        return LyricsCandidate(
            id = item["id"].toString(), title = title, artist = artist,
            text = lyrics.take(MAX_LYRICS_CHARS), source = LyricsSource.Lrclib,
            album = album, durationMs = durationMs,
            matchScore = lyricsMatchScore(track, title, listOf(artist), album, durationMs),
        )
    }

    private companion object {
        const val MAX_RESULTS = 5
        const val MAX_LYRICS_CHARS = 200_000
        const val AUTO_MATCH_SCORE = 85
        const val MANUAL_MATCH_SCORE = 45
        val LRCLIB_HEADERS = mapOf("User-Agent" to "AeMusic/0.1 (lyrics search; local application)")
        const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
}

private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())
private fun lrcTimestamp(milliseconds: Long): String = "[%02d:%02d.%03d]".format(milliseconds / 60_000, milliseconds / 1_000 % 60, milliseconds % 1_000)

private val timeTag = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

fun parseTimedLyrics(value: String): List<TimedLyricLine> = value.lineSequence().flatMap { rawLine ->
    val matches = timeTag.findAll(rawLine).toList()
    val text = rawLine.replace(timeTag, "").trim()
    if (matches.isEmpty() || text.isBlank()) emptySequence() else matches.asSequence().map { match ->
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fraction = match.groupValues[3]
        val millis = when (fraction.length) { 1 -> fraction.toLongOrNull()?.times(100); 2 -> fraction.toLongOrNull()?.times(10); 3 -> fraction.toLongOrNull(); else -> 0L } ?: 0L
        TimedLyricLine((minutes * 60 + seconds) * 1_000 + millis, text)
    }
}.distinctBy { it.timeMs to it.text }.sortedBy(TimedLyricLine::timeMs).toList()

internal fun lyricsMatchScore(
    track: Track,
    candidateTitle: String,
    candidateArtists: List<String>,
    candidateAlbum: String?,
    candidateDurationMs: Long?,
): Int {
    val expectedTitle = normalizedLyricsText(track.title)
    val actualTitle = normalizedLyricsText(candidateTitle)
    if (expectedTitle.isBlank() || actualTitle.isBlank()) return 0
    var score = when {
        expectedTitle == actualTitle -> 45
        expectedTitle.contains(actualTitle) || actualTitle.contains(expectedTitle) -> 32
        else -> (tokenSimilarity(expectedTitle, actualTitle) * 40).toInt()
    }
    val expectedVersions = versionTokens(track.title)
    val actualVersions = versionTokens(candidateTitle)
    if (expectedVersions != actualVersions && (expectedVersions.isNotEmpty() || actualVersions.isNotEmpty())) score -= 30
    val expectedArtists = track.artists.map { normalizedLyricsText(it.name) }.filter(String::isNotBlank)
    val actualArtists = candidateArtists.map(::normalizedLyricsText).filter(String::isNotBlank)
    if (expectedArtists.any { expected -> actualArtists.any { actual -> expected == actual || expected.contains(actual) || actual.contains(expected) } }) score += 25
    val expectedAlbum = track.album?.title?.let(::normalizedLyricsText).orEmpty()
    val actualAlbum = candidateAlbum?.let(::normalizedLyricsText).orEmpty()
    if (expectedAlbum.isNotBlank() && actualAlbum.isNotBlank() && expectedAlbum == actualAlbum) score += 10
    val expectedDuration = track.duration.milliseconds
    if (expectedDuration > 0 && candidateDurationMs != null && candidateDurationMs > 0) {
        score += when (kotlin.math.abs(expectedDuration - candidateDurationMs)) {
            in 0..2_000 -> 25
            in 2_001..5_000 -> 12
            in 5_001..10_000 -> 4
            else -> -20
        }
    }
    return score.coerceIn(0, 100)
}

private fun normalizedLyricsText(value: String): String = value.lowercase()
    .replace(Regex("\\b(official|audio|video|lyrics?|mv|hd|4k)\\b"), " ")
    .replace(Regex("(官方|完整版|动态歌词|音乐视频)"), " ")
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .trim().replace(Regex("\\s+"), " ")

private fun tokenSimilarity(first: String, second: String): Float {
    val left = first.split(' ').filter(String::isNotBlank).toSet()
    val right = second.split(' ').filter(String::isNotBlank).toSet()
    if (left.isEmpty() || right.isEmpty()) return 0f
    return left.intersect(right).size.toFloat() / left.union(right).size
}

private fun versionTokens(value: String): Set<String> {
    val normalized = value.lowercase()
    return mapOf(
        "live" to listOf("live", "现场"),
        "remix" to listOf("remix", "混音"),
        "instrumental" to listOf("instrumental", "伴奏", "纯音乐"),
        "cover" to listOf("cover", "翻唱"),
        "acoustic" to listOf("acoustic", "不插电"),
        "speed" to listOf("sped up", "slowed", "加速版", "慢速版"),
    ).filterValues { markers -> markers.any(normalized::contains) }.keys
}

private suspend fun <T> lyricsNetworkAttempt(block: suspend () -> T): T? = try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
