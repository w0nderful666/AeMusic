package com.aemusic.provider

internal data class StreamCandidate(
    val urls: List<String>,
    val bitrate: Long?,
    val mimeType: String?,
    val codec: String?,
    val lossless: Boolean = false,
    val label: String? = null,
)

internal fun orderStreamCandidates(
    candidates: List<StreamCandidate>,
    quality: StreamQuality,
): List<StreamCandidate> {
    val compatible = compareByDescending<StreamCandidate> { candidate ->
        candidate.codec?.contains("mp4a", ignoreCase = true) == true ||
            candidate.mimeType?.contains("audio/mp4", ignoreCase = true) == true
    }
    return when (quality) {
        StreamQuality.Standard -> candidates.sortedWith(
            compareBy<StreamCandidate> { it.lossless }
                .thenBy { it.bitrate ?: Long.MAX_VALUE }
                .then(compatible),
        )
        StreamQuality.Auto, StreamQuality.High -> candidates.sortedWith(
            compatible.thenByDescending { it.bitrate ?: 0L },
        )
        StreamQuality.Lossless -> candidates.sortedWith(
            compareByDescending<StreamCandidate> { it.lossless }
                .then(compatible)
                .thenByDescending { it.bitrate ?: 0L },
        )
    }
}
