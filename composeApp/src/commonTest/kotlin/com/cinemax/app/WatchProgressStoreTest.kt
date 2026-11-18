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
    fun completedItemsAreRemovedFromContinueWatching() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("tt003", "movie", "Finished")
        store.save(WatchProgress(item, 90_000L, 100_000L, 10L))

        assertTrue(store.load().isEmpty())
    }


    @Test
    fun olderProgressUpdateDoesNotDisplaceNewestEntry() {
        val store = WatchProgressStore(MapSettings())
        repeat(30) { index ->
            store.save(WatchProgress(StremioMetaPreview("tt$index", "movie", "Title $index"), 20_000L, 100_000L, index.toLong() + 100L))
        }
        store.save(WatchProgress(StremioMetaPreview("tt-old", "movie", "Old"), 20_000L, 100_000L, 1L))
        val history = store.load()
        assertEquals(30, history.size)
        assertTrue(history.none { it.item.id == "tt-old" })
        assertEquals("tt29", history.first().item.id)
    }

    @Test
    fun shortVideoProgressDoesNotDisappearImmediately() {
        val store = WatchProgressStore(MapSettings())
        store.save(WatchProgress(StremioMetaPreview("short", "movie", "Short"), 2_000L, 10_000L, 1L))
        assertEquals("short", store.load().single().item.id)
    }

    @Test
    fun completionRemovesExistingHistoryEntry() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("finished", "movie", "Finished")
        store.save(WatchProgress(item, 20_000L, 100_000L, 1L))
        store.save(WatchProgress(item, 99_000L, 100_000L, 2L))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun zeroPositionClearsPreviousResumePoint() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("restart", "movie", "Restart")
        store.save(WatchProgress(item, 20_000L, 100_000L, 1L))
        store.save(WatchProgress(item, 0L, 100_000L, 2L))
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun blankIdsCannotEnterHistory() {
        val store = WatchProgressStore(MapSettings())
        store.save(WatchProgress(StremioMetaPreview("", "movie", "No ID"), 10_000L, 100_000L, 1L))
        assertTrue(store.load().isEmpty())
    }


    @Test
    fun delayedCallbackCannotOverwriteNewerProgress() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("tt-stale", "movie", "Stale")
        store.save(WatchProgress(item, 60_000L, 100_000L, 200L))
        store.save(WatchProgress(item, 10_000L, 100_000L, 100L))
        assertEquals(60_000L, store.load().single().positionMillis)
    }

    @Test
    fun delayedCompletionCannotDeleteNewerProgress() {
        val store = WatchProgressStore(MapSettings())
        val item = StremioMetaPreview("tt-completion", "movie", "Completion")
        store.save(WatchProgress(item, 40_000L, 100_000L, 200L))
        store.save(WatchProgress(item, 100_000L, 100_000L, 100L))
        assertEquals(40_000L, store.load().single().positionMillis)
    }

    @Test
    fun historyLoadedFromStorageIsCappedAndDeduplicated() {
        val settings = MapSettings()
        val store = WatchProgressStore(settings)
        val item = StremioMetaPreview("same", "movie", "Same")
        store.save(WatchProgress(item, 10_000L, 100_000L, 1L))
        store.save(WatchProgress(item, 30_000L, 100_000L, 2L))
        assertEquals(1, store.load().size)
        assertEquals(30_000L, store.load().single().positionMillis)
    }

    @Test
    fun invalidStoredDataFallsBackToEmptyProgress() {
        val settings = MapSettings()
        settings.putString("cinemax.watch.progress.v1", "not-json")

        assertTrue(WatchProgressStore(settings).load().isEmpty())
    }
}
