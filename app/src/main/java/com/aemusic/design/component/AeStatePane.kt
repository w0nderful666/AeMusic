package com.aemusic.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aemusic.design.theme.AeSpacing

sealed interface AeStatePaneState {
    data class Loading(val message: String = "Loading") : AeStatePaneState
    data class Empty(val title: String, val message: String? = null) : AeStatePaneState
    data class Error(val title: String, val message: String? = null) : AeStatePaneState
}

@Composable
fun AeStatePane(
    state: AeStatePaneState,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AeSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeSpacing.sm),
    ) {
        when (state) {
            is AeStatePaneState.Loading -> {
                CircularProgressIndicator()
                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is AeStatePaneState.Empty -> StateMessage(state.title, state.message)
            is AeStatePaneState.Error -> StateMessage(state.title, state.message)
        }
        if (actionLabel != null && onAction != null && state !is AeStatePaneState.Loading) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StateMessage(title: String, message: String?) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    if (message != null) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
