package com.aemusic.core.cloud

import com.aemusic.core.data.TrackedPlaylist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudSyncRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun testWebDav(serverUrl: String, username: String, password: String): Result<String> = ioResult {
        val request = Request.Builder()
            .url(requireHttpsUrl(serverUrl))
            .header("Authorization", Credentials.basic(username.trim(), password))
            .build()
        execute(request) { response ->
            if (response.isSuccessful || response.code == 405) "HTTP ${response.code}"
            else error("HTTP ${response.code}")
        }
    }

    suspend fun testNavidrome(serverUrl: String, username: String, password: String): Result<String> = ioResult {
        val salt = randomSalt()
        val token = md5(password + salt)
        val base = requireHttpsUrl(serverUrl).trimEnd('/')
        val request = Request.Builder()
            .url("$base/rest/ping.view?u=${encode(username.trim())}&t=$token&s=$salt&v=1.16.1&c=AeMusic&f=json")
            .build()
        execute(request) { response ->
            if (response.isSuccessful) "Subsonic ping ok" else error("HTTP ${response.code}")
        }
    }

    suspend fun backupPlaylists(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String,
        playlists: List<TrackedPlaylist>,
    ): Result<Int> = ioResult {
        val request = Request.Builder()
            .url(targetUrl(serverUrl, remotePath))
            .header("Authorization", Credentials.basic(username.trim(), password))
            .put(json.encodeToString(playlists).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request) { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} ${response.message}")
        }
        playlists.size
    }

    suspend fun restorePlaylists(
        serverUrl: String,
        username: String,
        password: String,
        remotePath: String,
    ): Result<List<TrackedPlaylist>> = ioResult {
        val request = Request.Builder()
            .url(targetUrl(serverUrl, remotePath))
            .header("Authorization", Credentials.basic(username.trim(), password))
            .build()
        execute(request) { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} ${response.message}")
            val payload = response.body.string()
            require(payload.length <= MAX_BACKUP_CHARS) { "Backup response is too large" }
            json.decodeFromString<List<TrackedPlaylist>>(payload)
        }
    }

    private fun targetUrl(serverUrl: String, remotePath: String): String {
        val base = requireHttpsUrl(serverUrl).trimEnd('/')
        val path = remotePath.trim().trim('/')
        return when {
            path.isBlank() -> "$base/AeMusic/playlists.json"
            path.endsWith(".json", ignoreCase = true) -> "$base/$path"
            else -> "$base/$path/playlists.json"
        }
    }

    private fun requireHttpsUrl(value: String): String {
        val url = value.trim()
        require(url.startsWith("https://")) { "Cloud server must use HTTPS" }
        return url
    }

    private suspend inline fun <T> ioResult(crossinline block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun <T> execute(request: Request, read: (Response) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            val value = read(it)
                            if (continuation.isActive) continuation.resume(value)
                        } catch (error: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                    }
                }
            })
        }

    private fun randomSalt(): String = ByteArray(8).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
    private fun md5(value: String): String = MessageDigest.getInstance("MD5")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private companion object {
        const val MAX_BACKUP_CHARS = 2_000_000
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
