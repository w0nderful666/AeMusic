package com.aemusic.provider.netease

import com.aemusic.core.model.*
import com.aemusic.core.network.AeNetworkClient
import com.aemusic.provider.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.URLEncoder

data class OnlinePlaylist(
    val id: String,
    val name: String,
    val coverUrl: String,
    val playCount: Long,
    val trackCount: Int,
    val creator: String,
    val description: String,
)

data class OnlineToplist(
    val id: String,
    val name: String,
    val coverUrl: String,
    val updateFrequency: String,
    val playCount: Long,
    val description: String,
)

data class OnlinePlaylistDetail(
    val id: String,
    val name: String,
    val coverUrl: String,
    val creator: String,
    val description: String,
    val playCount: Long,
    val trackCount: Int,
    val tracks: List<Track>,
)

class NeteaseDiscoveryRepository(
    private val http: AeNetworkClient,
    private val neteaseProvider: NeteaseProvider,
) {
    private val playlistDetailCache = mutableMapOf<String, CachedPlaylistDetail>()
    val categories: List<String> = listOf(
        "全部", "华语", "流行", "摇滚", "民谣", "电子", "ACG", "说唱", "经典", "治愈", "二次元", "欧美", "日韩", "轻音乐"
    )

    private fun headers(): Map<String, String> {
        val base = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to "https://music.163.com",
        )
        val cookie = neteaseProvider.effectiveCookie()
        return if (!cookie.isNullOrBlank()) base + mapOf("Cookie" to cookie) else base
    }

    suspend fun fetchPlaylists(category: String = "全部", limit: Int = 30, offset: Int = 0): ProviderResult<List<OnlinePlaylist>> = withContext(Dispatchers.IO) {
        guarded {
            val encodedCat = URLEncoder.encode(category, Charsets.UTF_8.name())
            val url = "https://music.163.com/api/playlist/list?cat=$encodedCat&order=hot&offset=$offset&limit=$limit"
            val root = http.getJson(url, headers()).obj()
            val rawList = root?.arr("playlists").orEmpty()
            val playlists = rawList.mapNotNull { item ->
                val obj = item.obj() ?: return@mapNotNull null
                val id = obj.long("id").takeIf { it > 0 }?.toString() ?: return@mapNotNull null
                val name = obj.text("name").ifBlank { return@mapNotNull null }
                val cover = obj.text("coverImgUrl").replaceFirst(Regex("^http://"), "https://")
                val playCount = obj.long("playCount")
                val trackCount = obj.int("trackCount")
                val creator = obj.obj("creator")?.text("nickname").orEmpty()
                val desc = obj.text("description")
                OnlinePlaylist(id, name, cover, playCount, trackCount, creator, desc)
            }
            ProviderResult.Success(playlists)
        }
    }

    suspend fun fetchToplists(): ProviderResult<List<OnlineToplist>> = withContext(Dispatchers.IO) {
        guarded {
            val url = "https://music.163.com/api/toplist"
            val root = http.getJson(url, headers()).obj()
            val rawList = root?.arr("list").orEmpty()
            val toplists = rawList.mapNotNull { item ->
                val obj = item.obj() ?: return@mapNotNull null
                val id = obj.long("id").takeIf { it > 0 }?.toString() ?: return@mapNotNull null
                val name = obj.text("name").ifBlank { return@mapNotNull null }
                val cover = obj.text("coverImgUrl").replaceFirst(Regex("^http://"), "https://")
                val updateFreq = obj.text("updateFrequency").ifBlank { "实时更新" }
                val playCount = obj.long("playCount")
                val desc = obj.text("description")
                OnlineToplist(id, name, cover, updateFreq, playCount, desc)
            }
            ProviderResult.Success(toplists)
        }
    }

    suspend fun fetchPlaylistDetail(
        playlistId: String,
        forceRefresh: Boolean = false,
    ): ProviderResult<OnlinePlaylistDetail> = withContext(Dispatchers.IO) {
        playlistDetailCache[playlistId]
            ?.takeIf { !forceRefresh && System.currentTimeMillis() - it.savedAtEpochMs < PLAYLIST_CACHE_TTL_MS }
            ?.let { return@withContext ProviderResult.Success(it.detail) }
        guarded {
            val url = "https://music.163.com/api/v3/playlist/detail?id=$playlistId"
            val root = http.getJson(url, headers()).obj()
            val pl = root?.obj("playlist")
                ?: return@guarded ProviderResult.Failure(ProviderFailure.Parse("Missing playlist object"))
            val id = pl.long("id").toString()
            val name = pl.text("name").ifBlank { "歌单详情" }
            val cover = pl.text("coverImgUrl").replaceFirst(Regex("^http://"), "https://")
            val creator = pl.obj("creator")?.text("nickname").orEmpty()
            val desc = pl.text("description")
            val playCount = pl.long("playCount")
            val trackCount = pl.int("trackCount")

            val trackIds = pl.arr("trackIds").orEmpty().mapNotNull {
                it.obj()?.long("id")?.takeIf { idNum -> idNum > 0 }?.toString()
            }.take(100)

            val tracks = if (trackIds.isNotEmpty()) {
                val cJson = trackIds.joinToString(prefix = "[", postfix = "]") { "{\"id\":$it}" }
                val encodedC = URLEncoder.encode(cJson, Charsets.UTF_8.name())
                val songDetailUrl = "https://music.163.com/api/v3/song/detail?c=$encodedC"
                val songDetailRoot = http.getJson(songDetailUrl, headers()).obj()
                val songs = songDetailRoot?.arr("songs").orEmpty()
                songs.mapNotNull { neteaseProvider.parseTrack(it) }
            } else {
                emptyList()
            }

            val detail = OnlinePlaylistDetail(
                    id = id,
                    name = name,
                    coverUrl = cover,
                    creator = creator,
                    description = desc,
                    playCount = playCount,
                    trackCount = if (trackCount > 0) trackCount else tracks.size,
                    tracks = tracks,
                )
            playlistDetailCache[playlistId] = CachedPlaylistDetail(detail, System.currentTimeMillis())
            ProviderResult.Success(detail)
        }
    }

    private data class CachedPlaylistDetail(
        val detail: OnlinePlaylistDetail,
        val savedAtEpochMs: Long,
    )

    private companion object {
        const val PLAYLIST_CACHE_TTL_MS = 15 * 60_000L
    }

    suspend fun fetchDailyRecommendations(): ProviderResult<List<Track>> = withContext(Dispatchers.IO) {
        guarded {
            if (neteaseProvider.hasAccountLogin()) {
                val dailyUrl = "https://music.163.com/api/v3/discovery/recommend/songs"
                val dailyRoot = http.getJson(dailyUrl, headers()).obj()
                val dailySongs = dailyRoot?.obj("data")?.arr("dailySongs").orEmpty()
                if (dailySongs.isNotEmpty()) {
                    val tracks = dailySongs.mapNotNull { neteaseProvider.parseTrack(it) }
                    if (tracks.isNotEmpty()) return@guarded ProviderResult.Success(tracks)
                }
            }
            val url = "https://music.163.com/api/personalized/newsong?limit=30"
            val root = http.getJson(url, headers()).obj()
            val result = root?.arr("result").orEmpty()
            val tracks = result.mapNotNull { item ->
                val songObj = item.obj()?.obj("song") ?: item.obj() ?: return@mapNotNull null
                neteaseProvider.parseTrack(songObj)
            }
            ProviderResult.Success(tracks)
        }
    }

    suspend fun fetchPersonalFm(): ProviderResult<List<Track>> = withContext(Dispatchers.IO) {
        guarded {
            val url = "https://music.163.com/api/v1/radio/get"
            val root = http.getJson(url, headers()).obj()
            val rawList = root?.arr("data").orEmpty()
            val tracks = rawList.mapNotNull { neteaseProvider.parseTrack(it) }
            if (tracks.isEmpty()) ProviderResult.Failure(ProviderFailure.Unavailable("私人FM暂无推荐曲目"))
            else ProviderResult.Success(tracks)
        }
    }

    suspend fun parsePlaylistId(urlOrId: String): String? {
        val clean = urlOrId.trim()
        val match = Regex("""(?:playlist\?id=|\/playlist\/)(\d+)""").find(clean)
        if (match != null) return match.groupValues[1]
        return if (clean.all(Char::isDigit) && clean.isNotEmpty()) clean else null
    }

    private suspend fun <T> guarded(block: suspend () -> ProviderResult<T>): ProviderResult<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        ProviderResult.Failure(ProviderFailure.Network(e.message))
    }
}
