package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity
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
        sanitize(json.decodeFromString(ListSerializer(StremioMetaPreview.serializer()), settings.getString(KEY, "[]")))
    }.getOrDefault(emptyList())

    fun save(items: List<StremioMetaPreview>) {
        settings.putString(
            KEY,
            json.encodeToString(
                ListSerializer(StremioMetaPreview.serializer()),
                sanitize(items)
            )
        )
    }

    private fun sanitize(items: List<StremioMetaPreview>): List<StremioMetaPreview> =
        items.filter { it.id.isNotBlank() && it.type.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { identityKey(it) }.takeLast(MAX_ITEMS)

    private fun identityKey(item: StremioMetaPreview) =
        MediaIdentity.storageKey(item)

    private companion object {
        const val KEY = "cinemax.library.favorites.metadata.v2"
        const val MAX_ITEMS = 1_000
    }
}
