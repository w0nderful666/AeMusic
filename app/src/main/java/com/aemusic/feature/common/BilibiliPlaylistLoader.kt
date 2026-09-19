package com.aemusic.feature.common

import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.provider.ProviderResult
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.netease.OnlinePlaylistDetail

suspend fun BilibiliProvider.loadTrackedPlaylist(
    tracked: TrackedPlaylist,
    forceRefresh: Boolean = false,
): ProviderResult<OnlinePlaylistDetail> {
    if (!tracked.id.startsWith("BV", ignoreCase = true)) {
        return favoriteFolderDetail(
            tracked.id,
            tracked.title,
            tracked.coverUrl,
            tracked.creator,
            tracked.trackCount,
            forceRefresh,
        )
    }
    return when (val collection = collection(tracked.id)) {
        is ProviderResult.Success -> ProviderResult.Success(
            OnlinePlaylistDetail(
                id = tracked.id,
                name = collection.value.title,
                coverUrl = collection.value.artworkUrl.orEmpty(),
                creator = collection.value.author,
                description = "B站视频合集",
                playCount = 0L,
                trackCount = collection.value.parts.size,
                tracks = tracks(collection.value),
            ),
        )
        is ProviderResult.Failure -> collection
    }
}
