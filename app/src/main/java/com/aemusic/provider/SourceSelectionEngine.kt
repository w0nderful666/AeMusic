package com.aemusic.provider

import com.aemusic.core.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.withTimeoutOrNull

data class SourceCandidate(
    val track: Track,
    val stream: ResolvedStream,
    val matchScore: Int,
)

class SourceSelectionEngine(private val registry: ProviderRegistry) {
    private val candidateCache = ConcurrentHashMap<String, List<SourceCandidate>>()

    fun clearCache() {
        candidateCache.clear()
    }

    suspend fun candidates(original: Track, query: String = defaultQuery(original)): List<SourceCandidate> {
        val cacheKey = "${original.sourceId.value}:${original.id.value}:$query"
        candidateCache[cacheKey]?.let { return it }

        val results = coroutineScope {
            registry.searchable.map { provider ->
                async {
                    val tracks = withTimeoutOrNull(2500) {
                        val result = provider.search(query, SEARCH_LIMIT)
                        (result as? ProviderResult.Success)?.value.orEmpty()
                            .filterNot { it.sourceId == original.sourceId && it.id == original.id }
                    }.orEmpty()

                    val streamProvider = registry.stream(provider.descriptor.id) ?: return@async emptyList()
                    val matches = tracks.asSequence().map { it to sourceMatchScore(original, it) }
                        .filter { (_, score) -> score >= MIN_MANUAL_SCORE }
                        .sortedByDescending { it.second }
                        .take(MAX_RESOLVE_PER_PROVIDER)
                        .toList()

                    matches.map { (track, score) ->
                        async {
                            withTimeoutOrNull(1800) {
                                when (val resolved = streamProvider.resolve(track.playbackRef as com.aemusic.core.model.PlaybackReference.Provider, StreamQuality.High)) {
                                    is ProviderResult.Success -> SourceCandidate(track, resolved.value, score)
                                    is ProviderResult.Failure -> null
                                }
                            }
                        }
                    }.mapNotNull { it.await() }
                }
            }.map { it.await() }.flatten()
                .distinctBy { it.track.sourceId to it.track.id }
                .sortedWith(compareByDescending<SourceCandidate> { it.matchScore }.thenByDescending { it.stream.bitrate ?: 0 })
                .take(MAX_RESULTS)
        }

        if (results.isNotEmpty()) {
            candidateCache[cacheKey] = results
        }
        return results
    }

    companion object {
        const val HIGH_CONFIDENCE_SCORE = 60
        private const val MIN_MANUAL_SCORE = 40
        private const val SEARCH_LIMIT = 5
        private const val MAX_RESOLVE_PER_PROVIDER = 2
        private const val MAX_RESULTS = 8
        fun defaultQuery(track: Track): String {
            val rawArtist = track.artists.firstOrNull()?.name.orEmpty()
            val cleanTitle = track.title.replace(Regex("[【\\[(（].*?[】\\])）]"), " ")
                .replace(Regex("[^\\p{L}\\p{N}\\s]+"), " ").trim()
                .split(Regex("\\s+")).take(3).joinToString(" ")
            val cleanArtist = rawArtist.replace(Regex("[【\\[(（].*?[】\\])）]"), " ")
                .replace(Regex("[^\\p{L}\\p{N}\\s]+"), " ").trim()
                .split(Regex("\\s+")).firstOrNull().orEmpty()
            val fallbackTitle = track.title.take(30).replace(Regex("[^\\p{L}\\p{N}\\s]+"), " ").trim()
            val queryTitle = cleanTitle.ifBlank { fallbackTitle }
            return listOf(queryTitle, cleanArtist).filter(String::isNotBlank).joinToString(" ")
        }
    }
}

internal fun sourceMatchScore(expected: Track, candidate: Track): Int {
    val expectedAliases = extractAliases(expected.title).map(::normalizeSourceText).filter(String::isNotBlank)
    val candidateAliases = extractAliases(candidate.title).map(::normalizeSourceText).filter(String::isNotBlank)
    if (expectedAliases.isEmpty() || candidateAliases.isEmpty()) return 0

    var titleScore = 0
    for (exp in expectedAliases) {
        for (cand in candidateAliases) {
            when {
                exp == cand -> titleScore = maxOf(titleScore, 55)
                exp.contains(cand) || cand.contains(exp) -> titleScore = maxOf(titleScore, 45)
                else -> titleScore = maxOf(titleScore, tokenScore(exp, cand) * 45 / 100)
            }
        }
    }
    var score = titleScore

    val expectedVersions = sourceVersionTokens(expected.title)
    val candidateVersions = sourceVersionTokens(candidate.title)
    if (expectedVersions != candidateVersions && (expectedVersions.isNotEmpty() || candidateVersions.isNotEmpty())) score -= 35

    val expectedArtistAliases = expected.artists.flatMap { extractAliases(it.name) }.map(::normalizeSourceText).filter(String::isNotBlank)
    val candidateArtistAliases = candidate.artists.flatMap { extractAliases(it.name) }.map(::normalizeSourceText).filter(String::isNotBlank)
    if (expectedArtistAliases.any { left -> candidateArtistAliases.any { right -> left == right || left.contains(right) || right.contains(left) } }) {
        score += 30
    }

    val expectedDuration = expected.duration.milliseconds
    val candidateDuration = candidate.duration.milliseconds
    if (expectedDuration > 0 && candidateDuration > 0) score += when (kotlin.math.abs(expectedDuration - candidateDuration)) {
        in 0..2_000 -> 20
        in 2_001..5_000 -> 12
        in 5_001..10_000 -> 3
        else -> -20
    }
    return score.coerceIn(0, 100)
}

internal fun extractAliases(text: String): List<String> {
    val list = mutableListOf(text)
    val inside = Regex("[【\\[(（](.*?)[】\\])）]").findAll(text).map { it.groupValues[1].trim() }.filter(String::isNotBlank)
    list.addAll(inside)
    val stripped = text.replace(Regex("[【\\[(（].*?[】\\])）]"), " ").trim()
    if (stripped.isNotBlank()) list.add(stripped)
    val slashParts = text.split('/', '&', ',').map(String::trim).filter(String::isNotBlank)
    if (slashParts.size > 1) list.addAll(slashParts)
    return list.distinct()
}

private fun normalizeSourceText(value: String) = value.lowercase()
    .replace(Regex("(official|audio|video|lyrics?|mv|hd|4k|官方|完整版|动态歌词|音乐视频)"), " ")
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

private fun tokenScore(first: String, second: String): Int {
    val left = first.split(' ').filter(String::isNotBlank).toSet()
    val right = second.split(' ').filter(String::isNotBlank).toSet()
    if (left.isEmpty() || right.isEmpty()) return 0
    return (left.intersect(right).size * 100f / left.union(right).size).toInt()
}

private fun sourceVersionTokens(value: String): Set<String> = mapOf(
    "live" to listOf("live", "现场"), "remix" to listOf("remix", "混音"),
    "instrumental" to listOf("instrumental", "伴奏", "纯音乐"), "cover" to listOf("cover", "翻唱"),
    "speed" to listOf("sped up", "slowed", "加速版", "慢速版"),
).filterValues { markers -> markers.any(value.lowercase()::contains) }.keys
