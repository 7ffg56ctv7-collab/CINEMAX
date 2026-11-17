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
}
