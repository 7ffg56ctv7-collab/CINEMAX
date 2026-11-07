package com.cinemax.app

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CinemaxPreferencesStoreTest {
    @Test
    fun savesAndRestoresAppearanceAndPlaybackPreferences() {
        val store = CinemaxPreferencesStore(MapSettings())
        val expected = CinemaxPreferences(
            accent = "Blue",
            oledBlack = false,
            compactCards = true,
            rememberPlaybackPosition = false,
            autoplayNextEpisode = true,
            preferredSubtitleLanguage = "en",
            preferredAudioLanguage = "ar"
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }

    @Test
    fun missingPreferencesUseSafeDefaults() {
        val preferences = CinemaxPreferencesStore(MapSettings()).load()

        assertEquals("Violet", preferences.accent)
        assertTrue(preferences.oledBlack)
        assertFalse(preferences.compactCards)
        assertTrue(preferences.rememberPlaybackPosition)
        assertFalse(preferences.autoplayNextEpisode)
        assertEquals("ar", preferences.preferredSubtitleLanguage)
    }
}
