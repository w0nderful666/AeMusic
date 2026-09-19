package com.aemusic.core.data

import android.content.Context
import androidx.core.content.edit
import com.aemusic.provider.ProviderResult
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.netease.NeteaseDiscoveryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TrackedPlaylist(
    val id: String,
    val source: String, // "netease" or "bilibili"
    val title: String,
    val coverUrl: String? = null,
    val trackCount: Int = 0,
    val creator: String? = null,
    val pinnedAtEpochMs: Long = System.currentTimeMillis(),
)

class TrackedPlaylistStore(
    context: Context,
    private val discoveryRepository: NeteaseDiscoveryRepository,
    private val bilibiliProvider: BilibiliProvider,
) {
    private val preferences = context.getSharedPreferences("aemusic_tracked_playlists", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val _playlists = MutableStateFlow<List<TrackedPlaylist>>(loadFromDisk())
    val playlists: StateFlow<List<TrackedPlaylist>> = _playlists.asStateFlow()

    private fun loadFromDisk(): List<TrackedPlaylist> {
        val raw = preferences.getString(KEY_TRACKED_LIST, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<TrackedPlaylist>>(raw) }.getOrDefault(emptyList())
    }

    private fun persist(list: List<TrackedPlaylist>) {
        _playlists.value = list
        preferences.edit { putString(KEY_TRACKED_LIST, json.encodeToString(list)) }
    }

    suspend fun addTracked(input: String): Result<TrackedPlaylist> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        val bvMatch = Regex("""(BV[a-zA-Z0-9]+)""").find(trimmed)
        if (bvMatch != null) {
            val bvid = bvMatch.groupValues[1]
            when (val result = bilibiliProvider.collection(bvid)) {
                is ProviderResult.Success -> {
                    val collection = result.value
                    val item = TrackedPlaylist(
                        id = bvid,
                        source = "bilibili",
                        title = collection.title.ifBlank { bvid },
                        coverUrl = collection.artworkUrl,
                        trackCount = collection.parts.size,
                        creator = collection.author.ifBlank { "哔哩哔哩" },
                    )
                    val updated = (_playlists.value.filterNot { it.id == item.id } + item).sortedByDescending { it.pinnedAtEpochMs }
                    persist(updated)
                    Result.success(item)
                }
                is ProviderResult.Failure -> Result.failure(Exception("无法解析 B 站视频或合集: $bvid"))
            }
        } else {
            val playlistId = discoveryRepository.parsePlaylistId(trimmed)
                ?: return@withContext Result.failure(Exception("未识别到有效的网易云或 B 站歌单链接/ID"))
            when (val detailResult = discoveryRepository.fetchPlaylistDetail(playlistId)) {
                is ProviderResult.Success -> {
                    val detail = detailResult.value
                    val item = TrackedPlaylist(
                        id = detail.id,
                        source = "netease",
                        title = detail.name,
                        coverUrl = detail.coverUrl,
                        trackCount = detail.trackCount,
                        creator = detail.creator,
                    )
                    val updated = (_playlists.value.filterNot { it.id == item.id } + item).sortedByDescending { it.pinnedAtEpochMs }
                    persist(updated)
                    Result.success(item)
                }
                is ProviderResult.Failure -> {
                    Result.failure(Exception("获取网易云歌单失败"))
                }
            }
        }
    }

    fun addExplicit(item: TrackedPlaylist) {
        val updated = (_playlists.value.filterNot { it.id == item.id } + item).sortedByDescending { it.pinnedAtEpochMs }
        persist(updated)
    }

    fun updateMetadata(id: String, coverUrl: String? = null, title: String? = null, trackCount: Int? = null) {
        val current = _playlists.value
        val existing = current.firstOrNull { it.id == id } ?: return
        val updatedItem = existing.copy(
            coverUrl = coverUrl?.takeIf(String::isNotBlank) ?: existing.coverUrl,
            title = title?.takeIf(String::isNotBlank) ?: existing.title,
            trackCount = trackCount?.takeIf { it > 0 } ?: existing.trackCount,
        )
        if (updatedItem != existing) {
            val updated = current.map { if (it.id == id) updatedItem else it }
            persist(updated)
        }
    }

    fun mergePlaylists(list: List<TrackedPlaylist>) {
        val current = _playlists.value
        val merged = (current.filterNot { c -> list.any { it.id == c.id } } + list).sortedByDescending { it.pinnedAtEpochMs }
        persist(merged)
    }

    fun removeTracked(id: String) {
        val updated = _playlists.value.filterNot { it.id == id }
        persist(updated)
    }

    fun moveTracked(id: String, offset: Int) {
        val updated = reorderTracked(_playlists.value, id, offset)
        if (updated != _playlists.value) persist(updated)
    }

    private companion object {
        const val KEY_TRACKED_LIST = "tracked_playlists_json"
    }
}

internal fun reorderTracked(
    playlists: List<TrackedPlaylist>,
    id: String,
    offset: Int,
): List<TrackedPlaylist> {
    if (offset == 0 || playlists.isEmpty()) return playlists
    val current = playlists.toMutableList()
    val from = current.indexOfFirst { it.id == id }
    if (from < 0) return playlists
    val to = (from + offset).coerceIn(current.indices)
    if (from == to) return playlists
    val item = current.removeAt(from)
    current.add(to, item)
    return current
}
