package com.aemusic.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.Track
import com.aemusic.core.database.TrackKey
import com.aemusic.core.database.key
import com.aemusic.design.component.AeArtwork
import com.aemusic.design.component.AeSectionHeader
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.LocalAeCanvasPalette
import com.aemusic.feature.common.artistLabel
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.feature.settings.localized
import com.aemusic.provider.netease.OnlinePlaylist
import com.aemusic.provider.netease.OnlineToplist

data class HomeShortcutItem(
    val key: String,
    val title: String,
    val subtitle: String = "",
    val artwork: ArtworkRef = ArtworkRef.Missing,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val onClick: () -> Unit,
)

data class HomeShortcutCandidate(
    val key: String,
    val title: String,
    val subtitle: String = "",
)

@Composable
fun HomeScreen(
    padding: PaddingValues,
    track: Track?,
    isPlaying: Boolean,
    favorites: List<Track>,
    recent: List<Track>,
    mostPlayed: List<Track> = emptyList(),
    playCounts: Map<TrackKey, Long> = emptyMap(),
    trackedPlaylists: List<com.aemusic.core.data.TrackedPlaylist> = emptyList(),
    localTracks: List<Track>,
    onTrack: (List<Track>, Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    immersiveTitles: Boolean,
    onAddTrackedPlaylist: () -> Unit = {},
    onOpenTrackedPlaylist: (com.aemusic.core.data.TrackedPlaylist) -> Unit = {},
    onRemoveTrackedPlaylist: (String) -> Unit = {},
    savedShortcutKeys: List<String> = emptyList(),
    onSaveShortcuts: (List<String>) -> Unit = {},
    dailyRecommendations: List<Track> = emptyList(),
    onPlayDaily: () -> Unit = {},
    onPlayFm: () -> Unit = {},
) {
    val context = LocalContext.current
    val canvasPalette = LocalAeCanvasPalette.current
    var showManageShortcutsDialog by remember { mutableStateOf(false) }

    val likedSongsText = localized("Liked songs", "收藏歌曲")
    val daily30Text = localized("Daily 30", "今日推荐 30 首")
    val dailyRecText = localized("Daily recommendations", "每日推荐")
    val personalFmText = localized("Personal FM", "私人漫游 FM")
    val personalRadarText = localized("Personal radar", "私人漫游雷达")
    val personalizedText = localized("Personalized", "每日专属")
    val radarText = localized("Radar", "私人漫游")

    val allCandidates = remember(favorites.size, dailyRecommendations.size, trackedPlaylists, likedSongsText, daily30Text, dailyRecText, personalFmText, personalRadarText) {
        buildList {
            add(HomeShortcutCandidate("system:favorites", likedSongsText, "${favorites.size} 首"))
            add(HomeShortcutCandidate("system:daily", daily30Text, dailyRecText))
            add(HomeShortcutCandidate("system:fm", personalFmText, personalRadarText))
            trackedPlaylists.forEach { p ->
                add(HomeShortcutCandidate("tracked:${p.id}", p.title, "${if (p.source == "netease") "网易云" else "哔哩哔哩"} · ${p.trackCount} 首"))
            }
        }
    }

    val activeKeys = remember(savedShortcutKeys, allCandidates) {
        val validSaved = savedShortcutKeys.filter { key -> allCandidates.any { it.key == key } }
        val remaining = allCandidates.map { it.key }.filterNot { validSaved.contains(it) }
        (validSaved + remaining).take(6)
    }

    val activeShortcuts = remember(activeKeys, favorites, dailyRecommendations, trackedPlaylists, likedSongsText, daily30Text, personalizedText, personalFmText, radarText) {
        activeKeys.mapNotNull { key ->
            when {
                key == "system:favorites" -> HomeShortcutItem(
                    key = key,
                    title = likedSongsText,
                    subtitle = "${favorites.size} 首",
                    artwork = favorites.firstOrNull()?.artwork ?: ArtworkRef.Missing,
                    icon = AeIcons.Favorite,
                    onClick = { if (favorites.isNotEmpty()) onTrack(favorites, 0) },
                )
                key == "system:daily" -> HomeShortcutItem(
                    key = key,
                    title = daily30Text,
                    subtitle = personalizedText,
                    artwork = dailyRecommendations.firstOrNull()?.artwork ?: ArtworkRef.Missing,
                    icon = AeIcons.Music,
                    onClick = onPlayDaily,
                )
                key == "system:fm" -> HomeShortcutItem(
                    key = key,
                    title = personalFmText,
                    subtitle = radarText,
                    artwork = ArtworkRef.Missing,
                    icon = AeIcons.Play,
                    onClick = onPlayFm,
                )
                key.startsWith("tracked:") -> {
                    val tid = key.removePrefix("tracked:")
                    val tracked = trackedPlaylists.firstOrNull { it.id == tid }
                    tracked?.let {
                        HomeShortcutItem(
                            key = key,
                            title = it.title,
                            subtitle = "${if (it.source == "netease") "网易云" else "B站"} · ${it.trackCount}首",
                            artwork = it.coverUrl?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing,
                            onClick = { onOpenTrackedPlaylist(it) },
                        )
                    }
                }
                else -> null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = pagePadding(padding), verticalArrangement = Arrangement.spacedBy(AeSpacing.lg)) {
            item {
                Column(Modifier.statusBarsPadding()) {
                    if (immersiveTitles) Text(localized("Your music", "你的音乐"), color = canvasPalette.contentVariant, style = MaterialTheme.typography.titleMedium)
                    Text(localized("Home", "首页"), style = if (immersiveTitles) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            item { PlayingHero(track, isPlaying, onPlayPause, onPrevious, onNext) }

            // 1. 首页固定 2x3 六格快捷歌单
            item {
                HomeShortcutsGrid(
                    shortcuts = activeShortcuts,
                    onManage = { showManageShortcutsDialog = true },
                )
            }

            // 2. 个人收听排行榜 (从高到低排序)
            if (mostPlayed.isNotEmpty()) {
                item {
                    MostPlayedSection(
                        tracks = mostPlayed,
                        playCounts = playCounts,
                        onTrack = onTrack,
                    )
                }
            }

            // 3. 最近播放
            if (recent.isNotEmpty()) item { TrackSection(localized("Recently played", "最近播放"), recent.take(6), recent, onTrack) }

            // 4. 我的收藏
            if (favorites.isNotEmpty()) item { TrackSection(localized("Liked songs", "收藏歌曲"), favorites.take(6), favorites, onTrack) }

            // 5. 本地曲库
            if (localTracks.isNotEmpty()) item { TrackSection(localized("Local library", "本地曲库"), localTracks.take(6), localTracks, onTrack) }
        }

        if (showManageShortcutsDialog) {
            ManageHomeShortcutsDialog(
                allCandidates = allCandidates,
                initiallySelectedKeys = activeKeys,
                onDismiss = { showManageShortcutsDialog = false },
                onSave = { selected ->
                    onSaveShortcuts(selected)
                    showManageShortcutsDialog = false
                },
                onAddTracked = {
                    showManageShortcutsDialog = false
                    onAddTrackedPlaylist()
                },
            )
        }
    }
}

@Composable
private fun HomeShortcutsGrid(
    shortcuts: List<HomeShortcutItem>,
    onManage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AeSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AeSectionHeader(title = localized("Quick Shortcuts", "快捷歌单"))
            TextButton(onClick = onManage) {
                Icon(AeIcons.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(localized("Customize", "管理"))
            }
        }

        // 行 1 (3 列)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
        ) {
            for (i in 0 until 3) {
                val item = shortcuts.getOrNull(i)
                if (item != null) {
                    ShortcutTile(item = item, modifier = Modifier.weight(1f))
                } else {
                    EmptyShortcutTile(modifier = Modifier.weight(1f), onClick = onManage)
                }
            }
        }

        // 行 2 (3 列)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
        ) {
            for (i in 3 until 6) {
                val item = shortcuts.getOrNull(i)
                if (item != null) {
                    ShortcutTile(item = item, modifier = Modifier.weight(1f))
                } else {
                    EmptyShortcutTile(modifier = Modifier.weight(1f), onClick = onManage)
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    item: HomeShortcutItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AeSurface(
        role = AeSurfaceRole.Raised,
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = item.onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (item.artwork != ArtworkRef.Missing) {
                AeArtwork(
                    painter = rememberLocalArtworkPainter(context, item.artwork),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon ?: AeIcons.Music,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                        ),
                    )
                    .padding(start = 8.dp, top = 20.dp, end = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyShortcutTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AeSurface(
        role = AeSurfaceRole.Raised,
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AeSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = AeIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = localized("Add", "添加快捷"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ManageHomeShortcutsDialog(
    allCandidates: List<HomeShortcutCandidate>,
    initiallySelectedKeys: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    onAddTracked: () -> Unit,
) {
    var selectedKeys by remember { mutableStateOf(initiallySelectedKeys.take(6).toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized("Manage 6 Shortcuts", "自定义首页 6 个快捷歌单")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                Text(
                    localized("Select up to 6 playlists for the Home 2x3 cockpit (${selectedKeys.size}/6):", "最多勾选 6 个置顶于首页控制台（已选 ${selectedKeys.size}/6）："),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(AeSpacing.xs))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(allCandidates, key = { it.key }) { candidate ->
                        val isChecked = selectedKeys.contains(candidate.key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        selectedKeys = selectedKeys - candidate.key
                                    } else if (selectedKeys.size < 6) {
                                        selectedKeys = selectedKeys + candidate.key
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked && selectedKeys.size < 6) {
                                        selectedKeys = selectedKeys + candidate.key
                                    } else if (!checked) {
                                        selectedKeys = selectedKeys - candidate.key
                                    }
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(candidate.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (candidate.subtitle.isNotBlank()) {
                                    Text(candidate.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(AeSpacing.xs))
                TextButton(
                    onClick = onAddTracked,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(AeIcons.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(localized("Track new link...", "追踪新链接/ID..."))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedKeys.toList()) }) {
                Text(localized("Save (${selectedKeys.size}/6)", "保存 (${selectedKeys.size}/6)"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(localized("Cancel", "取消"))
            }
        },
    )
}

@Composable
private fun MostPlayedSection(
    tracks: List<Track>,
    playCounts: Map<TrackKey, Long>,
    onTrack: (List<Track>, Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
        AeSectionHeader(title = localized("Listening Top Chart", "收听排行榜"))
        tracks.take(10).forEachIndexed { index, item ->
            val count = playCounts[item.key()] ?: 0L
            val rankColor = when (index) {
                0 -> androidx.compose.ui.graphics.Color(0xFFFFD700)
                1 -> androidx.compose.ui.graphics.Color(0xFFC0C0C0)
                2 -> androidx.compose.ui.graphics.Color(0xFFCD7F32)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            }
            AeTrackRow(
                title = item.title,
                metadata = item.artistLabel(localized("Unknown artist", "未知艺人")),
                modifier = Modifier.clickable { onTrack(tracks, index) },
                artwork = rememberLocalArtworkPainter(LocalContext.current, item.artwork),
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                    ) {
                        if (count > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            ) {
                                Text(
                                    text = localized("$count plays", "常听 $count 次"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = rankColor,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun DailyRecommendationCard(
    tracks: List<Track>,
    onPlayAll: () -> Unit,
) {
    val context = LocalContext.current
    AeSurface(AeSurfaceRole.Raised, Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(AeSpacing.md)
                .clickable { onPlayAll() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeSpacing.md),
        ) {
            val firstArtwork = tracks.firstOrNull()?.artwork ?: ArtworkRef.Missing
            AeArtwork(
                painter = rememberLocalArtworkPainter(context, firstArtwork),
                contentDescription = "Daily Recommendation",
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = localized("DAILY 30", "每日专属"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = localized("Daily Recommendations", "今日推荐 30 首"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = localized("Tailored for your music taste", "每日根据听歌口味个性化生成"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPlayAll) {
                Icon(
                    imageVector = AeIcons.Play,
                    contentDescription = localized("Play All", "播放全部"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun ToplistCard(
    toplist: OnlineToplist,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(AeSpacing.xs),
    ) {
        Box {
            AeArtwork(
                painter = rememberLocalArtworkPainter(context, ArtworkRef.Reference(toplist.coverUrl)),
                contentDescription = toplist.name,
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            if (toplist.updateFrequency.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = toplist.updateFrequency,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Text(
            text = toplist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val playText = formatPlayCount(toplist.playCount)
        if (playText.isNotBlank()) {
            Text(
                text = playText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: OnlinePlaylist,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(AeSpacing.xs),
    ) {
        Box {
            AeArtwork(
                painter = rememberLocalArtworkPainter(context, ArtworkRef.Reference(playlist.coverUrl)),
                contentDescription = playlist.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            val playText = formatPlayCount(playlist.playCount)
            if (playText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = playText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
        )
        if (playlist.creator.isNotBlank()) {
            Text(
                text = playlist.creator,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackSection(title: String, visibleTracks: List<Track>, playbackQueue: List<Track>, onTrack: (List<Track>, Int) -> Unit) {
    Column {
        AeSectionHeader(title)
        visibleTracks.forEach { item ->
            AeTrackRow(
                title = item.title,
                metadata = item.artistLabel(localized("Unknown artist", "未知艺人")),
                modifier = Modifier.clickable {
                    val index = playbackQueue.indexOfFirst { it.id == item.id && it.sourceId == item.sourceId }
                    if (index >= 0) onTrack(playbackQueue, index)
                },
                artwork = rememberLocalArtworkPainter(LocalContext.current, item.artwork),
            )
        }
    }
}

@Composable
private fun PlayingHero(track: Track?, isPlaying: Boolean, onPlayPause: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    AeSurface(AeSurfaceRole.Raised, Modifier.fillMaxWidth()) {
        Row(Modifier.padding(AeSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
            AeArtwork(track?.let { rememberLocalArtworkPainter(LocalContext.current, it.artwork) }, track?.title, Modifier.size(112.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AeSpacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(localized("NOW PLAYING", "正在播放"), color = LocalContentColor.current.copy(alpha = 0.74f), style = MaterialTheme.typography.labelMedium)
                Text(track?.title ?: localized("Choose something to play", "选择一首歌曲开始播放"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(track?.artistLabel(localized("Unknown artist", "未知艺人")) ?: localized("Open Library to get started", "请前往曲库选择歌曲"), color = LocalContentColor.current.copy(alpha = 0.74f), textAlign = TextAlign.Center)
                if (track != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) { Icon(AeIcons.Previous, localized("Previous", "上一曲")) }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, onClick = onPlayPause) { Icon(if (isPlaying) AeIcons.Pause else AeIcons.Play, if (isPlaying) localized("Pause", "暂停") else localized("Play", "播放"), Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                    IconButton(onClick = onNext) { Icon(AeIcons.Next, localized("Next", "下一曲")) }
                }
            }
        }
    }
}

private fun formatPlayCount(count: Long): String {
    if (count <= 0) return ""
    return if (count >= 100_000_000) {
        "%.1f亿".format(count / 100_000_000.0)
    } else if (count >= 10_000) {
        "%.1f万".format(count / 10_000.0)
    } else {
        "$count"
    }
}

private fun pagePadding(padding: PaddingValues) = PaddingValues(start = AeSpacing.md, top = padding.calculateTopPadding() + 24.dp, end = AeSpacing.md, bottom = padding.calculateBottomPadding() + AeSpacing.lg)
