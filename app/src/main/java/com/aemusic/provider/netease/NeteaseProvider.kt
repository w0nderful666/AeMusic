package com.aemusic.provider.netease

import android.annotation.SuppressLint
import com.aemusic.core.model.*
import com.aemusic.core.network.AeNetworkClient
import com.aemusic.provider.*
import com.aemusic.core.diagnostics.DiagnosticEventStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class NeteaseProvider(
    private val http: AeNetworkClient,
    private val accountCookie: () -> String?,
    private val anonymousCookie: () -> String?,
    private val saveAnonymousCookie: (String) -> Unit,
    private val diagnostics: DiagnosticEventStore? = null,
) : SearchProvider, StreamProvider, LoginProvider {
    override val descriptor = ProviderDescriptor(SOURCE, "网易云音乐", ProviderCapabilities(true, true, true))
    override val loginUrl = "https://music.163.com/#/login"
    override val cookieUrl = "https://music.163.com/"

    override suspend fun validate(cookie: String): ProviderResult<String> = guarded {
        if (!hasLoginCookie(cookie)) return@guarded ProviderResult.Failure(ProviderFailure.Authentication("Missing MUSIC_U or MUSIC_A"))
        val profile = http.getJson("https://music.163.com/api/nuser/account/get", headers(cookie)).obj()?.obj("profile")
        if ((profile?.long("userId") ?: 0) <= 0) ProviderResult.Failure(ProviderFailure.Authentication("Expired account"))
        else ProviderResult.Success(profile?.text("nickname").orEmpty().ifBlank { "网易云用户" })
    }

    override suspend fun search(query: String, limit: Int): ProviderResult<List<Track>> = guarded {
        if (query.isBlank()) return@guarded ProviderResult.Success(emptyList())
        val fields = mapOf("s" to query.trim(), "type" to "1", "offset" to "0", "limit" to limit.coerceIn(1, 50).toString())
        val root = http.postFormJson("https://music.163.com/api/cloudsearch/pc", fields, searchHeaders()).obj()
        val songs = root?.obj("result")?.arr("songs").orEmpty()
        val parsed = songs.mapNotNull(::parseTrack)
        val missingArtworkIds = parsed.filter { it.artwork is ArtworkRef.Missing }.map { it.id.value }
        val artworkById = if (missingArtworkIds.isEmpty()) emptyMap() else fetchArtwork(missingArtworkIds)
        val result = parsed.map { track ->
            artworkById[track.id.value]?.let { track.copy(artwork = ArtworkRef.Reference(it)) } ?: track
        }
        diagnostics?.record("provider", "netease-search", SOURCE.value, outcome = "success", detail = "results=${result.size}; missingArtwork=${result.count { it.artwork is ArtworkRef.Missing }}")
        ProviderResult.Success(result)
    }

    suspend fun searchTracks(query: String, limit: Int): List<Track> = when (val result = search(query, limit)) {
        is ProviderResult.Success -> result.value
        is ProviderResult.Failure -> emptyList()
    }

    fun hasAccountLogin(): Boolean = accountCookie()?.takeIf(::hasAccountCookie) != null

    suspend fun userPlaylists(uid: String? = null): ProviderResult<List<OnlinePlaylist>> = guarded {
        val cookie = accountCookie()?.takeIf(::hasAccountCookie)
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Authentication("网易云未登录"))
        val userId = uid?.takeIf(String::isNotBlank) ?: run {
            val profile = http.getJson("https://music.163.com/api/nuser/account/get", headers(cookie)).obj()?.obj("profile")
            profile?.long("userId")?.takeIf { it > 0 }?.toString()
        } ?: return@guarded ProviderResult.Failure(ProviderFailure.Authentication("无法获取用户ID"))
        val root = http.getJson(
            "https://music.163.com/api/user/playlist?uid=$userId&limit=100&offset=0",
            headers(cookie),
        ).obj()
        val rawList = root?.arr("playlist").orEmpty()
        val playlists = rawList.mapNotNull { item ->
            val obj = item.obj() ?: return@mapNotNull null
            val id = obj.long("id").takeIf { it > 0 }?.toString() ?: return@mapNotNull null
            val name = obj.text("name").ifBlank { return@mapNotNull null }
            val cover = obj.text("coverImgUrl").replaceFirst(Regex("^http://"), "https://")
            val playCount = obj.long("playCount")
            val trackCount = obj.int("trackCount")
            val creator = obj.obj("creator")?.text("nickname").orEmpty()
            val desc = obj.text("description")
            OnlinePlaylist(id, name, cover, playCount, trackCount, creator, desc)
        }
        ProviderResult.Success(playlists)
    }

    suspend fun lyrics(mediaId: String): String? = guarded {
        val encoded = java.net.URLEncoder.encode(mediaId, Charsets.UTF_8.name())
        val root = http.getJson("https://music.163.com/api/song/lyric?id=$encoded&lv=1&kv=1&tv=-1", headers(effectiveCookie())).obj()
        ProviderResult.Success(root?.obj("lrc")?.text("lyric")?.takeIf(String::isNotBlank))
    }.let { (it as? ProviderResult.Success)?.value }

    override suspend fun resolve(reference: PlaybackReference.Provider, quality: StreamQuality): ProviderResult<ResolvedStream> = guarded {
        val sessionCookie = accountCookie()?.takeIf(::hasAccountCookie)?.let(::enrichAccountCookie) ?: ensureAnonymousCookie()
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Authentication("Unable to establish a NetEase session"))
        val level = when (quality) {
            StreamQuality.Standard -> "standard"
            StreamQuality.High, StreamQuality.Auto -> "exhigh"
            StreamQuality.Lossless -> "lossless"
        }
        val header = eapiHeader(sessionCookie)
        val payload = buildJsonObject {
            put("ids", "[${reference.mediaId}]")
            put("level", level)
            put("encodeType", "flac")
            put("e_r", true)
            put("header", header.toString())
        }.toString()
        val response = http.postFormBytesResponse(
            "https://interface.music.163.com/eapi/song/enhance/player/url/v1",
            mapOf("params" to encryptEapi("/api/song/enhance/player/url/v1", payload)),
            eapiHeaders(sessionCookie),
        )
        val root = http.json.parseToJsonElement(aesDecrypt(response.body)).obj()
        val data = root?.arr("data")?.firstOrNull()?.obj()
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Parse("NetEase returned no song data"))
        if (isTrialStream(data)) return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("NetEase returned a trial stream"))
        val candidate = parseStream(data, level)
            ?: return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("NetEase did not return a playable URL"))
        val probe = http.probeMedia(candidate.uri, candidate.headers)
        if (!probe.successful || !isLikelyAudioProbe(probe.contentType, probe.finalUrl)) {
            return@guarded ProviderResult.Failure(ProviderFailure.Unavailable("NetEase media probe failed: HTTP ${probe.statusCode}"))
        }
        val verified = candidate.copy(uri = probe.finalUrl, mimeType = candidate.mimeType ?: probe.contentType)
        diagnostics?.record("provider", "netease-eapi-v1", SOURCE.value, reference.mediaId, "success", verified.qualityLabel)
        ProviderResult.Success(verified)
    }

    private fun parseStream(data: JsonObject?, level: String): ResolvedStream? {
        data ?: return null
        val url = data.text("url")
        if (url.isBlank()) return null
        return ResolvedStream(
            uri = url.replaceFirst(Regex("^http://"), "https://"),
            headers = mediaHeaders(),
            expiresAtEpochMs = System.currentTimeMillis() + data.long("expi").coerceAtLeast(60) * 1000,
            bitrate = data.long("br").takeIf { it > 0 },
            mimeType = data.text("type").takeIf(String::isNotBlank),
            codec = data.text("encodeType").takeIf(String::isNotBlank),
            qualityLabel = data.text("level").ifBlank { level },
        )
    }

    private fun isTrialStream(data: JsonObject): Boolean = isNeteasePreview(data)

    private fun eapiHeader(cookie: String): JsonObject {
        return buildJsonObject {
            put("clientSign", cookieValue(cookie, "clientSign").orEmpty())
            put("os", cookieValue(cookie, "os") ?: "pc")
            put("appver", cookieValue(cookie, "appver") ?: "3.1.3.203419")
            put("deviceId", cookieValue(cookie, "deviceId").orEmpty())
            put("requestId", 0)
            put("osver", cookieValue(cookie, "osver").orEmpty())
        }
    }

    internal fun parseTrack(element: JsonElement): Track? {
        val item = element.obj() ?: return null
        val id = item.long("id").takeIf { it > 0 }?.toString() ?: return null
        val artists = (item.arr("artists") ?: item.arr("ar")).orEmpty().mapNotNull { it.obj()?.text("name")?.takeIf(String::isNotBlank)?.let(::Artist) }
        val album = item.obj("album") ?: item.obj("al")
        val duration = item.long("duration").takeIf { it > 0 } ?: item.long("dt")
        val artwork = album?.text("picUrl")?.ifBlank { album.text("pic_url") }?.ifBlank { album.text("blurPicUrl") }.orEmpty().secureHttpUrl()
        return Track(TrackId(id), SOURCE, item.text("name").ifBlank { return null }, artists, album?.text("name")?.takeIf(String::isNotBlank)?.let(::Album), TrackDuration.ofMilliseconds(duration), artwork.takeIf(String::isNotBlank)?.let(ArtworkRef::Reference) ?: ArtworkRef.Missing, PlaybackReference.Provider(SOURCE, id))
    }

    private suspend fun ensureAnonymousCookie(): String? {
        anonymousCookie()?.takeIf(::hasAnonymousCookie)?.let { return it }
        val deviceCookie = newAnonymousDeviceCookie()
        val header = eapiHeader(deviceCookie)
        val path = "/api/register/anonimous"
        val payload = buildJsonObject {
            put("username", anonymousUsername(cookieValue(deviceCookie, "deviceId").orEmpty()))
            put("e_r", true)
            put("header", header.toString())
        }.toString()
        val response = networkAttempt {
            http.postFormBytesResponse(
                "https://interface.music.163.com/eapi/register/anonimous",
                mapOf("params" to encryptEapi(path, payload)),
                eapiHeaders(deviceCookie),
            )
        } ?: return null
        val code = runCatching { http.json.parseToJsonElement(aesDecrypt(response.body)).obj()?.long("code") ?: 0L }.getOrDefault(0L)
        if (code !in setOf(200L, 201L)) return null
        val values = cookieMap(deviceCookie).toMutableMap()
        response.setCookies.mapNotNull(::parseSetCookie).forEach { (key, value) ->
            if (key in ANONYMOUS_COOKIE_FIELDS && value.isNotBlank()) values[key] = value
        }
        values["WEVNSM"] = "1.0.0"
        val combined = values.entries.joinToString("; ") { (key, value) -> "$key=$value" }
        if (!hasAnonymousCookie(combined)) return null
        saveAnonymousCookie(combined)
        diagnostics?.record("provider", "netease-anonymous-session", SOURCE.value, outcome = "success", detail = "MUSIC_A acquired")
        return combined
    }

    internal fun effectiveCookie(): String? =
        accountCookie()?.takeIf(::hasAccountCookie) ?: anonymousCookie()?.takeIf(::hasAnonymousCookie)

    private fun mediaHeaders() = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to "*/*",
        "Accept-Encoding" to "identity",
    )

    private fun newAnonymousDeviceCookie(): String {
        val random = SecureRandom()
        fun hex(bytes: Int) = ByteArray(bytes).also(random::nextBytes).joinToString("") { "%02X".format(it.toInt() and 0xff) }
        val deviceId = hex(26)
        val mac = ByteArray(6).also(random::nextBytes).joinToString(":") { "%02X".format(it.toInt() and 0xff) }
        val clientSign = "$mac@@@${hex(4)}@@@@@@${hex(32).lowercase()}"
        return "os=pc; deviceId=$deviceId; osver=Microsoft-Windows-10--build-26000-64bit; " +
            "clientSign=$clientSign; channel=netease; mode=AeMusic; appver=3.1.3.203419"
    }

    private fun enrichAccountCookie(accountCookie: String): String {
        val trimmed = accountCookie.trim()
        if (trimmed.contains("os=") && trimmed.contains("appver=")) return trimmed
        return "${newAnonymousDeviceCookie()}; $trimmed"
    }

    private fun anonymousUsername(deviceId: String): String {
        val transformed = deviceId.mapIndexed { index, char ->
            (char.code xor ANONYMOUS_XOR_KEY[index % ANONYMOUS_XOR_KEY.length].code).toChar()
        }.joinToString("")
        val digest = MessageDigest.getInstance("MD5").digest(transformed.toByteArray())
        val inner = Base64.getEncoder().encodeToString(digest)
        return Base64.getEncoder().encodeToString("$deviceId $inner".toByteArray())
    }

    private fun cookieMap(cookie: String): Map<String, String> = cookie.split(';').mapNotNull { part ->
        part.trim().takeIf { '=' in it }?.let { it.substringBefore('=') to it.substringAfter('=') }
    }.toMap()

    private fun parseSetCookie(value: String): Pair<String, String>? = value.substringBefore(';').trim().takeIf { '=' in it }
        ?.let { it.substringBefore('=') to it.substringAfter('=') }

    private suspend fun fetchArtwork(ids: List<String>): Map<String, String> {
        val idJson = ids.joinToString(prefix = "[", postfix = "]")
        val encoded = java.net.URLEncoder.encode(idJson, Charsets.UTF_8.name())
        val root = networkAttempt {
            http.getJson("https://music.163.com/api/song/detail/?ids=$encoded", headers(effectiveCookie())).obj()
        }
        return root?.arr("songs").orEmpty().mapNotNull { element ->
            val song = element.obj() ?: return@mapNotNull null
            val id = song.long("id").takeIf { it > 0 }?.toString() ?: return@mapNotNull null
            val album = song.obj("album") ?: song.obj("al")
            val artwork = album?.text("picUrl")?.ifBlank { album.text("pic_url") }.orEmpty().secureHttpUrl()
            artwork.takeIf(String::isNotBlank)?.let { id to it }
        }.toMap()
    }

    private fun searchHeaders() = mapOf("User-Agent" to DESKTOP_BROWSER_UA, "Referer" to "https://music.163.com")
    private fun headers(cookie: String?) = searchHeaders() + listOfNotNull(cookie?.takeIf(String::isNotBlank)?.let { "Cookie" to it }).toMap()
    private fun eapiHeaders(cookie: String) = mapOf("User-Agent" to USER_AGENT, "Origin" to "orpheus://orpheus", "Accept" to "*/*", "Cookie" to cookie)
    private fun encryptEapi(path: String, payload: String): String {
        val digest = md5("nobody${path}use${payload}md5forencrypt")
        return aesHex("$path-36cd479b6b5-$payload-36cd479b6b5-$digest")
    }
    @SuppressLint("GetInstance")
    private fun aesDecrypt(value: ByteArray): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(), "AES"))
        return cipher.doFinal(value).toString(Charsets.UTF_8)
    }
    // EAPI defines this exact legacy transport transform; it is not used for credential storage.
    @SuppressLint("GetInstance")
    private fun aesHex(value: String): String { val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(), "AES")); return cipher.doFinal(value.toByteArray()).joinToString("") { "%02X".format(it.toInt() and 0xff) } }

    companion object {
        val SOURCE = MusicSourceId("netease")
        val ANONYMOUS_SOURCE = MusicSourceId("netease-anonymous")
        private const val EAPI_KEY = "e82ckenh8dichen8"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.3.203419"
        private const val DESKTOP_BROWSER_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val ANONYMOUS_XOR_KEY = "3go8&$8*3*3h0k(2)2"
        private val ANONYMOUS_COOKIE_FIELDS = setOf("MUSIC_A", "NMTID", "__csrf")
        internal fun hasLoginCookie(cookie: String) = hasAccountCookie(cookie)
        internal fun hasAccountCookie(cookie: String) = cookieValue(cookie, "MUSIC_U")?.isNotBlank() == true
        internal fun hasAnonymousCookie(cookie: String) = cookieValue(cookie, "MUSIC_A")?.isNotBlank() == true
        private fun cookieValue(cookie: String, name: String) = cookie.split(';').map(String::trim).firstOrNull { it.startsWith("$name=") }?.substringAfter('=')
        internal fun md5(value: String) = MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

internal fun isLikelyAudioProbe(contentType: String?, finalUrl: String): Boolean {
    val type = contentType.orEmpty().substringBefore(';').trim().lowercase()
    if (type.startsWith("audio/") || type == "application/octet-stream") return true
    if (type.startsWith("text/") || type.contains("html") || type.contains("json")) return false
    val path = finalUrl.substringBefore('?').lowercase()
    return listOf(".mp3", ".m4a", ".aac", ".flac", ".ogg", ".opus", ".wav").any(path::endsWith)
}

internal fun isNeteasePreview(data: JsonObject): Boolean =
    data["freeTrialInfo"]?.let { it !is JsonNull } == true ||
        data["freeTrialSegmentInfo"]?.let { it !is JsonNull } == true || data.bool("freeTrial")

private suspend fun <T> guarded(block: suspend () -> ProviderResult<T>): ProviderResult<T> = try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (e: Exception) { ProviderResult.Failure(ProviderFailure.Network(e.message)) }
private suspend fun <T> networkAttempt(block: suspend () -> T): T? = try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
private fun String.secureHttpUrl(): String = replaceFirst(Regex("^http://"), "https://")
