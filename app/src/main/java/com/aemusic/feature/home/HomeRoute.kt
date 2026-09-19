package com.aemusic.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aemusic.core.database.TrackKey
import com.aemusic.core.database.key
import com.aemusic.core.model.Track
import com.aemusic.feature.library.LibraryUiState
import com.aemusic.feature.library.LibraryViewModel
import com.aemusic.feature.library.LocalMusicRepository
import com.aemusic.feature.playlist.PlaylistDetailScreen
import com.aemusic.provider.netease.NeteaseDiscoveryRepository

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.core.data.TrackedPlaylistStore
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.settings.localized
import com.aemusic.feature.common.loadTrackedPlaylist
import com.aemusic.provider.ProviderResult
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    padding: PaddingValues,
    repository: LocalMusicRepository,
    discoveryRepository: NeteaseDiscoveryRepository,
    bilibiliProvider: com.aemusic.provider.bilibili.BilibiliProvider,
    trackedPlaylistStore: TrackedPlaylistStore,
    favoriteKeys: Set<TrackKey>,
    recentKeys: List<TrackKey>,
    mostPlayedKeys: List<TrackKey>,
    playHistory: List<com.aemusic.core.database.PlayHistoryEntity> = emptyList(),
    savedTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    immersiveTitles: Boolean,
    onTrack: (List<Track>, Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLibraryAvailable: (List<Track>) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val model: LibraryViewModel = viewModel(key = "home-library", factory = LibraryViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    val homeModel: HomeViewModel = viewModel(
        key = "home-discovery",
        factory = HomeViewModel.factory(discoveryRepository),
    )
    val discoveryState by homeModel.discoveryState.collectAsStateWithLifecycle()
    val trackedPlaylists by trackedPlaylistStore.playlists.collectAsStateWithLifecycle()
    val preferences = remember { com.aemusic.feature.settings.AppUiPreferences(context) }
    var savedShortcutKeys by remember { mutableStateOf(preferences.homeShortcutPlaylists) }

    var showAddDialog by remember { mutableStateOf(false) }
    var addInput by remember { mutableStateOf("") }
    var addLoading by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    var isBilibiliCollectionLoading by remember { mutableStateOf(false) }
    var bilibiliCollectionError by remember { mutableStateOf<String?>(null) }
    var activeTrackedPlaylist by remember { mutableStateOf<TrackedPlaylist?>(null) }

    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                model.refresh(context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val tracks = (state as? LibraryUiState.Content)?.tracks.orEmpty()
    LaunchedEffect(tracks) {
        if (tracks.isNotEmpty()) onLibraryAvailable(tracks)
    }
    val byKey = (tracks + savedTracks).associateBy(Track::key)
    val playCounts = remember(playHistory) {
        playHistory.associate { TrackKey(it.sourceId, it.trackId) to it.playCount }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; addInput = ""; addError = null },
            title = { Text(localized("Track Playlist", "添加首页追踪歌单")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized("Paste NetEase or Bilibili share link, URL, or playlist ID:", "支持粘贴网易云或 B 站分享链接、网址或歌单 ID："),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = addInput,
                        onValueChange = { addInput = it; addError = null },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://music.163.com/... 或 BV...") },
                        singleLine = true,
                    )
                    if (addError != null) {
                        Text(addError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = addInput.isNotBlank() && !addLoading,
                    onClick = {
                        addLoading = true
                        scope.launch {
                            val res = trackedPlaylistStore.addTracked(addInput)
                            res.fold(
                                onSuccess = {
                                    showAddDialog = false
                                    addInput = ""
                                    addError = null
                                    addLoading = false
                                },
                                onFailure = {
                                    addError = it.message ?: "解析失败，请检查链接"
                                    addLoading = false
                                },
                            )
                        }
                    },
                ) {
                    if (addLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(localized("Track", "确认追踪"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; addInput = ""; addError = null }) {
                    Text(localized("Cancel", "取消"))
                }
            },
        )
    }

    val isDetailVisible = discoveryState.selectedPlaylistDetail != null ||
        discoveryState.isDetailLoading || discoveryState.detailError != null ||
        isBilibiliCollectionLoading || bilibiliCollectionError != null
    val onDismissDetail = {
        homeModel.closePlaylistDetail()
        isBilibiliCollectionLoading = false
        bilibiliCollectionError = null
        activeTrackedPlaylist = null
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            padding = padding,
            track = currentTrack,
            isPlaying = isPlaying,
            favorites = favoriteKeys.mapNotNull(byKey::get),
            recent = recentKeys.mapNotNull(byKey::get),
            mostPlayed = mostPlayedKeys.mapNotNull(byKey::get),
            playCounts = playCounts,
            trackedPlaylists = trackedPlaylists,
            localTracks = tracks,
            onTrack = onTrack,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            immersiveTitles = immersiveTitles,
            onAddTrackedPlaylist = { showAddDialog = true },
            onOpenTrackedPlaylist = { tracked ->
                activeTrackedPlaylist = tracked
                if (tracked.source == "bilibili") {
                    scope.launch {
                        isBilibiliCollectionLoading = true
                        bilibiliCollectionError = null
                        val result = bilibiliProvider.loadTrackedPlaylist(tracked)
                        when (result) {
                            is ProviderResult.Success -> {
                                val detail = result.value
                                homeModel.showExternalPlaylist(detail)
                                if (detail.coverUrl.isNotBlank() && tracked.coverUrl.isNullOrBlank()) {
                                    trackedPlaylistStore.updateMetadata(tracked.id, coverUrl = detail.coverUrl, trackCount = detail.trackCount)
                                }
                                isBilibiliCollectionLoading = false
                            }
                            is ProviderResult.Failure -> {
                                bilibiliCollectionError = result.reason.detail ?: "加载 B 站收藏失败"
                                isBilibiliCollectionLoading = false
                            }
                        }
                    }
                } else {
                    homeModel.openPlaylist(tracked.id)
                }
            },
            onRemoveTrackedPlaylist = trackedPlaylistStore::removeTracked,
            savedShortcutKeys = savedShortcutKeys,
            onSaveShortcuts = { keys ->
                savedShortcutKeys = keys
                preferences.homeShortcutPlaylists = keys
            },
            dailyRecommendations = discoveryState.dailyRecommendations,
            onPlayDaily = {
                if (discoveryState.dailyRecommendations.isNotEmpty()) {
                    onTrack(discoveryState.dailyRecommendations, 0)
                }
            },
            onPlayFm = {
                homeModel.loadPersonalFm { tracks ->
                    if (tracks.isNotEmpty()) onTrack(tracks, 0)
                }
            },
        )

        com.aemusic.design.component.PredictiveBackContainer(
            visible = isDetailVisible,
            onBack = onDismissDetail,
        ) {
            PlaylistDetailScreen(
                detail = discoveryState.selectedPlaylistDetail,
                isLoading = discoveryState.isDetailLoading || isBilibiliCollectionLoading,
                error = discoveryState.detailError ?: bilibiliCollectionError,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onBack = onDismissDetail,
                onTrackSelected = onTrack,
                onPlayAll = { playlistTracks -> if (playlistTracks.isNotEmpty()) onTrack(playlistTracks, 0) },
                onRefresh = {
                    val tracked = activeTrackedPlaylist ?: return@PlaylistDetailScreen
                    if (tracked.source == "bilibili") {
                        scope.launch {
                            isBilibiliCollectionLoading = true
                            when (val result = bilibiliProvider.loadTrackedPlaylist(tracked, forceRefresh = true)) {
                                is ProviderResult.Success -> {
                                    val detail = result.value
                                    homeModel.showExternalPlaylist(detail)
                                    if (detail.coverUrl.isNotBlank() && tracked.coverUrl.isNullOrBlank()) {
                                        trackedPlaylistStore.updateMetadata(tracked.id, coverUrl = detail.coverUrl, trackCount = detail.trackCount)
                                    }
                                }
                                is ProviderResult.Failure -> bilibiliCollectionError = result.reason.detail ?: "刷新失败"
                            }
                            isBilibiliCollectionLoading = false
                        }
                    } else {
                        homeModel.openPlaylist(tracked.id, forceRefresh = true)
                    }
                },
            )
        }
    }
}
