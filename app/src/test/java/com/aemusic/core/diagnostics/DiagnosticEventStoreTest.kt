package com.aemusic.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticEventStoreTest {
    @Test fun secretsAndUrlsAreRedacted() {
        val sanitized = sanitizeDiagnosticDetail("cookie=abc MUSIC_U=secret https://cdn.example/audio?id=1")!!
        assertFalse(sanitized.contains("abc"))
        assertFalse(sanitized.contains("secret"))
        assertFalse(sanitized.contains("cdn.example"))
        assertTrue(sanitized.contains("<redacted>"))
    }

    @Test fun eventBufferKeepsNewestHundred() {
        val store = DiagnosticEventStore()
        repeat(105) { store.record("test", "event-$it", outcome = "ok") }
        assertEquals(100, store.events.value.size)
        assertEquals("event-104", store.events.value.first().operation)
        assertEquals("event-5", store.events.value.last().operation)
    }
}
