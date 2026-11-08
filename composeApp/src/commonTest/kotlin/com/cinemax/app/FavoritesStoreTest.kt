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
                id = "tt001", type = "movie", name = "Example Film",
                poster = "https://example.com/poster.jpg",
                background = "https://example.com/background.jpg",
                description = "Saved description", releaseInfo = "2025", imdbRating = "8.1"
            )
        )
        first.save(saved)

        val restored = FavoritesStore(settings).load()

        assertEquals(saved, restored)
    }

    @Test
    fun invalidStoredDataFallsBackToEmptyFavorites() {
        val settings = MapSettings()
        settings.putString("cinemax.library.favorites.metadata.v2", "not-json")

        assertEquals(emptyList(), FavoritesStore(settings).load())
    }
}
