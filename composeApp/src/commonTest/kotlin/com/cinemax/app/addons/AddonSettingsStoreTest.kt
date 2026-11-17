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
    @Test fun duplicatesKeepTheMostRecentlyInstalledAddon() {
        val store = AddonSettingsStore(MapSettings())
        val old = InstalledAddon("https://addon.example/manifest.json", "addon", "Old", "1", installedAtEpochMillis = 1L)
        val recent = old.copy(name = "New", version = "2")
        store.save(listOf(old, recent))
        assertEquals(listOf(recent), store.load())
    }

    @Test fun invalidOrInsecureStoredAddonsAreDropped() {
        val store = AddonSettingsStore(MapSettings())
        val safe = InstalledAddon("https://safe.example/manifest.json", "safe", "Safe", "1", installedAtEpochMillis = 0L)
        val insecure = safe.copy(id = "unsafe", name = "Unsafe", manifestUrl = "http://unsafe.example")
        val broken = safe.copy(id = " ", name = "Invalid")
        store.save(listOf(insecure, safe, broken))
        assertEquals(listOf(safe), store.load())
    }

    @Test fun repeatedEntriesStayWithinMemoryLimit() {
        val store = AddonSettingsStore(MapSettings())
        store.save((0..110).map {
            InstalledAddon("https://addon.example/manifest.json", "addon$it", "Addon $it", "1", installedAtEpochMillis = 0L)
        })
        assertEquals(100, store.load().size)
    }

}
