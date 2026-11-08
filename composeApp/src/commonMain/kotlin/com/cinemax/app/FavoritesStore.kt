package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A metadata-backed watchlist that survives addon and catalog changes. */
class FavoritesStore(
    private val settings: Settings = Settings()
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<StremioMetaPreview> = runCatching {
        json.decodeFromString(ListSerializer(StremioMetaPreview.serializer()), settings.getString(KEY, "[]"))
    }.getOrDefault(emptyList())

    fun save(items: List<StremioMetaPreview>) {
        settings.putString(KEY, json.encodeToString(ListSerializer(StremioMetaPreview.serializer()), items.distinctBy { it.id }))
    }

    private companion object { const val KEY = "cinemax.library.favorites.metadata.v2" }
}
