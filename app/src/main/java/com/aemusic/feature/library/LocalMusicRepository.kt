package com.aemusic.feature.library

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aemusic.core.model.Album
import com.aemusic.core.model.Artist
import com.aemusic.core.model.ArtworkRef
import com.aemusic.core.model.MusicSourceId
import com.aemusic.core.model.Track
import com.aemusic.core.model.TrackDuration
import com.aemusic.core.model.TrackId
import com.aemusic.core.model.PlaybackReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LocalMusicRepository {
    suspend fun tracks(): List<Track>
}

class MediaStoreLocalMusicRepository(private val context: Context) : LocalMusicRepository {
    override suspend fun tracks(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        val result = mutableListOf<Track>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" }
                val artist = cursor.getString(artistColumn).orEmpty()
                    .takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
                val album = cursor.getString(albumColumn).orEmpty()
                    .takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
                result += Track(
                    id = TrackId(id.toString()),
                    sourceId = MusicSourceId.Local,
                    title = title,
                    artists = artist?.let { listOf(Artist(it)) }.orEmpty(),
                    album = album?.let(::Album),
                    duration = TrackDuration.ofMilliseconds(cursor.getLong(durationColumn)),
                    artwork = if (albumId > 0) ArtworkRef.Reference("content://media/external/audio/albumart/$albumId") else ArtworkRef.Missing,
                    playbackRef = PlaybackReference.Local(ContentUris.withAppendedId(collection, id).toString()),
                )
            }
        }
        result
    }
}
