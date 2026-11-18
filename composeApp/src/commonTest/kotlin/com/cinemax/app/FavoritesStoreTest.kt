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
    @Test fun blankEntriesAreNotRestoredToWatchlist() {
        val store = FavoritesStore(MapSettings())
        store.save(listOf(
            StremioMetaPreview("", "movie", "Bad"),
            StremioMetaPreview("valid", "movie", "Good"),
            StremioMetaPreview("bad", "movie", " ")
        ))
        assertEquals(listOf("Good"), store.load().map { it.name })
    }

    @Test fun watchlistIsCappedToProtectMemory() {
        val store = FavoritesStore(MapSettings())
        store.save((0..1001).map { StremioMetaPreview("id$it", "movie", "Title $it") })
        assertEquals(1_000, store.load().size)
        assertEquals("id1001", store.load().last().id)
    }

    @Test fun favoriteEpisodesOfSameSeriesHaveIndependentKeys() {
        val store = FavoritesStore(MapSettings())
        val show = StremioMetaPreview("tt1234567", "series", "Show")
        val first = com.cinemax.app.core.EpisodeCatalog.playbackPreview(show,
            com.cinemax.app.core.StremioEpisode("local-1", "One", 1, 1))
        val second = com.cinemax.app.core.EpisodeCatalog.playbackPreview(show,
            com.cinemax.app.core.StremioEpisode("local-2", "Two", 1, 2))
        store.save(listOf(first,second))
        assertEquals(2, store.load().size)
    }

}
