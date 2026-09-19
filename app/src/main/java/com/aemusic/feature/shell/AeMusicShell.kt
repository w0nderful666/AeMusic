package com.aemusic.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.aemusic.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aemusic.design.component.AeSurface
import com.aemusic.design.component.AeSurfaceRole
import com.aemusic.design.component.AeTrackRow
import com.aemusic.design.icon.AeIcons
import com.aemusic.design.theme.AeSpacing
import com.aemusic.design.theme.AeMotion
import com.aemusic.design.effect.AeGlassSpec
import com.aemusic.design.theme.LocalAeGlassSpec
import com.aemusic.design.theme.LocalAeVisualStyle
import com.aemusic.design.theme.AeVisualStyle
import com.aemusic.design.theme.AeCanvasPalette
import com.aemusic.design.theme.LocalAeCanvasPalette
import com.aemusic.design.theme.artworkCanvasPalette
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.aemusic.feature.settings.AppUiPreferences
import com.aemusic.feature.settings.AppLanguage
import com.aemusic.feature.settings.LocalAppLanguage
import com.aemusic.feature.settings.SettingsScreen
import com.aemusic.feature.settings.SettingsAction
import com.aemusic.feature.settings.SettingsUiState
import com.aemusic.feature.settings.DeveloperSettingsScreen
import com.aemusic.feature.settings.AudioPlaybackSettingsScreen
import com.aemusic.feature.settings.AppearanceSettingsScreen
import com.aemusic.feature.settings.CloudSyncSettingsScreen
import com.aemusic.feature.settings.BackupStorageSettingsScreen
import com.aemusic.feature.search.SearchRoute
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import com.aemusic.core.data.TrackedPlaylist
import com.aemusic.provider.StreamQuality
import com.aemusic.design.component.PredictiveBackContainer
import com.aemusic.feature.home.HomeRoute
import com.aemusic.feature.library.LibraryRoute
import com.aemusic.app.AppContainer
import com.aemusic.feature.player.NowPlayingScreen
import com.aemusic.feature.player.NowPlayingActions
import com.aemusic.playback.PlaybackUiState
import com.aemusic.core.model.Track
import com.aemusic.design.component.AeArtwork
import com.aemusic.feature.common.rememberLocalArtworkPainter
import com.aemusic.feature.common.artistLabel
import com.aemusic.feature.common.trackPalette
import com.aemusic.feature.common.rememberArtworkColors
import com.aemusic.core.database.key
import com.aemusic.feature.playlist.PlaylistPickerModel
import com.aemusic.feature.library.LibraryPlaylistActions

private enum class RootDestination(val label: String, val icon: ImageVector) {
    Home("Home", AeIcons.Home), Search("Search", AeIcons.Search),
    Library("Library", AeIcons.Library), Settings("Settings", AeIcons.Settings),
}

private val DefaultArtworkColors = listOf(Color(0xFF3A4050), Color(0xFF626B7D))
private const val CLOUD_WEBDAV_PASSWORD = com.aemusic.provider.account.ProviderCredentialStore.WEBDAV_PASSWORD
private const val CLOUD_NAVIDROME_TOKEN = com.aemusic.provider.account.ProviderCredentialStore.NAVIDROME_PASSWORD

 @Composable
fun AeMusicShell(container: AppContainer) {
    val playback by container.playbackConnection.state.collectAsStateWithLifecycle()
    val favoriteKeys by container.libraryDataRepository.favoriteKeys.collectAsStateWithLifecycle()
    val recentKeys by container.libraryDataRepository.recentKeys.collectAsStateWithLifecycle()
    val mostPlayedKeys by container.libraryDataRepository.mostPlayedKeys.collectAsStateWithLifecycle()
    val mostPlayed by container.libraryDataRepository.mostPlayed.collectAsStateWithLifecycle()
    val savedTracks by container.libraryDataRepository.savedTracks.collectAsStateWithLifecycle()
    val playlists by container.libraryDataRepository.playlists.collectAsStateWithLifecycle()
    val trackedPlaylists by container.trackedPlaylistStore.playlists.collectAsStateWithLifecycle()
    val persistenceScope = rememberCoroutineScope()
    val extractedArtworkColors = rememberArtworkColors(
        container.artworkPaletteStore,
        playback.currentTrack?.artwork,
        playback.currentTrack?.let(::trackPalette) ?: DefaultArtworkColors,
    )
    RootShell(
        container = container,
        playback = playback,
        extractedArtworkColors = extractedArtworkColors,
        favoriteKeys = favoriteKeys,
        recentKeys = recentKeys,
        mostPlayedKeys = mostPlayedKeys,
        mostPlayed = mostPlayed,
        savedTracks = savedTracks,
        playlists = playlists,
        trackedPlaylists = trackedPlaylists,
        persistenceScope = persistenceScope,
    )
}

@Composable
private fun RootShell(
    container: AppContainer,
    playback: PlaybackUiState,
    extractedArtworkColors: List<Color>,
    favoriteKeys: Set<com.aemusic.core.database.TrackKey>,
    recentKeys: List<com.aemusic.core.database.TrackKey>,
    mostPlayedKeys: List<com.aemusic.core.database.TrackKey>,
    mostPlayed: List<com.aemusic.core.database.PlayHistoryEntity>,
    savedTracks: List<Track>,
    playlists: List<com.aemusic.core.database.LocalPlaylist>,
    trackedPlaylists: List<TrackedPlaylist>,
    persistenceScope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    val preferences = remember(context) { AppUiPreferences(context) }
    var fourTabs by remember { mutableStateOf(preferences.fourTabs) }
    var settingsPage by remember { mutableStateOf<SettingsPage?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }
    val onShowNowPlaying = { showNowPlaying = true }
    var language by remember {
        mutableStateOf(
            AppLanguage.entries.firstOrNull { it.name == preferences.language }
                ?: AppLanguage.System,
        )
    }
    var immersiveTitles by remember { mutableStateOf(preferences.immersiveTitles) }
    var artworkColors by remember { mutableStateOf(preferences.artworkColors) }
    var colorStrength by remember { mutableFloatStateOf(preferences.colorStrength) }
    var glassClarity by remember { mutableFloatStateOf(preferences.glassClarity) }
    var glassBlurRadius by remember { mutableFloatStateOf(preferences.glassBlurRadius) }
    var glassWaterFilm by remember { mutableFloatStateOf(preferences.glassWaterFilm) }
    var glassRefractionStrength by remember { mutableFloatStateOf(preferences.glassRefractionStrength) }
    var glassTintStrength by remember { mutableFloatStateOf(preferences.glassTintStrength) }
    var glassBorderWidth by remember { mutableFloatStateOf(preferences.glassBorderWidth) }
    var glassShadowElevation by remember { mutableFloatStateOf(preferences.glassShadowElevation) }
    var artworkBackground by remember { mutableStateOf(preferences.artworkBackground) }
    var glassEnabled by remember { mutableStateOf(preferences.glassEnabled) }
    var pageSwipeEnabled by remember { mutableStateOf(true) }

    var defaultStreamQuality by remember { mutableStateOf(preferences.defaultStreamQuality) }
    var nextTrackPrefetch by remember { mutableStateOf(preferences.nextTrackPrefetch) }
    var bilibiliBatchCount by remember { mutableIntStateOf(preferences.bilibiliBatchCount) }
    var preferredLyricsSource by remember { mutableStateOf(preferences.preferredLyricsSource) }
    var showSourceReplacementBadge by remember { mutableStateOf(preferences.showSourceReplacementBadge) }
    var webDavServerUrl by remember { mutableStateOf(preferences.webDavServerUrl) }
    var webDavUsername by remember { mutableStateOf(preferences.webDavUsername) }
    var webDavPassword by remember {
        mutableStateOf(
            container.credentialStore.secret(CLOUD_WEBDAV_PASSWORD)
                ?: preferences.webDavPassword,
        )
    }
    var webDavPath by remember { mutableStateOf(preferences.webDavPath) }
    var navidromeServerUrl by remember { mutableStateOf(preferences.navidromeServerUrl) }
    var navidromeUsername by remember { mutableStateOf(preferences.navidromeUsername) }
    var navidromeToken by remember {
        mutableStateOf(
            container.credentialStore.secret(CLOUD_NAVIDROME_TOKEN)
                ?: preferences.navidromeToken,
        )
    }
    var cacheSizeBytes by remember { mutableLongStateOf(container.playbackCachePreferences.sizeBytes()) }

    LaunchedEffect(Unit) {
        preferences.webDavPassword.takeIf(String::isNotBlank)?.let {
            container.credentialStore.saveSecret(CLOUD_WEBDAV_PASSWORD, it)
            preferences.clearLegacyWebDavPassword()
        }
        preferences.navidromeToken.takeIf(String::isNotBlank)?.let {
            container.credentialStore.saveSecret(CLOUD_NAVIDROME_TOKEN, it)
            preferences.clearLegacyNavidromeToken()
        }
        val initialQuality = when (preferences.defaultStreamQuality) {
            "Standard" -> StreamQuality.Standard
            "High" -> StreamQuality.High
            "Lossless" -> StreamQuality.Lossless
            else -> StreamQuality.Auto
        }
        container.playbackConnection.setPreferredQuality(initialQuality)
        container.playbackConnection.setNextTrackPrefetchEnabled(preferences.nextTrackPrefetch)
        container.bilibiliProvider.candidateLimit = preferences.bilibiliBatchCount
        container.lyricsRepository.preferredSource = runCatching {
            com.aemusic.provider.lyrics.LyricsSource.valueOf(preferences.preferredLyricsSource)
        }.getOrDefault(com.aemusic.provider.lyrics.LyricsSource.Auto)
    }

    val systemLanguage = LocalConfiguration.current.locales[0].language
    val resolvedLanguage = when (language) {
        AppLanguage.System -> if (systemLanguage.startsWith("zh")) AppLanguage.Chinese else AppLanguage.English
        else -> language
    }
    val tabs = if (fourTabs) RootDestination.entries else RootDestination.entries.dropLast(1)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val destination = tabs.getOrElse(pagerState.currentPage.coerceIn(0, tabs.lastIndex)) { RootDestination.Home }

    var isDockScrolledVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        isDockScrolledVisible = true
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -14f && isDockScrolledVisible) {
                    isDockScrolledVisible = false
                } else if (delta > 14f && !isDockScrolledVisible) {
                    isDockScrolledVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val selectRoot: (RootDestination) -> Unit = { target ->
        settingsPage = null
        isDockScrolledVisible = true
        scope.launch {
            pagerState.animateScrollToPage(
                page = tabs.indexOf(target).coerceAtLeast(0),
            )
        }
    }

    val onTestWebDav: suspend (String, String, String) -> Result<String> = { url, user, pass ->
        container.cloudSyncRepository.testWebDav(url, user, pass)
    }

    val onTestNavidrome: suspend (String, String, String) -> Result<String> = { url, user, password ->
        container.cloudSyncRepository.testNavidrome(url, user, password)
    }

    val onBackupWebDav: suspend () -> Result<String> = {
        container.cloudSyncRepository.backupPlaylists(
            webDavServerUrl,
            webDavUsername,
            webDavPassword,
            webDavPath,
            container.trackedPlaylistStore.playlists.value,
        ).map { count -> "备份成功 ($count 个歌单已同步至 WebDAV)" }
    }

    val onRestoreWebDav: suspend () -> Result<String> = {
        container.cloudSyncRepository.restorePlaylists(
            webDavServerUrl,
            webDavUsername,
            webDavPassword,
            webDavPath,
        ).map { items ->
            container.trackedPlaylistStore.mergePlaylists(items)
            "恢复成功 (已合并导入 ${items.size} 个歌单)"
        }
    }

    val settingsState = SettingsUiState(
        fourTabs = fourTabs,
        language = language,
        immersiveTitles = immersiveTitles,
        artworkColors = artworkColors,
        artworkBackground = artworkBackground,
        colorStrength = colorStrength,
        glassEnabled = glassEnabled,
        glassClarity = glassClarity,
        defaultStreamQuality = defaultStreamQuality,
        nextTrackPrefetch = nextTrackPrefetch,
        bilibiliBatchCount = bilibiliBatchCount,
        preferredLyricsSource = preferredLyricsSource,
        showSourceReplacementBadge = showSourceReplacementBadge,
        webDavServerUrl = webDavServerUrl,
        webDavUsername = webDavUsername,
        webDavPassword = webDavPassword,
        webDavPath = webDavPath,
        navidromeServerUrl = navidromeServerUrl,
        navidromeUsername = navidromeUsername,
        navidromeToken = navidromeToken,
        cacheSizeBytes = cacheSizeBytes,
        trackedPlaylists = trackedPlaylists,
    )

    val onSettingsAction: (SettingsAction) -> Unit = { action ->
        when (action) {
            is SettingsAction.SetFourTabs -> {
                if (action.enabled) {
                    fourTabs = true
                    preferences.fourTabs = true
                    settingsPage = null
                    scope.launch {
                        pagerState.scrollToPage(RootDestination.Settings.ordinal)
                    }
                } else {
                    scope.launch {
                        if (pagerState.currentPage >= RootDestination.Library.ordinal) {
                            pagerState.scrollToPage(RootDestination.Library.ordinal)
                        }
                        fourTabs = false
                        preferences.fourTabs = false
                    }
                }
            }
            is SettingsAction.SetLanguage -> { language = action.language; preferences.language = action.language.name }
            is SettingsAction.SetImmersiveTitles -> { immersiveTitles = action.enabled; preferences.immersiveTitles = action.enabled }
            is SettingsAction.SetArtworkColors -> { artworkColors = action.enabled; preferences.artworkColors = action.enabled }
            is SettingsAction.SetArtworkBackground -> { artworkBackground = action.enabled; preferences.artworkBackground = action.enabled }
            is SettingsAction.SetColorStrength -> { colorStrength = action.strength; preferences.colorStrength = action.strength }
            is SettingsAction.SetGlassEnabled -> { glassEnabled = action.enabled; preferences.glassEnabled = action.enabled }
            is SettingsAction.SetGlassClarity -> { glassClarity = action.clarity; preferences.glassClarity = action.clarity }
            is SettingsAction.SetDefaultStreamQuality -> {
                defaultStreamQuality = action.quality
                preferences.defaultStreamQuality = action.quality
                val quality = when (action.quality) {
                    "Standard" -> StreamQuality.Standard
                    "High" -> StreamQuality.High
                    "Lossless" -> StreamQuality.Lossless
                    else -> StreamQuality.Auto
                }
                container.playbackConnection.setPreferredQuality(quality)
            }
            is SettingsAction.SetNextTrackPrefetch -> {
                nextTrackPrefetch = action.enabled
                preferences.nextTrackPrefetch = action.enabled
                container.playbackConnection.setNextTrackPrefetchEnabled(action.enabled)
            }
            is SettingsAction.SetBilibiliBatchCount -> {
                bilibiliBatchCount = action.count
                preferences.bilibiliBatchCount = action.count
                container.bilibiliProvider.candidateLimit = action.count
            }
            is SettingsAction.SetPreferredLyricsSource -> {
                preferredLyricsSource = action.source
                preferences.preferredLyricsSource = action.source
                container.lyricsRepository.preferredSource = runCatching {
                    com.aemusic.provider.lyrics.LyricsSource.valueOf(action.source)
                }.getOrDefault(com.aemusic.provider.lyrics.LyricsSource.Auto)
            }
            is SettingsAction.SetShowSourceReplacementBadge -> {
                showSourceReplacementBadge = action.enabled
                preferences.showSourceReplacementBadge = action.enabled
            }
            is SettingsAction.SaveWebDavConfig -> {
                webDavServerUrl = action.serverUrl
                webDavUsername = action.username
                webDavPassword = action.password
                webDavPath = action.path
                preferences.webDavServerUrl = action.serverUrl
                preferences.webDavUsername = action.username
                if (action.password.isBlank()) container.credentialStore.clearSecret(CLOUD_WEBDAV_PASSWORD)
                else container.credentialStore.saveSecret(CLOUD_WEBDAV_PASSWORD, action.password)
                preferences.webDavPath = action.path
            }
            is SettingsAction.SaveNavidromeConfig -> {
                navidromeServerUrl = action.serverUrl
                navidromeUsername = action.username
                navidromeToken = action.token
                preferences.navidromeServerUrl = action.serverUrl
                preferences.navidromeUsername = action.username
                if (action.token.isBlank()) container.credentialStore.clearSecret(CLOUD_NAVIDROME_TOKEN)
                else container.credentialStore.saveSecret(CLOUD_NAVIDROME_TOKEN, action.token)
            }
            SettingsAction.ClearPlaybackCache -> {
                container.playbackCachePreferences.clearRequested = true
                cacheSizeBytes = container.playbackCachePreferences.sizeBytes()
            }
            is SettingsAction.RemoveTrackedPlaylist -> {
                container.trackedPlaylistStore.removeTracked(action.id)
            }
            is SettingsAction.MoveTrackedPlaylist -> {
                container.trackedPlaylistStore.moveTracked(action.id, action.offset)
            }
            SettingsAction.OpenGlassSettings -> {
                settingsPage = SettingsPage.Glass
            }
            SettingsAction.OpenSources -> settingsPage = SettingsPage.Sources
            SettingsAction.OpenDeveloperOptions -> settingsPage = SettingsPage.Developer
            SettingsAction.OpenAudioSettings -> settingsPage = SettingsPage.Audio
            SettingsAction.OpenAppearanceSettings -> settingsPage = SettingsPage.Appearance
            SettingsAction.OpenCloudSyncSettings -> settingsPage = SettingsPage.CloudSync
            SettingsAction.OpenBackupStorageSettings -> settingsPage = SettingsPage.Backup
        }
    }
    CompositionLocalProvider(
        LocalAppLanguage provides resolvedLanguage,
        LocalAeVisualStyle provides if (glassEnabled) AeVisualStyle.GLASS else AeVisualStyle.MATERIAL,
        LocalAeGlassSpec provides AeGlassSpec(
            enabled = glassEnabled,
            blurRadiusDp = glassBlurRadius,
            surfaceOpacity = glassClarity,
            tintStrength = glassTintStrength,
            waterFilmStrength = glassWaterFilm,
            refractionStrength = glassRefractionStrength,
            borderWidthDp = glassBorderWidth,
            shadowElevationDp = glassShadowElevation,
        ).normalized(),
    ) {
    val canvasPalette = artworkCanvasPalette(
        artworkColors = extractedArtworkColors,
        canvas = MaterialTheme.colorScheme.background,
        onCanvas = MaterialTheme.colorScheme.onBackground,
        strength = colorStrength,
        enabled = artworkBackground,
    )
    val canvasStop0 by animateColorAsState(
        canvasPalette.backgroundStops.getOrElse(0) { MaterialTheme.colorScheme.background },
        tween(AeMotion.ColorTransitionMillis),
        label = "shell-artwork-background-0",
    )
    val canvasStop1 by animateColorAsState(
        canvasPalette.backgroundStops.getOrElse(1) { MaterialTheme.colorScheme.background },
        tween(AeMotion.ColorTransitionMillis),
        label = "shell-artwork-background-1",
    )
    val canvasStop2 by animateColorAsState(
        canvasPalette.backgroundStops.getOrElse(2) { MaterialTheme.colorScheme.background },
        tween(AeMotion.ColorTransitionMillis),
        label = "shell-artwork-background-2",
    )
    val canvasStop3 by animateColorAsState(
        canvasPalette.backgroundStops.getOrElse(3) { MaterialTheme.colorScheme.background },
        tween(AeMotion.ColorTransitionMillis),
        label = "shell-artwork-background-3",
    )
    val animatedCanvasPalette = AeCanvasPalette(
        backgroundStops = listOf(canvasStop0, canvasStop1, canvasStop2, canvasStop3),
        content = canvasPalette.content,
        contentVariant = canvasPalette.contentVariant,
        accentColors = canvasPalette.accentColors,
    )
    CompositionLocalProvider(LocalAeCanvasPalette provides animatedCanvasPalette) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(animatedCanvasPalette.backgroundStops))) {
    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        containerColor = Color.Transparent,
        contentColor = animatedCanvasPalette.content,
        bottomBar = {
            val isDockVisible = settingsPage == null && !showNowPlaying
            androidx.compose.animation.AnimatedVisibility(
                visible = isDockVisible,
                enter = slideInVertically(animationSpec = tween(220)) { it } + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(animationSpec = tween(180)) { it } + fadeOut(animationSpec = tween(180)),
            ) {
                val dockOffsetAnim by animateFloatAsState(
                    targetValue = if (isDockScrolledVisible) 0f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = 380f,
                    ),
                    label = "dockScrollOffset",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = dockOffsetAnim * size.height
                            alpha = (1f - dockOffsetAnim * 0.4f).coerceIn(0f, 1f)
                        },
                ) {
                    BottomDock(
                        tabs = tabs,
                        selected = destination,
                        playback = playback,
                        onSelected = selectRoot,
                        onPlayer = onShowNowPlaying,
                        onPlayPause = container.playbackConnection::playPause,
                        onTrackSelected = container.playbackConnection::seekToIndex,
                        selectionPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { tabs.getOrNull(it) ?: it },
            pageNestedScrollConnection = remember { object : NestedScrollConnection {} },
            userScrollEnabled = pageSwipeEnabled,
        ) { page ->
            when (tabs.getOrNull(page) ?: RootDestination.Home) {
                RootDestination.Home -> HomeRoute(
                    padding = padding,
                    repository = container.localMusicRepository,
                    discoveryRepository = container.neteaseDiscoveryRepository,
                    bilibiliProvider = container.bilibiliProvider,
                    trackedPlaylistStore = container.trackedPlaylistStore,
                    favoriteKeys = favoriteKeys,
                    recentKeys = recentKeys,
                    mostPlayedKeys = mostPlayedKeys,
                    playHistory = mostPlayed,
                    savedTracks = savedTracks,
                    currentTrack = playback.currentTrack,
                    isPlaying = playback.isPlaying,
                    immersiveTitles = immersiveTitles,
                    onTrack = { queue, index ->
                        val selectedTrack = queue[index]
                        if (playback.currentTrack?.key() == selectedTrack.key()) onShowNowPlaying()
                        else container.playbackConnection.play(queue, index)
                    },
                    onPlayPause = container.playbackConnection::playPause,
                    onPrevious = container.playbackConnection::previous,
                    onNext = container.playbackConnection::next,
                    onLibraryAvailable = { localTracks ->
                        container.playbackConnection.restoreWhenAvailable((localTracks + savedTracks).distinctBy(Track::key))
                    },
                )
                RootDestination.Search -> SearchRoute(
                    padding = padding,
                    repository = container.localMusicRepository,
                    providerRegistry = container.providerRegistry,
                    discoveryRepository = container.neteaseDiscoveryRepository,
                    currentTrack = playback.currentTrack,
                    isPlaying = playback.isPlaying,
                    onTrack = { queue, index ->
                        val selectedTrack = queue[index]
                        if (playback.currentTrack?.key() == selectedTrack.key()) onShowNowPlaying()
                        else container.playbackConnection.play(queue, index)
                    },
                )
                RootDestination.Library -> LibraryRoute(
                    padding = padding,
                    repository = container.localMusicRepository,
                    settingsEntry = !fourTabs,
                    immersiveTitles = immersiveTitles,
                    onSettings = { settingsPage = SettingsPage.Root },
                    currentTrack = playback.currentTrack,
                    onPlayTrack = container.playbackConnection::play,
                    onShowNowPlaying = onShowNowPlaying,
                    favoriteKeys = favoriteKeys,
                    savedTracks = savedTracks,
                    onToggleFavorite = { track -> scope.launch { container.libraryDataRepository.toggleFavorite(track) } },
                    playlists = playlists,
                    playlistActions = LibraryPlaylistActions(
                        create = { name -> scope.launch { container.libraryDataRepository.createPlaylist(name) } },
                        rename = { id, name -> scope.launch { container.libraryDataRepository.renamePlaylist(id, name) } },
                        delete = { id -> scope.launch { container.libraryDataRepository.deletePlaylist(id) } },
                        add = { id, track -> scope.launch { container.libraryDataRepository.addToPlaylist(id, track) } },
                        remove = { id, track -> scope.launch { container.libraryDataRepository.removeFromPlaylist(id, track) } },
                        createAndAdd = { name, track -> scope.launch {
                            val id = container.libraryDataRepository.createPlaylist(name)
                            container.libraryDataRepository.addToPlaylist(id, track)
                        } },
                    ),
                    trackedPlaylists = trackedPlaylists,
                    discoveryRepository = container.neteaseDiscoveryRepository,
                    bilibiliProvider = container.bilibiliProvider,
                    trackedPlaylistStore = container.trackedPlaylistStore,
                )
                RootDestination.Settings -> SettingsScreen(
                    padding = padding,
                    state = settingsState,
                    onAction = onSettingsAction,
                )
            }
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Root,
            onBack = { settingsPage = SettingsPage.Root.parent(fourTabs) },
        ) {
            SettingsScreen(
                padding = padding,
                state = settingsState,
                onAction = onSettingsAction,
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Developer,
            onBack = { settingsPage = SettingsPage.Developer.parent(fourTabs) },
        ) {
            DeveloperSettingsScreen(
                padding = padding,
                diagnostics = container.diagnosticEventStore,
                cache = container.playbackCachePreferences,
                onBack = { settingsPage = SettingsPage.Developer.parent(fourTabs) },
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Sources,
            onBack = { settingsPage = SettingsPage.Sources.parent(fourTabs) },
        ) {
            com.aemusic.feature.settings.ProviderAccountsScreen(
                padding = padding,
                neteaseProvider = container.neteaseProvider,
                bilibiliProvider = container.bilibiliProvider,
                credentialStore = container.credentialStore,
                trackedPlaylistStore = container.trackedPlaylistStore,
                preferences = preferences,
                onTestWebDav = onTestWebDav,
                onTestNavidrome = onTestNavidrome,
                onBackupWebDav = onBackupWebDav,
                onRestoreWebDav = onRestoreWebDav,
                onPlayTracks = { tracks, index ->
                    if (tracks.isNotEmpty()) container.playbackConnection.play(tracks, index)
                },
                onBack = { settingsPage = SettingsPage.Sources.parent(fourTabs) },
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Glass,
            onBack = { settingsPage = SettingsPage.Glass.parent(fourTabs) },
        ) {
            com.aemusic.feature.settings.GlassSettingsScreen(
                padding = padding,
                preferences = preferences,
                onBack = { settingsPage = SettingsPage.Glass.parent(fourTabs) },
                onGlassUpdated = { spec ->
                    glassEnabled = spec.enabled
                    glassClarity = spec.surfaceOpacity
                    glassBlurRadius = spec.blurRadiusDp
                    glassWaterFilm = spec.waterFilmStrength
                    glassRefractionStrength = spec.refractionStrength
                    glassTintStrength = spec.tintStrength
                    glassBorderWidth = spec.borderWidthDp
                    glassShadowElevation = spec.shadowElevationDp
                },
                onArtworkBackgroundUpdated = { artworkBackground = it },
                onColorStrengthUpdated = { colorStrength = it },
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Audio,
            onBack = { settingsPage = SettingsPage.Audio.parent(fourTabs) },
        ) {
            AudioPlaybackSettingsScreen(
                padding = padding,
                state = settingsState,
                onAction = onSettingsAction,
                onBack = { settingsPage = SettingsPage.Audio.parent(fourTabs) },
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Appearance,
            onBack = { settingsPage = SettingsPage.Appearance.parent(fourTabs) },
        ) {
            AppearanceSettingsScreen(
                padding = padding,
                state = settingsState,
                onAction = onSettingsAction,
                onBack = { settingsPage = SettingsPage.Appearance.parent(fourTabs) },
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.CloudSync,
            onBack = { settingsPage = SettingsPage.CloudSync.parent(fourTabs) },
        ) {
            CloudSyncSettingsScreen(
                padding = padding,
                state = settingsState,
                onAction = onSettingsAction,
                onTestWebDav = onTestWebDav,
                onTestNavidrome = onTestNavidrome,
                onBackupWebDav = onBackupWebDav,
                onRestoreWebDav = onRestoreWebDav,
                onBack = { settingsPage = SettingsPage.CloudSync.parent(fourTabs) },
            )
        }

        PredictiveBackContainer(
            visible = settingsPage == SettingsPage.Backup,
            onBack = { settingsPage = SettingsPage.Backup.parent(fourTabs) },
        ) {
            BackupStorageSettingsScreen(
                padding = padding,
                state = settingsState,
                onAction = onSettingsAction,
                onAddTrackedPlaylist = container.trackedPlaylistStore::addTracked,
                onExportBackup = container.settingsBackupManager::exportBackupJson,
                onImportBackup = container.settingsBackupManager::importBackupJson,
                onBack = { settingsPage = SettingsPage.Backup.parent(fourTabs) },
            )
        }
    }
    PlayerOverlay(
        visible = showNowPlaying,
        onDismiss = { showNowPlaying = false },
        content = {
            NowPlayingScreen(
                state = playback,
                artworkColors = extractedArtworkColors,
                isFavorite = playback.favoriteTargetTrack?.let { it.key() in favoriteKeys } == true,
                onToggleFavorite = { playback.favoriteTargetTrack?.let { track -> persistenceScope.launch { container.libraryDataRepository.toggleFavorite(track) } } },
                actions = NowPlayingActions(
                    back = { showNowPlaying = false },
                    playPause = container.playbackConnection::playPause,
                    previous = container.playbackConnection::previous,
                    next = container.playbackConnection::next,
                    selectTrack = container.playbackConnection::seekToIndex,
                    seek = container.playbackConnection::seekTo,
                    shuffle = container.playbackConnection::setShuffle,
                    cycleRepeat = container.playbackConnection::cycleRepeat,
                    retry = container.playbackConnection::retry,
                    removeFromQueue = container.playbackConnection::removeFromQueue,
                    changeQuality = container.playbackConnection::setPreferredQuality,
                    replaceCurrentTrack = container.playbackConnection::replaceCurrentTrack,
                    playQueue = { tracks -> container.playbackConnection.play(tracks, 0) },
                    setSleepTimer = container.playbackConnection::setSleepTimer,
                    setSoftwareVolume = container.playbackConnection::setSoftwareVolume,
                ),
                playlistPicker = PlaylistPickerModel(
                    playlists = playlists,
                    onAdd = { id -> playback.favoriteTargetTrack?.let { track -> persistenceScope.launch { container.libraryDataRepository.addToPlaylist(id, track) } } },
                    onCreateAndAdd = { name -> playback.favoriteTargetTrack?.let { track -> persistenceScope.launch {
                        val id = container.libraryDataRepository.createPlaylist(name)
                        container.libraryDataRepository.addToPlaylist(id, track)
                    } } },
                ),
                lyricsRepository = container.lyricsRepository,
                sourceSelectionEngine = container.sourceSelectionEngine,
                bilibiliProvider = container.bilibiliProvider,
                showSourceReplacementBadge = showSourceReplacementBadge,
            )
        },
    )
    }
    }
    }
}

@Composable
private fun PlayerOverlay(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = visible) { events ->
        try {
            events.collect { event -> backProgress = event.progress.coerceIn(0f, 1f) }
            onDismiss()
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }
    LaunchedEffect(visible) { if (!visible) backProgress = 0f }
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = AeMotion.spatialSpring(),
            initialScale = 0.12f,
            transformOrigin = TransformOrigin(0.5f, 1f),
        ) + slideInVertically(animationSpec = AeMotion.spatialSpring(), initialOffsetY = { it / 2 }) + fadeIn(tween(AeMotion.FastMillis)),
        exit = scaleOut(
            animationSpec = AeMotion.spatialSpring(),
            targetScale = 0.12f,
            transformOrigin = TransformOrigin(0.5f, 1f),
        ) + slideOutVertically(animationSpec = AeMotion.spatialSpring(), targetOffsetY = { it / 2 }) + fadeOut(tween(AeMotion.FastMillis)),
        modifier = Modifier.fillMaxSize().graphicsLayer {
            val eased = predictivePlayerProgress(backProgress)
            scaleX = 1f - 0.08f * eased
            scaleY = 1f - 0.08f * eased
            translationY = size.height * 0.05f * eased
            alpha = 1f - 0.08f * eased
            transformOrigin = TransformOrigin(0.5f, 1f)
        },
    ) { content() }
}

internal fun predictivePlayerProgress(progress: Float): Float =
    progress.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f

@Composable
private fun BottomDock(
    tabs: List<RootDestination>,
    selected: RootDestination,
    playback: PlaybackUiState,
    onSelected: (RootDestination) -> Unit,
    onPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    selectionPosition: () -> Float,
) {
    Column(Modifier.navigationBarsPadding().padding(horizontal = AeSpacing.xs, vertical = AeSpacing.xxs)) {
        val hasPlayer = playback.currentTrack != null && playback.queue.isNotEmpty()
        AeSurface(
            role = AeSurfaceRole.Floating,
            modifier = Modifier.fillMaxWidth().height(if (hasPlayer) 132.dp else 72.dp),
        ) {
          Column {
            if (hasPlayer) {
            val miniPlayerPager = rememberPagerState(initialPage = playback.currentIndex.coerceAtLeast(0), pageCount = { playback.queue.size })
            LaunchedEffect(playback.currentIndex) {
                if (playback.currentIndex >= 0 && miniPlayerPager.settledPage != playback.currentIndex) {
                    miniPlayerPager.animateScrollToPage(playback.currentIndex)
                }
            }
            LaunchedEffect(miniPlayerPager.settledPage) {
                if (miniPlayerPager.settledPage != playback.currentIndex) onTrackSelected(miniPlayerPager.settledPage)
            }
            HorizontalPager(
                state = miniPlayerPager,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                key = { "${playback.queue[it].sourceId.value}:${playback.queue[it].id.value}" },
            ) { page ->
                val pageTrack = playback.queue[page]
                Row(
                    Modifier.fillMaxSize().clickable(onClick = onPlayer).padding(horizontal = AeSpacing.md, vertical = AeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AeSpacing.sm),
                ) {
                    AeArtwork(rememberLocalArtworkPainter(LocalContext.current, pageTrack.artwork), pageTrack.title, Modifier.size(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pageTrack.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(pageTrack.artistLabel(tr("Unknown artist", "未知艺人")), style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.74f))
                    }
                    IconButton(onClick = onPlayPause) {
                        if (playback.isBuffering) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(if (playback.isPlaying) AeIcons.Pause else AeIcons.Play, if (playback.isPlaying) tr("Pause", "暂停") else tr("Play", "播放"))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                val progress = if (playback.durationMs > 0) playback.positionMs.toFloat() / playback.durationMs else 0f
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(MaterialTheme.colorScheme.primary))
            }
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                val itemWidth = maxWidth / tabs.size
                val indicatorWidth = 52.dp
                val indicatorHeight = 56.dp
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
                Box(
                    Modifier
                        .width(indicatorWidth)
                        .height(indicatorHeight)
                        .graphicsLayer {
                            translationX = itemWidth.toPx() * selectionPosition() +
                                (itemWidth.toPx() - indicatorWidth.toPx()) / 2f
                            translationY = 8.dp.toPx()
                            shadowElevation = 6.dp.toPx()
                            shape = CircleShape
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    secondaryContainer.copy(alpha = 0.90f),
                                    secondaryContainer.copy(alpha = 0.65f),
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    primaryColor.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.10f),
                                )
                            ),
                            shape = CircleShape,
                        ),
                )
                Row(Modifier.fillMaxSize()) {
                    tabs.forEach { tab ->
                        Column(
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelected(tab) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = destinationLabel(tab),
                                tint = if (tab == selected) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current.copy(alpha = 0.76f),
                            )
                            Text(
                                destinationLabel(tab),
                                color = if (tab == selected) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current.copy(alpha = 0.76f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
          }
        }
    }
}


 @Composable
private fun tr(english: String, chinese: String): String =
    if (LocalAppLanguage.current == AppLanguage.Chinese) chinese else english

@Composable
private fun destinationLabel(destination: RootDestination): String = when (destination) {
    RootDestination.Home -> if (LocalAppLanguage.current == AppLanguage.Chinese) "首页" else stringResource(R.string.nav_home)
    RootDestination.Search -> if (LocalAppLanguage.current == AppLanguage.Chinese) "搜索" else stringResource(R.string.nav_search)
    RootDestination.Library -> if (LocalAppLanguage.current == AppLanguage.Chinese) "曲库" else stringResource(R.string.nav_library)
    RootDestination.Settings -> if (LocalAppLanguage.current == AppLanguage.Chinese) "设置" else stringResource(R.string.nav_settings)
}
