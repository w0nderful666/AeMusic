package com.aemusic.provider

import com.aemusic.core.model.MusicSourceId
import com.aemusic.core.model.PlaybackReference
import com.aemusic.core.model.Track

data class ProviderCapabilities(val search: Boolean, val streaming: Boolean, val login: Boolean)
data class ProviderDescriptor(val id: MusicSourceId, val name: String, val capabilities: ProviderCapabilities)
enum class StreamQuality { Auto, Standard, High, Lossless }
data class ResolvedStream(
    val uri: String,
    val headers: Map<String, String> = emptyMap(),
    val fallbackUris: List<String> = emptyList(),
    val expiresAtEpochMs: Long? = null,
    val bitrate: Long? = null,
    val mimeType: String? = null,
    val codec: String? = null,
    val qualityLabel: String? = null,
)

sealed interface ProviderFailure {
    val detail: String?
    data class Network(override val detail: String?) : ProviderFailure
    data class Authentication(override val detail: String?) : ProviderFailure
    data class RateLimited(override val detail: String?) : ProviderFailure
    data class Unavailable(override val detail: String?) : ProviderFailure
    data class Parse(override val detail: String?) : ProviderFailure
}

sealed interface ProviderResult<out T> {
    data class Success<T>(val value: T) : ProviderResult<T>
    data class Failure(val reason: ProviderFailure) : ProviderResult<Nothing>
}

interface MusicProvider { val descriptor: ProviderDescriptor }
interface SearchProvider : MusicProvider { suspend fun search(query: String, limit: Int = 20): ProviderResult<List<Track>> }
interface StreamProvider : MusicProvider { suspend fun resolve(reference: PlaybackReference.Provider, quality: StreamQuality = StreamQuality.Auto): ProviderResult<ResolvedStream> }
interface LoginProvider : MusicProvider {
    val loginUrl: String
    val cookieUrl: String
    suspend fun validate(cookie: String): ProviderResult<String>
}

class ProviderRegistry(providers: List<MusicProvider>) {
    private val byId = providers.associateBy { it.descriptor.id }
    val searchable = providers.filterIsInstance<SearchProvider>()
    fun stream(id: MusicSourceId) = byId[id] as? StreamProvider
    fun login(id: MusicSourceId) = byId[id] as? LoginProvider
}

class PlaybackResolver(private val registry: ProviderRegistry) {
    suspend fun resolve(reference: PlaybackReference, quality: StreamQuality = StreamQuality.Auto): ProviderResult<ResolvedStream> = when (reference) {
        is PlaybackReference.Local -> ProviderResult.Success(ResolvedStream(reference.contentUri))
        is PlaybackReference.Provider -> registry.stream(reference.sourceId)?.resolve(reference, quality)
            ?: ProviderResult.Failure(ProviderFailure.Unavailable("Source does not support playback"))
    }
}
