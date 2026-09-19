package com.aemusic.feature.common

import androidx.compose.ui.graphics.Color
import com.aemusic.core.model.Track

private val trackPalettes = listOf(
    listOf(Color(0xFF7656E8), Color(0xFFD5529A)),
    listOf(Color(0xFF168A75), Color(0xFF74B956)),
    listOf(Color(0xFFD34867), Color(0xFF8548BD)),
    listOf(Color(0xFF3677CE), Color(0xFF44B8C9)),
    listOf(Color(0xFFF06B47), Color(0xFFF3B34C)),
    listOf(Color(0xFF374A8B), Color(0xFF9568D0)),
)

fun trackPalette(track: Track): List<Color> = trackPalettes[(track.id.value.hashCode() and Int.MAX_VALUE) % trackPalettes.size]

fun Track.artistLabel(fallback: String): String = artists.joinToString { it.name }.ifBlank { fallback }
