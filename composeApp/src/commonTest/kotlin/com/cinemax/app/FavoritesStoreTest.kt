package com.cinemax.app

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesStoreTest {
    @Test
    fun favoritesPersistAcrossStoreInstances() {
        val settings = MapSettings()
        val first = FavoritesStore(settings)
        first.save(setOf("tt001", "tt002"))

        val restored = FavoritesStore(settings).load()

        assertEquals(setOf("tt001", "tt002"), restored)
    }

    @Test
    fun invalidStoredDataFallsBackToEmptyFavorites() {
        val settings = MapSettings()
        settings.putString("cinemax.library.favorites.v1", "not-json")

        assertEquals(emptySet(), FavoritesStore(settings).load())
    }
}
