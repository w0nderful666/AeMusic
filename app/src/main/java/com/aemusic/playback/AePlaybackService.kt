package com.aemusic.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class AePlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var mediaCache: SimpleCache? = null

    override fun onCreate() {
        super.onCreate()
        val cachePreferences = PlaybackCachePreferences(this)
        if (cachePreferences.clearRequested) {
            cachePreferences.cacheDirectory().deleteRecursively()
            cachePreferences.clearRequested = false
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android) AeMusic/0.1")
        val baseUpstream = DefaultDataSource.Factory(this, httpFactory)
        val upstream = ResolvingDataSource.Factory(baseUpstream) { dataSpec ->
            val url = dataSpec.uri.toString()
            val sourceHeaders = StreamRequestHeaders.get(dataSpec.key)
                .ifEmpty { StreamRequestHeaders.get(url) }
                .ifEmpty { StreamRequestHeaders.getForUri(dataSpec.uri) }
            if (sourceHeaders.isEmpty()) dataSpec else dataSpec.withAdditionalHeaders(sourceHeaders)
        }
        val cache = SimpleCache(
            java.io.File(cacheDir, "media-playback"),
            LeastRecentlyUsedCacheEvictor(cachePreferences.maxBytes),
            StandaloneDatabaseProvider(this),
        ).also { mediaCache = it }
        val cachedDataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cachedDataSource))
            .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        mediaCache?.release()
        mediaCache = null
        super.onDestroy()
    }

}
