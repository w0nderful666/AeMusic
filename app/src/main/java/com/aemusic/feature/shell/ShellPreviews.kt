package com.aemusic.feature.shell

import com.aemusic.feature.search.SearchScreen
import com.aemusic.feature.home.HomeScreen
import com.aemusic.feature.library.LibraryScreen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aemusic.design.preview.AeThemePreview
import com.aemusic.design.theme.AeTheme
import com.aemusic.feature.common.UiContentState
import com.aemusic.app.AppContainer
import androidx.compose.ui.platform.LocalContext
import com.aemusic.feature.library.LibraryUiState

@AeThemePreview
@Composable
private fun AppShellPreview() {
    AeTheme { AeMusicShell(AppContainer(LocalContext.current)) }
}

@Preview(name = "Home large font", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun HomeLargeFontPreview() {
    AeTheme {
        HomeScreen(
            padding = PaddingValues(0.dp),
            track = null,
            isPlaying = false,
            favorites = emptyList(),
            recent = emptyList(),
            mostPlayed = emptyList(),
            playCounts = emptyMap(),
            trackedPlaylists = emptyList(),
            localTracks = emptyList(),
            onTrack = { _, _ -> },
            onPlayPause = {},
            onPrevious = {},
            onNext = {},
            immersiveTitles = true,
        )
    }
}

@Preview(name = "Search empty", widthDp = 360, heightDp = 800)
@Composable
private fun SearchEmptyPreview() {
    AeTheme {
        SearchScreen(
            padding = PaddingValues(0.dp), state = LibraryUiState.Empty, query = "", onQuery = {}, submittedQuery = "", onSearch = {}, history = emptyList(), onHistory = {},
            sourceOptions = emptyList(), selectedSources = emptySet(), onToggleSource = {},
            remoteResults = emptyList(), remoteLoading = false, remoteError = false,
            canLoadMore = false, onLoadMore = {},
            currentTrack = null, onTrack = { _, _ -> },
        )
    }
}

@Preview(name = "Library error", widthDp = 360, heightDp = 800)
@Composable
private fun LibraryErrorPreview() {
    AeTheme {
        LibraryScreen(
            padding = PaddingValues(0.dp),
            settingsEntry = false,
            immersiveTitles = true,
            onSettings = {},
            state = LibraryUiState.Error("Preview error"),
            onRequestPermission = {},
            onRetry = {},
            currentTrack = null,
            onPlayTrack = { _, _ -> },
            onToggleFavorite = {},
            userData = com.aemusic.feature.library.LibraryUserData(
                emptySet(),
                emptyList(),
                emptyList(),
                com.aemusic.feature.library.LibraryPlaylistActions({}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, { _, _ -> }),
            ),
        )
    }
}
