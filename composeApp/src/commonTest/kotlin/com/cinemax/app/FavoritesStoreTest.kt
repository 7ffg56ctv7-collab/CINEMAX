package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesStoreTest {
    @Test
    fun favoritesPersistWithMetadataAcrossStoreInstances() {
        val settings = MapSettings()
        val first = FavoritesStore(settings)
        val saved = listOf(
            StremioMetaPreview(
                id = "tt1234567", type = "movie", name = "Example Film",
                poster = "https://example.com/poster.jpg",
                background = "https://example.com/background.jpg",
                description = "Saved description", releaseInfo = "2025", imdbRating = "8.1"
            )
        )
        first.save(saved)
        assertEquals(saved, FavoritesStore(settings).load())
    }

    @Test
    fun canonicalIdentityDeduplicatesSameTitleAcrossAddonIds() {
        val store = FavoritesStore(MapSettings())
        store.save(listOf(
            StremioMetaPreview("tt1234567", "movie", "Example Film", releaseInfo = "2025"),
            StremioMetaPreview("tt1234567", "movie", "Different display title", releaseInfo = "2024")
        ))
        assertEquals(1, store.load().size)
    }

    @Test
    fun opaqueIdDoesNotMergeDifferentTitles() {
        val store = FavoritesStore(MapSettings())
        store.save(listOf(
            StremioMetaPreview("catalog-7", "movie", "Dune", releaseInfo = "2021"),
            StremioMetaPreview("catalog-7", "movie", "Arrival", releaseInfo = "2016")
        ))
        assertEquals(2, store.load().size)
    }

    @Test
    fun invalidStoredDataFallsBackToEmptyFavorites() {
        val settings = MapSettings()
        settings.putString("cinemax.library.favorites.metadata.v2", "not-json")
        assertEquals(emptyList(), FavoritesStore(settings).load())
    }
}
