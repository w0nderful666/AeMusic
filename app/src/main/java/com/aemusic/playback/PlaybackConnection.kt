package com.aemusic.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.Track
import com.aemusic.core.model.PlaybackReference
import com.aemusic.core.database.LibraryDataRepository
import com.aemusic.core.database.TrackKey
import com.aemusic.core.database.key
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.aemusic.provider.PlaybackResolver
import com.aemusic.provider.ProviderFailure
import com.aemusic.provider.ProviderResult
import com.aemusic.provider.ResolvedStream
import com.aemusic.provider.StreamQuality
import com.aemusic.provider.SourceSelectionEngine
import com.aemusic.core.diagnostics.DiagnosticEventStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AeRepeatMode { Off, All, One }

enum class SleepTimerMode(val minutes: Int) {
    Off(0),
    Minutes15(15),
    Minutes30(30),
    Minutes45(45),
    Minutes60(60),
    Minutes90(90),
    EndOfTrack(-1),
}

data class PlaybackUiState(
    val connected: Boolean = false,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: AeRepeatMode = AeRepeatMode.Off,
    val error: String? = null,
    val technicalInfo: PlaybackTechnicalInfo? = null,
    val preferredQuality: StreamQuality = StreamQuality.Auto,
    val originalTrack: Track? = null,
    val sleepTimerMode: SleepTimerMode = SleepTimerMode.Off,
    val sleepTimerRemainingSeconds: Int = 0,
    val softwareVolume: Float = 1.0f,
) {
    val currentTrack: Track? get() = queue.getOrNull(currentIndex)
    val favoriteTargetTrack: Track? get() = originalTrack ?: currentTrack
}

data class PlaybackTechnicalInfo(
    val source: String,
    val bitrate: Long? = null,
    val mimeType: String? = null,
    val codec: String? = null,
    val qualityLabel: String? = null,
    val replacementSource: String? = null,
)

class PlaybackConnection(
    context: Context,
    private val libraryDataRepository: LibraryDataRepository,
    private val playbackResolver: PlaybackResolver,
    private val diagnostics: DiagnosticEventStore,
    private val sourceSelectionEngine: SourceSelectionEngine? = null,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()
    private var queue: List<Track> = emptyList()
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordedMediaId: String? = null
    private var restored = false
    private var lastSavedSnapshot: PlaybackSnapshot? = null
    private var technicalInfo: PlaybackTechnicalInfo? = null
    private var preferredQuality = StreamQuality.Auto
    private var queueGeneration = 0L
    private var selectionGeneration = 0L
    private val resolvedStreams = mutableMapOf<Int, ResolvedStream>()
    private val fallbackPositions = mutableMapOf<Int, Int>()
    private val streamRefreshAttempts = mutableMapOf<Int, Int>()
    private val resolutionJobs = mutableMapOf<Int, Job>()
    private var resolvingIndex: Int? = null
    private var resolutionError: String? = null
    private val activeReplacements = mutableMapOf<Int, Track>()
    private val originalTrackMap = mutableMapOf<TrackKey, Track>()
    private var sleepTimerJob: Job? = null
    private var sleepTimerMode = SleepTimerMode.Off
    private var sleepTimerRemainingSeconds = 0
    private var softwareVolume = 1.0f
    private var nextTrackPrefetchEnabled = true
    private var lastObservedMediaItemIndex = -1
    private val playbackHistory = mutableListOf<Int>()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            val index = player.currentMediaItemIndex

            if (sleepTimerMode == SleepTimerMode.EndOfTrack) {
                if (player.playbackState == Player.STATE_ENDED ||
                    (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) && lastObservedMediaItemIndex != -1 && lastObservedMediaItemIndex != index)
                ) {
                    player.pause()
                    setSleepTimer(SleepTimerMode.Off)
                }
            }

            lastObservedMediaItemIndex = index

            if (index in queue.indices) {
                if (queue[index].playbackRef is PlaybackReference.Provider && !hasUsableStream(index)) {
                    player.pause()
                    selectIndex(index, player.currentPosition, shouldPlay = true)
                } else {
                    updateTechnicalInfo(index)
                    prefetchAround(index)
                    player.nextMediaItemIndex.takeIf { it >= 0 }?.let(::prefetch)
                }
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            val index = controller?.currentMediaItemIndex ?: -1
            val transportFailure = error.errorCodeName.contains("IO_")
            if (transportFailure && index in queue.indices && tryNextFallback(index, controller?.currentPosition ?: 0L)) {
                return
            } else if (transportFailure && index in queue.indices && queue[index].playbackRef is PlaybackReference.Provider &&
                (streamRefreshAttempts[index] ?: 0) < MAX_STREAM_REFRESH_ATTEMPTS
            ) {
                streamRefreshAttempts[index] = (streamRefreshAttempts[index] ?: 0) + 1
                resolvedStreams.remove(index)
                fallbackPositions.remove(index)
                selectIndex(index, controller?.currentPosition ?: 0L, shouldPlay = true)
            } else {
                resolutionError = error.errorCodeName
                currentDiagnosticTrack()?.let { diagnostics.record("playback", "media3", it.sourceId.value, it.id.value, "failure", error.errorCodeName) }
                _state.value = _state.value.copy(error = resolutionError, isBuffering = false)
            }
        }
    }

    private val progressTicker = object : Runnable {
        override fun run() {
            controller?.let(::publish)
            mainHandler.postDelayed(this, 500)
        }
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, AePlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            {
                val connectedController = try {
                    future.get()
                } catch (error: Exception) {
                    Log.e(TAG, "Unable to connect to playback service", error)
                    _state.value = _state.value.copy(error = "PLAYBACK_CONNECTION_FAILED")
                    return@addListener
                }
                controller = connectedController.also {
                    it.addListener(playerListener)
                    it.volume = softwareVolume
                }
                _state.value = _state.value.copy(connected = true, softwareVolume = softwareVolume)
                pendingActions.toList().also { pendingActions.clear() }.forEach { it(connectedController) }
                publish(connectedController)
                mainHandler.post(progressTicker)
                if (connectedController.mediaItemCount == 0) {
                    restorePlaybackSession()
                }
            },
            { command -> mainHandler.post(command) },
        )
    }

    fun play(requestedQueue: List<Track>, startIndex: Int) = playAt(requestedQueue, startIndex, 0, shouldPlay = true)

    private fun playAt(
        requestedQueue: List<Track>,
        startIndex: Int,
        startPositionMs: Long,
        shouldPlay: Boolean,
        restoredShuffle: Boolean? = null,
        restoredRepeatMode: Int? = null,
    ) {
        if (requestedQueue.isEmpty()) return
        val playableIndex = startIndex.coerceIn(requestedQueue.indices)
        val selected = requestedQueue[playableIndex]
        val generation = ++queueGeneration
        val selection = ++selectionGeneration
        resolutionJobs.values.forEach(Job::cancel)
        resolutionJobs.clear()
        resolvedStreams.clear()
        fallbackPositions.clear()
        streamRefreshAttempts.clear()
        activeReplacements.clear()
        queue = requestedQueue.toList()
        resolvingIndex = playableIndex
        resolutionError = null
        withController { player ->
            player.stop()
            player.setMediaItems(queue.map { it.toMediaItem(placeholderStream(it)) }, playableIndex, startPositionMs)
            restoredShuffle?.let { player.shuffleModeEnabled = it }
            restoredRepeatMode?.takeIf(::isValidRepeatMode)?.let { player.repeatMode = it }
            publish(player)
        }
        _state.value = _state.value.copy(queue = queue, currentIndex = playableIndex, isPlaying = false, isBuffering = true, error = null)
        resolutionJobs[playableIndex] = persistenceScope.launch {
            when (val result = playbackResolver.resolve(selected.playbackRef, preferredQuality)) {
                is ProviderResult.Success -> mainHandler.post {
                    if (generation != queueGeneration || selection != selectionGeneration) return@post
                    resolutionJobs.remove(playableIndex)
                    withController { player ->
                        resolvedStreams[playableIndex] = result.value
                        fallbackPositions[playableIndex] = 0
                        streamRefreshAttempts[playableIndex] = 0
                        resolvingIndex = null
                        resolutionError = null
                        diagnostics.record("playback", "initial-resolve", selected.sourceId.value, selected.id.value, "success", result.value.qualityLabel)
                        updateTechnicalInfo(playableIndex)
                        player.setMediaItems(queue.mapIndexed { index, track ->
                            track.toMediaItem(if (index == playableIndex) result.value else placeholderStream(track))
                        }, playableIndex, startPositionMs)
                        restoredShuffle?.let { player.shuffleModeEnabled = it }
                        restoredRepeatMode?.takeIf(::isValidRepeatMode)?.let { player.repeatMode = it }
                        player.prepare()
                        if (shouldPlay) player.play() else player.pause()
                        publish(player)
                        prefetchAround(playableIndex)
                    }
                }
                is ProviderResult.Failure -> {
                    val original = selected
                    val replacement = if (original.playbackRef is PlaybackReference.Provider) {
                        sourceSelectionEngine?.candidates(original)?.firstOrNull { it.matchScore >= SourceSelectionEngine.HIGH_CONFIDENCE_SCORE }
                    } else null
                    mainHandler.post {
                        if (generation != queueGeneration || selection != selectionGeneration) return@post
                        resolutionJobs.remove(playableIndex)
                        resolvingIndex = null
                        if (replacement != null) {
                            activeReplacements[playableIndex] = replacement.track
                            resolvedStreams[playableIndex] = replacement.stream
                            fallbackPositions[playableIndex] = 0
                            streamRefreshAttempts[playableIndex] = 0
                            resolutionError = null
                            diagnostics.record("playback", "auto-source", original.sourceId.value, original.id.value, "success", "resolved=${replacement.track.sourceId.value}; score=${replacement.matchScore}")
                            withController { player ->
                                updateTechnicalInfo(playableIndex)
                                player.setMediaItems(queue.mapIndexed { index, track ->
                                    track.toMediaItem(if (index == playableIndex) replacement.stream else placeholderStream(track))
                                }, playableIndex, startPositionMs)
                                restoredShuffle?.let { player.shuffleModeEnabled = it }
                                restoredRepeatMode?.takeIf(::isValidRepeatMode)?.let { player.repeatMode = it }
                                player.prepare()
                                if (shouldPlay) player.play() else player.pause()
                                publish(player)
                                prefetchAround(playableIndex)
                            }
                        } else {
                            resolutionError = result.reason.userCode()
                            diagnostics.record("playback", "initial-resolve", selected.sourceId.value, selected.id.value, "failure", result.reason.detail())
                            _state.value = _state.value.copy(isBuffering = false, error = resolutionError)
                        }
                    }
                }
            }
        }
    }

    fun setPreferredQuality(quality: StreamQuality) {
        if (preferredQuality == quality) return
        preferredQuality = quality
        val current = _state.value
        if (current.currentIndex in queue.indices) {
            resolvedStreams.remove(current.currentIndex)
            fallbackPositions.remove(current.currentIndex)
            streamRefreshAttempts.remove(current.currentIndex)
            selectIndex(current.currentIndex, current.positionMs, shouldPlay = current.isPlaying)
        }
    }

    fun setNextTrackPrefetchEnabled(enabled: Boolean) {
        nextTrackPrefetchEnabled = enabled
        if (!enabled) {
            val currentIndex = controller?.currentMediaItemIndex ?: -1
            resolutionJobs.keys.filter { it != currentIndex }.forEach { index ->
                resolutionJobs.remove(index)?.cancel()
            }
        } else {
            controller?.currentMediaItemIndex?.takeIf { it in queue.indices }?.let(::prefetchAround)
        }
    }

    fun invalidateProviderStreams() {
        resolvedStreams.keys.filter { index -> queue.getOrNull(index)?.playbackRef is PlaybackReference.Provider }.forEach { index ->
            resolvedStreams.remove(index)
            fallbackPositions.remove(index)
            streamRefreshAttempts.remove(index)
        }
        resolutionJobs.values.forEach(Job::cancel)
        resolutionJobs.clear()
    }

    fun restorePlaybackSession() {
        if (restored) return
        restored = true
        persistenceScope.launch {
            try {
                val restoredData = libraryDataRepository.restorePlaybackSessionWithTracks() ?: return@launch
                val stored = restoredData.first
                val restoredQueue = restoredData.second
                val currentKey = TrackKey(stored.session.currentSourceId, stored.session.currentTrackId)
                val restoredIndex = restoredQueue.indexOfFirst { it.key() == currentKey }.takeIf { it >= 0 } ?: 0
                if (restoredIndex !in restoredQueue.indices) return@launch
                if (restoredQueue[restoredIndex].playbackRef is PlaybackReference.Provider) {
                    playAt(
                        restoredQueue,
                        restoredIndex,
                        stored.session.positionMs,
                        shouldPlay = false,
                        restoredShuffle = stored.session.shuffleEnabled,
                        restoredRepeatMode = stored.session.repeatMode,
                    )
                    return@launch
                }
                mainHandler.post {
                    if (queue.isNotEmpty()) return@post
                    withController { player ->
                        queue = restoredQueue
                        player.setMediaItems(restoredQueue.map { it.toMediaItem(placeholderStream(it)) }, restoredIndex, stored.session.positionMs)
                        player.shuffleModeEnabled = stored.session.shuffleEnabled
                        player.repeatMode = stored.session.repeatMode.takeIf(::isValidRepeatMode) ?: Player.REPEAT_MODE_OFF
                        player.prepare()
                        player.pause()
                        publish(player)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Unable to restore playback session", error)
            }
        }
    }

    fun restoreWhenAvailable(tracks: List<Track> = emptyList()) {
        restorePlaybackSession()
    }

    fun setSoftwareVolume(volume: Float) {
        val coerced = volume.coerceIn(0.01f, 1.0f)
        softwareVolume = coerced
        withController { player -> player.volume = coerced }
        _state.value = _state.value.copy(softwareVolume = coerced)
    }

    fun setSleepTimer(mode: SleepTimerMode) {
        sleepTimerJob?.cancel()
        sleepTimerMode = mode
        if (mode == SleepTimerMode.Off) {
            sleepTimerRemainingSeconds = 0
            withController { player -> player.volume = softwareVolume }
            _state.value = _state.value.copy(sleepTimerMode = SleepTimerMode.Off, sleepTimerRemainingSeconds = 0)
            return
        }
        if (mode == SleepTimerMode.EndOfTrack) {
            sleepTimerRemainingSeconds = 0
            _state.value = _state.value.copy(sleepTimerMode = SleepTimerMode.EndOfTrack, sleepTimerRemainingSeconds = 0)
            return
        }
        sleepTimerRemainingSeconds = mode.minutes * 60
        _state.value = _state.value.copy(sleepTimerMode = mode, sleepTimerRemainingSeconds = sleepTimerRemainingSeconds)
        sleepTimerJob = persistenceScope.launch {
            while (sleepTimerRemainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                sleepTimerRemainingSeconds--
                if (sleepTimerRemainingSeconds in 1..5) {
                    val factor = sleepTimerRemainingSeconds / 5f
                    mainHandler.post {
                        controller?.volume = softwareVolume * factor
                    }
                }
                _state.value = _state.value.copy(sleepTimerRemainingSeconds = sleepTimerRemainingSeconds)
            }
            mainHandler.post {
                withController { player ->
                    player.pause()
                    player.volume = softwareVolume
                }
                sleepTimerMode = SleepTimerMode.Off
                _state.value = _state.value.copy(sleepTimerMode = SleepTimerMode.Off, sleepTimerRemainingSeconds = 0)
            }
        }
    }

    fun playPause() = withController {
        if (it.isPlaying) it.pause()
        else if (it.currentMediaItemIndex in queue.indices && !hasUsableStream(it.currentMediaItemIndex)) selectIndex(it.currentMediaItemIndex, it.currentPosition, true)
        else it.play()
    }
    fun seekTo(positionMs: Long) = withController { player ->
        val durationMs = player.duration.takeIf { it > 0 }
        player.seekTo(safeSeekPosition(positionMs, durationMs))
    }
    fun seekToIndex(index: Int) = selectIndex(index, 0, shouldPlay = true)
    fun replaceCurrentTrack(track: Track) {
        val current = _state.value
        val index = current.currentIndex
        if (index !in queue.indices) return
        val original = queue[index]
        val rootOriginal = originalTrackMap[original.key()] ?: original
        originalTrackMap[track.key()] = rootOriginal
        val updated = queue.toMutableList().apply { this[index] = track }
        playAt(updated, index, current.positionMs, shouldPlay = current.isPlaying)
    }
    fun previous() = withController { player ->
        if (shouldRestartCurrentTrack(player.currentPosition)) {
            player.seekTo(0)
        } else if (player.shuffleModeEnabled && playbackHistory.isNotEmpty()) {
            val prevIndex = playbackHistory.removeLastOrNull()
            if (prevIndex != null && prevIndex in queue.indices) {
                selectIndex(prevIndex, 0, true)
            } else {
                player.previousMediaItemIndex.takeIf { it >= 0 }?.let { selectIndex(it, 0, true) }
            }
        } else {
            player.previousMediaItemIndex.takeIf { it >= 0 }?.let { selectIndex(it, 0, true) }
        }
    }
    fun next() = withController { player ->
        val currentIdx = player.currentMediaItemIndex
        if (currentIdx in queue.indices) {
            playbackHistory.add(currentIdx)
            if (playbackHistory.size > 50) playbackHistory.removeAt(0)
        }
        val nextIndex = if (player.shuffleModeEnabled) {
            val candidate = player.nextMediaItemIndex.takeIf { it >= 0 }
            if (candidate != null) {
                candidate
            } else if (queue.size > 1) {
                (queue.indices.toList() - currentIdx).randomOrNull()
            } else null
        } else {
            player.nextMediaItemIndex.takeIf { it >= 0 }
                ?: if (player.repeatMode == Player.REPEAT_MODE_ALL && queue.isNotEmpty()) 0 else null
        }
        nextIndex?.let { selectIndex(it, 0, true) }
    }
    fun retry() {
        val current = _state.value
        if (current.currentIndex in queue.indices) {
            resolvedStreams.remove(current.currentIndex)
            fallbackPositions.remove(current.currentIndex)
            streamRefreshAttempts.remove(current.currentIndex)
            activeReplacements.remove(current.currentIndex)
            selectIndex(current.currentIndex, current.positionMs, shouldPlay = true)
        }
    }
    fun removeFromQueue(index: Int) {
        if (index !in queue.indices || queue.size <= 1) return
        val current = _state.value
        val currentTrack = current.currentTrack
        val updated = removeQueueItem(queue, index)
        val restoredIndex = currentTrack?.let { track -> updated.indexOfFirst { it.key() == track.key() } }
            ?.takeIf { it >= 0 } ?: index.coerceAtMost(updated.lastIndex)
        val restoredPosition = if (currentTrack?.key() == updated[restoredIndex].key()) current.positionMs else 0L
        playAt(updated, restoredIndex, restoredPosition, shouldPlay = current.isPlaying)
    }
    fun setShuffle(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }
    fun cycleRepeat() = withController { player ->
        player.repeatMode = nextRepeatMode(player.repeatMode)
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action) ?: pendingActions.add(action)
    }

    private fun selectIndex(index: Int, positionMs: Long, shouldPlay: Boolean) {
        if (index !in queue.indices) return
        if (resolvingIndex == index && resolutionJobs[index]?.isActive == true) return
        val generation = queueGeneration
        val selection = ++selectionGeneration
        resolutionJobs.values.forEach(Job::cancel)
        resolutionJobs.clear()
        val existing = resolvedStreams[index]
        if (queue[index].playbackRef is PlaybackReference.Local || existing?.isUsable() == true) {
            resolvingIndex = null
            resolutionError = null
            withController { player ->
                if (existing != null && index < player.mediaItemCount) player.replaceMediaItem(index, queue[index].toMediaItem(existing))
                player.seekTo(index, positionMs.coerceAtLeast(0))
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                if (shouldPlay) player.play() else player.pause()
                updateTechnicalInfo(index)
                prefetchAround(index)
            }
            return
        }
        resolvingIndex = index
        resolutionError = null
        withController { player ->
            player.stop()
            player.seekTo(index, positionMs.coerceAtLeast(0))
            publish(player)
        }
        _state.value = _state.value.copy(isBuffering = true, error = null)
        resolutionJobs[index] = persistenceScope.launch {
            when (val result = playbackResolver.resolve(queue[index].playbackRef, preferredQuality)) {
                is ProviderResult.Success -> mainHandler.post {
                    if (generation != queueGeneration || selection != selectionGeneration || index !in queue.indices) return@post
                    resolutionJobs.remove(index)
                    resolvedStreams[index] = result.value
                    fallbackPositions[index] = 0
                    resolvingIndex = null
                    resolutionError = null
                    queue.getOrNull(index)?.let { diagnostics.record("playback", "select-resolve", it.sourceId.value, it.id.value, "success", result.value.qualityLabel) }
                    withController { player ->
                        if (index < player.mediaItemCount) player.replaceMediaItem(index, queue[index].toMediaItem(result.value))
                        player.seekTo(index, positionMs.coerceAtLeast(0))
                        if (player.playbackState == Player.STATE_IDLE) player.prepare()
                        if (shouldPlay) player.play() else player.pause()
                        updateTechnicalInfo(index)
                        publish(player)
                        prefetchAround(index)
                    }
                }
                is ProviderResult.Failure -> {
                    val original = queue.getOrNull(index)
                    val replacement = if (original?.playbackRef is PlaybackReference.Provider) {
                        sourceSelectionEngine?.candidates(original)?.firstOrNull { it.matchScore >= SourceSelectionEngine.HIGH_CONFIDENCE_SCORE }
                    } else null
                    mainHandler.post {
                        if (generation != queueGeneration || selection != selectionGeneration) return@post
                        resolutionJobs.remove(index)
                        resolvingIndex = null
                        if (replacement != null && original != null) {
                            activeReplacements[index] = replacement.track
                            resolvedStreams[index] = replacement.stream
                            fallbackPositions[index] = 0
                            resolutionError = null
                            diagnostics.record("playback", "auto-source", original.sourceId.value, original.id.value, "success", "resolved=${replacement.track.sourceId.value}; score=${replacement.matchScore}")
                            withController { player ->
                                if (index < player.mediaItemCount) player.replaceMediaItem(index, original.toMediaItem(replacement.stream))
                                player.seekTo(index, positionMs.coerceAtLeast(0))
                                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                                if (shouldPlay) player.play() else player.pause()
                                updateTechnicalInfo(index)
                                publish(player)
                                prefetchAround(index)
                            }
                        } else {
                            resolutionError = result.reason.userCode()
                            original?.let { diagnostics.record("playback", "select-resolve", it.sourceId.value, it.id.value, "failure", result.reason.detail()) }
                            _state.value = _state.value.copy(isBuffering = false, error = resolutionError)
                        }
                    }
                }
            }
        }
    }

    private fun prefetchAround(index: Int) {
        if (!nextTrackPrefetchEnabled) return
        (index + 1).takeIf { it in queue.indices }?.let(::prefetch)
    }

    private fun prefetch(index: Int) {
        val track = queue.getOrNull(index) ?: return
        if (hasUsableStream(index) || resolutionJobs[index]?.isActive == true) return
        val generation = queueGeneration
        resolutionJobs[index] = persistenceScope.launch {
            val result = playbackResolver.resolve(track.playbackRef, preferredQuality)
            mainHandler.post {
                if (generation != queueGeneration || queue.getOrNull(index)?.key() != track.key()) return@post
                resolutionJobs.remove(index)
                if (result is ProviderResult.Success) {
                    resolvedStreams[index] = result.value
                    fallbackPositions[index] = 0
                    withController { player -> if (index < player.mediaItemCount) player.replaceMediaItem(index, track.toMediaItem(result.value)) }
                }
            }
        }
    }

    private fun hasUsableStream(index: Int): Boolean =
        queue.getOrNull(index)?.playbackRef is PlaybackReference.Local || resolvedStreams[index]?.isUsable() == true

    private fun tryNextFallback(index: Int, positionMs: Long): Boolean {
        val stream = resolvedStreams[index] ?: return false
        val next = (fallbackPositions[index] ?: 0) + 1
        val uri = stream.fallbackUris.getOrNull(next - 1) ?: return false
        fallbackPositions[index] = next
        val replacement = stream.copy(uri = uri)
        resolvedStreams[index] = replacement
        queue.getOrNull(index)?.let { track ->
            diagnostics.record("playback", "cdn-fallback", track.sourceId.value, track.id.value, "retry", "candidate=$next")
            withController { player ->
                player.replaceMediaItem(index, track.toMediaItem(replacement))
                player.seekTo(index, positionMs.coerceAtLeast(0))
                player.prepare()
                player.play()
            }
        }
        return true
    }

    private fun updateTechnicalInfo(index: Int) {
        val track = queue.getOrNull(index) ?: return
        val stream = resolvedStreams[index]
        val replacement = activeReplacements[index]
        technicalInfo = PlaybackTechnicalInfo(
            source = track.sourceId.value,
            bitrate = stream?.bitrate,
            mimeType = stream?.mimeType,
            codec = stream?.codec,
            qualityLabel = stream?.qualityLabel,
            replacementSource = replacement?.sourceId?.value,
        )
    }

    private fun currentDiagnosticTrack(): Track? = queue.getOrNull(controller?.currentMediaItemIndex ?: -1)

    private fun publish(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId
        val durationMs = player.duration.takeIf { it > 0 }
            ?: queue.getOrNull(player.currentMediaItemIndex)?.duration?.milliseconds
            ?: 0
        if (
            mediaId != null &&
            mediaId != recordedMediaId &&
            player.currentMediaItemIndex in queue.indices &&
            qualifiesForPlayHistory(player.currentPosition, durationMs)
        ) {
            recordedMediaId = mediaId
            launchPersistence("record play history") { libraryDataRepository.recordPlayed(queue[player.currentMediaItemIndex]) }
        }
        persistPlaybackSnapshot(player, durationMs)
        val current = queue.getOrNull(player.currentMediaItemIndex)
        val orig = current?.let { originalTrackMap[it.key()] }
        _state.value = PlaybackUiState(
            connected = true,
            queue = queue,
            currentIndex = player.currentMediaItemIndex.takeIf { it >= 0 } ?: -1,
            isPlaying = player.isPlaying,
            isBuffering = resolvingIndex != null || player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = durationMs,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> AeRepeatMode.All
                Player.REPEAT_MODE_ONE -> AeRepeatMode.One
                else -> AeRepeatMode.Off
            },
            error = resolutionError ?: player.playerError?.errorCodeName,
            technicalInfo = technicalInfo,
            preferredQuality = preferredQuality,
            originalTrack = orig,
            sleepTimerMode = sleepTimerMode,
            sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
            softwareVolume = softwareVolume,
        )
    }

    private fun persistPlaybackSnapshot(player: Player, durationMs: Long) {
        val index = player.currentMediaItemIndex
        if (index !in queue.indices) return
        val position = safeSeekPosition(player.currentPosition, durationMs.takeIf { it > 0 })
        val snapshot = PlaybackSnapshot(
            mediaId = player.currentMediaItem?.mediaId.orEmpty(),
            positionBucket = position / SESSION_SAVE_INTERVAL_MS,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queueKeys = queue.map(Track::key),
        )
        if (snapshot == lastSavedSnapshot) return
        lastSavedSnapshot = snapshot
        val queueSnapshot = queue.toList()
        launchPersistence("save playback session") {
            libraryDataRepository.savePlaybackSession(queueSnapshot, index, position, player.shuffleModeEnabled, player.repeatMode)
        }
    }

    private fun launchPersistence(operation: String, block: suspend () -> Unit) {
        persistenceScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Unable to $operation", error)
            }
        }
    }

    private companion object {
        const val MAX_STREAM_REFRESH_ATTEMPTS = 1
        const val TAG = "PlaybackConnection"
        const val SESSION_SAVE_INTERVAL_MS = 5_000L
    }
}

private data class PlaybackSnapshot(
    val mediaId: String,
    val positionBucket: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
    val queueKeys: List<TrackKey>,
)

internal fun shouldRestartCurrentTrack(positionMs: Long): Boolean = positionMs > 3_000

internal fun <T> removeQueueItem(queue: List<T>, index: Int): List<T> =
    if (index !in queue.indices || queue.size <= 1) queue else queue.filterIndexed { itemIndex, _ -> itemIndex != index }

internal fun safeSeekPosition(requestedMs: Long, durationMs: Long?): Long =
    requestedMs.coerceAtLeast(0).let { requested -> durationMs?.takeIf { it > 0 }?.let(requested::coerceAtMost) ?: requested }

internal fun isValidRepeatMode(mode: Int): Boolean = mode in setOf(
    Player.REPEAT_MODE_OFF,
    Player.REPEAT_MODE_ONE,
    Player.REPEAT_MODE_ALL,
)

internal fun qualifiesForPlayHistory(positionMs: Long, durationMs: Long): Boolean {
    if (durationMs <= 0) return positionMs >= 30_000
    return positionMs >= minOf(30_000, durationMs / 2)
}

internal fun nextRepeatMode(current: Int): Int = when (current) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
private fun Track.toMediaItem(stream: ResolvedStream): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artists.joinToString { it.name })
        .setAlbumTitle(album?.title)
        .apply { if (artwork is ArtworkRef.Reference) setArtworkUri(artwork.key.toUri()) }
        .build()
    val cacheKey = playbackCacheKey(this, stream)
    if (playbackRef is PlaybackReference.Provider) {
        StreamRequestHeaders.put(cacheKey, stream.headers)
        StreamRequestHeaders.put(stream.uri, stream.headers)
        stream.fallbackUris.forEach { StreamRequestHeaders.put(it, stream.headers) }
    }
    return MediaItem.Builder()
        .setMediaId("${sourceId.value}:${id.value}")
        .setUri(stream.uri.toUri())
        .apply {
            if (playbackRef is PlaybackReference.Provider) {
                setCustomCacheKey(cacheKey)
            }
        }
        .setMediaMetadata(metadata)
        .build()
}

internal fun playbackCacheKey(track: Track, stream: ResolvedStream): String = buildString {
    append(track.sourceId.value)
    append(':').append(track.id.value)
    append(':').append(stream.codec?.lowercase()?.replace(Regex("[^a-z0-9._-]"), "_") ?: "unknown-codec")
    append(':').append(stream.bitrate ?: 0L)
    append(':').append(stream.mimeType?.lowercase()?.replace(Regex("[^a-z0-9._-]"), "_") ?: "unknown-type")
}

private fun placeholderStream(track: Track): ResolvedStream = when (val reference = track.playbackRef) {
    is PlaybackReference.Local -> ResolvedStream(reference.contentUri)
    is PlaybackReference.Provider -> ResolvedStream("aemusic-unresolved://${reference.sourceId.value}/${reference.mediaId}")
}

private fun ResolvedStream.isUsable(now: Long = System.currentTimeMillis()): Boolean =
    expiresAtEpochMs == null || expiresAtEpochMs > now + 30_000

private fun ProviderFailure.userCode(): String = when (this) {
    is ProviderFailure.Authentication -> "SOURCE_LOGIN_REQUIRED"
    is ProviderFailure.Network -> "SOURCE_NETWORK_ERROR"
    is ProviderFailure.RateLimited -> "SOURCE_RATE_LIMITED"
    is ProviderFailure.Unavailable -> "SOURCE_UNAVAILABLE"
    is ProviderFailure.Parse -> "SOURCE_RESPONSE_INVALID"
}

private fun ProviderFailure.detail(): String? = when (this) {
    is ProviderFailure.Authentication -> detail
    is ProviderFailure.Network -> detail
    is ProviderFailure.RateLimited -> detail
    is ProviderFailure.Unavailable -> detail
    is ProviderFailure.Parse -> detail
}
