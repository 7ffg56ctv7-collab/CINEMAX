package com.cinemax.app.addons

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamQualityOrderTest {
    @Test fun recognizesQualityFromNamesAndTitles() {
        val cases = listOf(
            "Movie 4K HDR" to "4K",
            "2160p" to "4K",
            "2160" to "4K",
            "UHD" to "4K",
            "Full HD 1080p" to "1080p",
            "1080" to "1080p",
            "720p" to "720p",
            "720" to "720p",
            "480p" to "480p",
            "Unknown release" to "Unknown",
            "x1080p" to "Unknown",
            "prefix720p" to "Unknown",
            "1080pixel" to "Unknown",
            "abc4k" to "Unknown",
            "480preview" to "Unknown"
        )
        cases.forEach { (name, expected) ->
            assertEquals(expected, StreamQualityOrder.qualityLabel(StremioStream(name = name)), name)
        }
        assertEquals("4K", StreamQualityOrder.qualityLabel(StremioStream(title = "Movie 4k")))
    }

    @Test fun sortsHighQualityFirstAndPreservesTies() {
        val streams = listOf(
            StremioStream(name = "Unknown A"),
            StremioStream(name = "720p A"),
            StremioStream(name = "4K A"),
            StremioStream(name = "1080p A"),
            StremioStream(name = "4K B"),
            StremioStream(name = "Unknown B"),
            StremioStream(name = "480p A")
        )
        assertEquals(
            listOf("4K A", "4K B", "1080p A", "720p A", "480p A", "Unknown A", "Unknown B"),
            StreamQualityOrder.sorted(streams).map { it.name }
        )
    }

    @Test fun emptyListStaysEmpty() {
        assertEquals(emptyList(), StreamQualityOrder.sorted(emptyList()))
    }
}
