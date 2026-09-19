package com.aemusic.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aemusic.design.theme.AeSpacing
import kotlinx.coroutines.launch

@Composable
fun CloudSyncSettingsScreen(
    padding: PaddingValues,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onTestWebDav: (suspend (url: String, user: String, pass: String) -> Result<String>)? = null,
    onTestNavidrome: (suspend (url: String, user: String, token: String) -> Result<String>)? = null,
    onBackupWebDav: (suspend () -> Result<String>)? = null,
    onRestoreWebDav: (suspend () -> Result<String>)? = null,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var webDavUrl by remember(state.webDavServerUrl) { mutableStateOf(state.webDavServerUrl) }
    var webDavUser by remember(state.webDavUsername) { mutableStateOf(state.webDavUsername) }
    var webDavPass by remember(state.webDavPassword) { mutableStateOf(state.webDavPassword) }
    var webDavPath by remember(state.webDavPath) { mutableStateOf(state.webDavPath) }
    var webDavTestStatus by remember { mutableStateOf<String?>(null) }
    var webDavLoading by remember { mutableStateOf(false) }
    var webDavActionStatus by remember { mutableStateOf<String?>(null) }
    var webDavActionLoading by remember { mutableStateOf(false) }

    var navidromeUrl by remember(state.navidromeServerUrl) { mutableStateOf(state.navidromeServerUrl) }
    var navidromeUser by remember(state.navidromeUsername) { mutableStateOf(state.navidromeUsername) }
    var navidromeToken by remember(state.navidromeToken) { mutableStateOf(state.navidromeToken) }
    var navidromeTestStatus by remember { mutableStateOf<String?>(null) }
    var navidromeLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsSubpageHeader(
                title = localized("Cloud Sync & Self-Hosted", "云端同步与自建流媒体"),
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
                // 1. WebDAV 漫游云备份
                item {
                    SettingsGroup(localized("WebDAV Cloud Sync", "WebDAV 漫游云备份")) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                        ) {
                            Text(
                                localized(
                                    "Configure your WebDAV server (e.g. Jianguoyun, Nextcloud) to sync playlists and settings across devices.",
                                    "配置坚果云、Nextcloud 或其他自建 WebDAV 服务器，实现自建歌单与偏好设置的跨设备云端漫游备份。",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = webDavUrl,
                                onValueChange = { webDavUrl = it },
                                label = { Text("Server URL (服务器地址)") },
                                placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                            ) {
                                OutlinedTextField(
                                    value = webDavUser,
                                    onValueChange = { webDavUser = it },
                                    label = { Text("Username (用户名)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = webDavPass,
                                    onValueChange = { webDavPass = it },
                                    label = { Text("Password (应用密码)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = webDavPath,
                                onValueChange = { webDavPath = it },
                                label = { Text("Remote Path (存储路径)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            webDavTestStatus?.let {
                                Text(
                                    it,
                                    color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    enabled = webDavUrl.isNotBlank() && !webDavLoading,
                                    onClick = {
                                        if (onTestWebDav != null) {
                                            webDavLoading = true
                                            scope.launch {
                                                val res = onTestWebDav(webDavUrl, webDavUser, webDavPass)
                                                res.fold(
                                                    onSuccess = { webDavTestStatus = "✓ $it"; webDavLoading = false },
                                                    onFailure = { webDavTestStatus = "✗ ${it.message ?: "连接失败"}"; webDavLoading = false },
                                                )
                                            }
                                        } else {
                                            webDavTestStatus = "✓ 已配置就绪"
                                        }
                                    },
                                ) {
                                    if (webDavLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text(localized("Test Connection", "测试连接"))
                                }
                                Button(
                                    onClick = {
                                        onAction(SettingsAction.SaveWebDavConfig(webDavUrl, webDavUser, webDavPass, webDavPath))
                                        webDavTestStatus = "✓ 配置已保存"
                                    },
                                ) {
                                    Text(localized("Save Config", "保存配置"))
                                }
                            }
                            if (onBackupWebDav != null && onRestoreWebDav != null) {
                                GroupDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                                ) {
                                    OutlinedButton(
                                        enabled = webDavUrl.isNotBlank() && !webDavActionLoading,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            scope.launch {
                                                webDavActionLoading = true
                                                val res = onBackupWebDav()
                                                res.fold(
                                                    onSuccess = { webDavActionStatus = "✓ 备份成功: $it" },
                                                    onFailure = { webDavActionStatus = "✗ 备份失败: ${it.message}" },
                                                )
                                                webDavActionLoading = false
                                            }
                                        },
                                    ) {
                                        Text(localized("Backup to WebDAV", "立即备份到云端"))
                                    }
                                    OutlinedButton(
                                        enabled = webDavUrl.isNotBlank() && !webDavActionLoading,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            scope.launch {
                                                webDavActionLoading = true
                                                val res = onRestoreWebDav()
                                                res.fold(
                                                    onSuccess = { webDavActionStatus = "✓ 恢复成功: $it" },
                                                    onFailure = { webDavActionStatus = "✗ 恢复失败: ${it.message}" },
                                                )
                                                webDavActionLoading = false
                                            }
                                        },
                                    ) {
                                        Text(localized("Restore from Cloud", "从云端同步恢复"))
                                    }
                                }
                                webDavActionStatus?.let {
                                    Text(
                                        it,
                                        color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Navidrome / Subsonic 私有流媒体
                item {
                    SettingsGroup(localized("Navidrome / Subsonic", "Navidrome 私有音乐库")) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AeSpacing.md, vertical = AeSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                        ) {
                            Text(
                                localized(
                                    "Connect to your self-hosted Navidrome or Subsonic-compatible server.",
                                    "连接自建 Navidrome / Subsonic 私有流媒体服务器，直接串流播放个人 NAS / VPS 上的高质量无损音乐曲库。",
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = navidromeUrl,
                                onValueChange = { navidromeUrl = it },
                                label = { Text("Server URL (服务器地址)") },
                                placeholder = { Text("https://music.example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                            ) {
                                OutlinedTextField(
                                    value = navidromeUser,
                                    onValueChange = { navidromeUser = it },
                                    label = { Text("Username (用户名)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = navidromeToken,
                                    onValueChange = { navidromeToken = it },
                                    label = { Text("Token / Password (凭证)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                            }
                            navidromeTestStatus?.let {
                                Text(
                                    it,
                                    color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    enabled = navidromeUrl.isNotBlank() && !navidromeLoading,
                                    onClick = {
                                        if (onTestNavidrome != null) {
                                            navidromeLoading = true
                                            scope.launch {
                                                val res = onTestNavidrome(navidromeUrl, navidromeUser, navidromeToken)
                                                res.fold(
                                                    onSuccess = { navidromeTestStatus = "✓ $it"; navidromeLoading = false },
                                                    onFailure = { navidromeTestStatus = "✗ ${it.message ?: "连接失败"}"; navidromeLoading = false },
                                                )
                                            }
                                        } else {
                                            navidromeTestStatus = "✓ 已配置就绪"
                                        }
                                    },
                                ) {
                                    if (navidromeLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text(localized("Test Connection", "测试连接"))
                                }
                                Button(
                                    onClick = {
                                        onAction(SettingsAction.SaveNavidromeConfig(navidromeUrl, navidromeUser, navidromeToken))
                                        navidromeTestStatus = "✓ 配置已保存"
                                    },
                                ) {
                                    Text(localized("Save Config", "保存配置"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
