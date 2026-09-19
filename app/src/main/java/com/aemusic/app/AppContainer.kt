package com.aemusic.app

import android.content.Context
import com.aemusic.feature.library.LocalMusicRepository
import com.aemusic.feature.library.MediaStoreLocalMusicRepository
import com.aemusic.playback.PlaybackConnection
import com.aemusic.core.database.AeMusicDatabase
import com.aemusic.core.database.LibraryDataRepository
import com.aemusic.design.theme.ArtworkPaletteStore
import com.aemusic.core.network.AeNetworkClient
import com.aemusic.provider.PlaybackResolver
import com.aemusic.provider.ProviderRegistry
import com.aemusic.provider.SourceSelectionEngine
import com.aemusic.provider.account.ProviderCredentialStore
import com.aemusic.provider.bilibili.BilibiliProvider
import com.aemusic.provider.netease.NeteaseProvider
import com.aemusic.provider.kuwo.KuwoProvider
import com.aemusic.provider.lyrics.LyricsRepository
import com.aemusic.core.diagnostics.DiagnosticEventStore
import com.aemusic.playback.PlaybackCachePreferences
import com.aemusic.core.cloud.CloudSyncRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = AeMusicDatabase.create(context.applicationContext)
    val libraryDataRepository = LibraryDataRepository(database.libraryDataDao())
    val localMusicRepository: LocalMusicRepository = MediaStoreLocalMusicRepository(context.applicationContext)
    val artworkPaletteStore = ArtworkPaletteStore(context.applicationContext)
    val credentialStore = ProviderCredentialStore(appContext)
    private val networkClient = AeNetworkClient()
    val diagnosticEventStore = DiagnosticEventStore(appContext)
    val playbackCachePreferences = PlaybackCachePreferences(appContext)
    val cloudSyncRepository = CloudSyncRepository()
    val bilibiliProvider = BilibiliProvider(
        networkClient,
        accountCookie = { credentialStore.cookie(BilibiliProvider.SOURCE) },
        anonymousCookie = { credentialStore.cookie(BilibiliProvider.ANONYMOUS_SOURCE) },
        saveAnonymousCookie = { credentialStore.save(BilibiliProvider.ANONYMOUS_SOURCE, it) },
        diagnostics = diagnosticEventStore,
    )
    val neteaseProvider = NeteaseProvider(
        networkClient,
        accountCookie = { credentialStore.cookie(NeteaseProvider.SOURCE) },
        anonymousCookie = { credentialStore.cookie(NeteaseProvider.ANONYMOUS_SOURCE) },
        saveAnonymousCookie = { credentialStore.save(NeteaseProvider.ANONYMOUS_SOURCE, it) },
        diagnostics = diagnosticEventStore,
    )
    val kuwoProvider = KuwoProvider(networkClient)
    val providerRegistry = ProviderRegistry(listOf(bilibiliProvider, neteaseProvider, kuwoProvider))
    val sourceSelectionEngine = SourceSelectionEngine(providerRegistry)
    val lyricsRepository = LyricsRepository(appContext, networkClient, neteaseProvider, bilibiliProvider, kuwoProvider, libraryDataRepository)
    val neteaseDiscoveryRepository = com.aemusic.provider.netease.NeteaseDiscoveryRepository(networkClient, neteaseProvider)
    val trackedPlaylistStore = com.aemusic.core.data.TrackedPlaylistStore(appContext, neteaseDiscoveryRepository, bilibiliProvider)
    val settingsBackupManager = com.aemusic.core.backup.SettingsBackupManager(
        preferences = com.aemusic.feature.settings.AppUiPreferences(appContext),
        trackedPlaylistStore = trackedPlaylistStore,
        libraryDataRepository = libraryDataRepository,
    )
    val playbackConnection = PlaybackConnection(appContext, libraryDataRepository, PlaybackResolver(providerRegistry), diagnosticEventStore, sourceSelectionEngine)
}
