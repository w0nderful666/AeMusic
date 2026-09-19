package com.aemusic.feature.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.aemusic.core.database.LocalPlaylist
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.feature.settings.localized

data class PlaylistPickerModel(
    val playlists: List<LocalPlaylist>,
    val onAdd: (String) -> Unit,
    val onCreateAndAdd: (String) -> Unit,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlaylistPicker(
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = AeSpacing.lg, vertical = AeSpacing.md), verticalArrangement = Arrangement.spacedBy(AeSpacing.sm)) {
            Text(localized("Add to playlist", "添加到歌单"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = AeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeSpacing.md),
            ) {
                Icon(AeIcons.Add, null, tint = MaterialTheme.colorScheme.primary)
                Text(localized("New playlist", "新建歌单"), style = MaterialTheme.typography.titleMedium)
            }
            playlists.forEach { playlist ->
                Row(
                    Modifier.fillMaxWidth().clickable { onAdd(playlist.id); onDismiss() }.padding(vertical = AeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(playlist.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(localized("${playlist.trackKeys.size} songs", "${playlist.trackKeys.size} 首"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (creating) PlaylistNameDialog(
        title = localized("New playlist", "新建歌单"),
        initialName = "",
        onDismiss = { creating = false },
        onConfirm = { onCreateAndAdd(it); creating = false; onDismiss() },
    )
}

@Composable
fun PlaylistNameDialog(title: String, initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.take(80) }, label = { Text(localized("Playlist name", "歌单名称")) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) { Text(localized("Save", "保存")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localized("Cancel", "取消")) } },
    )
}
