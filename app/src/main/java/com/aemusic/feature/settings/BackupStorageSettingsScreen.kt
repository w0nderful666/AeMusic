package com.aemusic.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aemusic.core.backup.ImportSummary
import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.core.model.ArtworkRef
import com.aemusic.design.component.AeArtwork
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.common.rememberLocalArtworkPainter
import kotlinx.coroutines.launch

@Composable
@Suppress("DEPRECATION")
fun BackupStorageSettingsScreen(
    padding: PaddingValues,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onAddTrackedPlaylist: (suspend (String) -> Result<TrackedPlaylist>)? = null,
    onExportBackup: (suspend () -> String)? = null,
    onImportBackup: (suspend (String) -> Result<ImportSummary>)? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showAddTrackedDialog by remember { mutableStateOf(false) }
    var addTrackedInput by remember { mutableStateOf("") }
    var addTrackedLoading by remember { mutableStateOf(false) }
    var addTrackedError by remember { mutableStateOf<String?>(null) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonString by remember { mutableStateOf("") }
    var exportLoading by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var importLoading by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }
    val cacheClearedSuccessText = localized(
        "Cache will be cleared safely when the playback service restarts.",
        "播放服务下次重启时将安全清理缓存。",
    )

    // Add Tracked Playlist Dialog
    if (showAddTrackedDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTrackedDialog = false
                addTrackedInput = ""
                addTrackedError = null
            },
            title = { Text(localized("Add Tracked Playlist", "添加快捷追踪歌单")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized(
                            "Paste a NetEase playlist link/ID or Bilibili video/collection (BV...) to track on the Home cockpit.",
                            "支持粘贴网易云歌单分享链接/ID，或 B 站视频/合集（BV号）进行快捷追踪。",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = addTrackedInput,
                        onValueChange = { addTrackedInput = it; addTrackedError = null },
                        placeholder = { Text("https://music.163.com/... 或 BV...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = addTrackedError != null,
                    )
                    addTrackedError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = addTrackedInput.isNotBlank() && !addTrackedLoading,
                    onClick = {
                        if (onAddTrackedPlaylist != null) {
                            addTrackedLoading = true
                            scope.launch {
                                val res = onAddTrackedPlaylist(addTrackedInput)
                                res.fold(
                                    onSuccess = {
                                        showAddTrackedDialog = false
                                        addTrackedInput = ""
                                        addTrackedError = null
                                        addTrackedLoading = false
                                    },
                                    onFailure = {
                                        addTrackedError = it.message ?: "解析失败，请检查链接有效性"
                                        addTrackedLoading = false
                                    },
                                )
                            }
                        } else {
                            showAddTrackedDialog = false
                        }
                    },
                ) {
                    if (addTrackedLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(localized("Track", "确认追踪"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrackedDialog = false; addTrackedInput = ""; addTrackedError = null }) {
                    Text(localized("Cancel", "取消"))
                }
            },
        )
    }

    // Export Dialog
    if (showExportDialog) {
        val backupCopiedToast = localized("Backup JSON copied to clipboard", "备份 JSON 已复制到剪贴板")
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(localized("Full Backup Exported", "完整配置备份已生成")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized(
                            "The backup JSON contains your complete UI settings, favorites, local playlists, tracked playlists, and lyric bindings. You can copy it to clipboard to save in notes or transfer to another device.",
                            "备份 JSON 已成功生成，包含完整的界面偏好、收藏、自建歌单、追踪歌单及自定义歌词绑定。你可以一键复制保存或在其他设备一键恢复。",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = exportJsonString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(exportJsonString))
                    showExportDialog = false
                    Toast.makeText(context, backupCopiedToast, Toast.LENGTH_SHORT).show()
                }) {
                    Text(localized("Copy to Clipboard", "复制到剪贴板"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(localized("Close", "关闭"))
                }
            },
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { if (!importLoading) showImportDialog = false },
            title = { Text(localized("Import Full Backup", "导入完整配置备份")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized(
                            "Paste your backup JSON below to restore preferences, playlists, and favorites.",
                            "在下方粘贴备份 JSON 文本内容，即可一键恢复应用配置、自建歌单、追踪列表与收藏。",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp),
                        placeholder = { Text(localized("Paste backup JSON here...", "在此粘贴备份 JSON 文本...")) },
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            clipboardManager.getText()?.text?.let { importJsonInput = it }
                        }) {
                            Text(localized("Paste from Clipboard", "从剪贴板粘贴"))
                        }
                    }
                    importStatusMessage?.let {
                        Text(
                            it,
                            color = if (it.startsWith("恢复成功") || it.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = importJsonInput.isNotBlank() && !importLoading,
                    onClick = {
                        scope.launch {
                            importLoading = true
                            importStatusMessage = null
                            val res = onImportBackup?.invoke(importJsonInput)
                            importLoading = false
                            if (res != null && res.isSuccess) {
                                val summary = res.getOrThrow()
                                importStatusMessage = "恢复成功！已恢复 ${summary.favoritesCount} 首收藏，${summary.playlistsCount} 个歌单，${summary.trackedPlaylistsCount} 个追踪，${summary.lyricBindingsCount} 条歌词绑定。"
                            } else {
                                importStatusMessage = "导入失败: ${res?.exceptionOrNull()?.localizedMessage ?: "数据格式无效"}"
                            }
                        }
                    },
                ) {
                    if (importLoading) CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text(localized("Validate & Restore", "校验并恢复"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(localized("Close", "关闭"))
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsSubpageHeader(
                title = localized("Storage & Full Backup", "存储管理与完整备份"),
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
                // 1. 离线完整配置备份与恢复
                item {
                    SettingsGroup(localized("Full Offline Backup", "离线配置完整备份与恢复")) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SettingText(
                                localized("Export Full Backup", "导出完整备份 JSON"),
                                localized("Export all preferences, favorites, playlists, and lyric bindings to offline JSON", "将界面设置、收藏、自建歌单及歌词绑定完整打包导出"),
                                Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        exportLoading = true
                                        exportJsonString = onExportBackup?.invoke().orEmpty()
                                        exportLoading = false
                                        showExportDialog = true
                                    }
                                },
                            ) {
                                if (exportLoading) CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                else Text(localized("Export", "导出"))
                            }
                        }
                        GroupDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SettingText(
                                localized("Import & Restore", "导入恢复备份"),
                                localized("Restore all settings and library data from backup JSON", "从离线备份 JSON 一键恢复所有设置与曲库"),
                                Modifier.weight(1f),
                            )
                            OutlinedButton(onClick = {
                                importJsonInput = ""
                                importStatusMessage = null
                                showImportDialog = true
                            }) {
                                Text(localized("Import", "导入"))
                            }
                        }
                    }
                }

                // 2. 首页追踪歌单管理
                item {
                    SettingsGroup(localized("Tracked Playlists", "快捷追踪歌单管理")) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingText(
                                localized("Tracked Sources", "已追踪的在线歌单/合集"),
                                localized("Manage playlists pinned to Home cockpit and Library", "在首页快捷控制台与曲库中直观展示"),
                                Modifier.weight(1f),
                            )
                            Button(onClick = { showAddTrackedDialog = true }) {
                                Icon(AeIcons.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(localized("Add", "添加"))
                            }
                        }
                        if (state.trackedPlaylists.isNotEmpty()) {
                            GroupDivider()
                            state.trackedPlaylists.forEachIndexed { index, playlist ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AeSpacing.md, vertical = AeSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                                ) {
                                    AeArtwork(
                                        painter = rememberLocalArtworkPainter(context, playlist.coverUrl?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing),
                                        contentDescription = playlist.title,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(playlist.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${if (playlist.source == "netease") "网易云" else "哔哩哔哩"} · ${playlist.trackCount} 首 · ${playlist.creator.orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = { onAction(SettingsAction.MoveTrackedPlaylist(playlist.id, -1)) },
                                    ) {
                                        Icon(
                                            AeIcons.Down,
                                            localized("Move up", "上移"),
                                            Modifier.graphicsLayer { rotationZ = 180f },
                                        )
                                    }
                                    IconButton(
                                        enabled = index < state.trackedPlaylists.lastIndex,
                                        onClick = { onAction(SettingsAction.MoveTrackedPlaylist(playlist.id, 1)) },
                                    ) {
                                        Icon(AeIcons.Down, localized("Move down", "下移"))
                                    }
                                    IconButton(onClick = { onAction(SettingsAction.RemoveTrackedPlaylist(playlist.id)) }) {
                                        Icon(AeIcons.Delete, localized("Remove", "移除"), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 存储与本地播放缓存
                item {
                    SettingsGroup(localized("Storage & Cache", "存储与播放缓存")) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SettingText(
                                localized("Playback cache used", "播放与图片缓存已用"),
                                formatBytes(state.cacheSizeBytes),
                                Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = {
                                    onAction(SettingsAction.ClearPlaybackCache)
                                    cacheClearedMessage = cacheClearedSuccessText
                                },
                            ) {
                                Text(localized("Clear Cache", "清理缓存"))
                            }
                        }
                        cacheClearedMessage?.let {
                            Text(
                                it,
                                Modifier.padding(horizontal = AeSpacing.md, vertical = AeSpacing.xs),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
