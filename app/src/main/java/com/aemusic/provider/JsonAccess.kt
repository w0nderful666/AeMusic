package com.aemusic.provider

import kotlinx.serialization.json.*

internal fun JsonElement.obj() = this as? JsonObject
internal fun JsonObject.obj(key: String) = this[key] as? JsonObject
internal fun JsonObject.arr(key: String) = this[key] as? JsonArray
private fun JsonObject.primitive(key: String) = this[key] as? JsonPrimitive
internal fun JsonObject.text(key: String) = primitive(key)?.contentOrNull.orEmpty()
internal fun JsonObject.long(key: String) = primitive(key)?.longOrNull ?: 0L
internal fun JsonObject.int(key: String) = primitive(key)?.intOrNull ?: 0
internal fun JsonObject.bool(key: String) = primitive(key)?.booleanOrNull ?: false
