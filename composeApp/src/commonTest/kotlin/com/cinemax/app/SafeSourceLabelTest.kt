package com.cinemax.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SafeSourceLabelTest {
    @Test fun trimsAndCapsSourceDisplayNames() {
        assertEquals("Provider • 1080p", SafeSourceLabel.normalize("  Provider • 1080p  "))
        assertEquals(256, SafeSourceLabel.normalize("a".repeat(400))?.length)
    }

    @Test fun refusesUrlAndEncodedUrlLabels() {
        assertNull(SafeSourceLabel.normalize("https://cdn.example/video.mp4"))
        assertNull(SafeSourceLabel.normalize("https%3A%2F%2Fcdn.example"))
    }

    @Test fun refusesSecretsAfterLabelTruncationPoint() {
        assertNull(SafeSourceLabel.normalize("a".repeat(300) + "?token=secret"))
        assertNull(SafeSourceLabel.normalize("Provider &access_token=123"))
        assertNull(SafeSourceLabel.normalize("Provider api_key=123"))
    }

    @Test fun rejectsMultilineAndDirectionSpoofing() {
        assertNull(SafeSourceLabel.normalize("Provider\nInjected"))
        assertNull(SafeSourceLabel.normalize("Provider\u202eName"))
        assertNull(SafeSourceLabel.normalize("Provider\u2067Name"))
    }

    @Test fun safeUnicodeArabicLabelsAreAllowed() {
        assertEquals("مزود عربي • 4K", SafeSourceLabel.normalize("مزود عربي • 4K"))
    }
}
