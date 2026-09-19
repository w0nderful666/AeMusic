package com.aemusic.design.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import com.aemusic.core.model.ArtworkRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware

class ArtworkPaletteStore(private val context: Context) {
    private val cache = ConcurrentHashMap<String, List<Color>>()
    suspend fun colors(artwork: ArtworkRef): List<Color>? {
        val reference = (artwork as? ArtworkRef.Reference)?.key ?: return null
        cache[reference]?.let { return it }
        return withContext(Dispatchers.IO) { loadBitmap(reference)?.let(::extractArtworkColors)?.also { cache[reference] = it } }
    }
    private suspend fun loadBitmap(reference: String): Bitmap? = if (reference.startsWith("http://") || reference.startsWith("https://")) {
        val request = ImageRequest.Builder(context)
            .data(reference)
            .size(320)
            .allowHardware(false)
            .build()
        ((context.imageLoader.execute(request) as? SuccessResult)?.image as? BitmapImage)?.bitmap
    } else try {
        val uri = reference.toUri()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = artworkSampleSize(bounds.outWidth, bounds.outHeight, 320) }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (_: SecurityException) { null } catch (_: IOException) { null } catch (_: IllegalArgumentException) { null }
}

internal fun extractArtworkColors(bitmap: Bitmap): List<Color> {
    val sampled = bitmap.scale(48, 48)
    var saturationSum = 0f
    var luminanceSum = 0f
    var count = 0
    val hsl = FloatArray(3)
    for (y in 0 until sampled.height step 2) for (x in 0 until sampled.width step 2) {
        val pixel = sampled[x, y]
        if (android.graphics.Color.alpha(pixel) < 128) continue
        ColorUtils.colorToHSL(pixel, hsl)
        saturationSum += hsl[1]; luminanceSum += hsl[2]; count++
    }
    if (sampled !== bitmap) sampled.recycle()
    val averageSaturation = if (count == 0) 0f else saturationSum / count
    val averageLuminance = if (count == 0) 0.5f else luminanceSum / count
    if (isNeutralArtwork(averageSaturation)) {
        val first = if (averageLuminance > 0.55f) 0.24f else 0.14f
        return listOf(
            hslColor(220f, 0.08f, first),
            hslColor(220f, 0.06f, (first + 0.08f).coerceAtMost(0.32f)),
            hslColor(220f, 0.04f, (first + 0.16f).coerceAtMost(0.40f)),
            hslColor(220f, 0.02f, (first + 0.04f).coerceAtMost(0.25f)),
        )
    }
    val palette = Palette.from(bitmap).maximumColorCount(24).generate()
    val vibrant = palette.vibrantSwatch?.rgb
    val darkVibrant = palette.darkVibrantSwatch?.rgb
    val lightVibrant = palette.lightVibrantSwatch?.rgb
    val muted = palette.mutedSwatch?.rgb
    val darkMuted = palette.darkMutedSwatch?.rgb
    val dominant = palette.dominantSwatch?.rgb

    val primary = vibrant ?: dominant ?: darkVibrant
    val secondary = muted ?: darkMuted ?: primary
    val tertiary = lightVibrant ?: vibrant ?: secondary
    val darkBase = darkVibrant ?: darkMuted ?: dominant

    return listOf(
        normalizeForPlayer(primary, 0.20f),
        normalizeForPlayer(secondary, 0.28f, saturationScale = 0.85f),
        normalizeForPlayer(tertiary, 0.36f, saturationScale = 0.75f),
        normalizeForPlayer(darkBase, 0.14f, saturationScale = 0.90f),
    )
}

internal fun isNeutralArtwork(averageSaturation: Float): Boolean = averageSaturation < 0.14f

internal fun artworkSampleSize(width: Int, height: Int, target: Int): Int {
    var size = 1
    while (width / (size * 2) >= target && height / (size * 2) >= target) size *= 2
    return size
}

private fun normalizeForPlayer(color: Int?, lightness: Float, saturationScale: Float = 1f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color ?: android.graphics.Color.DKGRAY, hsl)
    hsl[1] = (hsl[1] * saturationScale).coerceIn(0.08f, 0.72f); hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun hslColor(hue: Float, saturation: Float, lightness: Float) = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))
