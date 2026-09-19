package com.aemusic.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aemusic.core.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object PermissionRequired : LibraryUiState
    data object Empty : LibraryUiState
    data class Content(val tracks: List<Track>) : LibraryUiState
    data class Error(val message: String?) : LibraryUiState
}

class LibraryViewModel(private val repository: LocalMusicRepository) : ViewModel() {
    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh(hasPermission: Boolean) {
        if (!hasPermission) {
            refreshJob?.cancel()
            _state.value = LibraryUiState.PermissionRequired
            return
        }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = LibraryUiState.Loading
            try {
                val tracks = repository.tracks()
                _state.value = if (tracks.isEmpty()) LibraryUiState.Empty else LibraryUiState.Content(tracks)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: SecurityException) {
                _state.value = LibraryUiState.PermissionRequired
            } catch (error: Exception) {
                _state.value = LibraryUiState.Error(error.message)
            }
        }
    }

    companion object {
        fun factory(repository: LocalMusicRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repository) as T
            }
    }
}
