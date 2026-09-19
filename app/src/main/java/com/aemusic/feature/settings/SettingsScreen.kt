package com.aemusic.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing

data class SettingsUiState(
    val fourTabs: Boolean,
    val language: AppLanguage,
    val immersiveTitles: Boolean,
    val artworkColors: Boolean,
    val artworkBackground: Boolean,
    val colorStrength: Float,
    val glassEnabled: Boolean,
    val glassClarity: Float,
    val defaultStreamQuality: String = "Auto",
    val nextTrackPrefetch: Boolean = true,
    val bilibiliBatchCount: Int = 15,
    val preferredLyricsSource: String = "Auto",
    val showSourceReplacementBadge: Boolean = true,
    val webDavServerUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val webDavPath: String = "/AeMusic/playlists.json",
    val navidromeServerUrl: String = "",
    val navidromeUsername: String = "",
    val navidromeToken: String = "",
    val cacheSizeBytes: Long = 0L,
    val trackedPlaylists: List<TrackedPlaylist> = emptyList(),
)

sealed interface SettingsAction {
    data class SetFourTabs(val enabled: Boolean) : SettingsAction
    data class SetLanguage(val language: AppLanguage) : SettingsAction
    data class SetImmersiveTitles(val enabled: Boolean) : SettingsAction
    data class SetArtworkColors(val enabled: Boolean) : SettingsAction
    data class SetArtworkBackground(val enabled: Boolean) : SettingsAction
    data class SetColorStrength(val strength: Float) : SettingsAction
    data class SetGlassEnabled(val enabled: Boolean) : SettingsAction
    data class SetGlassClarity(val clarity: Float) : SettingsAction
    data class SetDefaultStreamQuality(val quality: String) : SettingsAction
    data class SetNextTrackPrefetch(val enabled: Boolean) : SettingsAction
    data class SetBilibiliBatchCount(val count: Int) : SettingsAction
    data class SetPreferredLyricsSource(val source: String) : SettingsAction
    data class SetShowSourceReplacementBadge(val enabled: Boolean) : SettingsAction
    data class SaveWebDavConfig(val serverUrl: String, val username: String, val password: String, val path: String) : SettingsAction
    data class SaveNavidromeConfig(val serverUrl: String, val username: String, val token: String) : SettingsAction
    data object ClearPlaybackCache : SettingsAction
    data class RemoveTrackedPlaylist(val id: String) : SettingsAction
    data class MoveTrackedPlaylist(val id: String, val offset: Int) : SettingsAction
    data object OpenGlassSettings : SettingsAction
    data object OpenSources : SettingsAction
    data object OpenDeveloperOptions : SettingsAction
    data object OpenAudioSettings : SettingsAction
    data object OpenAppearanceSettings : SettingsAction
    data object OpenCloudSyncSettings : SettingsAction
    data object OpenBackupStorageSettings : SettingsAction
}

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = AeSpacing.md,
                top = padding.calculateTopPadding() + 16.dp,
                end = AeSpacing.md,
                bottom = padding.calculateBottomPadding() + AeSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.md),
        ) {
            item {
                Text(
                    text = localized("Settings", "设置"),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(vertical = AeSpacing.xs),
                    style = if (state.immersiveTitles) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 1. 音乐平台与账号
            item {
                SettingsCategoryCard(
                    icon = AeIcons.Account,
                    title = localized("Music Platforms & Accounts", "音乐平台与账号"),
                    subtitle = localized(
                        "NetEase & Bilibili account login, QR codes, and Cookie credentials",
                        "网易云音乐、哔哩哔哩账号登录与 Cookie 凭据管理",
                    ),
                    onClick = { onAction(SettingsAction.OpenSources) },
                )
            }

            // 2. 音质与播放设置
            item {
                SettingsCategoryCard(
                    icon = AeIcons.Music,
                    title = localized("Audio & Playback", "音质与播放"),
                    subtitle = localized(
                        "Stream quality, prefetch, candidate concurrency, and lyrics priority",
                        "流媒体音质 · 下一首智能预解析 · 歌词优先级 · 换源角标",
                    ),
                    tag = when (state.defaultStreamQuality) {
                        "Auto" -> localized("Auto", "自动最佳")
                        "Lossless" -> "FLAC"
                        "High" -> "320k"
                        "Standard" -> "128k"
                        else -> state.defaultStreamQuality
                    },
                    onClick = { onAction(SettingsAction.OpenAudioSettings) },
                )
            }

            // 3. 界面外观与个性化
            item {
                SettingsCategoryCard(
                    icon = AeIcons.Palette,
                    title = localized("Appearance & Visuals", "外观与个性化"),
                    subtitle = localized(
                        "Liquid glass customization, artwork color extraction, navigation, and language",
                        "液态玻璃参数定制 · 封面背景取色 · 导航栏 · 语言",
                    ),
                    tag = if (state.glassEnabled) localized("Liquid Glass", "液态玻璃") else "Material",
                    onClick = { onAction(SettingsAction.OpenAppearanceSettings) },
                )
            }

            // 4. 自建云端同步与流媒体
            item {
                SettingsCategoryCard(
                    icon = AeIcons.Cloud,
                    title = localized("Cloud Sync & Self-Hosted", "云端同步与自建流媒体"),
                    subtitle = localized(
                        "WebDAV cross-device backup and Navidrome / Subsonic private library",
                        "WebDAV 跨设备漫游备份 · Navidrome / Subsonic 私有流媒体",
                    ),
                    tag = if (state.webDavServerUrl.isNotBlank() || state.navidromeServerUrl.isNotBlank()) {
                        localized("Configured", "已配置")
                    } else null,
                    onClick = { onAction(SettingsAction.OpenCloudSyncSettings) },
                )
            }

            // 5. 存储管理与完整备份
            item {
                SettingsCategoryCard(
                    icon = AeIcons.Storage,
                    title = localized("Storage & Full Backup", "存储管理与完整备份"),
                    subtitle = localized(
                        "Offline JSON backup & restore, tracked playlists, and cache clearing",
                        "离线完整 JSON 导出导入 · 首页追踪歌单 · 播放缓存清理",
                    ),
                    tag = formatBytes(state.cacheSizeBytes),
                    onClick = { onAction(SettingsAction.OpenBackupStorageSettings) },
                )
            }

            // 6. 开发者选项与系统诊断
            item {
                SettingsCategoryCard(
                    icon = AeIcons.Settings,
                    title = localized("Developer Options", "开发者选项与系统诊断"),
                    subtitle = localized(
                        "Playback cache limits, safe diagnostics, and error reporting",
                        "本地播放缓存上限 · 故障排查 · 运行状态脱敏日志",
                    ),
                    onClick = { onAction(SettingsAction.OpenDeveloperOptions) },
                )
            }
        }
    }
}
