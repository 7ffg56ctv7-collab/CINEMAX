package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackResumeTest {
    private val media = StremioMetaPreview("tt1234567", "movie", "Film")
    private fun record(item: StremioMetaPreview = media, pos: Long = 22_000L, duration: Long = 120_000L) =
        WatchProgress(item, pos, duration, 100L)

    @Test fun disabledSettingNeverSeeksToSavedPosition() {
        assertEquals(0L, PlaybackResume.initialPosition(media, listOf(record()), enabled = false))
    }

    @Test fun restoresValidCanonicalPositionWhenEnabled() {
        assertEquals(22_000L, PlaybackResume.initialPosition(
            media.copy(name = "Updated name"), listOf(record()), enabled = true))
    }

    @Test fun doesNotResumeUnrelatedFilmOrSeries() {
        val other = StremioMetaPreview("tt7654321", "series", "Other")
        assertEquals(0L, PlaybackResume.initialPosition(other, listOf(record()), true))
    }

    @Test fun completedOrInvalidProgressNeverResumes() {
        assertEquals(0L, PlaybackResume.initialPosition(
            media, listOf(record(pos = 118_000L)), true))
        assertEquals(0L, PlaybackResume.initialPosition(
            media, listOf(record(pos = -10L)), true))
    }

    @Test fun noMediaDoesNotCauseCrash() {
        assertEquals(0L, PlaybackResume.initialPosition(null, listOf(record()), true))
    }
}
