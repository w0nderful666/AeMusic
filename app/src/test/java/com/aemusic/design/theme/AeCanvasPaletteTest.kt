package com.aemusic.design.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class AeCanvasPaletteTest {
    private val artworks = listOf(
        listOf(Color(0xFF7656E8), Color(0xFFD5529A)),
        listOf(Color(0xFF168A75), Color(0xFF74B956)),
        listOf(Color(0xFFD34867), Color(0xFF8548BD)),
        listOf(Color(0xFF3677CE), Color(0xFF44B8C9)),
        listOf(Color(0xFFF06B47), Color(0xFFF3B34C)),
        listOf(Color.Black, Color.White),
    )

    @Test
    fun lightCanvas_allStopsKeepBodyTextContrast() {
        verify(
            canvas = Color(0xFFFAF8FC),
            content = Color(0xFF19171D),
        )
    }

    @Test
    fun darkCanvas_allStopsKeepBodyTextContrast() {
        verify(
            canvas = Color(0xFF111014),
            content = Color(0xFFE9E4EC),
        )
    }

    @Test
    fun oledCanvas_allStopsKeepBodyTextContrast() {
        verify(
            canvas = Color.Black,
            content = Color(0xFFE9E4EC),
        )
    }

    @Test
    fun disabledArtworkBackground_returnsThemeCanvas() {
        val canvas = Color(0xFFFAF8FC)
        val palette = artworkCanvasPalette(artworks.first(), canvas, Color(0xFF19171D), 1f, false)
        assertTrue(palette.backgroundStops.all { it == canvas })
    }

    @Test
    fun neutralArtwork_remainsNeutralOnAppCanvas() {
        val palette = artworkCanvasPalette(
            artworkColors = listOf(Color(0xFF282828), Color(0xFF686868)),
            canvas = Color(0xFF111014),
            onCanvas = Color(0xFFE9E4EC),
            strength = 1f,
            enabled = true,
        )
        palette.backgroundStops.take(2).forEach { stop ->
            assertTrue(kotlin.math.abs(stop.red - stop.green) < 0.02f)
            assertTrue(kotlin.math.abs(stop.green - stop.blue) < 0.02f)
        }
    }

    private fun verify(canvas: Color, content: Color) {
        artworks.forEach { artwork ->
            listOf(0f, 0.5f, 1f).forEach { strength ->
                val palette = artworkCanvasPalette(artwork, canvas, content, strength, true)
                palette.backgroundStops.forEach { background ->
                    assertTrue(
                        "main contrast=${contrastRatio(palette.content, background)} background=$background",
                        contrastRatio(palette.content, background) >= 4.5f,
                    )
                    assertTrue(
                        "variant contrast=${contrastRatio(palette.contentVariant, background)} background=$background",
                        contrastRatio(palette.contentVariant, background) >= 4.5f,
                    )
                }
            }
        }
    }
}
