package com.aemusic.playback

import android.content.Context
import androidx.core.content.edit
import java.io.File

class PlaybackCachePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("playback_cache", Context.MODE_PRIVATE)
    var maxBytes: Long
        get() = preferences.getLong("max_bytes", DEFAULT_BYTES).coerceIn(MIN_BYTES, MAX_BYTES)
        set(value) = preferences.edit { putLong("max_bytes", value.coerceIn(MIN_BYTES, MAX_BYTES)) }
    var clearRequested: Boolean
        get() = preferences.getBoolean("clear_requested", false)
        set(value) = preferences.edit { putBoolean("clear_requested", value) }

    fun sizeBytes(): Long = cacheDirectory().walkTopDown().filter(File::isFile).sumOf(File::length)
    fun cacheDirectory(): File = File(appContext.cacheDir, "media-playback")

    companion object {
        const val DEFAULT_BYTES = 256L * 1024 * 1024
        const val MIN_BYTES = 128L * 1024 * 1024
        const val MAX_BYTES = 1024L * 1024 * 1024
        val OPTIONS = listOf(128L, 256L, 512L, 1024L).map { it * 1024 * 1024 }
    }
}
