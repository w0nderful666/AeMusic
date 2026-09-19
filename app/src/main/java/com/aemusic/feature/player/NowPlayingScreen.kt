package com.aemusic.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aemusic.core.model.Track
import com.aemusic.design.component.AeArtwork
import com.aemusic.design.component.AeStatePane
import com.aemusic.design.component.AeStatePaneState
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.component.AeFavoriteButton
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeMotion
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.common.artistLabel
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.feature.common.trackPalette
import com.aemusic.feature.settings.localized
import com.aemusic.playback.AeRepeatMode
import com.aemusic.playback.PlaybackUiState
import com.aemusic.feature.playlist.PlaylistPicker
import com.aemusic.feature.playlist.PlaylistPickerModel
import com.aemusic.provider.lyrics.LyricsRepository
import com.aemusic.provider.lyrics.LyricsSource
import com.aemusic.provider.lyrics.LyricsCandidate
import com.aemusic.provider.lyrics.parseTimedLyrics
import com.aemusic.provider.StreamQuality
import com.aemusic.provider.SourceSelectionEngine
import com.aemusic.provider.SourceCandidate
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.ProviderResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow

private enum class PlayerSheet { Lyrics, Queue, Source, More }

data class NowPlayingActions(
    val back: () -> Unit,
    val playPause: () -> Unit,
    val previous: () -> Unit,
    val next: () -> Unit,
    val selectTrack: (Int) -> Unit,
    val seek: (Long) -> Unit,
    val shuffle: (Boolean) -> Unit,
    val cycleRepeat: () -> Unit,
    val retry: () -> Unit,
    val removeFromQueue: (Int) -> Unit,
    val changeQuality: (StreamQuality) -> Unit,
    val replaceCurrentTrack: (Track) -> Unit,
    val playQueue: (List<Track>) -> Unit,
    val setSleepTimer: (com.aemusic.playback.SleepTimerMode) -> Unit = {},
    val setSoftwareVolume: (Float) -> Unit = {},
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
fun NowPlayingScreen(
    state: PlaybackUiState,
    artworkColors: List<Color>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    actions: NowPlayingActions,
    playlistPicker: PlaylistPickerModel,
    lyricsRepository: LyricsRepository,
    sourceSelectionEngine: SourceSelectionEngine,
    bilibiliProvider: BilibiliProvider,
    showSourceReplacementBadge: Boolean = true,
) {
    val track = state.currentTrack
    if (track == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding(), verticalArrangement = Arrangement.Center) {
            AeStatePane(AeStatePaneState.Empty(localized("Nothing playing", "当前没有播放"), localized("Choose a song from your Library.", "请从曲库中选择一首歌曲。")), actionLabel = localized("Close", "关闭"), onAction = actions.back)
        }
        return
    }
    var activeSheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val pager = rememberPagerState(initialPage = state.currentIndex.coerceAtLeast(0), pageCount = { state.queue.size })
    val colors = artworkColors
    val backgroundTop by animateColorAsState(colors.first(), tween(AeMotion.ColorTransitionMillis), label = "player-background-top")
    val backgroundMiddle by animateColorAsState(colors.last(), tween(AeMotion.ColorTransitionMillis), label = "player-background-middle")
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex >= 0 && pager.settledPage != state.currentIndex) pager.animateScrollToPage(state.currentIndex)
    }
    LaunchedEffect(pager.settledPage) {
        if (pager.settledPage != state.currentIndex) actions.selectTrack(pager.settledPage)
    }
    LaunchedEffect(state.positionMs, state.durationMs, dragging) {
        if (!dragging) sliderValue = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(backgroundTop, backgroundMiddle, MaterialTheme.colorScheme.background)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(AeSpacing.lg), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.back) { Icon(AeIcons.Down, localized("Close player", "关闭播放器"), tint = Color.White) }
                Text(localized("NOW PLAYING", "正在播放"), Modifier.weight(1f), color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { activeSheet = PlayerSheet.More }) { Icon(AeIcons.More, localized("More actions", "更多操作"), tint = Color.White) }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), key = { "${state.queue[it].sourceId.value}:${state.queue[it].id.value}" }) { page ->
                val pageTrack = state.queue[page]
                AeArtwork(rememberLocalArtworkPainter(LocalContext.current, pageTrack.artwork), pageTrack.title, Modifier.fillMaxWidth())
            }
            Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Spacer(Modifier.width(48.dp))
                    Column(Modifier.weight(1f).padding(horizontal = AeSpacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(track.title, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(track.artistLabel(localized("Unknown artist", "未知艺人")), color = Color.White.copy(.76f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (showSourceReplacementBadge) {
                            state.technicalInfo?.replacementSource?.let { replacement ->
                                Text(
                                    localized("Playing via $replacement", "已切换至 $replacement 音源"),
                                    color = Color.White.copy(alpha = 0.82f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    AeFavoriteButton(
                        isFavorite = isFavorite,
                        contentDescription = localized("Favorite", "收藏"),
                        addToPlaylistDescription = localized("Add to playlist", "添加到歌单"),
                        onClick = onToggleFavorite,
                        onLongClick = { showPlaylistPicker = true },
                        modifier = Modifier.size(48.dp),
                        selectedColor = Color(0xFF7BF0A2),
                        unselectedColor = Color.White,
                    )
                }
                Slider(
                    value = sliderValue.coerceIn(0f, 1f),
                    onValueChange = { dragging = true; sliderValue = it },
                    onValueChangeFinished = {
                        dragging = false
                        seekPositionFromFraction(sliderValue, state.durationMs)?.let(actions.seek)
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime((sliderValue * state.durationMs).toLong()), color = Color.White.copy(.72f))
                    Text(formatTime(state.durationMs), color = Color.White.copy(.72f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { actions.shuffle(!state.shuffleEnabled) }) { Icon(AeIcons.Shuffle, localized("Shuffle", "随机播放"), tint = if (state.shuffleEnabled) Color(0xFF7BF0A2) else Color.White) }
                    IconButton(onClick = actions.previous, modifier = Modifier.size(56.dp)) { Icon(AeIcons.Previous, localized("Previous", "上一曲"), tint = Color.White, modifier = Modifier.size(32.dp)) }
                    Surface(shape = CircleShape, color = Color.White, onClick = actions.playPause) {
                        if (state.isBuffering) CircularProgressIndicator(Modifier.padding(25.dp).size(28.dp), color = Color.Black, strokeWidth = 3.dp)
                        else Icon(if (state.isPlaying) AeIcons.Pause else AeIcons.Play, if (state.isPlaying) localized("Pause", "暂停") else localized("Play", "播放"), Modifier.padding(22.dp).size(34.dp), tint = Color.Black)
                    }
                    IconButton(onClick = actions.next, modifier = Modifier.size(56.dp)) { Icon(AeIcons.Next, localized("Next", "下一曲"), tint = Color.White, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = actions.cycleRepeat) { RepeatModeIcon(state.repeatMode) }
                }
                if (state.error != null) {
                    val (errorTitle, errorDetail) = friendlyErrorMessage(state.error)
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current
                    val copiedToastText = localized("Diagnostic log copied", "已复制诊断日志到剪贴板")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth().padding(vertical = AeSpacing.xs),
                    ) {
                        Column(
                            modifier = Modifier.padding(AeSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                            ) {
                                Icon(AeIcons.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(errorTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(errorDetail, style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = AeSpacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                            ) {
                                Button(
                                    onClick = actions.retry,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(localized("Retry", "重试"))
                                }
                                if (track.sourceId.value != "local") {
                                    OutlinedButton(
                                        onClick = { activeSheet = PlayerSheet.Source },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(localized("Source", "换源"))
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val diagnosticText = buildString {
                                            appendLine("【AeMusic 播放诊断日志】")
                                            appendLine("歌曲: ${track.title}")
                                            appendLine("艺人: ${track.artistLabel("未知艺人")}")
                                            appendLine("来源: ${track.sourceId.value} (ID: ${track.id.value})")
                                            appendLine("错误代码: ${state.error}")
                                            state.technicalInfo?.let {
                                                appendLine("解析源: ${it.source}")
                                                appendLine("码率: ${it.bitrate?.let { b -> "${b / 1000} kbps" } ?: "未知"}")
                                                appendLine("媒体类型: ${it.mimeType ?: "未知"}")
                                                appendLine("替换音源: ${it.replacementSource ?: "无"}")
                                            }
                                            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                            appendLine("记录时间: $timeStr")
                                        }
                                        clipboardManager.setText(AnnotatedString(diagnosticText))
                                        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(localized("Copy Log", "复制日志"))
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PlayerUtility(localized("Lyrics", "歌词"), AeIcons.Lyrics) { activeSheet = PlayerSheet.Lyrics }
                    if (track.sourceId.value != "local") PlayerUtility(localized("Source", "播放源"), AeIcons.Search) { activeSheet = PlayerSheet.Source }
                    PlayerUtility(localized("Queue", "队列"), AeIcons.Queue) { activeSheet = PlayerSheet.Queue }
                }
            }
        }
        activeSheet?.let { sheet ->
            ModalBottomSheet(
                onDismissRequest = { activeSheet = null },
                containerColor = Color.Transparent,
                dragHandle = null,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AeSurface(
                        role = AeSurfaceRole.Floating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AeSpacing.sm),
                        ) {
                            Box(
                                Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                                    .align(Alignment.CenterHorizontally),
                            )
                            PlayerSheetContent(
                                sheet = sheet,
                                state = state,
                                actions = actions,
                                lyricsRepository = lyricsRepository,
                                sourceSelectionEngine = sourceSelectionEngine,
                                bilibiliProvider = bilibiliProvider,
                            ) { activeSheet = null }
                        }
                    }
                }
            }
        }
        if (showPlaylistPicker) PlaylistPicker(
            playlists = playlistPicker.playlists,
            onDismiss = { showPlaylistPicker = false },
            onAdd = playlistPicker.onAdd,
            onCreateAndAdd = playlistPicker.onCreateAndAdd,
        )
    }
}

@Composable
private fun PlayerUtility(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.clip(CircleShape).clickable(onClick = onClick).padding(AeSpacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) { Icon(icon, label, tint = Color.White); Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun PlayerSheetContent(sheet: PlayerSheet, state: PlaybackUiState, actions: NowPlayingActions, lyricsRepository: LyricsRepository, sourceSelectionEngine: SourceSelectionEngine, bilibiliProvider: BilibiliProvider, onDismiss: () -> Unit) {
    val track = state.currentTrack ?: return
    Column(Modifier.fillMaxWidth().padding(horizontal = AeSpacing.lg, vertical = AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
        when (sheet) {
            PlayerSheet.Lyrics -> LyricsSearchPane(track, state.positionMs, actions.seek, lyricsRepository)
            PlayerSheet.Source -> SourceSelectionPane(track, sourceSelectionEngine) { replacement ->
                actions.replaceCurrentTrack(replacement)
                onDismiss()
            }
            PlayerSheet.Queue -> {
                Text(localized("Queue", "播放队列"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 560.dp),
                    contentPadding = PaddingValues(bottom = AeSpacing.xl),
                ) {
                    itemsIndexed(state.queue, key = { index, item -> "queue:$index:${item.sourceId.value}:${item.id.value}" }) { index, item ->
                        AeTrackRow(
                            title = item.title,
                            metadata = item.artistLabel(localized("Unknown artist", "未知艺人")),
                            modifier = Modifier.clickable {
                                if (index != state.currentIndex) actions.selectTrack(index)
                                onDismiss()
                            },
                            artwork = rememberLocalArtworkPainter(LocalContext.current, item.artwork),
                            isPlaying = index == state.currentIndex,
                            trailingContent = {
                                if (index == state.currentIndex) Text(localized("Playing", "播放中"))
                                else IconButton(onClick = { actions.removeFromQueue(index) }) { Icon(AeIcons.Delete, localized("Remove from queue", "从队列移除")) }
                            },
                        )
                    }
                }
            }
            PlayerSheet.More -> {
                Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(track.artistLabel(localized("Unknown artist", "未知艺人")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                val info = state.technicalInfo
                DetailLine(localized("Source", "来源"), sourceLabel(track.sourceId.value))
                track.album?.title?.let { DetailLine(localized("Album", "专辑"), it) }
                DetailLine(localized("Duration", "时长"), formatTime(track.duration.milliseconds))
                info?.qualityLabel?.let { DetailLine(localized("Quality", "音质"), it) }
                info?.bitrate?.takeIf { it > 0 }?.let { DetailLine(localized("Source bitrate", "来源码率"), "${it / 1_000} kbps") }
                info?.codec?.let { DetailLine(localized("Codec", "编码"), it) }
                info?.mimeType?.let { DetailLine(localized("Media type", "媒体类型"), it) }
                if (track.sourceId.value != "local") {
                    Text(localized("Preferred quality", "首选音质"), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                        StreamQuality.entries.forEach { quality ->
                            FilterChip(
                                selected = state.preferredQuality == quality,
                                onClick = { actions.changeQuality(quality) },
                                label = { Text(qualityLabel(quality)) },
                            )
                        }
                    }
                    Text(localized("Changing quality re-resolves the current stream and keeps the current position when possible.", "切换音质会重新解析当前音频，并尽量保持当前进度。"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }

                Text(localized("Sleep timer", "睡眠定时关闭"), style = MaterialTheme.typography.titleMedium)
                if (state.sleepTimerMode != com.aemusic.playback.SleepTimerMode.Off) {
                    val remainingText = if (state.sleepTimerMode == com.aemusic.playback.SleepTimerMode.EndOfTrack) {
                        localized("Will pause after current track finishes", "将在当前歌曲播放完毕后自动暂停")
                    } else {
                        val minutes = state.sleepTimerRemainingSeconds / 60
                        val seconds = state.sleepTimerRemainingSeconds % 60
                        localized("Will pause in %d:%02d (with smooth fade-out)", "将在 %d 分 %02d 秒后淡出暂停").format(minutes, seconds)
                    }
                    Text(remainingText, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                    listOf(
                        com.aemusic.playback.SleepTimerMode.Off to localized("Off", "关闭"),
                        com.aemusic.playback.SleepTimerMode.Minutes15 to localized("15m", "15分钟"),
                        com.aemusic.playback.SleepTimerMode.Minutes30 to localized("30m", "30分钟"),
                        com.aemusic.playback.SleepTimerMode.Minutes45 to localized("45m", "45分钟"),
                        com.aemusic.playback.SleepTimerMode.Minutes60 to localized("60m", "60分钟"),
                        com.aemusic.playback.SleepTimerMode.Minutes90 to localized("90m", "90分钟"),
                        com.aemusic.playback.SleepTimerMode.EndOfTrack to localized("End of track", "播完本曲"),
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = state.sleepTimerMode == mode,
                            onClick = { actions.setSleepTimer(mode) },
                            label = { Text(label) },
                        )
                    }
                }

                Text(localized("Night volume attenuation", "夜间微量音量微调"), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Slider(
                        value = state.softwareVolume,
                        onValueChange = actions.setSoftwareVolume,
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${(state.softwareVolume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Text(localized("Software attenuation for ultra-quiet late-night listening even at minimum system volume.", "软件级微量音量衰减，在系统最低音量仍嫌大时实现深夜极轻柔听音。"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)

                if (track.sourceId == BilibiliProvider.SOURCE) {
                    BilibiliPartsPane(track, bilibiliProvider) { tracks ->
                        actions.playQueue(tracks)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun BilibiliPartsPane(track: Track, provider: BilibiliProvider, onPlayAll: (List<Track>) -> Unit) {
    var loading by remember(track.id) { mutableStateOf(false) }
    var parts by remember(track.id) { mutableStateOf(emptyList<Track>()) }
    var checked by remember(track.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Button(
        enabled = !loading,
        onClick = {
            loading = true
            scope.launch {
                val reference = track.playbackRef as? com.aemusic.core.model.PlaybackReference.Provider
                val result = reference?.let { provider.collection(it.mediaId) }
                parts = (result as? ProviderResult.Success)?.value?.let(provider::tracks).orEmpty()
                checked = true
                loading = false
            }
        },
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text(localized("Check video parts", "检测视频分P"))
    }
    if (checked && parts.size <= 1) Text(localized("This video has no multi-part music queue.", "当前视频没有可展开的多分P队列。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (parts.size > 1) {
        Text(localized("${parts.size} parts detected", "检测到 ${parts.size} 个分P"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Button(onClick = { onPlayAll(parts) }) { Text(localized("Play all parts", "播放全部分P")) }
        parts.take(20).forEachIndexed { index, part -> Text("P${index + 1} · ${part.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (parts.size > 20) Text(localized("${parts.size - 20} more parts will be added to the queue.", "其余 ${parts.size - 20} 个分P会一并加入队列。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SourceSelectionPane(track: Track, engine: SourceSelectionEngine, onSelect: (Track) -> Unit) {
    var query by remember(track.id, track.sourceId) { mutableStateOf(SourceSelectionEngine.defaultQuery(track)) }
    var loading by remember { mutableStateOf(false) }
    var candidates by remember(track.id, track.sourceId) { mutableStateOf(emptyList<SourceCandidate>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Text(localized("Choose playback source", "选择播放源"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(localized("Candidates are verified before they appear. The original and actual source remain visible.", "候选音源会先经过媒体探测；原始来源与实际播放来源会分别保留。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(localized("Title and artist", "歌曲名与歌手")) })
    Button(
        enabled = query.isNotBlank() && !loading,
        onClick = {
            loading = true; error = null
            scope.launch {
                try { candidates = engine.candidates(track, query) }
                catch (cause: Exception) { error = cause.message }
                finally { loading = false }
            }
        },
    ) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(localized("Find verified sources", "查找可用音源")) }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (!loading && candidates.isEmpty()) Text(localized("Search to compare playable sources.", "搜索后可比较已经验证可播放的音源。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    candidates.forEach { candidate ->
        AeTrackRow(
            title = candidate.track.title,
            metadata = listOf(candidate.track.artistLabel(localized("Unknown artist", "未知艺人")), sourceLabel(candidate.track.sourceId.value), "${candidate.matchScore}%", candidate.stream.qualityLabel).filterNotNull().joinToString(" · "),
            modifier = Modifier.clickable { onSelect(candidate.track) },
            artwork = rememberLocalArtworkPainter(LocalContext.current, candidate.track.artwork),
            trailingContent = { Text(localized("Use", "选用"), color = MaterialTheme.colorScheme.primary) },
        )
    }
}

@Composable
private fun LyricsSearchPane(track: Track, positionMs: Long, onSeek: (Long) -> Unit, repository: LyricsRepository) {
    var query by remember(track.id, track.sourceId) { mutableStateOf(track.title) }
    var source by remember { mutableStateOf(repository.preferredSource) }
    var results by remember(track.id, track.sourceId) { mutableStateOf(emptyList<LyricsCandidate>()) }
    var selected by remember(track.id, track.sourceId) { mutableStateOf<LyricsCandidate?>(null) }
    var loading by remember { mutableStateOf(false) }
    var editingSource by remember(track.id, track.sourceId) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val lyricListState = rememberLazyListState()
    val lyricListDragging by lyricListState.interactionSource.collectIsDraggedAsState()
    var followCurrentLine by remember(track.id, track.sourceId) { mutableStateOf(true) }
    var timedLines by remember(track.id, track.sourceId) { mutableStateOf(emptyList<com.aemusic.provider.lyrics.TimedLyricLine>()) }
    var plainLines by remember(track.id, track.sourceId) { mutableStateOf(emptyList<String>()) }
    val activeLine = remember(timedLines, positionMs) { timedLines.indexOfLast { it.timeMs <= positionMs } }
    LaunchedEffect(track.id, track.sourceId) {
        selected = repository.saved(track)
        if (selected == null) {
            loading = true
            try {
                results = repository.search(track, query, LyricsSource.Auto)
                results.firstOrNull { it.matchScore >= 85 }?.let { candidate ->
                    selected = candidate
                    repository.select(track, candidate)
                }
            } finally {
                loading = false
            }
        }
        editingSource = selected == null
    }
    LaunchedEffect(selected) {
        val lyrics = selected?.text.orEmpty()
        val parsed = withContext(Dispatchers.Default) {
            parseTimedLyrics(lyrics) to lyrics.lineSequence().filter(String::isNotBlank).take(500).toList()
        }
        timedLines = parsed.first
        plainLines = parsed.second
    }
    LaunchedEffect(lyricListDragging) {
        if (lyricListDragging) followCurrentLine = false
    }
    LaunchedEffect(activeLine, followCurrentLine) {
        if (activeLine >= 0 && followCurrentLine && !lyricListDragging) {
            lyricListState.animateScrollToItem(activeLine, scrollOffset = -140)
        }
    }
    Text(localized("Lyrics", "歌词"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    if (editingSource) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
            LyricsSource.entries.forEach { option ->
                FilterChip(
                    selected = source == option,
                    onClick = { source = option; repository.preferredSource = option },
                    label = { Text(when (option) {
                        LyricsSource.Auto -> localized("Auto", "自动")
                        LyricsSource.Netease -> "网易云"
                        LyricsSource.BilibiliCc -> "B站 CC"
                        LyricsSource.Lrclib -> "LRCLIB"
                        LyricsSource.Kugou -> "酷狗"
                        LyricsSource.Kuwo -> "酷我"
                    }) },
                )
            }
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(localized("Song title or keywords", "歌曲名或关键词")) })
        Button(
            enabled = !loading && query.isNotBlank(),
            onClick = {
                loading = true
                scope.launch {
                    try {
                        results = repository.search(track, query, source)
                        if (source == LyricsSource.Auto) {
                            results.firstOrNull { it.matchScore >= 85 }?.let { candidate ->
                                selected = candidate
                                repository.select(track, candidate)
                                editingSource = false
                            }
                        }
                    } finally {
                        loading = false
                    }
                }
            },
        ) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(localized("Search lyrics", "搜索歌词")) }
        results.forEach { candidate ->
            FilterChip(
                selected = selected == candidate,
                onClick = {
                    selected = candidate
                    editingSource = false
                    scope.launch { repository.select(track, candidate) }
                },
                label = {
                    val duration = candidate.durationMs?.let(::formatTime)
                    Text(listOfNotNull(candidate.title, candidate.artist, candidate.album, duration, "${candidate.matchScore}%", candidate.source.name).joinToString(" · "))
                },
            )
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val chosen = selected
            Column(Modifier.weight(1f)) {
                Text(chosen?.title ?: track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(chosen?.artist, chosen?.source?.name).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { editingSource = true }) { Text(localized("Change", "更换")) }
        }
    }
    if (selected != null) {
        if (!followCurrentLine && timedLines.isNotEmpty()) {
            Button(onClick = {
                followCurrentLine = true
                if (activeLine >= 0) scope.launch { lyricListState.animateScrollToItem(activeLine, scrollOffset = -140) }
            }) { Text(localized("Return to current lyric", "回到当前歌词")) }
        }
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 540.dp),
            state = lyricListState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 100.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.md),
        ) {
            if (timedLines.isNotEmpty()) {
                itemsIndexed(timedLines, key = { index, line -> "${line.timeMs}:$index" }) { index, line ->
                    Text(
                        line.text,
                        Modifier.fillMaxWidth().clickable {
                            followCurrentLine = true
                            onSeek(line.timeMs)
                        }.padding(horizontal = AeSpacing.sm, vertical = AeSpacing.xs),
                        color = if (index == activeLine) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f),
                        style = if (index == activeLine) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = if (index == activeLine) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                itemsIndexed(plainLines) { _, line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else if (!loading) {
        Text(localized("Search and choose a matching lyric result. The selected source is remembered.", "搜索并选择匹配的歌词，所选歌词来源会自动记忆。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun sourceLabel(id: String) = when (id) {
    "local" -> localized("Local music", "本地音乐")
    "netease" -> "网易云音乐"
    "bilibili" -> "哔哩哔哩"
    "kuwo" -> "酷我音乐"
    else -> id
}

@Composable
private fun qualityLabel(quality: StreamQuality) = when (quality) {
    StreamQuality.Auto -> localized("Auto", "自动")
    StreamQuality.Standard -> localized("Standard", "标准")
    StreamQuality.High -> localized("High", "高音质")
    StreamQuality.Lossless -> localized("Lossless", "无损优先")
}

@Composable
private fun RepeatModeIcon(mode: AeRepeatMode) {
    val tint = if (mode == AeRepeatMode.Off) Color.White else Color(0xFF7BF0A2)
    Box(contentAlignment = Alignment.Center) {
        Icon(AeIcons.Repeat, when (mode) { AeRepeatMode.Off -> localized("Repeat off", "关闭循环"); AeRepeatMode.All -> localized("Repeat all", "列表循环"); AeRepeatMode.One -> localized("Repeat one", "单曲循环") }, tint = tint)
        if (mode == AeRepeatMode.One) Text("1", color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(milliseconds: Long): String { val seconds = milliseconds.coerceAtLeast(0) / 1_000; return "%d:%02d".format(seconds / 60, seconds % 60) }

internal fun seekPositionFromFraction(fraction: Float, durationMs: Long): Long? {
    if (!fraction.isFinite() || durationMs <= 0) return null
    return (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0, durationMs)
}

@Composable
private fun friendlyErrorMessage(error: String?): Pair<String, String> {
    if (error == null) return "" to ""
    return when {
        error.contains("403") || error.contains("BAD_HTTP_STATUS") ->
            localized("Audio source access restricted (403)", "音源访问受限或鉴权失效 (403)") to
                localized("The audio stream could not be loaded. Try switching source or retry.", "当前播放链接无法访问 (HTTP 403 Forbidden)，防盗链或链接过期，可尝试换源。")
        error.contains("NETWORK") || error.contains("TIMEOUT") || error.contains("IO_") ->
            localized("Network timeout", "网络连接不稳定") to
                localized("Audio request timed out. Please check your internet connection.", "请求音频流超时，请检查网络连接后重试。")
        error.contains("PARSING") || error.contains("DECODER") || error.contains("MALFORMED") ->
            localized("Decoding error", "音频流解析失败") to
                localized("Unable to parse audio stream. The format may be unsupported.", "音频编码异常或格式不受支持。")
        error.contains("UNSUPPORTED") ->
            localized("Format unsupported", "暂不支持该格式") to
                localized("Audio format or DRM protection is not supported.", "该歌曲格式或加密方式暂时无法解码。")
        else ->
            localized("Playback exception", "播放遇到问题") to
                localized("Error code: $error", "错误代码: $error")
    }
}
