package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchProgressStoreTest {
    @Test
    fun progressPersistsAndIsSortedByMostRecent() {
        val settings = MapSettings()
        val store = WatchProgressStore(settings)
        val first = WatchProgress(
            StremioMetaPreview("tt001", "movie", "First"), 20_000L, 100_000L, 10L
        )
        val second = WatchProgress(
            StremioMetaPreview("tt002", "series", "Second"), 30_000L, 200_000L, 20L
        )
        store.save(first)
        store.save(second)
        val restored = WatchProgressStore(settings).load()
        assertEquals(listOf("tt002", "tt001"), restored.map { it.item.id })
        assertEquals(0.15f, restored.first().progressFraction)
    }

    @Test
    fun canonicalIdentityReplacesProgressAcrossAddonIds() {
        val store = WatchProgressStore(MapSettings())
        store.save(WatchProgress(StremioMetaPreview("tt1234567", "movie", "Film", releaseInfo = "2025"), 20_000L, 100_000L, 1L))
        store.save(WatchProgress(StremioMetaPreview("tt1234567", "movie", "Film renamed", releaseInfo = "2024"), 40_000L, 100_000L, 2L))
        assertEquals(1, store.load().size)
        assertEquals(40_000L, store.load().single().positionMillis)
    }

    @Test
    fun opaqueIdsWithDifferentTitlesKeepSeparateProgress() {
        val store = WatchProgressStore(MapSettings())
        store.save(WatchProgress(StremioMetaPreview("catalog-7", "movie", "Dune", releaseInfo = "2021"), 20_000L, 100_000L, 1L))
        store.save(WatchProgress(StremioMetaPreview("catalog-7", "movie", "Arrival", releaseInfo = "2016"), 30_000L, 100_000L, 2L))
        assertEquals(2, store.load().size)
    }

    @Test
    fun completedItemsAreRemovedFromContinueWatching() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("tt003", "movie", "Finished")
        store.save(WatchProgress(item, 90_000L, 100_000L, 10L))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun shortVideoCanResumeBeforeProportionalCompletionWindow() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("short-1", "movie", "Short clip")
        store.save(WatchProgress(item, 10_000L, 20_000L, 1L))
        assertEquals(10_000L, store.load().single().positionMillis)
        store.save(WatchProgress(item, 18_000L, 20_000L, 2L))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun completionRuleMatchesUIHistoryLogic() {
        val item = StremioMetaPreview("short-2", "movie", "Short clip")
        assertTrue(WatchProgress(item, 95_000L, 100_000L, 1L).isComplete)
        assertTrue(!WatchProgress(item, 10_000L, 20_000L, 1L).isComplete)
    }

    @Test
    fun invalidStoredDataFallsBackToEmptyProgress() {
        val settings = MapSettings()
        settings.putString("cinemax.watch.progress.v1", "not-json")
        assertTrue(WatchProgressStore(settings).load().isEmpty())
    }
    @Test fun oldProgressUpdateCannotOverwriteNewerProgress() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("tt1234567", "movie", "Dune")
        store.save(WatchProgress(item, 40_000L, 120_000L, 200L))
        store.save(WatchProgress(item, 20_000L, 120_000L, 100L))
        assertEquals(40_000L, store.load().single().positionMillis)
    }

    @Test fun zeroAndNegativePositionsCannotReplaceSavedProgress() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("tt1234567", "movie", "Dune")
        store.save(WatchProgress(item, 35_000L, 120_000L, 100L))
        store.save(WatchProgress(item, 0L, 120_000L, 200L))
        store.save(WatchProgress(item, -1L, 120_000L, 300L))
        assertEquals(35_000L, store.load().single().positionMillis)
    }

    @Test fun clearRemovesAllProgressButNotOtherPreferences() {
        val settings = MapSettings()
        val store = WatchProgressStore(settings)
        val item = StremioMetaPreview("id", "series", "Show")
        store.save(WatchProgress(item, 10_000L, 100_000L, 100L))
        store.clear()
        assertTrue(store.load().isEmpty())
    }

    @Test fun savedEpisodeProgressKeepsEachEpisodeSeparate() {
        val store = WatchProgressStore(MapSettings())
        val show = StremioMetaPreview("tt1234567", "series", "Show")
        val first = com.cinemax.app.core.EpisodeCatalog.playbackPreview(show,
            com.cinemax.app.core.StremioEpisode("local-ep-a", "One", 1, 1))
        val second = com.cinemax.app.core.EpisodeCatalog.playbackPreview(show,
            com.cinemax.app.core.StremioEpisode("local-ep-b", "Two", 1, 2))
        store.save(WatchProgress(first, 10_000L, 120_000L, 1L))
        store.save(WatchProgress(second, 20_000L, 120_000L, 2L))
        assertEquals(2, store.load().size)
    }

    @Test fun sameEpisodeFromDifferentAddonsReplacesProgressNotDuplicates() {
        val store = WatchProgressStore(MapSettings())
        val show = StremioMetaPreview("tt1234567", "series", "Show")
        val a = com.cinemax.app.core.EpisodeCatalog.playbackPreview(show,
            com.cinemax.app.core.StremioEpisode("local-a", "One", 1, 1))
        val b = com.cinemax.app.core.EpisodeCatalog.playbackPreview(show,
            com.cinemax.app.core.StremioEpisode("local-b", "Uno", 1, 1))
        store.save(WatchProgress(a, 10_000L, 120_000L, 1L))
        store.save(WatchProgress(b, 40_000L, 120_000L, 2L))
        assertEquals(1, store.load().size)
        assertEquals(40_000L, store.load().single().positionMillis)
    }

}
