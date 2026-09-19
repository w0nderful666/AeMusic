package com.aemusic.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aemusic.core.model.Track
import com.aemusic.core.database.key
import com.aemusic.design.component.AeStatePane
import com.aemusic.design.component.AeStatePaneState
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.common.artistLabel
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.feature.library.LibraryUiState
import com.aemusic.feature.settings.localized

@Composable
fun SearchScreen(
    padding: PaddingValues,
    state: LibraryUiState,
    query: String,
    onQuery: (String) -> Unit,
    submittedQuery: String,
    onSearch: () -> Unit,
    history: List<String>,
    onHistory: (String) -> Unit,
    sourceOptions: List<SearchSourceOption>,
    selectedSources: Set<String>,
    onToggleSource: (String) -> Unit,
    remoteResults: List<Track>,
    remoteLoading: Boolean,
    remoteError: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    currentTrack: Track?,
    onTrack: (List<Track>, Int) -> Unit,
    discoveryState: com.aemusic.feature.home.HomeDiscoveryUiState = com.aemusic.feature.home.HomeDiscoveryUiState(),
    onCategorySelect: (String) -> Unit = {},
    onOpenPlaylist: (String) -> Unit = {},
    onPlayDaily: (List<Track>) -> Unit = {},
    onPlayFm: () -> Unit = {},
) {
    val tracks = (state as? LibraryUiState.Content)?.tracks.orEmpty()
    val localResults = remember(tracks, submittedQuery, selectedSources) {
        if ("local" !in selectedSources) emptyList() else
        if (submittedQuery.isBlank()) emptyList() else tracks.filter { track ->
            track.title.contains(submittedQuery, true) || track.artists.any { it.name.contains(submittedQuery, true) } || track.album?.title?.contains(submittedQuery, true) == true
        }
    }
    LazyColumn(contentPadding = pagePadding(padding), verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
        item { Text(localized("Search", "搜索"), Modifier.statusBarsPadding(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold) }
        item {
            OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.extraLarge, leadingIcon = { Icon(AeIcons.Search, null) }, trailingIcon = { IconButton(onClick = onSearch) { Icon(AeIcons.ChevronRight, localized("Search", "开始搜索")) } }, placeholder = { Text(localized("Search songs, artists and albums", "搜索歌曲、艺人和专辑")) })
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                sourceOptions.forEach { source ->
                    FilterChip(
                        selected = source.id in selectedSources,
                        onClick = { onToggleSource(source.id) },
                        label = { Text(localized(source.englishName, source.chineseName)) },
                    )
                }
            }
        }
        if (submittedQuery.isBlank() && history.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                Text(localized("Recent searches", "搜索历史"), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                    history.forEach { term -> FilterChip(selected = false, onClick = { onHistory(term) }, label = { Text(term) }) }
                }
            }
        }
        when {
            state == LibraryUiState.Loading -> item { AeStatePane(AeStatePaneState.Loading(localized("Loading local music", "正在加载本地音乐"))) }
            state == LibraryUiState.PermissionRequired -> item { AeStatePane(AeStatePaneState.Empty(localized("Music access required", "需要音乐访问权限"), localized("Open Library to grant access.", "请前往曲库授予权限。"))) }
            state is LibraryUiState.Error -> item { AeStatePane(AeStatePaneState.Error(localized("Search unavailable", "搜索不可用"), state.message)) }
            submittedQuery.isBlank() -> {
                // 1. 每日推荐 30 首
                if (discoveryState.dailyRecommendations.isNotEmpty()) {
                    item {
                        DailyRecommendationCard(
                            tracks = discoveryState.dailyRecommendations,
                            onPlayAll = { onPlayDaily(discoveryState.dailyRecommendations) },
                        )
                    }
                }

                // 2. 私人漫游 FM (网易云个人推荐流)
                item {
                    PersonalFmCard(
                        onPlayFm = onPlayFm,
                        isLoading = discoveryState.isFmLoading,
                    )
                }

                // 3. 权威排行榜 (63 官方榜单)
                if (discoveryState.toplists.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                            com.aemusic.design.component.AeSectionHeader(title = localized("Official Toplists", "权威排行榜"))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.md),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                            ) {
                                items(discoveryState.toplists, key = { it.id }) { toplist ->
                                    ToplistCard(toplist = toplist, onClick = { onOpenPlaylist(toplist.id) })
                                }
                            }
                        }
                    }
                }

                // 3. 歌单广场 (14 分类)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                        com.aemusic.design.component.AeSectionHeader(title = localized("Playlist Square", "歌单广场"))
                        if (discoveryState.categories.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                            ) {
                                items(discoveryState.categories) { cat ->
                                    val selected = cat == discoveryState.selectedCategory
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onCategorySelect(cat) },
                                        label = { Text(cat, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    )
                                }
                            }
                        }

                        if (discoveryState.isPlaylistsLoading && discoveryState.playlists.isEmpty()) {
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        } else if (discoveryState.playlists.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.md),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                            ) {
                                items(discoveryState.playlists, key = { it.id }) { playlist ->
                                    PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                                }
                            }
                        }
                    }
                }
            }
            localResults.isEmpty() && remoteResults.isEmpty() && !remoteLoading -> item { AeStatePane(AeStatePaneState.Empty(localized("No results", "没有搜索结果"), localized("Try another title, artist, album or source.", "请尝试其他歌曲、艺人、专辑或来源。"))) }
            else -> {
                val results = (localResults + remoteResults).distinctBy(Track::key)
                items(results, key = { "${it.sourceId.value}:${it.id.value}" }) { track ->
                    AeTrackRow(title = track.title, metadata = listOfNotNull(track.artistLabel(localized("Unknown artist", "未知艺人")), sourceOptions.firstOrNull { it.id == track.sourceId.value }?.let { localized(it.englishName, it.chineseName) }).joinToString(" · "), modifier = Modifier.clickable { onTrack(results, results.indexOf(track)) }, artwork = rememberLocalArtworkPainter(LocalContext.current, track.artwork), isPlaying = currentTrack?.id == track.id && currentTrack.sourceId == track.sourceId)
                }
            }
        }
        if (remoteLoading) item { CircularProgressIndicator() }
        if (canLoadMore && !remoteLoading) item {
            OutlinedButton(
                onClick = onLoadMore,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(localized("Show more results", "显示更多结果"))
            }
        }
        if (remoteError) item { Text(localized("Some online sources could not be reached.", "部分在线来源暂时无法连接。"), color = MaterialTheme.colorScheme.error) }
    }
}

private fun pagePadding(padding: PaddingValues) = PaddingValues(start = AeSpacing.md, top = padding.calculateTopPadding() + 20.dp, end = AeSpacing.md, bottom = padding.calculateBottomPadding() + AeSpacing.lg)
