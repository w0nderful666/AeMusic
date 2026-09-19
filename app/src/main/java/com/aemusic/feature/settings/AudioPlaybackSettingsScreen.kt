package com.aemusic.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aemusic.design.theme.AeSpacing

@Composable
fun AudioPlaybackSettingsScreen(
    padding: PaddingValues,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsSubpageHeader(
                title = localized("Audio & Playback", "音质与播放"),
                onBack = onBack,
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AeSpacing.md,
                    top = AeSpacing.sm,
                    end = AeSpacing.md,
                    bottom = padding.calculateBottomPadding() + AeSpacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(AeSpacing.lg),
            ) {
                // 1. 流媒体音质
                item {
                    SettingsGroup(localized("Audio Stream Quality", "音频流媒体音质")) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                        ) {
                            SettingText(
                                localized("Default stream quality", "默认流媒体音质"),
                                localized("Preferred audio stream quality for online playback", "优先请求的在线流媒体音质级别（根据音源平台 VIP/版权限制自动优雅降级）"),
                            )
                            Spacer(Modifier.height(AeSpacing.sm))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                            ) {
                                listOf(
                                    "Auto" to localized("Auto (Best)", "自动最佳"),
                                    "Standard" to localized("Standard 128k", "标准 128k"),
                                    "High" to localized("High 320k", "极高 320k"),
                                    "Lossless" to localized("Lossless FLAC", "无损 FLAC"),
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = state.defaultStreamQuality == key,
                                        onClick = { onAction(SettingsAction.SetDefaultStreamQuality(key)) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. 播放缓冲与预取
                item {
                    SettingsGroup(localized("Playback Prefetch & Buffer", "播放体验与预取")) {
                        ToggleRow(
                            title = localized("Pre-resolve next track", "下一首智能预解析"),
                            description = localized("Pre-resolve next track stream ahead of time for seamless gapless playback", "在当前歌曲播完前提前预取并解析下一首音频链接，实现秒切无缝换歌体验"),
                            checked = state.nextTrackPrefetch,
                            onChecked = { onAction(SettingsAction.SetNextTrackPrefetch(it)) },
                        )
                        GroupDivider()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                        ) {
                            SettingText(
                                localized("Bilibili parse candidate count", "B站音频流候选探测并发深度"),
                                localized("Max candidate DASH streams extracted during Bilibili resolving", "单次解析探测的候选音轨上限，更高数值提升高码率或合集命中率"),
                            )
                            Spacer(Modifier.height(AeSpacing.sm))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                            ) {
                                listOf(5, 10, 15, 25).forEach { count ->
                                    FilterChip(
                                        selected = state.bilibiliBatchCount == count,
                                        onClick = { onAction(SettingsAction.SetBilibiliBatchCount(count)) },
                                        label = { Text("$count 条候选") },
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 歌词与音源提示
                item {
                    SettingsGroup(localized("Lyrics & Source Badges", "歌词与音源辅助")) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                        ) {
                            SettingText(
                                localized("Preferred lyrics source", "默认优先歌词源"),
                                localized("Priority engine used when searching lyrics automatically", "自动匹配歌词时最优先检索的平台引擎"),
                            )
                            Spacer(Modifier.height(AeSpacing.sm))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                            ) {
                                listOf(
                                    "Auto" to localized("Auto", "自动最佳"),
                                    "Netease" to "网易云音乐",
                                    "BilibiliCc" to "B站 CC字幕",
                                    "Kugou" to "酷狗音乐",
                                    "Kuwo" to "酷我音乐",
                                    "Lrclib" to "LRCLIB",
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = state.preferredLyricsSource == key,
                                        onClick = { onAction(SettingsAction.SetPreferredLyricsSource(key)) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                        GroupDivider()
                        ToggleRow(
                            title = localized("Show source replacement badge", "显示音源替补角标"),
                            description = localized("Show which secondary provider resolved the audio when original source fails", "当歌曲触发全网轻量换源时，在标题旁直观显示替补平台小角标"),
                            checked = state.showSourceReplacementBadge,
                            onChecked = { onAction(SettingsAction.SetShowSourceReplacementBadge(it)) },
                        )
                    }
                }
            }
        }
    }
}
