package com.cinemax.app

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CinemaxPreferencesStoreTest {
    @Test
    fun savesAndRestoresAppearanceAndPlaybackPreferencesAcrossStoreInstances() {
        val settings = MapSettings()
        val expected = CinemaxPreferences(
            accent = "Blue",
            oledBlack = false,
            compactCards = true,
            rememberPlaybackPosition = false,
            autoplayNextEpisode = true,
            preferredSubtitleLanguage = "en",
            preferredAudioLanguage = "ar"
        )

        CinemaxPreferencesStore(settings).save(expected)

        assertEquals(expected, CinemaxPreferencesStore(settings).load())
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
        assertEquals("device", preferences.preferredAudioLanguage)
    }

    @Test
    fun unsupportedStoredOptionsFallBackToSafeDefaults() {
        val settings = MapSettings().apply {
            putString("cinemax.ui.accent", "NotATheme")
            putString("cinemax.subtitle.language", "xx")
            putString("cinemax.audio.language", "unknown")
        }

        val preferences = CinemaxPreferencesStore(settings).load()

        assertEquals("Violet", preferences.accent)
        assertEquals("ar", preferences.preferredSubtitleLanguage)
        assertEquals("device", preferences.preferredAudioLanguage)
    }

    @Test
    fun savingUnsupportedOptionsStoresSafeDefaults() {
        val settings = MapSettings()
        CinemaxPreferencesStore(settings).save(
            CinemaxPreferences(
                accent = "invalid",
                preferredSubtitleLanguage = "xx",
                preferredAudioLanguage = "unknown"
            )
        )

        val preferences = CinemaxPreferencesStore(settings).load()
        assertEquals("Violet", preferences.accent)
        assertEquals("ar", preferences.preferredSubtitleLanguage)
        assertEquals("device", preferences.preferredAudioLanguage)
    }
}
