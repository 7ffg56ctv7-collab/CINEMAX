package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceHealthStoreTest {
    private val first = StremioMetaPreview("tt1234567", "movie", "Film")
    private val second = StremioMetaPreview("tt2345678", "movie", "Other")

    @Test fun brokenSourcePersistsAndIsScopedToOneMovie() {
        val settings = MapSettings()
        SourceHealthStore(settings).markBroken(first, "Provider • 1080p")
        assertEquals(setOf("Provider • 1080p"), SourceHealthStore(settings).brokenLabels(first))
        assertTrue(SourceHealthStore(settings).brokenLabels(second).isEmpty())
    }

    @Test fun userCanRestoreAReportedSource() {
        val store = SourceHealthStore(MapSettings())
        store.markBroken(first, "Server")
        store.clearBroken(first, "Server")
        assertFalse("Server" in store.brokenLabels(first))
    }

    @Test fun repeatedReportsDoNotDuplicateEntries() {
        val store = SourceHealthStore(MapSettings())
        store.markBroken(first, "Server")
        store.markBroken(first, "Server")
        assertEquals(1, store.brokenLabels(first).size)
    }

    @Test fun sourceUrlsAndBlankLabelsAreNeverRecorded() {
        val store = SourceHealthStore(MapSettings())
        store.markBroken(first, "https://cdn.example/private?token=value")
        store.markBroken(first, " ")
        assertTrue(store.brokenLabels(first).isEmpty())
    }

    @Test fun canonicalMediaIdentitySurvivesRenaming() {
        val store = SourceHealthStore(MapSettings())
        store.markBroken(first, "Server")
        assertEquals(
            setOf("Server"),
            store.brokenLabels(first.copy(name = "Retitled", releaseInfo = "2022"))
        )
    }
    @Test fun episodeBrokenReportsDoNotLeakToOtherEpisodes() {
        val store = SourceHealthStore(MapSettings())
        val series = StremioMetaPreview("tt1234567", "series", "Show")
        val first = com.cinemax.app.core.EpisodeCatalog.playbackPreview(series,
            com.cinemax.app.core.StremioEpisode("local-1", "One", 1, 1))
        val second = com.cinemax.app.core.EpisodeCatalog.playbackPreview(series,
            com.cinemax.app.core.StremioEpisode("local-2", "Two", 1, 2))
        store.markBroken(first, "Broken")
        assertEquals(setOf("Broken"), store.brokenLabels(first))
        assertTrue(store.brokenLabels(second).isEmpty())
    }

    @Test fun sameEpisodeAcrossAddonsSharesBrokenReport() {
        val store = SourceHealthStore(MapSettings())
        val series = StremioMetaPreview("tt1234567", "series", "Show")
        val first = com.cinemax.app.core.EpisodeCatalog.playbackPreview(series,
            com.cinemax.app.core.StremioEpisode("local-a", "One", 1, 1))
        val other = first.copy(id = "local-b", name = "Different label")
        store.markBroken(first, "Provider")
        assertEquals(setOf("Provider"), store.brokenLabels(other))
    }

    @Test fun labelsContainingTokensOrControlCharactersAreRejected() {
        val store = SourceHealthStore(MapSettings())
        store.markBroken(first, "Watch?token=secret")
        store.markBroken(first, "Source\nHeader")
        assertTrue(store.brokenLabels(first).isEmpty())
    }

}
