package com.aemusic.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aemusic.core.diagnostics.DiagnosticEventStore
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.playback.PlaybackCachePreferences

@Composable
fun DeveloperSettingsScreen(
    padding: PaddingValues,
    diagnostics: DiagnosticEventStore,
    cache: PlaybackCachePreferences,
    onBack: () -> Unit,
) {
    val events by diagnostics.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var maxBytes by remember { mutableLongStateOf(cache.maxBytes) }
    var cacheBytes by remember { mutableLongStateOf(cache.sizeBytes()) }
    var message by remember { mutableStateOf<String?>(null) }
    val clearScheduledMessage = localized("Cache will be cleared when the playback service restarts.", "播放服务重启时将清理缓存。")
    val copiedMessage = localized("Diagnostic report copied.", "诊断报告已复制。")
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(AeSpacing.md, padding.calculateTopPadding() + 16.dp, AeSpacing.md, padding.calculateBottomPadding() + AeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AeSpacing.md),
        ) {
            item {
                Row(Modifier.statusBarsPadding().fillMaxWidth()) {
                    IconButton(onClick = onBack) { Icon(AeIcons.Down, localized("Back", "返回")) }
                    Column(Modifier.weight(1f)) {
                        Text(localized("Developer options", "开发者选项"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(localized("Diagnostics contain no cookies or signed URLs.", "诊断信息不包含 Cookie 或签名地址。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                DeveloperGroup(localized("Playback cache", "播放缓存")) {
                    Text(localized("Used: ${formatBytes(cacheBytes)}", "已使用：${formatBytes(cacheBytes)}"))
                    Text(localized("Limit applies the next time the playback service starts.", "容量上限会在播放服务下次启动时生效。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AeSpacing.xs)) {
                        PlaybackCachePreferences.OPTIONS.forEach { option ->
                            FilterChip(selected = maxBytes == option, onClick = { maxBytes = option; cache.maxBytes = option }, label = { Text(formatBytes(option)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                        Button(onClick = { cacheBytes = cache.sizeBytes() }) { Text(localized("Refresh", "刷新")) }
                        Button(onClick = { cache.clearRequested = true; message = clearScheduledMessage }) { Text(localized("Clear cache", "清理缓存")) }
                    }
                }
            }
            item {
                DeveloperGroup(localized("Diagnostic log", "诊断日志")) {
                    Text(localized("${events.size} of 100 recent events", "最近 ${events.size}/100 条事件"))
                    Row(horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
                        Button(onClick = { copyText(context, diagnostics.report()); message = copiedMessage }) { Text(localized("Copy report", "复制报告")) }
                        OutlinedButton(onClick = diagnostics::clear) { Text(localized("Clear", "清空")) }
                    }
                }
            }
            message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
            items(events) { event ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${event.category} · ${event.operation} · ${event.outcome}", fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(event.sourceId, event.mediaId, event.detail).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun DeveloperGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); content() }
    }
}

private fun copyText(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("AeMusic diagnostics", text))
}
