package com.aemusic.playback

import android.net.Uri
import java.util.LinkedHashMap

/** Process-local bridge from a stable Media3 cache key / URI to immutable per-stream headers. */
internal object StreamRequestHeaders {
    private const val MAX_ENTRIES = 256
    private val values = object : LinkedHashMap<String, Map<String, String>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>?): Boolean =
            size > MAX_ENTRIES
    }

    private val DEFAULT_BILIBILI_HEADERS = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to "https://www.bilibili.com/",
    )

    private val DEFAULT_NETEASE_HEADERS = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Referer" to "https://music.163.com/",
    )

    @Synchronized fun put(key: String, headers: Map<String, String>) {
        if (key.isNotBlank() && headers.isNotEmpty()) {
            values[key] = headers.toMap()
        }
    }

    @Synchronized fun get(key: String?): Map<String, String> = key?.let(values::get).orEmpty()

    @Synchronized fun getForUrl(url: String?): Map<String, String> {
        if (url.isNullOrBlank()) return emptyMap()
        val direct = values[url]
        if (!direct.isNullOrEmpty()) return direct

        // Check host-based anti-leech fallback for online streaming nodes
        val host = runCatching { java.net.URI.create(url).host }.getOrNull()?.lowercase()
            ?: url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
        return when {
            host.contains("bilivideo.com") || host.contains("bilibili.com") || host.contains("bilivideo.cn") ->
                DEFAULT_BILIBILI_HEADERS
            host.contains("music.126.net") || host.contains("163.com") ->
                DEFAULT_NETEASE_HEADERS
            else -> emptyMap()
        }
    }

    @Synchronized fun getForUri(uri: Uri?): Map<String, String> = getForUrl(uri?.toString())
}
