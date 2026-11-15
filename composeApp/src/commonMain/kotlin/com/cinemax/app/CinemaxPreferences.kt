package com.cinemax.app

import com.russhwolf.settings.Settings

/** User-facing appearance and playback preferences shared by Android and iOS. */
data class CinemaxPreferences(
    val accent: String = "Violet",
    val oledBlack: Boolean = true,
    val compactCards: Boolean = false,
    val rememberPlaybackPosition: Boolean = true,
    val autoplayNextEpisode: Boolean = false,
    val preferredSubtitleLanguage: String = "ar",
    val preferredAudioLanguage: String = "device"
)

class CinemaxPreferencesStore(
    private val settings: Settings = Settings()
) {
    fun load() = CinemaxPreferences(
        accent = settings.getString(ACCENT, DEFAULT_ACCENT).takeIf { it in ACCENTS } ?: DEFAULT_ACCENT,
        oledBlack = settings.getBoolean(OLED, true),
        compactCards = settings.getBoolean(COMPACT, false),
        rememberPlaybackPosition = settings.getBoolean(RESUME, true),
        autoplayNextEpisode = settings.getBoolean(AUTOPLAY, false),
        preferredSubtitleLanguage = settings.getString(SUBTITLE, DEFAULT_SUBTITLE).takeIf { it in SUBTITLE_LANGUAGES } ?: DEFAULT_SUBTITLE,
        preferredAudioLanguage = settings.getString(AUDIO, DEFAULT_AUDIO).takeIf { it in AUDIO_LANGUAGES } ?: DEFAULT_AUDIO
    )

    fun save(value: CinemaxPreferences) {
        settings.putString(ACCENT, value.accent.takeIf { it in ACCENTS } ?: DEFAULT_ACCENT)
        settings.putBoolean(OLED, value.oledBlack)
        settings.putBoolean(COMPACT, value.compactCards)
        settings.putBoolean(RESUME, value.rememberPlaybackPosition)
        settings.putBoolean(AUTOPLAY, value.autoplayNextEpisode)
        settings.putString(SUBTITLE, value.preferredSubtitleLanguage.takeIf { it in SUBTITLE_LANGUAGES } ?: DEFAULT_SUBTITLE)
        settings.putString(AUDIO, value.preferredAudioLanguage.takeIf { it in AUDIO_LANGUAGES } ?: DEFAULT_AUDIO)
    }

    private companion object {
        const val ACCENT = "cinemax.ui.accent"
        const val OLED = "cinemax.ui.oled"
        const val COMPACT = "cinemax.ui.compact"
        const val RESUME = "cinemax.playback.resume"
        const val AUTOPLAY = "cinemax.playback.autoplay"
        const val SUBTITLE = "cinemax.subtitle.language"
        const val AUDIO = "cinemax.audio.language"
        const val DEFAULT_ACCENT = "Violet"
        const val DEFAULT_SUBTITLE = "ar"
        const val DEFAULT_AUDIO = "device"
        val ACCENTS = setOf("Violet", "Blue", "Rose", "Emerald", "Amber")
        val SUBTITLE_LANGUAGES = setOf("ar", "en", "off")
        val AUDIO_LANGUAGES = setOf("device", "ar", "en")
    }
}
