package com.aemusic.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aemusic.core.model.Track
import com.aemusic.provider.ProviderResult
import com.aemusic.provider.netease.NeteaseDiscoveryRepository
import com.aemusic.provider.netease.OnlinePlaylist
import com.aemusic.provider.netease.OnlinePlaylistDetail
import com.aemusic.provider.netease.OnlineToplist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class HomeDiscoveryUiState(
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "全部",
    val playlists: List<OnlinePlaylist> = emptyList(),
    val isPlaylistsLoading: Boolean = false,
    val toplists: List<OnlineToplist> = emptyList(),
    val isToplistsLoading: Boolean = false,
    val dailyRecommendations: List<Track> = emptyList(),
    val isDailyLoading: Boolean = false,
    val personalFm: List<Track> = emptyList(),
    val isFmLoading: Boolean = false,
    val selectedPlaylistDetail: OnlinePlaylistDetail? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null,
)

class HomeViewModel(
    private val discoveryRepository: NeteaseDiscoveryRepository,
) : ViewModel() {
    private var playlistsJob: Job? = null
    private var detailJob: Job? = null

    private val _discoveryState = MutableStateFlow(HomeDiscoveryUiState(categories = discoveryRepository.categories))
    val discoveryState: StateFlow<HomeDiscoveryUiState> = _discoveryState.asStateFlow()

    init {
        loadDailyRecommendations()
        loadToplists()
        loadPlaylists("全部")
    }

    fun selectCategory(category: String) {
        if (_discoveryState.value.selectedCategory == category) return
        _discoveryState.value = _discoveryState.value.copy(selectedCategory = category)
        loadPlaylists(category)
    }

    fun loadPlaylists(category: String) {
        playlistsJob?.cancel()
        playlistsJob = viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(isPlaylistsLoading = true)
            when (val res = discoveryRepository.fetchPlaylists(category = category, limit = 20)) {
                is ProviderResult.Success -> {
                    if (_discoveryState.value.selectedCategory != category) return@launch
                    _discoveryState.value = _discoveryState.value.copy(
                        playlists = res.value,
                        isPlaylistsLoading = false,
                    )
                }
                is ProviderResult.Failure -> {
                    _discoveryState.value = _discoveryState.value.copy(isPlaylistsLoading = false)
                }
            }
        }
    }

    fun loadToplists() {
        viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(isToplistsLoading = true)
            when (val res = discoveryRepository.fetchToplists()) {
                is ProviderResult.Success -> {
                    _discoveryState.value = _discoveryState.value.copy(
                        toplists = res.value,
                        isToplistsLoading = false,
                    )
                }
                is ProviderResult.Failure -> {
                    _discoveryState.value = _discoveryState.value.copy(isToplistsLoading = false)
                }
            }
        }
    }

    fun loadDailyRecommendations() {
        viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(isDailyLoading = true)
            when (val res = discoveryRepository.fetchDailyRecommendations()) {
                is ProviderResult.Success -> {
                    _discoveryState.value = _discoveryState.value.copy(
                        dailyRecommendations = res.value,
                        isDailyLoading = false,
                    )
                }
                is ProviderResult.Failure -> {
                    _discoveryState.value = _discoveryState.value.copy(isDailyLoading = false)
                }
            }
        }
    }

    fun loadPersonalFm(onLoaded: ((List<Track>) -> Unit)? = null) {
        viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(isFmLoading = true)
            when (val res = discoveryRepository.fetchPersonalFm()) {
                is ProviderResult.Success -> {
                    _discoveryState.value = _discoveryState.value.copy(
                        personalFm = res.value,
                        isFmLoading = false,
                    )
                    onLoaded?.invoke(res.value)
                }
                is ProviderResult.Failure -> {
                    _discoveryState.value = _discoveryState.value.copy(isFmLoading = false)
                }
            }
        }
    }

    fun openPlaylist(playlistId: String, forceRefresh: Boolean = false) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(
                isDetailLoading = true,
                detailError = null,
                selectedPlaylistDetail = null,
            )
            when (val res = discoveryRepository.fetchPlaylistDetail(playlistId, forceRefresh)) {
                is ProviderResult.Success -> {
                    _discoveryState.value = _discoveryState.value.copy(
                        selectedPlaylistDetail = res.value,
                        isDetailLoading = false,
                    )
                }
                is ProviderResult.Failure -> {
                    _discoveryState.value = _discoveryState.value.copy(
                        isDetailLoading = false,
                        detailError = "加载歌单失败，请检查网络",
                    )
                }
            }
        }
    }

    fun closePlaylistDetail() {
        detailJob?.cancel()
        _discoveryState.value = _discoveryState.value.copy(
            selectedPlaylistDetail = null,
            isDetailLoading = false,
            detailError = null,
        )
    }

    fun showExternalPlaylist(detail: OnlinePlaylistDetail) {
        detailJob?.cancel()
        _discoveryState.value = _discoveryState.value.copy(
            selectedPlaylistDetail = detail,
            isDetailLoading = false,
            detailError = null,
        )
    }

    companion object {
        fun factory(discoveryRepository: NeteaseDiscoveryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(discoveryRepository) as T
        }
    }
}
