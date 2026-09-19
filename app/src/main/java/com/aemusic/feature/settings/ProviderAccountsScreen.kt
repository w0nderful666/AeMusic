package com.aemusic.feature.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.core.data.TrackedPlaylistStore
import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.Track
import com.aemusic.design.component.AeArtwork
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.provider.LoginProvider
import com.aemusic.provider.ProviderResult
import com.aemusic.provider.account.ProviderCredentialStore
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.netease.NeteaseProvider
import com.aemusic.provider.netease.OnlinePlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderAccountsScreen(
    padding: PaddingValues,
    neteaseProvider: NeteaseProvider,
    bilibiliProvider: BilibiliProvider,
    credentialStore: ProviderCredentialStore,
    trackedPlaylistStore: TrackedPlaylistStore,
    preferences: AppUiPreferences,
    onTestWebDav: suspend (String, String, String) -> Result<String>,
    onTestNavidrome: suspend (String, String, String) -> Result<String>,
    onBackupWebDav: (suspend () -> Result<String>)? = null,
    onRestoreWebDav: (suspend () -> Result<String>)? = null,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var loginProvider by remember { mutableStateOf<LoginProvider?>(null) }
    var manualCookieProvider by remember { mutableStateOf<LoginProvider?>(null) }
    var manualCookieText by remember { mutableStateOf("") }
    var manualCookieError by remember { mutableStateOf<String?>(null) }
    var manualCookieLoading by remember { mutableStateOf(false) }

    // Cloud Playlist Sheets state: Pair(source, playlists)
    var activePlaylistSource by remember { mutableStateOf<String?>(null) }
    var accountPlaylists by remember { mutableStateOf<List<OnlinePlaylist>>(emptyList()) }
    var loadingAccountPlaylists by remember { mutableStateOf(false) }
    var accountPlaylistsError by remember { mutableStateOf<String?>(null) }

    // Legacy duplicate cloud controls are retained until the dedicated cloud page
    // has completed real-server validation; they share the same persistence boundary.
    var showWebDavDialog by remember { mutableStateOf(false) }
    var webDavUrl by remember { mutableStateOf(preferences.webDavServerUrl) }
    var webDavUser by remember { mutableStateOf(preferences.webDavUsername) }
    var webDavPass by remember { mutableStateOf(credentialStore.secret(ProviderCredentialStore.WEBDAV_PASSWORD).orEmpty()) }
    var webDavPath by remember { mutableStateOf(preferences.webDavPath) }
    var webDavTestStatus by remember { mutableStateOf<String?>(null) }
    var webDavLoading by remember { mutableStateOf(false) }
    var showNavidromeDialog by remember { mutableStateOf(false) }
    var navidromeUrl by remember { mutableStateOf(preferences.navidromeServerUrl) }
    var navidromeUser by remember { mutableStateOf(preferences.navidromeUsername) }
    var navidromeToken by remember { mutableStateOf(credentialStore.secret(ProviderCredentialStore.NAVIDROME_PASSWORD).orEmpty()) }
    var navidromeTestStatus by remember { mutableStateOf<String?>(null) }
    var navidromeLoading by remember { mutableStateOf(false) }

    // Toast/Snackbar status
    var statusNotification by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activePlaylistSource != null || loginProvider != null) {
        if (activePlaylistSource != null) {
            activePlaylistSource = null
        } else if (loginProvider != null) {
            loginProvider = null
        }
    }

    if (loginProvider != null) {
        ProviderLoginPage(
            provider = loginProvider!!,
            credentialStore = credentialStore,
            onFinished = { saved -> loginProvider = null; if (saved) revision++ },
        )
        return
    }

    val verificationFailedMessage = localized("Verification failed: check your Cookie", "验证未通过，请检查凭证是否有效")
    if (manualCookieProvider != null) {
        val target = manualCookieProvider!!
        AlertDialog(
            onDismissRequest = { manualCookieProvider = null; manualCookieText = ""; manualCookieError = null },
            title = { Text(localized("Paste Cookie / Token", "导入 Cookie / Token 凭证")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized(
                            "Paste account Cookie string or token directly to sign in without web redirect issues.",
                            "直接粘贴账号 Cookie 字符串或 Token，彻底规避手机端登录跳转问题。",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = manualCookieText,
                        onValueChange = { manualCookieText = it; manualCookieError = null },
                        placeholder = { Text(if (target.descriptor.name.contains("网易")) "MUSIC_U=... 或仅粘贴 MUSIC_U 值" else "SESSDATA=... 或仅粘贴 SESSDATA 值") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                    )
                    manualCookieError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = manualCookieText.isNotBlank() && !manualCookieLoading,
                    onClick = {
                        manualCookieLoading = true
                        scope.launch {
                            val raw = manualCookieText.trim()
                            val formatted = if (raw.contains("=")) raw else {
                                if (target.descriptor.name.contains("网易")) "MUSIC_U=$raw" else "SESSDATA=$raw"
                            }
                            when (val result = target.validate(formatted)) {
                                is ProviderResult.Success -> {
                                    credentialStore.save(target.descriptor.id, formatted)
                                    manualCookieProvider = null
                                    manualCookieText = ""
                                    manualCookieError = null
                                    manualCookieLoading = false
                                    revision++
                                }
                                is ProviderResult.Failure -> {
                                    manualCookieError = verificationFailedMessage
                                    manualCookieLoading = false
                                }
                            }
                        }
                    },
                ) {
                    if (manualCookieLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(localized("Verify & Save", "验证并保存"))
                }
            },
            dismissButton = {
                TextButton(onClick = { manualCookieProvider = null; manualCookieText = ""; manualCookieError = null }) {
                    Text(localized("Cancel", "取消"))
                }
            },
        )
    }

    // WebDAV Dialog
    if (showWebDavDialog) {
        AlertDialog(
            onDismissRequest = { showWebDavDialog = false; webDavTestStatus = null },
            title = { Text(localized("WebDAV Cloud Sync", "WebDAV 漫游与备份")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized("Sync your playlists and history via any standard WebDAV cloud storage.", "通过标准 WebDAV 协议漫游和备份歌单（支持坚果云、Nextcloud、AList、群晖等）。"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = webDavUrl,
                        onValueChange = { webDavUrl = it },
                        label = { Text(localized("Server URL", "服务器地址")) },
                        placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = webDavUser,
                        onValueChange = { webDavUser = it },
                        label = { Text(localized("Username", "账号 / 邮箱")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = webDavPass,
                        onValueChange = { webDavPass = it },
                        label = { Text(localized("Password / App Token", "密码 / 应用授权码")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = webDavPath,
                        onValueChange = { webDavPath = it },
                        label = { Text(localized("Remote Path", "备份文件路径")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    webDavTestStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                    OutlinedButton(
                        enabled = !webDavLoading && webDavUrl.isNotBlank(),
                        onClick = {
                            webDavLoading = true
                            scope.launch {
                                val res = onTestWebDav(webDavUrl, webDavUser, webDavPass)
                                webDavTestStatus = res.getOrElse { it.message ?: "连接失败" }
                                webDavLoading = false
                            }
                        },
                    ) {
                        if (webDavLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(localized("Test", "测试连接"))
                    }
                    if (onBackupWebDav != null) {
                        OutlinedButton(
                            enabled = !webDavLoading && webDavUrl.isNotBlank(),
                            onClick = {
                                webDavLoading = true
                                scope.launch {
                                    val res = onBackupWebDav()
                                    webDavTestStatus = res.getOrElse { it.message ?: "备份失败" }
                                    webDavLoading = false
                                }
                            },
                        ) {
                            Text(localized("Backup", "备份到云端"))
                        }
                    }
                    if (onRestoreWebDav != null) {
                        OutlinedButton(
                            enabled = !webDavLoading && webDavUrl.isNotBlank(),
                            onClick = {
                                webDavLoading = true
                                scope.launch {
                                    val res = onRestoreWebDav()
                                    webDavTestStatus = res.getOrElse { it.message ?: "恢复失败" }
                                    webDavLoading = false
                                }
                            },
                        ) {
                            Text(localized("Restore", "从云端恢复"))
                        }
                    }
                    Button(
                        onClick = {
                            preferences.webDavServerUrl = webDavUrl.trim()
                            preferences.webDavUsername = webDavUser.trim()
                            credentialStore.saveSecret(ProviderCredentialStore.WEBDAV_PASSWORD, webDavPass)
                            preferences.webDavPath = webDavPath.trim()
                            showWebDavDialog = false
                            webDavTestStatus = null
                            revision++
                        },
                    ) {
                        Text(localized("Save", "保存"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebDavDialog = false; webDavTestStatus = null }) {
                    Text(localized("Cancel", "取消"))
                }
            },
        )
    }

    // Navidrome Dialog
    if (showNavidromeDialog) {
        AlertDialog(
            onDismissRequest = { showNavidromeDialog = false; navidromeTestStatus = null },
            title = { Text(localized("Navidrome / Subsonic Library", "Navidrome 私有音乐库")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized("Stream from your personal Navidrome / Subsonic music server.", "通过 Subsonic REST 协议直连个人自建 NAS 或服务器，随时随地畅听私有曲库。"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = navidromeUrl,
                        onValueChange = { navidromeUrl = it },
                        label = { Text(localized("Server URL", "服务器地址")) },
                        placeholder = { Text("http://192.168.1.100:4533") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = navidromeUser,
                        onValueChange = { navidromeUser = it },
                        label = { Text(localized("Username", "账号")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = navidromeToken,
                        onValueChange = { navidromeToken = it },
                        label = { Text(localized("Password / Token", "密码 / 访问凭证")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    navidromeTestStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                    OutlinedButton(
                        enabled = !navidromeLoading && navidromeUrl.isNotBlank(),
                        onClick = {
                            navidromeLoading = true
                            scope.launch {
                                val res = onTestNavidrome(navidromeUrl, navidromeUser, navidromeToken)
                                navidromeTestStatus = res.getOrElse { it.message ?: "连接失败" }
                                navidromeLoading = false
                            }
                        },
                    ) {
                        if (navidromeLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(localized("Test", "测试连接"))
                    }
                    Button(
                        onClick = {
                            preferences.navidromeServerUrl = navidromeUrl.trim()
                            preferences.navidromeUsername = navidromeUser.trim()
                            credentialStore.saveSecret(ProviderCredentialStore.NAVIDROME_PASSWORD, navidromeToken)
                            showNavidromeDialog = false
                            navidromeTestStatus = null
                            revision++
                        },
                    ) {
                        Text(localized("Save", "保存"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNavidromeDialog = false; navidromeTestStatus = null }) {
                    Text(localized("Cancel", "取消"))
                }
            },
        )
    }

    // Account Playlists Bottom Sheet
    if (activePlaylistSource != null) {
        ModalBottomSheet(
            onDismissRequest = { activePlaylistSource = null },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AeSpacing.md, vertical = AeSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AeSpacing.sm),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (activePlaylistSource == "netease") localized("My NetEase Playlists", "我的网易云歌单")
                        else localized("My Bilibili Favorites", "我的 B 站收藏夹"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { activePlaylistSource = null }) {
                        Text(localized("Close", "关闭"))
                    }
                }

                if (loadingAccountPlaylists) {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (accountPlaylistsError != null) {
                    Text(
                        accountPlaylistsError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = AeSpacing.md),
                    )
                } else if (accountPlaylists.isEmpty()) {
                    Text(
                        localized("No playlists found for this account.", "该账号下暂未找到歌单或收藏夹。"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = AeSpacing.md),
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(AeSpacing.xs),
                    ) {
                        items(accountPlaylists, key = OnlinePlaylist::id) { pl ->
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Row(
                                    Modifier.padding(AeSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                                ) {
                                    if (pl.coverUrl.isNotBlank()) {
                                        AeArtwork(
                                            painter = rememberLocalArtworkPainter(context, ArtworkRef.Reference(pl.coverUrl)),
                                            contentDescription = pl.name,
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                        )
                                    } else {
                                        AeArtwork(
                                            painter = null,
                                            contentDescription = pl.name,
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(pl.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${pl.trackCount} 首 · ${pl.creator}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val item = TrackedPlaylist(
                                                id = pl.id,
                                                source = activePlaylistSource ?: "netease",
                                                title = pl.name,
                                                coverUrl = pl.coverUrl.takeIf(String::isNotBlank),
                                                trackCount = pl.trackCount,
                                                creator = pl.creator,
                                            )
                                            trackedPlaylistStore.addExplicit(item)
                                            statusNotification = "已添加至首页追踪"
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(localized("Track", "追踪"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(
                    start = AeSpacing.md,
                    top = padding.calculateTopPadding() + AeSpacing.md,
                    end = AeSpacing.md,
                    bottom = padding.calculateBottomPadding() + AeSpacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.lg),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text(localized("Back", "返回")) }
                    Spacer(Modifier.weight(1f))
                    statusNotification?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(localized("Music Sources & Accounts", "音乐来源与账号"), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    localized("Sessions stay encrypted on this device. AeMusic connects directly to platform APIs and personal clouds.", "登录会话仅加密保存在本机，支持主流在线音乐平台、私有 NAS 流媒体与 WebDAV 漫游。"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 1. 在线音乐平台
            item {
                Text(localized("Online Platforms", "在线音乐平台"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            // 网易云音乐卡片
            item {
                key("netease", revision) {
                    val connected = credentialStore.isConnected(NeteaseProvider.SOURCE)
                    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                                Column(Modifier.weight(1f)) {
                                    Text("网易云音乐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (connected) localized("Connected", "已连接 (支持 VIP 专属歌单与日推)") else localized("Not connected", "未连接"),
                                        color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (connected) {
                                    OutlinedButton(onClick = { credentialStore.clear(NeteaseProvider.SOURCE); revision++ }) {
                                        Text(localized("Sign out", "退出"))
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                                        OutlinedButton(onClick = { manualCookieProvider = neteaseProvider; manualCookieText = "" }) {
                                            Text(localized("Cookie", "Cookie"))
                                        }
                                        Button(onClick = { loginProvider = neteaseProvider }) {
                                            Text(localized("Sign in", "网页登录"))
                                        }
                                    }
                                }
                            }
                            if (connected) {
                                Button(
                                    onClick = {
                                        activePlaylistSource = "netease"
                                        loadingAccountPlaylists = true
                                        accountPlaylistsError = null
                                        scope.launch {
                                            when (val res = neteaseProvider.userPlaylists()) {
                                                is ProviderResult.Success -> {
                                                    accountPlaylists = res.value
                                                    loadingAccountPlaylists = false
                                                }
                                                is ProviderResult.Failure -> {
                                                    accountPlaylistsError = res.reason.detail ?: "获取歌单失败"
                                                    loadingAccountPlaylists = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(AeIcons.Library, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(localized("View My Playlists", "查看并导入我的歌单"))
                                }
                            }
                        }
                    }
                }
            }

            // 哔哩哔哩卡片
            item {
                key("bilibili", revision) {
                    val connected = credentialStore.isConnected(BilibiliProvider.SOURCE)
                    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                                Column(Modifier.weight(1f)) {
                                    Text("哔哩哔哩 (B站)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (connected) localized("Connected", "已连接 (支持全画质音轨与个人收藏)") else localized("Not connected", "未连接 (匿名模式可解析大部分音频)"),
                                        color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (connected) {
                                    OutlinedButton(onClick = { credentialStore.clear(BilibiliProvider.SOURCE); revision++ }) {
                                        Text(localized("Sign out", "退出"))
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                                        OutlinedButton(onClick = { manualCookieProvider = bilibiliProvider; manualCookieText = "" }) {
                                            Text(localized("Cookie", "Cookie"))
                                        }
                                        Button(onClick = { loginProvider = bilibiliProvider }) {
                                            Text(localized("Sign in", "网页登录"))
                                        }
                                    }
                                }
                            }
                            if (connected) {
                                Button(
                                    onClick = {
                                        activePlaylistSource = "bilibili"
                                        loadingAccountPlaylists = true
                                        accountPlaylistsError = null
                                        scope.launch {
                                            when (val res = bilibiliProvider.userFavorites()) {
                                                is ProviderResult.Success -> {
                                                    accountPlaylists = res.value
                                                    loadingAccountPlaylists = false
                                                }
                                                is ProviderResult.Failure -> {
                                                    accountPlaylistsError = res.reason.detail ?: "获取收藏夹失败"
                                                    loadingAccountPlaylists = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(AeIcons.Library, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(localized("View My Favorites", "查看并导入我的收藏夹"))
                                }
                            }
                        }
                    }
                }
            }

            // 酷我音乐卡片
            item {
                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.padding(AeSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                        Column(Modifier.weight(1f)) {
                            Text("酷我音乐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(localized("Built-in source with available high-quality playback URLs and automatic fallback", "免登录内置音源，提供可用高品质播放地址与自动匹配替补"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(localized("Active", "已就绪")) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }

            // 2. 自建与私有云
            item {
                Spacer(Modifier.height(AeSpacing.xs))
                Text(localized("Self-Hosted & Cloud Sync", "自建私有云与歌单漫游"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            // WebDAV 卡片
            item {
                val configured = preferences.webDavServerUrl.isNotBlank()
                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.padding(AeSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                        Column(Modifier.weight(1f)) {
                            Text("WebDAV 歌单全平台漫游", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (configured) "已配置: ${preferences.webDavServerUrl}" else localized("Not configured. Sync playlists across devices.", "未配置。支持坚果云、Nextcloud 等全平台歌单漫游与备份。"),
                                color = if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(onClick = {
                            webDavUrl = preferences.webDavServerUrl
                            webDavUser = preferences.webDavUsername
                            webDavPass = credentialStore.secret(ProviderCredentialStore.WEBDAV_PASSWORD).orEmpty()
                            webDavPath = preferences.webDavPath
                            showWebDavDialog = true
                        }) {
                            Text(if (configured) localized("Sync", "配置 / 同步") else localized("Setup", "去配置"))
                        }
                    }
                }
            }

            // Navidrome 卡片
            item {
                val configured = preferences.navidromeServerUrl.isNotBlank()
                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.padding(AeSpacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeSpacing.md)) {
                        Column(Modifier.weight(1f)) {
                            Text("Navidrome / Subsonic 私有曲库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (configured) "已连接: ${preferences.navidromeServerUrl}" else localized("Not connected. Stream from home NAS or VPS.", "未连接。直连家庭 NAS 或 VPS 私有音乐服务器。"),
                                color = if (configured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(onClick = {
                            navidromeUrl = preferences.navidromeServerUrl
                            navidromeUser = preferences.navidromeUsername
                            navidromeToken = credentialStore.secret(ProviderCredentialStore.NAVIDROME_PASSWORD).orEmpty()
                            showNavidromeDialog = true
                        }) {
                            Text(if (configured) localized("Manage", "配置 / 测试") else localized("Setup", "去配置"))
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ProviderLoginPage(provider: LoginProvider, credentialStore: ProviderCredentialStore, onFinished: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUrl by remember(provider) { mutableStateOf(provider.loginUrl) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPasteCookieDialog by remember { mutableStateOf(false) }
    var manualCookieText by remember { mutableStateOf("") }
    val completeLoginError = localized("Complete sign-in first", "请先完成登录")
    val validationError = localized("The session could not be verified", "登录会话验证失败")

    BackHandler { onFinished(false) }

    if (showPasteCookieDialog) {
        AlertDialog(
            onDismissRequest = { showPasteCookieDialog = false },
            title = { Text(localized("Manual Cookie / Token Entry", "手动粘贴 Cookie / 凭据")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                    Text(
                        localized("If SMS verification code fails, you can paste account Cookie directly.", "若网页验证码发送受阻，可直接在此粘贴浏览器或抓包获取的 Cookie 凭据。"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = manualCookieText,
                        onValueChange = { manualCookieText = it; error = null },
                        label = { Text("Cookie / Token") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = manualCookieText.isNotBlank() && !checking,
                    onClick = {
                        val cookie = manualCookieText.trim()
                        checking = true
                        showPasteCookieDialog = false
                        scope.launch {
                            when (val result = provider.validate(cookie)) {
                                is ProviderResult.Success -> {
                                    credentialStore.save(provider.descriptor.id, cookie)
                                    onFinished(true)
                                }
                                is ProviderResult.Failure -> {
                                    error = validationError
                                    checking = false
                                }
                            }
                        }
                    },
                ) {
                    Text(localized("Verify & Save", "验证并保存"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCookieDialog = false }) {
                    Text(localized("Cancel", "取消"))
                }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Surface(tonalElevation = AeSpacing.xs) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(AeSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onFinished(false) }) { Text(localized("Cancel", "取消")) }
                    Text(provider.descriptor.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showPasteCookieDialog = true }) {
                        Icon(AeIcons.Settings, localized("Manual Cookie", "手动粘贴凭据"))
                    }
                    Button(
                        enabled = !checking,
                        onClick = {
                            val cookie = CookieManager.getInstance().getCookie(provider.cookieUrl).orEmpty()
                            if (cookie.isBlank()) { error = completeLoginError; return@Button }
                            checking = true
                            scope.launch {
                                when (val result = provider.validate(cookie)) {
                                    is ProviderResult.Success -> { credentialStore.save(provider.descriptor.id, cookie); onFinished(true) }
                                    is ProviderResult.Failure -> { error = validationError; checking = false }
                                }
                            }
                        },
                    ) { if (checking) CircularProgressIndicator(Modifier.size(AeSpacing.md), strokeWidth = AeSpacing.xxs) else Text(localized("Done", "完成")) }
                }
            }
            error?.let { Text(it, Modifier.padding(horizontal = AeSpacing.md, vertical = AeSpacing.xs), color = MaterialTheme.colorScheme.error) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                url?.let { currentUrl = it }
                                CookieManager.getInstance().flush()
                            }
                        }
                        loadUrl(provider.loginUrl)
                    }
                },
            )
        }
    }
}
