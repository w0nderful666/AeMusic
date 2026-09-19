package com.aemusic.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.aemusic.design.component.AeSectionHeader
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.feature.home.HomeDiscoveryUiState
import com.aemusic.feature.settings.localized
import com.aemusic.provider.netease.OnlinePlaylist
import com.aemusic.provider.netease.OnlineToplist

@Composable
fun DailyRecommendationCard(
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
fun PersonalFmCard(
    onPlayFm: () -> Unit,
    isLoading: Boolean = false,
) {
    AeSurface(AeSurfaceRole.Raised, Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(AeSpacing.md)
                .clickable { if (!isLoading) onPlayFm() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AeSpacing.md),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AeIcons.Music,
                        contentDescription = "Personal FM",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = localized("PERSONAL FM", "私人雷达"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = localized("Personal FM", "私人漫游 FM"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = localized("Infinite stream matched to your taste", "随心听，与喜欢的音乐不期而遇"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onPlayFm) {
                    Icon(
                        imageVector = AeIcons.Play,
                        contentDescription = localized("Start FM", "开始电台"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ToplistCard(
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
fun PlaylistCard(
    playlist: OnlinePlaylist,
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
                painter = rememberLocalArtworkPainter(context, ArtworkRef.Reference(playlist.coverUrl)),
                contentDescription = playlist.name,
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            val playText = formatPlayCount(playlist.playCount)
            if (playText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = playText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun formatPlayCount(count: Long): String = when {
    count >= 100_000_000 -> "%.1f亿".format(count / 100_000_000.0)
    count >= 10_000 -> "%.1f万".format(count / 10_000.0)
    count > 0 -> count.toString()
    else -> ""
}
