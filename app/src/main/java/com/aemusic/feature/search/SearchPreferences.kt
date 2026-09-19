package com.aemusic.feature.search

import android.content.Context
import androidx.core.content.edit

class SearchPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("search_preferences", Context.MODE_PRIVATE)
    var selectedSources: Set<String>
        get() = preferences.getStringSet("sources", setOf("local"))?.toSet().orEmpty().ifEmpty { setOf("local") }
        set(value) = preferences.edit { putStringSet("sources", value) }
    var history: List<String>
        get() = preferences.getString("history", "").orEmpty().split(SEPARATOR).filter(String::isNotBlank)
        set(value) = preferences.edit { putString("history", value.take(MAX_HISTORY).joinToString(SEPARATOR)) }

    fun record(query: String): List<String> = (listOf(query.trim()) + history.filterNot { it.equals(query.trim(), true) })
        .filter(String::isNotBlank).take(MAX_HISTORY).also { history = it }

    private companion object { const val MAX_HISTORY = 8; const val SEPARATOR = "\u001f" }
}
