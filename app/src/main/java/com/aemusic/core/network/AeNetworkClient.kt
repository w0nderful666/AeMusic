package com.aemusic.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class AeNetworkClient {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true).build()

    suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement = execute(
        Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build(),
    )

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String = await(
        Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build(),
    ) { response ->
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        response.body.string().also { if (it.length > MAX_JSON_CHARS) throw IOException("Response too large") }
    }

    suspend fun postFormJson(url: String, fields: Map<String, String>, headers: Map<String, String> = emptyMap()): JsonElement {
        val body = FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()
        return execute(Request.Builder().url(url).post(body).apply { headers.forEach { (k, v) -> header(k, v) } }.build())
    }

    suspend fun postFormJsonResponse(
        url: String,
        fields: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): NetworkJsonResponse {
        val body = FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()
        return await(Request.Builder().url(url).post(body).apply { headers.forEach { (k, v) -> header(k, v) } }.build()) { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val text = response.body.string()
            if (text.length > MAX_JSON_CHARS) throw IOException("Response too large")
            NetworkJsonResponse(json.parseToJsonElement(text), response.headers.values("Set-Cookie"))
        }
    }

    suspend fun postFormBytesResponse(
        url: String,
        fields: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): NetworkBytesResponse {
        val body = FormBody.Builder().apply { fields.forEach { (k, v) -> add(k, v) } }.build()
        return await(Request.Builder().url(url).post(body).apply { headers.forEach { (k, v) -> header(k, v) } }.build()) { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val bytes = response.body.bytes()
            if (bytes.size > MAX_JSON_BYTES) throw IOException("Response too large")
            NetworkBytesResponse(bytes, response.headers.values("Set-Cookie"))
        }
    }

    suspend fun probeMedia(url: String, headers: Map<String, String> = emptyMap()): NetworkProbe {
        val request = Request.Builder().url(url).header("Range", "bytes=0-0")
            .apply { headers.forEach { (key, value) -> header(key, value) } }.build()
        return await(request) { response ->
            val contentType = response.header("Content-Type")
            val firstByte = if (response.isSuccessful) response.body.byteStream().read() else -1
            NetworkProbe(
                successful = response.isSuccessful && firstByte >= 0,
                statusCode = response.code,
                finalUrl = response.request.url.toString(),
                contentType = contentType,
                contentLength = response.header("Content-Length")?.toLongOrNull(),
                contentRange = response.header("Content-Range"),
                receivedMediaByte = firstByte >= 0,
            )
        }
    }

    private suspend fun execute(request: Request): JsonElement = await(request) { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body.string()
            if (body.length > MAX_JSON_CHARS) throw IOException("Response too large")
            json.parseToJsonElement(body)
    }

    private suspend fun <T> await(request: Request, read: (Response) -> T): T = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val result = runCatching { read(it) }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            }
        })
    }

    private companion object {
        const val MAX_JSON_CHARS = 4_000_000
        const val MAX_JSON_BYTES = 4_000_000
    }
}

data class NetworkProbe(
    val successful: Boolean,
    val statusCode: Int,
    val finalUrl: String,
    val contentType: String?,
    val contentLength: Long? = null,
    val contentRange: String? = null,
    val receivedMediaByte: Boolean = false,
)

data class NetworkJsonResponse(
    val body: JsonElement,
    val setCookies: List<String>,
)

data class NetworkBytesResponse(
    val body: ByteArray,
    val setCookies: List<String>,
)
