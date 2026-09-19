package com.aemusic.feature.common

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.aemusic.core.model.ArtworkRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import coil3.compose.rememberAsyncImagePainter

@Composable
internal fun rememberLocalArtworkPainter(context: Context, artwork: ArtworkRef): Painter? {
    if (artwork is ArtworkRef.Reference && artwork.key.startsWith("http")) {
        return rememberAsyncImagePainter(artwork.key)
    }
    val painter by produceState<Painter?>(initialValue = null, artwork) {
        value = when (artwork) {
            ArtworkRef.Missing -> null
            is ArtworkRef.Reference -> loadArtwork(context, artwork.key)
        }
    }
    return painter
}

private suspend fun loadArtwork(context: Context, reference: String): Painter? = withContext(Dispatchers.IO) {
    try {
        val uri = reference.toUri()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 256) }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()?.let(::BitmapPainter)
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun sampleSize(width: Int, height: Int, target: Int): Int {
    var size = 1
    while (width / (size * 2) >= target && height / (size * 2) >= target) size *= 2
    return size
}
