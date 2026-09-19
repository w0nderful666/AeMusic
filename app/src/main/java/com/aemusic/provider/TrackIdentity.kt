package com.aemusic.provider

data class RecognizedTrackIdentity(
    val title: String,
    val artist: String,
    val changed: Boolean,
)

private val bracketedTitle = Regex("《(.*?)》")
private val leadingNumber = Regex("^\\s*\\d+[.\\-\\s_、]+")
private val bracketNoise = Regex("[【\\[(（].*?[】\\])）]")
private val quoteNoise = Regex("['\"‘’“”`·•]")
private val marketingNoise = Regex(
    "(hi-?res|1080p|4k|hd|无损|音质|高品质|完整版|官方|mv|现场|live|全集|合集|纯享|翻唱|伴奏|自制|动态歌词)",
    RegexOption.IGNORE_CASE,
)
private val separators = Regex("[-—_|/｜·•~～\\s]+")

fun recognizeTrackIdentity(rawTitle: String, rawArtist: String = ""): RecognizedTrackIdentity {
    if (rawTitle.isBlank()) return RecognizedTrackIdentity("", rawArtist, false)
    val titleWithoutNumber = rawTitle.replace(leadingNumber, "").trim()
    bracketedTitle.find(titleWithoutNumber)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)?.let { title ->
        val remainder = titleWithoutNumber.replace("《$title》", " ")
            .replace(bracketNoise, " ").replace(quoteNoise, " ").replace(marketingNoise, " ")
        val artist = remainder.split(separators).firstOrNull { candidate ->
            candidate.isNotBlank() && candidate.length <= 20 && listOf("音质", "无损", "故事", "制作", "出品", "演唱").none(candidate::contains)
        } ?: rawArtist
        return RecognizedTrackIdentity(title, artist, title != rawTitle || artist != rawArtist)
    }
    val parts = titleWithoutNumber.replace(bracketNoise, " ").replace(quoteNoise, " ").replace(marketingNoise, " ")
        .split(separators).filter(String::isNotBlank)
    val (title, artist) = when {
        parts.size >= 2 && rawArtist.isNotBlank() && (parts[0].contains(rawArtist) || parts[0].length <= 4) -> parts[1] to parts[0]
        parts.size >= 2 -> parts[0] to parts[1]
        parts.size == 1 -> parts[0] to rawArtist
        else -> rawTitle to rawArtist
    }
    return RecognizedTrackIdentity(title, artist, title != rawTitle || artist != rawArtist)
}
