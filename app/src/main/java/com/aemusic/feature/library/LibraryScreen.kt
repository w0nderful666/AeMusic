package com.aemusic.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.aemusic.core.model.Track
import com.aemusic.core.model.ArtworkRef
import com.aemusic.design.component.AeSectionHeader
import com.aemusic.design.component.AeStatePane
import com.aemusic.design.component.AeStatePaneState
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.component.AeArtwork
import com.aemusic.design.component.AeFavoriteButton
import com.aemusic.design.component.AeAlphabetFastScroller
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.LocalAeCanvasPalette
import com.aemusic.feature.settings.localized
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.core.database.TrackKey
import com.aemusic.core.database.key
import com.aemusic.core.database.LocalPlaylist
import com.aemusic.feature.playlist.PlaylistNameDialog
import com.aemusic.feature.playlist.PlaylistPicker
import kotlinx.coroutines.launch

data class LibraryPlaylistActions(
    val create: (String) -> Unit,
    val rename: (String, String) -> Unit,
    val delete: (String) -> Unit,
    val add: (String, Track) -> Unit,
    val remove: (String, Track) -> Unit,
    val createAndAdd: (String, Track) -> Unit,
)
data class LibraryUserData(
    val favoriteKeys: Set<TrackKey>,
    val savedTracks: List<Track>,
    val playlists: List<LocalPlaylist>,
    val playlistActions: LibraryPlaylistActions,
    val trackedPlaylists: List<com.aemusic.core.data.TrackedPlaylist> = emptyList(),
    val onOpenTrackedPlaylist: (com.aemusic.core.data.TrackedPlaylist) -> Unit = {},
)

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    settingsEntry: Boolean,
    immersiveTitles: Boolean,
    onSettings: () -> Unit,
    state: LibraryUiState,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    currentTrack: Track?,
    onPlayTrack: (List<Track>, Int) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    userData: LibraryUserData,
) {
    val favoriteKeys = userData.favoriteKeys
    val playlists = userData.playlists
    val playlistActions = userData.playlistActions
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val contentTracks = (state as? LibraryUiState.Content)?.tracks.orEmpty()
    val sortedTracks = remember(contentTracks) { contentTracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }) }
    val sections = remember(sortedTracks) { sortedTracks.map(::sectionFor).distinct() }
    val sectionIndexes = remember(sortedTracks) { sections.associateWith { section -> sortedTracks.indexOfFirst { sectionFor(it) == section } } }
    val currentIndex = currentTrack?.let { current -> sortedTracks.indexOfFirst { it.id == current.id && it.sourceId == current.sourceId } } ?: -1
    val tracksByKey = remember(sortedTracks, userData.savedTracks) {
        (sortedTracks + userData.savedTracks).associateBy(Track::key)
    }
    val favoriteTracks = favoriteKeys.mapNotNull(tracksByKey::get)
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var createPlaylist by remember { mutableStateOf(false) }
    var pickerTrack by remember { mutableStateOf<Track?>(null) }
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }
    val selectedTracks = when (selectedPlaylistId) {
        FAVORITES_PLAYLIST_ID -> favoriteTracks
        else -> selectedPlaylist?.trackKeys?.mapNotNull(tracksByKey::get).orEmpty()
    }
    val playlistEntries = buildList {
        add(
            LibraryPlaylistTileModel(
                id = FAVORITES_PLAYLIST_ID,
                name = localized("Liked songs", "收藏歌曲"),
                subtitle = localized("${favoriteTracks.size} songs", "${favoriteTracks.size} 首歌曲"),
                artwork = favoriteTracks.firstOrNull()?.artwork ?: ArtworkRef.Missing,
                onClick = { selectedPlaylistId = FAVORITES_PLAYLIST_ID },
            ),
        )
        playlists.forEach { playlist ->
            add(
                LibraryPlaylistTileModel(
                    id = playlist.id,
                    name = playlist.name,
                    subtitle = localized("${playlist.trackKeys.size} songs", "${playlist.trackKeys.size} 首歌曲"),
                    artwork = playlist.trackKeys.firstOrNull()?.let(tracksByKey::get)?.artwork ?: ArtworkRef.Missing,
                    onClick = { selectedPlaylistId = playlist.id },
                ),
            )
        }
        userData.trackedPlaylists.forEach { tracked ->
            add(
                LibraryPlaylistTileModel(
                    id = "tracked:${tracked.source}:${tracked.id}",
                    name = tracked.title,
                    subtitle = "${if (tracked.source == "netease") "网易云" else "哔哩哔哩"} · ${tracked.trackCount} 首",
                    artwork = tracked.coverUrl?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing,
                    onClick = { userData.onOpenTrackedPlaylist(tracked) },
                ),
            )
        }
    }
    val trackStartIndex = 5 + playlists.size
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, contentPadding = pagePadding(padding), verticalArrangement = Arrangement.spacedBy(AeSpacing.lg)) {
                item { LibraryTitle(settingsEntry, immersiveTitles, onSettings) }
                item {
                    AeSectionHeader(
                        title = localized("Playlists", "歌单"),
                        actionLabel = localized("New", "新建"),
                        onAction = { createPlaylist = true },
                    )
                }
                item { LibraryPlaylistGrid(playlistEntries) }
                item {
                    AeSectionHeader(
                        title = localized("Songs", "歌曲"),
                        actionLabel = localized("Shuffle", "随机播放"),
                        onAction = { if (sortedTracks.isNotEmpty()) onPlayTrack(sortedTracks.shuffled(), 0) },
                    )
                }
                item { LocalSourceSummary(trackCount = sortedTracks.size) }
                if (state is LibraryUiState.Loading) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(AeSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Spacer(Modifier.height(AeSpacing.sm))
                            Text(localized("Scanning device audio...", "正在扫描本机音频..."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (sortedTracks.isEmpty()) {
                    item {
                        AeStatePane(
                            state = AeStatePaneState.Empty(
                                localized("No local music found", "未找到本地音乐"),
                                localized("Add audio files to your device storage or grant permission.", "请向设备导入音频文件，或授予读取权限。"),
                            ),
                            actionLabel = localized("Grant permission", "授予权限"),
                            onAction = onRequestPermission,
                        )
                    }
                } else {
                    itemsIndexed(sortedTracks, key = { _, track -> "${track.sourceId.value}:${track.id.value}" }) { index, track ->
                        LocalTrackRow(
                            track = track,
                            isPlaying = index == currentIndex,
                            isFavorite = favoriteKeys.contains(track.key()),
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToPlaylist = { pickerTrack = track },
                            onClick = { onPlayTrack(sortedTracks, index) },
                        )
                    }
                }
            }
            if (state is LibraryUiState.Content && sortedTracks.size >= 12) {
                AeAlphabetFastScroller(
                    sections = sections,
                    expanded = listState.isScrollInProgress,
                    onSection = { section -> sectionIndexes[section]?.let { index -> scope.launch { listState.scrollToItem(trackStartIndex + index) } } },
                    modifier = Modifier.matchParentSize().padding(top = padding.calculateTopPadding() + 120.dp, bottom = padding.calculateBottomPadding() + 40.dp),
                )
            }
        }

        com.aemusic.design.component.PredictiveBackContainer(
            visible = selectedPlaylistId != null,
            onBack = { selectedPlaylistId = null },
        ) {
            PlaylistDetailScreen(
                padding = padding,
                name = if (selectedPlaylistId == FAVORITES_PLAYLIST_ID) localized("Liked songs", "收藏歌曲") else selectedPlaylist?.name.orEmpty(),
                tracks = selectedTracks,
                editable = selectedPlaylist != null,
                currentTrack = currentTrack,
                onBack = { selectedPlaylistId = null },
                onPlayTrack = onPlayTrack,
                onRename = { name -> selectedPlaylist?.let { playlistActions.rename(it.id, name) } },
                onDelete = { selectedPlaylist?.let { playlistActions.delete(it.id) }; selectedPlaylistId = null },
                onRemove = { track -> selectedPlaylist?.let { playlistActions.remove(it.id, track) } },
            )
        }
    }
    if (createPlaylist) PlaylistNameDialog(localized("New playlist", "新建歌单"), "", { createPlaylist = false }) {
        playlistActions.create(it); createPlaylist = false
    }
    pickerTrack?.let { track ->
        PlaylistPicker(
            playlists = playlists,
            onDismiss = { pickerTrack = null },
            onAdd = { playlistActions.add(it, track) },
            onCreateAndAdd = { playlistActions.createAndAdd(it, track) },
        )
    }
}

internal fun sectionFor(track: Track): Char = track.title.trim().firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

@Composable
private fun LibraryTitle(settingsEntry: Boolean, immersiveTitles: Boolean, onSettings: () -> Unit) {
    Column(Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Library", "曲库"), Modifier.weight(1f), style = if (immersiveTitles) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (settingsEntry) IconButton(onClick = onSettings) { Icon(AeIcons.Settings, localized("Settings", "设置")) }
        }
        if (immersiveTitles) Text(localized("Your music, across every source", "汇集每一个来源的音乐"), color = LocalAeCanvasPalette.current.contentVariant)
    }
}

@Composable
private fun LocalSourceSummary(trackCount: Int) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface) {
        Row(Modifier.padding(AeSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
            Icon(AeIcons.Library, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(localized("Local music", "本地音乐"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(localized("$trackCount songs on this device", "本机共有 $trackCount 首歌曲"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(localized("Connected", "已连接"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LocalTrackRow(track: Track, isPlaying: Boolean, isFavorite: Boolean, onToggleFavorite: () -> Unit, onAddToPlaylist: () -> Unit, onClick: () -> Unit) {
    val artwork = rememberLocalArtworkPainter(LocalContext.current, track.artwork)
    val artist = track.artists.joinToString { it.name }.ifBlank { localized("Unknown artist", "未知艺人") }
    val metadata = listOfNotNull(artist, track.album?.title, formatDuration(track.duration.milliseconds)).joinToString(" · ")
    AeTrackRow(title = track.title, metadata = metadata, modifier = Modifier.clickable(onClick = onClick), artwork = artwork, isPlaying = isPlaying, trailingContent = {
        AeFavoriteButton(isFavorite, localized("Favorite", "收藏"), localized("Add to playlist", "添加到歌单"), onToggleFavorite, onAddToPlaylist)
    })
}

private data class LibraryPlaylistTileModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val artwork: ArtworkRef,
    val onClick: () -> Unit,
)

@Composable
private fun LibraryPlaylistGrid(entries: List<LibraryPlaylistTileModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
        entries.chunked(2).forEach { rowEntries ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                rowEntries.forEach { entry -> LibraryPlaylistTile(entry, Modifier.weight(1f)) }
                if (rowEntries.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryPlaylistTile(entry: LibraryPlaylistTileModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.aspectRatio(1f).clickable(onClick = entry.onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(Modifier.fillMaxSize()) {
            AeArtwork(
                painter = rememberLocalArtworkPainter(context, entry.artwork),
                contentDescription = entry.name,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f))))
                    .padding(start = AeSpacing.sm, top = AeSpacing.xl, end = AeSpacing.sm, bottom = AeSpacing.sm),
            ) {
                Text(entry.name, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(entry.subtitle, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlaylistDetailScreen(
    padding: PaddingValues,
    name: String,
    tracks: List<Track>,
    editable: Boolean,
    currentTrack: Track?,
    onBack: () -> Unit,
    onPlayTrack: (List<Track>, Int) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemove: (Track) -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = pagePadding(padding), verticalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
        item {
            Row(Modifier.statusBarsPadding().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(AeIcons.Down, localized("Back", "返回")) }
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (tracks.isNotEmpty()) IconButton(onClick = { onPlayTrack(tracks, 0) }) { Icon(AeIcons.Play, localized("Play playlist", "播放歌单")) }
                if (editable) IconButton(onClick = { renaming = true }) { Icon(AeIcons.More, localized("Playlist actions", "歌单操作")) }
                if (editable) IconButton(onClick = { deleting = true }) { Icon(AeIcons.Delete, localized("Delete playlist", "删除歌单")) }
            }
        }
        if (tracks.isEmpty()) item { AeStatePane(AeStatePaneState.Empty(localized("Empty playlist", "空歌单"), localized("Long-press a heart to add songs.", "长按红心即可添加歌曲。"))) }
        items(tracks, key = { "playlist:${it.sourceId.value}:${it.id.value}" }) { track ->
            AeTrackRow(
                title = track.title,
                metadata = track.artists.joinToString { it.name }.ifBlank { localized("Unknown artist", "未知艺人") },
                modifier = Modifier.clickable { onPlayTrack(tracks, tracks.indexOf(track)) },
                artwork = rememberLocalArtworkPainter(LocalContext.current, track.artwork),
                isPlaying = currentTrack?.key() == track.key(),
                trailingContent = if (editable) {{ IconButton(onClick = { onRemove(track) }) { Icon(AeIcons.Delete, localized("Remove from playlist", "从歌单移除")) } }} else null,
            )
        }
    }
    if (renaming) PlaylistNameDialog(localized("Rename playlist", "重命名歌单"), name, { renaming = false }) { onRename(it); renaming = false }
    if (deleting) androidx.compose.material3.AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text(localized("Delete playlist?", "删除歌单？")) },
        text = { Text(localized("Songs will stay in your library.", "歌曲仍会保留在曲库中。")) },
        confirmButton = { TextButton(onClick = onDelete) { Text(localized("Delete", "删除")) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(localized("Cancel", "取消")) } },
    )
}

private const val FAVORITES_PLAYLIST_ID = "system:favorites"

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun pagePadding(padding: PaddingValues) = PaddingValues(start = AeSpacing.md, top = padding.calculateTopPadding() + 20.dp, end = AeSpacing.md, bottom = (padding.calculateBottomPadding() + 80.dp).coerceAtLeast(190.dp))
