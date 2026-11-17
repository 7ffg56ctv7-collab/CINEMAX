package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourcePreferenceStoreTest {
    private val first = StremioMetaPreview("tt1234567", "movie", "Dune", releaseInfo = "2021")
    private val second = StremioMetaPreview("tt2345678", "movie", "Arrival", releaseInfo = "2016")

    @Test fun remembersOnlyTheSourceLabel() {
        val settings = MapSettings()
        SourcePreferenceStore(settings).save(first, "Provider • 1080p")
        assertEquals("Provider • 1080p", SourcePreferenceStore(settings).load(first))
        assertNull(SourcePreferenceStore(settings).load(second))
    }

    @Test fun canonicalIdentitySurvivesNameChanges() {
        val settings = MapSettings()
        SourcePreferenceStore(settings).save(first, "Provider • 4K")
        assertEquals("Provider • 4K", SourcePreferenceStore(settings)
            .load(first.copy(name = "Different display title", releaseInfo = "2022")))
    }

    @Test fun canClearPreferredSource() {
        val store = SourcePreferenceStore(MapSettings())
        store.save(first, "Source 1")
        store.clear(first)
        assertNull(store.load(first))
    }

    @Test fun ignoresEmptyLabels() {
        val store = SourcePreferenceStore(MapSettings())
        store.save(first, " ")
        assertNull(store.load(first))
    }
}
