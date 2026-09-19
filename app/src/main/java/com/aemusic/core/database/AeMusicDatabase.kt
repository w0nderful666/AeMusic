package com.aemusic.core.database

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.AutoMigration
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "favorite_tracks", primaryKeys = ["sourceId", "trackId"])
data class FavoriteTrackEntity(val sourceId: String, val trackId: String, val addedAtEpochMs: Long)

@Serializable
@Entity(tableName = "play_history", primaryKeys = ["sourceId", "trackId"])
data class PlayHistoryEntity(val sourceId: String, val trackId: String, val lastPlayedAtEpochMs: Long, val playCount: Long)

@Serializable
@Entity(tableName = "playback_session")
data class PlaybackSessionEntity(
    @androidx.room3.PrimaryKey val id: Int = 1,
    val currentSourceId: String,
    val currentTrackId: String,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
    val updatedAtEpochMs: Long,
)

@Serializable
@Entity(tableName = "playback_queue", primaryKeys = ["sessionId", "queueIndex"])
data class PlaybackQueueEntity(
    val sessionId: Int = 1,
    val queueIndex: Int,
    val sourceId: String,
    val trackId: String,
)

@Serializable
@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @androidx.room3.PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Serializable
@Entity(tableName = "local_playlist_tracks", primaryKeys = ["playlistId", "sourceId", "trackId"])
data class LocalPlaylistTrackEntity(
    val playlistId: String,
    val sourceId: String,
    val trackId: String,
    val addedAtEpochMs: Long,
)

@Serializable
@Entity(tableName = "track_snapshots", primaryKeys = ["sourceId", "trackId"])
data class TrackSnapshotEntity(
    val sourceId: String,
    val trackId: String,
    val title: String,
    val artists: String,
    val albumTitle: String?,
    val durationMs: Long,
    val artworkKey: String?,
    val playbackKind: String,
    val playbackValue: String,
    val playbackPartId: String?,
    val updatedAtEpochMs: Long,
)

@Serializable
@Entity(tableName = "lyric_bindings", primaryKeys = ["sourceId", "trackId"])
data class LyricBindingEntity(
    val sourceId: String,
    val trackId: String,
    val provider: String,
    val candidateId: String,
    val matchedTitle: String,
    val matchedArtist: String,
    val lyrics: String,
    val updatedAtEpochMs: Long,
)

data class StoredPlaybackSession(
    val session: PlaybackSessionEntity,
    val queue: List<PlaybackQueueEntity>,
)

@Dao
interface LibraryDataDao {
    @Query("SELECT * FROM track_snapshots")
    fun trackSnapshots(): Flow<List<TrackSnapshotEntity>>

    @Query("SELECT * FROM track_snapshots")
    suspend fun allTrackSnapshots(): List<TrackSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTrackSnapshot(entity: TrackSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTrackSnapshots(snapshots: List<TrackSnapshotEntity>)

    @Query("SELECT * FROM lyric_bindings WHERE sourceId = :sourceId AND trackId = :trackId LIMIT 1")
    suspend fun lyricBinding(sourceId: String, trackId: String): LyricBindingEntity?

    @Query("SELECT * FROM lyric_bindings")
    suspend fun allLyricBindings(): List<LyricBindingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLyricBinding(entity: LyricBindingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putLyricBindings(entities: List<LyricBindingEntity>)

    @Transaction
    suspend fun putLyricBindingWithSnapshot(snapshot: TrackSnapshotEntity, binding: LyricBindingEntity) {
        putTrackSnapshot(snapshot)
        putLyricBinding(binding)
    }

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAtEpochMs DESC")
    fun favorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAtEpochMs DESC")
    suspend fun allFavorites(): List<FavoriteTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFavorite(entity: FavoriteTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFavorites(entities: List<FavoriteTrackEntity>)

    @Transaction
    suspend fun putFavoriteWithSnapshot(snapshot: TrackSnapshotEntity, favorite: FavoriteTrackEntity) {
        putTrackSnapshot(snapshot)
        putFavorite(favorite)
    }

    @Query("DELETE FROM favorite_tracks WHERE sourceId = :sourceId AND trackId = :trackId")
    suspend fun removeFavorite(sourceId: String, trackId: String)

    @Query("SELECT * FROM play_history WHERE sourceId = :sourceId AND trackId = :trackId")
    suspend fun history(sourceId: String, trackId: String): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putHistory(entity: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY lastPlayedAtEpochMs DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY playCount DESC, lastPlayedAtEpochMs DESC LIMIT :limit")
    fun mostPlayed(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM playback_session WHERE id = 1")
    suspend fun playbackSession(): PlaybackSessionEntity?

    @Query("SELECT * FROM playback_queue WHERE sessionId = 1 ORDER BY queueIndex")
    suspend fun playbackQueue(): List<PlaybackQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPlaybackSession(entity: PlaybackSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPlaybackQueue(entities: List<PlaybackQueueEntity>)

    @Query("DELETE FROM playback_queue WHERE sessionId = 1")
    suspend fun clearPlaybackQueue()

    @Transaction
    suspend fun replacePlaybackSession(session: PlaybackSessionEntity, queue: List<PlaybackQueueEntity>) {
        clearPlaybackQueue()
        putPlaybackSession(session)
        putPlaybackQueue(queue)
    }

    @Transaction
    suspend fun replacePlaybackSessionWithSnapshots(
        session: PlaybackSessionEntity,
        queue: List<PlaybackQueueEntity>,
        snapshots: List<TrackSnapshotEntity>,
    ) {
        snapshots.forEach { putTrackSnapshot(it) }
        replacePlaybackSession(session, queue)
    }

    @Query("SELECT * FROM local_playlists ORDER BY updatedAtEpochMs DESC")
    fun playlists(): Flow<List<LocalPlaylistEntity>>

    @Query("SELECT * FROM local_playlists ORDER BY updatedAtEpochMs DESC")
    suspend fun allPlaylists(): List<LocalPlaylistEntity>

    @Query("SELECT * FROM local_playlist_tracks ORDER BY addedAtEpochMs DESC")
    fun playlistTracks(): Flow<List<LocalPlaylistTrackEntity>>

    @Query("SELECT * FROM local_playlist_tracks ORDER BY addedAtEpochMs DESC")
    suspend fun allPlaylistTracks(): List<LocalPlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPlaylists(entities: List<LocalPlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPlaylistTracks(entities: List<LocalPlaylistTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun putPlaylist(entity: LocalPlaylistEntity)

    @Query("UPDATE local_playlists SET name = :name, updatedAtEpochMs = :updatedAt WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String, updatedAt: Long)

    @Query("DELETE FROM local_playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    @Query("DELETE FROM local_playlists WHERE id = :playlistId")
    suspend fun deletePlaylistRow(playlistId: String)

    @Transaction
    suspend fun deletePlaylist(playlistId: String) {
        clearPlaylist(playlistId)
        deletePlaylistRow(playlistId)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPlaylistTrack(entity: LocalPlaylistTrackEntity): Long

    @Transaction
    suspend fun addPlaylistTrackWithSnapshot(snapshot: TrackSnapshotEntity, entity: LocalPlaylistTrackEntity): Long {
        putTrackSnapshot(snapshot)
        return addPlaylistTrack(entity)
    }

    @Query("DELETE FROM local_playlist_tracks WHERE playlistId = :playlistId AND sourceId = :sourceId AND trackId = :trackId")
    suspend fun removePlaylistTrack(playlistId: String, sourceId: String, trackId: String)

    @Query("UPDATE local_playlists SET updatedAtEpochMs = :updatedAt WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: String, updatedAt: Long)
}

@Database(
    entities = [FavoriteTrackEntity::class, PlayHistoryEntity::class, PlaybackSessionEntity::class, PlaybackQueueEntity::class, LocalPlaylistEntity::class, LocalPlaylistTrackEntity::class, TrackSnapshotEntity::class, LyricBindingEntity::class],
    version = 4,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
    exportSchema = true,
)
abstract class AeMusicDatabase : RoomDatabase() {
    abstract fun libraryDataDao(): LibraryDataDao

    companion object {
        fun create(context: Context): AeMusicDatabase = Room.databaseBuilder<AeMusicDatabase>(
            context = context.applicationContext,
            name = "aemusic.db",
        ).build()
    }
}
