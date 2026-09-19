package com.aemusic.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import com.aemusic.core.model.ArtworkRef
import com.aemusic.design.theme.ArtworkPaletteStore

@Composable
fun rememberArtworkColors(store: ArtworkPaletteStore, artwork: ArtworkRef?, fallback: List<Color>): List<Color> {
    val colors by produceState(initialValue = fallback, artwork, fallback) { value = artwork?.let { store.colors(it) } ?: fallback }
    return colors
}
