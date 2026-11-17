package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceStartupStoreTest {
    private val film = StremioMetaPreview("tt1234567", "movie", "Film")
    private val series = StremioMetaPreview("tt7654321", "series", "Series")

    @Test fun storesOnlySuccessfulStartupDurationsByMediaItem() {
        val settings = MapSettings()
        SourceStartupStore(settings).recordSuccess(film, "Provider • 4K", 1000L, 2500L)
        assertEquals(mapOf("Provider • 4K" to 1500L), SourceStartupStore(settings).durations(film))
        assertTrue(SourceStartupStore(settings).durations(series).isEmpty())
    }

    @Test fun ignoresInvalidAndVerySlowTimings() {
        val store = SourceStartupStore(MapSettings())
        store.recordSuccess(film, "Provider", 0L, 500L)
        store.recordSuccess(film, "Provider", 2000L, 1000L)
        store.recordSuccess(film, "Provider", 1000L, 123000L)
        assertTrue(store.durations(film).isEmpty())
    }

    @Test fun rejectsSensitiveUrlLikeLabels() {
        val store = SourceStartupStore(MapSettings())
        store.recordSuccess(film, "https://private.example/?token=secret", 1000L, 2000L)
        assertTrue(store.durations(film).isEmpty())
    }

    @Test fun nextSuccessfulPlayUpdatesPreviousTiming() {
        val store = SourceStartupStore(MapSettings())
        store.recordSuccess(film, "Provider", 1000L, 5000L)
        store.recordSuccess(film, "Provider", 1000L, 2000L)
        assertEquals(1000L, store.durations(film)["Provider"])
    }

    @Test fun userCanResetRecordedTiming() {
        val store = SourceStartupStore(MapSettings())
        store.recordSuccess(film, "Provider", 1000L, 2000L)
        store.clear(film, "Provider")
        assertTrue(store.durations(film).isEmpty())
    }
}
