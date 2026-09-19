package com.aemusic.feature.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
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
import com.aemusic.feature.library.LibraryViewModel
import com.aemusic.feature.library.LocalMusicRepository
import com.aemusic.provider.ProviderRegistry
import com.aemusic.provider.ProviderResult
import com.aemusic.core.database.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

import androidx.activity.compose.BackHandler
import com.aemusic.feature.home.HomeViewModel
import com.aemusic.feature.playlist.PlaylistDetailScreen
import com.aemusic.provider.netease.NeteaseDiscoveryRepository

@Composable
fun SearchRoute(
    padding: PaddingValues,
    repository: LocalMusicRepository,
    providerRegistry: ProviderRegistry,
    discoveryRepository: NeteaseDiscoveryRepository,
    currentTrack: Track?,
    isPlaying: Boolean = false,
    onTrack: (List<Track>, Int) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val model: LibraryViewModel = viewModel(key = "search-library", factory = LibraryViewModel.factory(repository))
    val state by model.state.collectAsStateWithLifecycle()
    val discoveryModel: HomeViewModel = viewModel(key = "search-discovery", factory = HomeViewModel.factory(discoveryRepository))
    val discoveryState by discoveryModel.discoveryState.collectAsStateWithLifecycle()
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
    val searchPreferences = remember(context) { SearchPreferences(context.applicationContext) }
    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(searchPreferences.history) }
    var selectedSources by remember { mutableStateOf(searchPreferences.selectedSources) }
    var remoteResults by remember { mutableStateOf(emptyList<Track>()) }
    var remoteLoading by remember { mutableStateOf(false) }
    var remoteError by remember { mutableStateOf(false) }
    var remoteLimit by remember { mutableIntStateOf(INITIAL_REMOTE_LIMIT) }
    val sourceOptions = remember(providerRegistry) {
        listOf(SearchSourceOption("local", "Local", "本地")) + providerRegistry.searchable.map {
            SearchSourceOption(it.descriptor.id.value, it.descriptor.name, it.descriptor.name)
        }
    }
    LaunchedEffect(submittedQuery, selectedSources, remoteLimit) {
        remoteResults = emptyList()
        remoteError = false
        if (submittedQuery.isBlank()) return@LaunchedEffect
        val providers = providerRegistry.searchable.filter { it.descriptor.id.value in selectedSources }
        if (providers.isEmpty()) return@LaunchedEffect
        remoteLoading = true
        try {
            val responses = coroutineScope { providers.map { async { it.search(submittedQuery, remoteLimit) } }.awaitAll() }
            remoteResults = responses.filterIsInstance<ProviderResult.Success<List<Track>>>().flatMap { it.value }.distinctBy(Track::key)
            remoteError = responses.any { it is ProviderResult.Failure }
        } finally { remoteLoading = false }
    }
    val isDetailVisible = discoveryState.selectedPlaylistDetail != null || discoveryState.isDetailLoading || discoveryState.detailError != null

    Box(Modifier.fillMaxSize()) {
        SearchScreen(
            padding = padding,
            state = state,
            query = query,
            onQuery = { query = it },
            submittedQuery = submittedQuery,
            onSearch = {
                val term = query.trim()
                if (term.isNotBlank()) {
                    remoteLimit = INITIAL_REMOTE_LIMIT
                    submittedQuery = term
                    history = searchPreferences.record(term)
                }
            },
            history = history,
            onHistory = { term ->
                remoteLimit = INITIAL_REMOTE_LIMIT
                query = term
                submittedQuery = term
                history = searchPreferences.record(term)
            },
            sourceOptions = sourceOptions,
            selectedSources = selectedSources,
            onToggleSource = { id ->
                remoteLimit = INITIAL_REMOTE_LIMIT
                selectedSources = (if (id in selectedSources) selectedSources - id else selectedSources + id).ifEmpty { setOf("local") }
                searchPreferences.selectedSources = selectedSources
            },
            remoteResults = remoteResults,
            remoteLoading = remoteLoading,
            remoteError = remoteError,
            canLoadMore = submittedQuery.isNotBlank() && selectedSources.any { it != "local" } && remoteLimit < MAX_REMOTE_LIMIT,
            onLoadMore = { remoteLimit = (remoteLimit + REMOTE_LIMIT_STEP).coerceAtMost(MAX_REMOTE_LIMIT) },
            currentTrack = currentTrack,
            onTrack = onTrack,
            discoveryState = discoveryState,
            onCategorySelect = discoveryModel::selectCategory,
            onOpenPlaylist = discoveryModel::openPlaylist,
            onPlayDaily = { dailyTracks -> if (dailyTracks.isNotEmpty()) onTrack(dailyTracks, 0) },
            onPlayFm = {
                discoveryModel.loadPersonalFm { fmTracks ->
                    if (fmTracks.isNotEmpty()) onTrack(fmTracks, 0)
                }
            },
        )

        com.aemusic.design.component.PredictiveBackContainer(
            visible = isDetailVisible,
            onBack = discoveryModel::closePlaylistDetail,
        ) {
            PlaylistDetailScreen(
                detail = discoveryState.selectedPlaylistDetail,
                isLoading = discoveryState.isDetailLoading,
                error = discoveryState.detailError,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onBack = discoveryModel::closePlaylistDetail,
                onTrackSelected = onTrack,
                onPlayAll = { playlistTracks -> if (playlistTracks.isNotEmpty()) onTrack(playlistTracks, 0) },
                onRefresh = {
                    discoveryState.selectedPlaylistDetail?.id?.let { id ->
                        discoveryModel.openPlaylist(id, forceRefresh = true)
                    }
                },
            )
        }
    }
}

data class SearchSourceOption(val id: String, val englishName: String, val chineseName: String)

private const val INITIAL_REMOTE_LIMIT = 20
private const val REMOTE_LIMIT_STEP = 15
private const val MAX_REMOTE_LIMIT = 50
