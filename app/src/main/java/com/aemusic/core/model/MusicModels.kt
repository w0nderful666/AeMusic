package com.aemusic.core.model

@JvmInline
value class TrackId(val value: String) {
    init { require(value.isNotBlank()) { "TrackId cannot be blank" } }
}

@JvmInline
value class MusicSourceId(val value: String) {
    init { require(value.isNotBlank()) { "MusicSourceId cannot be blank" } }

    companion object { val Local = MusicSourceId("local") }
}

@JvmInline
value class TrackDuration private constructor(val milliseconds: Long) {
    companion object {
        val Zero = TrackDuration(0)
        fun ofMilliseconds(value: Long) = TrackDuration(value.coerceAtLeast(0))
    }
}

data class Artist(val name: String)

data class Album(val title: String)

sealed interface ArtworkRef {
    data object Missing : ArtworkRef
    data class Reference(val key: String) : ArtworkRef {
        init { require(key.isNotBlank()) { "Artwork reference cannot be blank" } }
    }
}

sealed interface PlaybackReference {
    data class Local(val contentUri: String) : PlaybackReference {
        init { require(contentUri.isNotBlank()) }
    }
    data class Provider(val sourceId: MusicSourceId, val mediaId: String, val partId: String? = null) : PlaybackReference {
        init { require(mediaId.isNotBlank()) }
    }
}

data class Track(
    val id: TrackId,
    val sourceId: MusicSourceId,
    val title: String,
    val artists: List<Artist>,
    val album: Album?,
    val duration: TrackDuration,
    val artwork: ArtworkRef,
    val playbackRef: PlaybackReference,
) {
    init {
        require(title.isNotBlank()) { "Track title cannot be blank" }
    }
}
