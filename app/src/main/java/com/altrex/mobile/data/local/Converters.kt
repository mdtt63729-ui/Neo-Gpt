package com.altrex.mobile.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Room type converters for serialising complex types to and from JSON strings
 * stored in database columns.
 *
 * Uses kotlinx.serialization throughout. The [Json] instance is configured with
 * [ignoreUnknownKeys] and [encodeDefaults] for forward-compatibility with model
 * changes.
 */
class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    // ── String lists ────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return json.encodeToString(ListSerializer(String.serializer()), value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return null
        return json.decodeFromString(ListSerializer(String.serializer()), value)
    }

    // ── Int lists ───────────────────────────────────────────────────────────

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        if (value == null) return null
        return json.encodeToString(ListSerializer(Int.serializer()), value)
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrEmpty()) return null
        return json.decodeFromString(ListSerializer(Int.serializer()), value)
    }

    // ── Long lists ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        if (value == null) return null
        return json.encodeToString(ListSerializer(Long.serializer()), value)
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        if (value.isNullOrEmpty()) return null
        return json.decodeFromString(ListSerializer(Long.serializer()), value)
    }

    // ── String-to-String maps ───────────────────────────────────────────────

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        if (value == null) return null
        return json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            value
        )
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        if (value.isNullOrEmpty()) return null
        return json.decodeFromString(
            MapSerializer(String.serializer(), String.serializer()),
            value
        )
    }

    // ── JsonObject (arbitrary JSON) ─────────────────────────────────────────

    @TypeConverter
    fun fromJsonObject(value: JsonObject?): String? {
        if (value == null) return null
        return json.encodeToString(JsonObject.serializer(), value)
    }

    @TypeConverter
    fun toJsonObject(value: String?): JsonObject? {
        if (value.isNullOrEmpty()) return null
        return json.decodeFromString(JsonObject.serializer(), value)
    }

    // ── Boolean wrapper (nullable) ───────────────────────────────────────────

    @TypeConverter
    fun fromBoolean(value: Boolean?): Int? {
        return when (value) {
            null -> null
            true -> 1
            false -> 0
        }
    }

    @TypeConverter
    fun toBoolean(value: Int?): Boolean? {
        return when (value) {
            null -> null
            1 -> true
            0 -> false
            else -> null
        }
    }
}
