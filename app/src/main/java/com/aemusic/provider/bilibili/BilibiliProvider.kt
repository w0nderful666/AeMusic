package com.aemusic.provider.bilibili

import com.aemusic.core.diagnostics.DiagnosticEventStore
import com.aemusic.core.model.*
import com.aemusic.core.network.AeNetworkClient
import com.aemusic.provider.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.net.URLEncoder
import java.security.SecureRandom

data class BilibiliPart(val page: Int, val cid: String, val title: String, val artist: String, val durationMs: Long)
data class BilibiliCollection(val bvid: String, val title: String, val author: String, val artworkUrl: String?, val parts: List<BilibiliPart>)

class BilibiliProvider(
    private val http: AeNetworkClient,
    private val accountCookie: () -> String?,
    private val anonymousCookie: () -> String?,
    private val saveAnonymousCookie: (String) -> Unit,
    private val diagnostics: DiagnosticEventStore? = null,
) : SearchProvider, StreamProvider, LoginProvider {
    private val favoriteFoldersCache = mutableMapOf<Int, CachedValue<List<com.aemusic.provider.netease.OnlinePlaylist>>>()
    private val favoriteTracksCache = mutableMapOf<String, CachedValue<List<Track>>>()
    private val collectionCache = mutableMapOf<String, CachedValue<BilibiliCollection>>()
    @Volatile
    var candidateLimit: Int = DEFAULT_CANDIDATE_LIMIT
        set(value) { field = value.coerceIn(1, MAX_CANDIDATE_LIMIT) }

    override val descriptor = ProviderDescriptor(SOURCE, "哔哩哔哩", ProviderCapabilities(true, true, true))
    override val loginUrl = "https://passport.bilibili.com/login"
    override val cookieUrl = "https://www.bilibili.com/"

    override suspend fun validate(cookie: String): ProviderResult<String> = guarded {
        val data = http.getJson("https://api.bilibili.com/x/web-interface/nav", headers(cookie)).obj()?.obj("data")
        if (data?.bool("isLogin") != true) ProviderResult.Failure(ProviderFailure.Authentication("Expired account"))
        else ProviderResult.Success(data.text("uname").ifBlank { "B站用户" })
    }

    override suspend fun search(query: String, limit: Int): ProviderResult<List<Track>> = guarded {
        if (query.isBlank()) return@guarded ProviderResult.Success(emptyList())
        val encoded = encode(query.trim())
        val root = http.getJson(
            "https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword=$encoded&page=1&page_size=${limit.coerceIn(1, 20)}",
            headers(effectiveCookie()),
        ).obj()
        val code = root?.int("code") ?: -1
        if (code != 0) return@guarded if (code in setOf(-412, -352)) ProviderResult.Failure(ProviderFailure.RateLimited(root?.text("message")))
        else ProviderResult.Failure(ProviderFailure.Unavailable(root?.text("message")))
        val tracks = root?.obj("data")?.arr("result").orEmpty().mapNotNull(::parseTrack)
        diagnostics?.record("provider", "bilibili-search", SOURCE.value, outcome = "success", detail = "results=${tracks.size}")
        ProviderResult.Success(tracks)
    }

    suspend fun collection(bvid: String): ProviderResult<BilibiliCollection> = guarded {
        collectionCache[bvid]?.takeIf { it.isFresh() }?.let { return@guarded ProviderResult.Success(it.value) }
        val data = http.getJson("https://api.bilibili.com/x/web-interface/view?bvid=${encode(bvid)}", videoHeaders(bvid)).obj()?.obj("data")
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("Video not found"))
        val author = data.obj("owner")?.text("name").orEmpty()
        val parts = data.arr("pages").orEmpty().mapNotNull { element ->
            val item = element.obj() ?: return@mapNotNull null
            val cid = item.long("cid").takeIf { it > 0 }?.toString() ?: return@mapNotNull null
            val rawTitle = item.text("part").ifBlank { "P${item.int("page")}" }
            val identity = recognizeTrackIdentity(rawTitle, author)
            BilibiliPart(item.int("page"), cid, rawTitle, author.ifBlank { identity.artist }, item.long("duration") * 1_000)
        }
        val coll = BilibiliCollection(bvid, data.text("title"), author, secureUrl(data.text("pic")), parts)
        collectionCache[bvid] = CachedValue(coll)
        ProviderResult.Success(coll)
    }

    suspend fun userFavorites(forceRefresh: Boolean = false): ProviderResult<List<com.aemusic.provider.netease.OnlinePlaylist>> = guarded {
        val cookie = accountCookie()?.takeIf(String::isNotBlank)
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Authentication("B站未登录"))
        val accountKey = cookie.hashCode()
        favoriteFoldersCache[accountKey]
            ?.takeIf { !forceRefresh && it.isFresh() }
            ?.let { return@guarded ProviderResult.Success(it.value) }
        val nav = http.getJson("https://api.bilibili.com/x/web-interface/nav", headers(cookie)).obj()?.obj("data")
        val mid = nav?.long("mid") ?: 0L
        if (mid <= 0) return@guarded ProviderResult.Failure(ProviderFailure.Authentication("未获取到B站用户ID"))
        val root = http.getJson(
            "https://api.bilibili.com/x/v3/fav/folder/created/list-all?up_mid=$mid&platform=web",
            headers(cookie),
        ).obj()
        val code = root?.int("code") ?: -1
        if (code != 0) {
            diagnostics?.record("provider", "bilibili-favorites", SOURCE.value, outcome = "failure", detail = "code=$code; ${root?.text("message")}")
            return@guarded when (code) {
                -101, -111 -> ProviderResult.Failure(ProviderFailure.Authentication(root?.text("message")))
                -412, -352 -> ProviderResult.Failure(ProviderFailure.RateLimited(root?.text("message")))
                else -> ProviderResult.Failure(ProviderFailure.Unavailable(root?.text("message")))
            }
        }
        val list = root?.obj("data")?.arr("list").orEmpty().mapNotNull { el ->
            val obj = el.obj() ?: return@mapNotNull null
            val id = obj.long("id").takeIf { it > 0 }?.toString() ?: return@mapNotNull null
            val title = obj.text("title").ifBlank { "B站收藏夹" }
            val count = obj.int("media_count")
            com.aemusic.provider.netease.OnlinePlaylist(
                id = id,
                name = title,
                coverUrl = secureUrl(obj.text("cover")).orEmpty(),
                playCount = 0L,
                trackCount = count,
                creator = nav?.text("uname")?.ifBlank { "我" } ?: "我",
                description = "B站个人收藏夹",
            )
        }
        diagnostics?.record("provider", "bilibili-favorites", SOURCE.value, outcome = "success", detail = "folders=${list.size}")
        favoriteFoldersCache[accountKey] = CachedValue(list)
        ProviderResult.Success(list)
    }

    suspend fun favoriteFolderTracks(
        mediaId: String,
        page: Int = 1,
        pageSize: Int = 20,
        forceRefresh: Boolean = false,
    ): ProviderResult<List<Track>> = guarded {
        val cookie = effectiveCookie()
        val cacheKey = "${cookie.hashCode()}:$mediaId:$page:$pageSize"
        favoriteTracksCache[cacheKey]
            ?.takeIf { !forceRefresh && it.isFresh() }
            ?.let { return@guarded ProviderResult.Success(it.value) }
        val root = http.getJson(
            "https://api.bilibili.com/x/v3/fav/resource/list?media_id=$mediaId&pn=${page.coerceAtLeast(1)}&ps=${favoritePageSize(pageSize)}&order=mtime&platform=web",
            headers(cookie),
        ).obj()
        val code = root?.int("code") ?: -1
        if (code != 0) {
            diagnostics?.record("provider", "bilibili-favorite-tracks", SOURCE.value, mediaId, "failure", "code=$code; ${root?.text("message")}")
            return@guarded when (code) {
                -101, -111, -403 -> ProviderResult.Failure(ProviderFailure.Authentication(root?.text("message")))
                -412, -352 -> ProviderResult.Failure(ProviderFailure.RateLimited(root?.text("message")))
                else -> ProviderResult.Failure(ProviderFailure.Unavailable(root?.text("message")))
            }
        }
        val medias = root?.obj("data")?.arr("medias").orEmpty()
        val tracks = medias.mapNotNull { el ->
            val item = el.obj() ?: return@mapNotNull null
            val bvid = item.text("bvid").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val rawTitle = decodeHtml(item.text("title")).ifBlank { return@mapNotNull null }
            val author = item.obj("upper")?.text("name").orEmpty()
            val cid = item.obj("ugc")?.long("first_cid")?.takeIf { it > 0 }?.toString()
            Track(
                TrackId(bvid), SOURCE, rawTitle,
                listOfNotNull(author.takeIf(String::isNotBlank)?.let(::Artist)),
                Album("B站收藏夹"),
                TrackDuration.ofMilliseconds(item.long("duration") * 1_000),
                secureUrl(item.text("cover"))?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing,
                PlaybackReference.Provider(SOURCE, bvid, cid),
            )
        }
        diagnostics?.record("provider", "bilibili-favorite-tracks", SOURCE.value, mediaId, "success", "page=$page; tracks=${tracks.size}")
        favoriteTracksCache[cacheKey] = CachedValue(tracks)
        ProviderResult.Success(tracks)
    }

    suspend fun favoriteFolderDetail(
        mediaId: String,
        title: String,
        coverUrl: String?,
        creator: String?,
        expectedTrackCount: Int,
        forceRefresh: Boolean = false,
    ): ProviderResult<com.aemusic.provider.netease.OnlinePlaylistDetail> =
        when (val result = favoriteFolderTracks(mediaId, forceRefresh = forceRefresh)) {
            is ProviderResult.Success -> {
                val tracks = result.value
                val resolvedCover = coverUrl?.takeIf(String::isNotBlank)
                    ?: (tracks.firstOrNull()?.artwork as? ArtworkRef.Reference)?.key
                    ?: ""
                ProviderResult.Success(
                    com.aemusic.provider.netease.OnlinePlaylistDetail(
                        id = mediaId,
                        name = title,
                        coverUrl = resolvedCover,
                        creator = creator.orEmpty(),
                        description = "B站个人收藏夹",
                        playCount = 0L,
                        trackCount = expectedTrackCount.takeIf { it > 0 } ?: tracks.size,
                        tracks = tracks,
                    ),
                )
            }
            is ProviderResult.Failure -> result
        }

    fun tracks(collection: BilibiliCollection): List<Track> = collection.parts.map { part ->
        Track(
            TrackId("${collection.bvid}:${part.cid}"), SOURCE, part.title,
            listOfNotNull(part.artist.takeIf(String::isNotBlank)?.let(::Artist)), collection.title.takeIf(String::isNotBlank)?.let(::Album),
            TrackDuration.ofMilliseconds(part.durationMs), collection.artworkUrl?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing,
            PlaybackReference.Provider(SOURCE, collection.bvid, part.cid),
        )
    }

    override suspend fun resolve(reference: PlaybackReference.Provider, quality: StreamQuality): ProviderResult<ResolvedStream> = guarded {
        val bvid = reference.mediaId
        val requestHeaders = videoHeaders(bvid)
        val cid = reference.partId ?: when (val result = collection(bvid)) {
            is ProviderResult.Success -> result.value.parts.firstOrNull()?.cid
            is ProviderResult.Failure -> null
        } ?: return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("Video part not found"))
        val fnval = if (quality == StreamQuality.Lossless) (4048 or 4096) else 4048
        val root = http.getJson(
            "https://api.bilibili.com/x/player/playurl?bvid=${encode(bvid)}&cid=${encode(cid)}&fnval=$fnval&fnver=0&fourk=1&otype=json",
            requestHeaders,
        ).obj()
        val code = root?.int("code") ?: -1
        if (code != 0) return@guarded if (code in setOf(-412, -352)) ProviderResult.Failure(ProviderFailure.RateLimited(root?.text("message")))
        else ProviderResult.Failure(ProviderFailure.Unavailable(root?.text("message")))
        val candidates = orderStreamCandidates(parseAudioCandidates(root?.obj("data")?.obj("dash")), quality)
            .take(candidateLimit)
        if (candidates.isEmpty()) return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("No DASH audio"))
        val isLoggedIn = accountCookie()?.isNotBlank() == true
        for (candidate in candidates) {
            val urls = sortCdnCandidates(candidate.urls)
            for ((index, url) in urls.withIndex()) {
                val probe = try { http.probeMedia(url, requestHeaders) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
                if (probe?.successful == true) {
                    val ordered = listOf(probe.finalUrl) + urls.filterIndexed { candidateIndex, value -> candidateIndex != index && value != probe.finalUrl }
                    diagnostics?.record("provider", "bilibili-resolve", SOURCE.value, bvid, "success", "${candidate.label}; loggedIn=$isLoggedIn; br=${candidate.bitrate}; candidates=${ordered.size}")
                    return@guarded ProviderResult.Success(
                        ResolvedStream(
                            ordered.first(), requestHeaders, ordered.drop(1), System.currentTimeMillis() + 2 * 60 * 60_000,
                            candidate.bitrate, candidate.mimeType, candidate.codec, candidate.label ?: "B站 DASH",
                        ),
                    )
                }
            }
        }
        ProviderResult.Failure(ProviderFailure.Unavailable("DASH candidates failed media probe"))
    }

    private fun parseTrack(element: JsonElement): Track? {
        val item = element.obj() ?: return null
        val bvid = item.text("bvid").takeIf(String::isNotBlank) ?: return null
        val rawTitle = decodeHtml(item.text("title").replace(Regex("<[^>]+>"), "")).ifBlank { return null }
        val identity = recognizeTrackIdentity(rawTitle, item.text("author"))
        return Track(
            TrackId(bvid), SOURCE, rawTitle, listOfNotNull((item.text("author").ifBlank { identity.artist }).takeIf(String::isNotBlank)?.let(::Artist)),
            Album("B站音轨"), TrackDuration.ofMilliseconds(parseDuration(item.text("duration"))),
            secureUrl(item.text("pic"))?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing, PlaybackReference.Provider(SOURCE, bvid),
        )
    }

    internal fun effectiveCookie(): String {
        val account = accountCookie()?.takeIf(String::isNotBlank)
        if (account != null) {
            val hasBuvid = account.contains("buvid3=")
            val hasFnval = account.contains("CURRENT_FNVAL=")
            if (hasBuvid && hasFnval) return account
            val anon = anonymousCookie()?.takeIf(String::isNotBlank) ?: newAnonymousCookie().also(saveAnonymousCookie)
            val extras = buildList {
                if (!hasBuvid) {
                    val buvidPart = anon.split(";").map(String::trim).firstOrNull { it.startsWith("buvid3=") }
                    if (buvidPart != null) add(buvidPart)
                }
                if (!hasFnval) {
                    add("CURRENT_FNVAL=4048")
                }
            }
            return if (extras.isEmpty()) account else "$account; ${extras.joinToString("; ")}"
        }
        return anonymousCookie()?.takeIf(String::isNotBlank) ?: newAnonymousCookie().also(saveAnonymousCookie)
    }

    private fun newAnonymousCookie(): String {
        val random = SecureRandom()
        val value = ByteArray(21).also(random::nextBytes).joinToString("") { "%02x".format(it.toInt() and 0xff) } + "infoc"
        return "buvid3=$value; b_nut=123456789; CURRENT_FNVAL=4048"
    }

    private data class CachedValue<T>(
        val value: T,
        val savedAtEpochMs: Long = System.currentTimeMillis(),
    ) {
        fun isFresh(): Boolean = System.currentTimeMillis() - savedAtEpochMs < FAVORITES_CACHE_TTL_MS
    }

    private fun headers(cookie: String?) = buildMap {
        put("User-Agent", USER_AGENT); put("Referer", "https://www.bilibili.com/")
        cookie?.takeIf(String::isNotBlank)?.let { put("Cookie", it) }
    }
    private fun videoHeaders(bvid: String) = headers(effectiveCookie()) + mapOf("Referer" to "https://www.bilibili.com/video/$bvid", "Accept-Encoding" to "identity")

    companion object {
        private const val DEFAULT_CANDIDATE_LIMIT = 15
        private const val MAX_CANDIDATE_LIMIT = 25
        private const val FAVORITES_CACHE_TTL_MS = 15 * 60_000L
        val SOURCE = MusicSourceId("bilibili")
        val ANONYMOUS_SOURCE = MusicSourceId("bilibili-anonymous")
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
        private fun secureUrl(value: String): String? = value.takeIf(String::isNotBlank)?.let { when { it.startsWith("//") -> "https:$it"; it.startsWith("http://") -> it.replaceFirst("http://", "https://"); else -> it } }
        private fun decodeHtml(value: String) = value.replace("&amp;", "&").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">")
        private fun parseDuration(value: String): Long { val p = value.split(':').mapNotNull(String::toLongOrNull); return when (p.size) { 2 -> (p[0] * 60 + p[1]) * 1_000; 3 -> (p[0] * 3600 + p[1] * 60 + p[2]) * 1_000; else -> 0 } }
        internal fun sortCdnCandidates(urls: List<String>): List<String> = urls.filter(String::isNotBlank).distinct().sortedBy {
            when { it.contains("bilivideo.com") && !it.contains("mcdn") && !it.contains(":8082") -> 0; it.contains("mcdn") || it.contains(":8082") -> 2; else -> 1 }
        }
    }
}

internal fun parseAudioCandidates(dash: JsonObject?): List<StreamCandidate> {
    fun parse(item: JsonObject, lossless: Boolean, label: String): StreamCandidate? {
        val urls = buildList {
            item.text("baseUrl").takeIf(String::isNotBlank)?.let(::add); item.text("base_url").takeIf(String::isNotBlank)?.let(::add)
            (item.arr("backupUrl") ?: item.arr("backup_url")).orEmpty().mapNotNullTo(this) { it.jsonPrimitive.contentOrNull }
        }.distinct()
        if (urls.isEmpty()) return null
        return StreamCandidate(urls, item.long("bandwidth").takeIf { it > 0 }, item.text("mimeType").ifBlank { item.text("mime_type") }.takeIf(String::isNotBlank), item.text("codecs").takeIf(String::isNotBlank), lossless, label)
    }
    return buildList {
        dash?.arr("audio").orEmpty().mapNotNullTo(this) { it.obj()?.let { item -> parse(item, false, "B站 DASH") } }
        dash?.obj("dolby")?.arr("audio").orEmpty().mapNotNullTo(this) { it.obj()?.let { item -> parse(item, true, "B站 Dolby") } }
        dash?.obj("flac")?.obj("audio")?.let { parse(it, true, "B站 FLAC") }?.let(::add)
    }
}

internal fun favoritePageSize(requested: Int): Int = requested.coerceIn(1, 20)

private suspend fun <T> guarded(block: suspend () -> ProviderResult<T>): ProviderResult<T> = try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { ProviderResult.Failure(ProviderFailure.Network(error.message)) }
