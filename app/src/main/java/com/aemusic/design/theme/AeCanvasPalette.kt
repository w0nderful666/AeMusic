package com.aemusic.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class AeCanvasPalette(
    val backgroundStops: List<Color>,
    val content: Color,
    val contentVariant: Color,
    val accentColors: List<Color> = emptyList(),
)

/** Builds a themed artwork canvas whose foreground works against every gradient stop. */
fun artworkCanvasPalette(
    artworkColors: List<Color>,
    canvas: Color,
    onCanvas: Color,
    strength: Float,
    enabled: Boolean,
): AeCanvasPalette {
    if (!enabled || artworkColors.isEmpty()) {
        return AeCanvasPalette(listOf(canvas, canvas, canvas, canvas), onCanvas, onCanvas.copy(alpha = 0.74f), emptyList())
    }
    val darkCanvas = canvas.luminance() < 0.5f
    val safeTargets = artworkColors.map { it.withHslTone(if (darkCanvas) 0.16f else 0.84f) }
    val amount = strength.coerceIn(0f, 1f)
    val c0 = safeTargets.getOrNull(0) ?: canvas
    val c1 = safeTargets.getOrNull(1) ?: c0
    val c2 = safeTargets.getOrNull(2) ?: c1
    val c3 = safeTargets.getOrNull(3) ?: canvas

    val candidates = listOf(
        lerp(canvas, c0, 0.18f + amount * 0.76f),
        lerp(canvas, c1, 0.12f + amount * 0.64f),
        lerp(canvas, c2, 0.08f + amount * 0.50f),
        lerp(canvas, c3, 0.04f + amount * 0.35f),
    )
    val stops = candidates.map { it.ensureContrastWith(onCanvas, canvas, MinimumBodyContrast) }
    val variant = softenedContentColor(onCanvas, stops)
    return AeCanvasPalette(stops, onCanvas, variant, safeTargets)
}

fun contrastRatio(first: Color, second: Color): Float {
    val lighter = max(first.luminance(), second.luminance())
    val darker = min(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun softenedContentColor(content: Color, backgrounds: List<Color>): Color {
    var candidate = content
    repeat(12) {
        val next = lerp(candidate, backgrounds.first(), 0.04f)
        if (backgrounds.all { contrastRatio(next, it) >= MinimumBodyContrast }) candidate = next
    }
    return candidate
}

private fun Color.ensureContrastWith(content: Color, fallback: Color, minimum: Float): Color {
    var candidate = this
    repeat(12) {
        if (contrastRatio(candidate, content) >= minimum) return candidate
        candidate = lerp(candidate, fallback, 0.18f)
    }
    return fallback
}

private fun Color.withHslTone(lightness: Float): Color {
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
    val delta = maxChannel - minChannel
    val hue = when {
        delta == 0f -> 0f
        maxChannel == red -> 60f * (((green - blue) / delta) % 6f)
        maxChannel == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val originalLightness = (maxChannel + minChannel) / 2f
    val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * originalLightness - 1f))
    val safeSaturation = if (saturation < 0.14f) 0f else saturation.coerceIn(0.24f, 0.72f)
    return hslToColor(hue, safeSaturation, lightness, alpha)
}

private fun hslToColor(hue: Float, saturation: Float, lightness: Float, alpha: Float): Color {
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val section = hue / 60f
    val x = chroma * (1f - abs(section % 2f - 1f))
    val (r1, g1, b1) = when (section.toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    val match = lightness - chroma / 2f
    return Color(r1 + match, g1 + match, b1 + match, alpha)
}

private const val MinimumBodyContrast = 4.5f
