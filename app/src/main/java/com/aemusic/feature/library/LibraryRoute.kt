package com.aemusic.feature.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aemusic.core.model.Track
import com.aemusic.core.database.TrackKey
import com.aemusic.core.database.LocalPlaylist
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.aemusic.feature.common.loadTrackedPlaylist

@Composable
fun LibraryRoute(
    padding: PaddingValues,
    repository: LocalMusicRepository,
    settingsEntry: Boolean,
    immersiveTitles: Boolean,
    onSettings: () -> Unit,
    currentTrack: Track?,
    onPlayTrack: (List<Track>, Int) -> Unit,
    onShowNowPlaying: () -> Unit,
    favoriteKeys: Set<TrackKey>,
    savedTracks: List<Track>,
    onToggleFavorite: (Track) -> Unit,
    playlists: List<LocalPlaylist>,
    playlistActions: LibraryPlaylistActions,
    trackedPlaylists: List<com.aemusic.core.data.TrackedPlaylist> = emptyList(),
    discoveryRepository: com.aemusic.provider.netease.NeteaseDiscoveryRepository? = null,
    bilibiliProvider: com.aemusic.provider.bilibili.BilibiliProvider? = null,
    trackedPlaylistStore: com.aemusic.core.data.TrackedPlaylistStore? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = audioPermission()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh(context.hasAudioPermission())
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var selectedTrackedDetail by remember { mutableStateOf<com.aemusic.provider.netease.OnlinePlaylistDetail?>(null) }
    var isTrackedLoading by remember { mutableStateOf(false) }
    var trackedError by remember { mutableStateOf<String?>(null) }
    var activeTrackedPlaylist by remember { mutableStateOf<com.aemusic.core.data.TrackedPlaylist?>(null) }
    val isTrackedDetailVisible = selectedTrackedDetail != null || isTrackedLoading || trackedError != null
    val onDismissTrackedDetail = {
        selectedTrackedDetail = null
        isTrackedLoading = false
        trackedError = null
        activeTrackedPlaylist = null
    }

    val onOpenTracked: (com.aemusic.core.data.TrackedPlaylist) -> Unit = { tracked ->
        activeTrackedPlaylist = tracked
        if (tracked.source == "bilibili" && bilibiliProvider != null) {
            scope.launch {
                isTrackedLoading = true
                trackedError = null
                val result = bilibiliProvider.loadTrackedPlaylist(tracked)
                when (result) {
                    is com.aemusic.provider.ProviderResult.Success -> {
                        val detail = result.value
                        selectedTrackedDetail = detail
                        if (detail.coverUrl.isNotBlank() && tracked.coverUrl.isNullOrBlank()) {
                            trackedPlaylistStore?.updateMetadata(tracked.id, coverUrl = detail.coverUrl, trackCount = detail.trackCount)
                        }
                        isTrackedLoading = false
                    }
                    is com.aemusic.provider.ProviderResult.Failure -> {
                        trackedError = result.reason.detail ?: "加载 B 站收藏失败"
                        isTrackedLoading = false
                    }
                }
            }
        } else if (discoveryRepository != null) {
            scope.launch {
                isTrackedLoading = true
                when (val res = discoveryRepository.fetchPlaylistDetail(tracked.id)) {
                    is com.aemusic.provider.ProviderResult.Success -> {
                        val detail = res.value
                        selectedTrackedDetail = detail
                        if (detail.coverUrl.isNotBlank() && tracked.coverUrl.isNullOrBlank()) {
                            trackedPlaylistStore?.updateMetadata(tracked.id, coverUrl = detail.coverUrl, trackCount = detail.trackCount)
                        }
                        isTrackedLoading = false
                    }
                    is com.aemusic.provider.ProviderResult.Failure -> {
                        trackedError = "加载歌单详情失败"
                        isTrackedLoading = false
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(context.hasAudioPermission())
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        LibraryScreen(
            padding = padding,
            settingsEntry = settingsEntry,
            immersiveTitles = immersiveTitles,
            onSettings = onSettings,
            state = state,
            onRequestPermission = { permissionLauncher.launch(permission) },
            onRetry = { viewModel.refresh(context.hasAudioPermission()) },
            currentTrack = currentTrack,
            onToggleFavorite = onToggleFavorite,
            userData = LibraryUserData(
                favoriteKeys = favoriteKeys,
                savedTracks = savedTracks,
                playlists = playlists,
                playlistActions = playlistActions,
                trackedPlaylists = trackedPlaylists,
                onOpenTrackedPlaylist = onOpenTracked,
            ),
            onPlayTrack = { tracks, index ->
                val selected = tracks[index]
                if (currentTrack?.id == selected.id && currentTrack.sourceId == selected.sourceId) {
                    onShowNowPlaying()
                } else {
                    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onPlayTrack(tracks, index)
                }
            },
        )

        com.aemusic.design.component.PredictiveBackContainer(
            visible = isTrackedDetailVisible,
            onBack = onDismissTrackedDetail,
        ) {
            com.aemusic.feature.playlist.PlaylistDetailScreen(
                detail = selectedTrackedDetail,
                isLoading = isTrackedLoading,
                error = trackedError,
                currentTrack = currentTrack,
                isPlaying = false,
                onBack = onDismissTrackedDetail,
                onTrackSelected = onPlayTrack,
                onPlayAll = { pTracks -> if (pTracks.isNotEmpty()) onPlayTrack(pTracks, 0) },
                onRefresh = {
                    val tracked = activeTrackedPlaylist ?: return@PlaylistDetailScreen
                    scope.launch {
                        isTrackedLoading = true
                        trackedError = null
                        val result = when {
                            tracked.source == "bilibili" && bilibiliProvider != null ->
                                bilibiliProvider.loadTrackedPlaylist(tracked, forceRefresh = true)
                            discoveryRepository != null ->
                                discoveryRepository.fetchPlaylistDetail(tracked.id, forceRefresh = true)
                            else -> null
                        }
                        when (result) {
                            is com.aemusic.provider.ProviderResult.Success -> {
                                val detail = result.value
                                selectedTrackedDetail = detail
                                if (detail.coverUrl.isNotBlank() && tracked.coverUrl.isNullOrBlank()) {
                                    trackedPlaylistStore?.updateMetadata(tracked.id, coverUrl = detail.coverUrl, trackCount = detail.trackCount)
                                }
                            }
                            is com.aemusic.provider.ProviderResult.Failure -> trackedError = result.reason.detail ?: "刷新失败"
                            null -> Unit
                        }
                        isTrackedLoading = false
                    }
                },
            )
        }
    }
}

private fun audioPermission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

private fun Context.hasAudioPermission(): Boolean = checkSelfPermission(audioPermission()) == PackageManager.PERMISSION_GRANTED
