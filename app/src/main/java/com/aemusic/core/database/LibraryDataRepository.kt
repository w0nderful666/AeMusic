package com.aemusic.core.database

import com.aemusic.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class TrackKey(val sourceId: String, val trackId: String)
data class LocalPlaylist(
    val id: String,
    val name: String,
    val trackKeys: List<TrackKey>,
) {
    val coverKey: TrackKey? get() = trackKeys.firstOrNull()
}

class LibraryDataRepository(private val dao: LibraryDataDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playbackSessionMutex = Mutex()
    val favoriteKeys: StateFlow<Set<TrackKey>> = dao.favorites()
        .map { rows -> rows.mapTo(mutableSetOf()) { TrackKey(it.sourceId, it.trackId) } }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    val recentKeys: StateFlow<List<TrackKey>> = dao.recent(50)
        .map { rows -> rows.map { TrackKey(it.sourceId, it.trackId) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val mostPlayed: StateFlow<List<PlayHistoryEntity>> = dao.mostPlayed(50)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val mostPlayedKeys: StateFlow<List<TrackKey>> = dao.mostPlayed(50)
        .map { rows -> rows.map { TrackKey(it.sourceId, it.trackId) } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val savedTracks: StateFlow<List<Track>> = dao.trackSnapshots()
        .map { rows -> rows.mapNotNull(TrackSnapshotEntity::toTrack) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val playlists: StateFlow<List<LocalPlaylist>> = combine(dao.playlists(), dao.playlistTracks()) { lists, members ->
        val byPlaylist = members.groupBy(LocalPlaylistTrackEntity::playlistId)
        lists.map { list ->
            LocalPlaylist(
                id = list.id,
                name = list.name,
                trackKeys = byPlaylist[list.id].orEmpty().map { TrackKey(it.sourceId, it.trackId) },
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun isFavorite(track: Track): Boolean = track.key() in favoriteKeys.value

    suspend fun toggleFavorite(track: Track) = withContext(Dispatchers.IO) {
        if (isFavorite(track)) dao.removeFavorite(track.sourceId.value, track.id.value)
        else {
            dao.putFavoriteWithSnapshot(track.toSnapshot(), FavoriteTrackEntity(track.sourceId.value, track.id.value, System.currentTimeMillis()))
        }
    }

    suspend fun recordPlayed(track: Track) = withContext(Dispatchers.IO) {
        dao.putTrackSnapshot(track.toSnapshot())
        val old = dao.history(track.sourceId.value, track.id.value)
        dao.putHistory(PlayHistoryEntity(track.sourceId.value, track.id.value, System.currentTimeMillis(), (old?.playCount ?: 0) + 1))
    }

    suspend fun playbackSession(): StoredPlaybackSession? = withContext(Dispatchers.IO) {
        val session = dao.playbackSession() ?: return@withContext null
        StoredPlaybackSession(session, dao.playbackQueue())
    }

    suspend fun savePlaybackSession(
        queue: List<Track>,
        currentIndex: Int,
        positionMs: Long,
        shuffleEnabled: Boolean,
        repeatMode: Int,
    ) = withContext(Dispatchers.IO) {
        val current = queue.getOrNull(currentIndex) ?: return@withContext
        playbackSessionMutex.withLock {
            dao.replacePlaybackSessionWithSnapshots(
                PlaybackSessionEntity(
                    currentSourceId = current.sourceId.value,
                    currentTrackId = current.id.value,
                    positionMs = positionMs.coerceAtLeast(0),
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
                queue.mapIndexed { index, track ->
                    PlaybackQueueEntity(queueIndex = index, sourceId = track.sourceId.value, trackId = track.id.value)
                },
                queue.map { it.toSnapshot() },
            )
        }
    }

    suspend fun createPlaylist(name: String): String = withContext(Dispatchers.IO) {
        val normalized = normalizePlaylistName(name)
        val now = System.currentTimeMillis()
        UUID.randomUUID().toString().also { id ->
            dao.putPlaylist(LocalPlaylistEntity(id, normalized, now, now))
        }
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        dao.renamePlaylist(playlistId, normalizePlaylistName(name), System.currentTimeMillis())
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addToPlaylist(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (dao.addPlaylistTrackWithSnapshot(track.toSnapshot(now), LocalPlaylistTrackEntity(playlistId, track.sourceId.value, track.id.value, now)) != -1L) {
            dao.touchPlaylist(playlistId, now)
        }
    }

    suspend fun lyricBinding(track: Track): LyricBindingEntity? = withContext(Dispatchers.IO) {
        dao.lyricBinding(track.sourceId.value, track.id.value)
    }

    suspend fun saveLyricBinding(track: Track, binding: LyricBindingEntity) = withContext(Dispatchers.IO) {
        dao.putLyricBindingWithSnapshot(
            track.toSnapshot(),
            binding.copy(sourceId = track.sourceId.value, trackId = track.id.value),
        )
    }

    suspend fun restorePlaybackSessionWithTracks(): Pair<StoredPlaybackSession, List<Track>>? = withContext(Dispatchers.IO) {
        val session = dao.playbackSession() ?: return@withContext null
        val queueEntities = dao.playbackQueue()
        if (queueEntities.isEmpty()) return@withContext null
        val snapshots = dao.allTrackSnapshots().associateBy { TrackKey(it.sourceId, it.trackId) }
        val tracks = queueEntities.mapNotNull { q ->
            snapshots[TrackKey(q.sourceId, q.trackId)]?.toTrack()
        }
        if (tracks.isEmpty()) return@withContext null
        StoredPlaybackSession(session, queueEntities) to tracks
    }

    suspend fun allFavorites(): List<FavoriteTrackEntity> = withContext(Dispatchers.IO) { dao.allFavorites() }
    suspend fun allPlaylists(): List<LocalPlaylistEntity> = withContext(Dispatchers.IO) { dao.allPlaylists() }
    suspend fun allPlaylistTracks(): List<LocalPlaylistTrackEntity> = withContext(Dispatchers.IO) { dao.allPlaylistTracks() }
    suspend fun allTrackSnapshots(): List<TrackSnapshotEntity> = withContext(Dispatchers.IO) { dao.allTrackSnapshots() }
    suspend fun allLyricBindings(): List<LyricBindingEntity> = withContext(Dispatchers.IO) { dao.allLyricBindings() }

    suspend fun restoreBackup(
        snapshots: List<TrackSnapshotEntity>,
        favorites: List<FavoriteTrackEntity>,
        playlists: List<LocalPlaylistEntity>,
        playlistTracks: List<LocalPlaylistTrackEntity>,
        lyricBindings: List<LyricBindingEntity>,
    ) = withContext(Dispatchers.IO) {
        if (snapshots.isNotEmpty()) dao.putTrackSnapshots(snapshots)
        if (favorites.isNotEmpty()) dao.putFavorites(favorites)
        if (playlists.isNotEmpty()) dao.putPlaylists(playlists)
        if (playlistTracks.isNotEmpty()) dao.putPlaylistTracks(playlistTracks)
        if (lyricBindings.isNotEmpty()) dao.putLyricBindings(lyricBindings)
    }

    suspend fun removeFromPlaylist(playlistId: String, track: Track) = withContext(Dispatchers.IO) {
        dao.removePlaylistTrack(playlistId, track.sourceId.value, track.id.value)
        dao.touchPlaylist(playlistId, System.currentTimeMillis())
    }
}

internal fun normalizePlaylistName(name: String): String = name.trim().take(80).ifBlank { "New playlist" }

fun Track.key() = TrackKey(sourceId.value, id.value)

private const val ARTIST_SEPARATOR = "\u001f"

internal fun Track.toSnapshot(now: Long = System.currentTimeMillis()) = TrackSnapshotEntity(
    sourceId = sourceId.value,
    trackId = id.value,
    title = title,
    artists = artists.joinToString(ARTIST_SEPARATOR) { it.name },
    albumTitle = album?.title,
    durationMs = duration.milliseconds,
    artworkKey = (artwork as? ArtworkRef.Reference)?.key,
    playbackKind = when (playbackRef) { is PlaybackReference.Local -> "local"; is PlaybackReference.Provider -> "provider" },
    playbackValue = when (val reference = playbackRef) { is PlaybackReference.Local -> reference.contentUri; is PlaybackReference.Provider -> reference.mediaId },
    playbackPartId = (playbackRef as? PlaybackReference.Provider)?.partId,
    updatedAtEpochMs = now,
)

internal fun TrackSnapshotEntity.toTrack(): Track? = runCatching {
    val source = MusicSourceId(sourceId)
    Track(
        id = TrackId(trackId),
        sourceId = source,
        title = title,
        artists = artists.split(ARTIST_SEPARATOR).filter(String::isNotBlank).map(::Artist),
        album = albumTitle?.takeIf(String::isNotBlank)?.let(::Album),
        duration = TrackDuration.ofMilliseconds(durationMs),
        artwork = artworkKey?.takeIf(String::isNotBlank)?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing,
        playbackRef = if (playbackKind == "local") PlaybackReference.Local(playbackValue) else PlaybackReference.Provider(source, playbackValue, playbackPartId),
    )
}.getOrNull()

