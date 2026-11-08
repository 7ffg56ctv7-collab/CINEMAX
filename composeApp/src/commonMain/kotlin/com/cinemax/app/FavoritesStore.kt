package com.cinemax.app

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Local watchlist persistence. IDs are stored independently of the current catalog page. */
class FavoritesStore(
    private val settings: Settings = Settings()
) {
    private val json = Json
    fun load(): Set<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), settings.getString(KEY, "[]")).toSet()
    }.getOrDefault(emptySet())

    fun save(ids: Set<String>) {
        settings.putString(KEY, json.encodeToString(ListSerializer(String.serializer()), ids.toList()))
    }

    private companion object { const val KEY = "cinemax.library.favorites.v1" }
}
