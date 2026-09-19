package com.aemusic.feature.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.Track
import com.aemusic.design.component.AeArtwork
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.LocalAeCanvasPalette
import com.aemusic.feature.common.artistLabel
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.feature.settings.localized
import com.aemusic.provider.netease.OnlinePlaylistDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    detail: OnlinePlaylistDetail?,
    isLoading: Boolean,
    error: String?,
    currentTrack: Track?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onTrackSelected: (List<Track>, Int) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
) {
    val context = LocalContext.current
    val canvasPalette = LocalAeCanvasPalette.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AeSpacing.sm, vertical = AeSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AeIcons.Back,
                        contentDescription = localized("Back", "返回"),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = detail?.name ?: localized("Playlist Detail", "歌单详情"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AeSpacing.xs),
                )
            }

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AeSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else if (detail != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 200.dp),
                ) {
                    // Header Hero Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AeSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AeSpacing.md),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val coverArt = ArtworkRef.Reference(detail.coverUrl)
                                AeArtwork(
                                    painter = rememberLocalArtworkPainter(context, coverArt),
                                    contentDescription = detail.name,
                                    modifier = Modifier
                                        .size(128.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                                ) {
                                    Text(
                                        text = detail.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (detail.creator.isNotBlank()) {
                                        Text(
                                            text = localized("By ", "创作者: ") + detail.creator,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = canvasPalette.contentVariant,
                                        )
                                    }
                                    val playCountText = formatPlayCount(detail.playCount)
                                    Text(
                                        text = localized("Tracks: ", "曲目: ") + "${detail.tracks.size}" +
                                                (if (playCountText.isNotBlank()) "  •  $playCountText" else ""),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = canvasPalette.contentVariant,
                                    )
                                }
                            }

                            if (detail.description.isNotBlank()) {
                                Text(
                                    text = detail.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = canvasPalette.contentVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Play All Action Button
                            Button(
                                onClick = { onPlayAll(detail.tracks) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape,
                                enabled = detail.tracks.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = AeIcons.Play,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = localized("Play All (${detail.tracks.size})", "播放全部 (${detail.tracks.size}首)"),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Track Items
                    itemsIndexed(detail.tracks) { index, track ->
                        val isCurrent = currentTrack?.id == track.id && currentTrack.sourceId == track.sourceId
                        AeTrackRow(
                            title = track.title,
                            metadata = track.artistLabel(localized("Unknown artist", "未知艺人")),
                            artwork = rememberLocalArtworkPainter(context, track.artwork),
                            isPlaying = isCurrent && isPlaying,
                            modifier = Modifier.clickable {
                                onTrackSelected(detail.tracks, index)
                            },
                            trailingContent = {
                                Text(
                                    text = formatDuration(track.duration.milliseconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = canvasPalette.contentVariant,
                                    modifier = Modifier.padding(end = AeSpacing.sm),
                                )
                            }
                        )
                    }
                }
            }
            }
        }
    }
}

private fun formatPlayCount(count: Long): String {
    if (count <= 0) return ""
    return if (count >= 100_000_000) {
        "%.1f亿次播放".format(count / 100_000_000.0)
    } else if (count >= 10_000) {
        "%.1f万次播放".format(count / 10_000.0)
    } else {
        "${count}次播放"
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
