package com.aemusic.core.diagnostics

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticEvent(
    val timeEpochMs: Long,
    val category: String,
    val operation: String,
    val sourceId: String?,
    val mediaId: String?,
    val outcome: String,
    val detail: String?,
)

class DiagnosticEventStore(context: Context? = null) {
    private val file = context?.applicationContext?.filesDir?.resolve("diagnostics/playback-events.log")
    private val lock = Any()
    private val _events = MutableStateFlow(loadEvents())
    val events: StateFlow<List<DiagnosticEvent>> = _events.asStateFlow()

    fun record(category: String, operation: String, sourceId: String? = null, mediaId: String? = null, outcome: String, detail: String? = null) {
        val event = DiagnosticEvent(System.currentTimeMillis(), category, operation, sourceId, mediaId, outcome, sanitizeDiagnosticDetail(detail))
        synchronized(lock) {
            _events.value = (listOf(event) + _events.value).take(MAX_EVENTS)
            persistLocked()
        }
    }

    fun clear() { synchronized(lock) { _events.value = emptyList(); runCatching { file?.delete() } } }

    fun recordCrash(thread: Thread, error: Throwable) {
        val trace = error.stackTrace.take(16).joinToString(" <- ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
        record("crash", "uncaught", outcome = error::class.java.simpleName, detail = "thread=${thread.name}; ${error.message}; $trace")
    }

    fun report(): String = _events.value.joinToString("\n") { event ->
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(event.timeEpochMs))
        listOfNotNull(time, event.category, event.operation, event.sourceId, event.mediaId, event.outcome, event.detail).joinToString(" | ")
    }

    private fun loadEvents(): List<DiagnosticEvent> = runCatching {
        if (file?.isFile != true) return@runCatching emptyList()
        file.readLines().mapNotNull(::decodeEvent).take(MAX_EVENTS)
    }.getOrDefault(emptyList())

    private fun persistLocked() {
        runCatching {
            val destination = file ?: return@runCatching
            destination.parentFile?.mkdirs()
            val temporary = destination.resolveSibling("${destination.name}.tmp")
            temporary.writeText(_events.value.joinToString("\n", transform = ::encodeEvent))
            if (!temporary.renameTo(destination)) {
                destination.writeText(temporary.readText())
                temporary.delete()
            }
        }
    }

    private companion object {
        const val MAX_EVENTS = 100
        const val SEPARATOR = '\u001f'

        fun encodeEvent(event: DiagnosticEvent): String = listOf(
            event.timeEpochMs.toString(), event.category, event.operation, event.sourceId.orEmpty(),
            event.mediaId.orEmpty(), event.outcome, event.detail.orEmpty(),
        ).joinToString(SEPARATOR.toString()) { it.replace('\n', ' ').replace(SEPARATOR, ' ') }

        fun decodeEvent(line: String): DiagnosticEvent? {
            val values = line.split(SEPARATOR)
            if (values.size != 7) return null
            return DiagnosticEvent(
                values[0].toLongOrNull() ?: return null, values[1], values[2],
                values[3].takeIf(String::isNotEmpty), values[4].takeIf(String::isNotEmpty),
                values[5], values[6].takeIf(String::isNotEmpty),
            )
        }
    }
}

internal fun sanitizeDiagnosticDetail(value: String?): String? = value?.take(1_000)
    ?.replace(Regex("(?i)(cookie|music_u|music_a|csrf|token|key|params|signature)=([^;&\\s]+)"), "$1=<redacted>")
    ?.replace(Regex("https?://[^\\s]+"), "<url-redacted>")
