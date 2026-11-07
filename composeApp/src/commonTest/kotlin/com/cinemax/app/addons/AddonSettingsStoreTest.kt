package com.cinemax.app.addons

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddonSettingsStoreTest {
    @Test
    fun savesAndRestoresInstalledAddons() {
        val store = AddonSettingsStore(MapSettings())
        val addon = InstalledAddon(
            manifestUrl = "https://addon.example/manifest.json",
            id = "org.example.catalog",
            name = "Example Catalog",
            version = "1.0.0",
            description = "Test addon",
            installedAtEpochMillis = 1234L
        )

        store.save(listOf(addon))

        assertEquals(listOf(addon), store.load())
    }

    @Test
    fun malformedStoredDataDoesNotCrashStartup() {
        val settings = MapSettings()
        settings.putString("cinemax.installed.addons.v1", "{not-json")
        val store = AddonSettingsStore(settings)

        assertTrue(store.load().isEmpty())
    }

    @Test
    fun clearRemovesStoredAddons() {
        val settings = MapSettings()
        val store = AddonSettingsStore(settings)
        store.save(
            listOf(
                InstalledAddon(
                    manifestUrl = "https://addon.example/manifest.json",
                    id = "org.example.catalog",
                    name = "Example Catalog",
                    version = "1.0.0",
                    installedAtEpochMillis = 1234L
                )
            )
        )

        store.clear()

        assertTrue(store.load().isEmpty())
    }
}
