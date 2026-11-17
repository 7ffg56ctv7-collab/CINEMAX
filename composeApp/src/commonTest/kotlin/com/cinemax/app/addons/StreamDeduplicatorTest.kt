package com.cinemax.app.addons

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamDeduplicatorTest {
    @Test fun removesDuplicateExactUrls() {
        val streams = listOf(
            StremioStream(name = "A", url = "https://cdn.example/a.m3u8"),
            StremioStream(name = "B", url = "https://cdn.example/a.m3u8")
        )
        assertEquals(listOf("A"), StreamDeduplicator.unique(streams).map { it.name })
    }

    @Test fun preservesDistinctSignedUrls() {
        val streams = listOf(
            StremioStream(url = "https://cdn.example/a?token=one"),
            StremioStream(url = "https://cdn.example/a?token=two")
        )
        assertEquals(2, StreamDeduplicator.unique(streams).size)
    }

    @Test fun normalizesHashCaseAndWhitespace() {
        val streams = listOf(
            StremioStream(name = "First", infoHash = " ABC "),
            StremioStream(name = "Second", infoHash = "abc")
        )
        assertEquals(1, StreamDeduplicator.unique(streams).size)
    }

    @Test fun retainsDifferentTorrentFileIndices() {
        val streams = listOf(
            StremioStream(infoHash = "abc", fileIdx = 0),
            StremioStream(infoHash = "abc", fileIdx = 1)
        )
        assertEquals(2, StreamDeduplicator.unique(streams).size)
    }

    @Test fun keepsEntriesWithoutPlayableIdentifiers() {
        assertEquals(2, StreamDeduplicator.unique(listOf(StremioStream(name = "A"), StremioStream(name = "B"))).size)
    }

    @Test fun retainsFirstProviderOrdering() {
        val streams = listOf(
            StremioStream(name = "First", url = "https://a.example"),
            StremioStream(name = "Second", url = "https://b.example"),
            StremioStream(name = "Duplicate", url = "https://a.example")
        )
        assertEquals(listOf("First", "Second"), StreamDeduplicator.unique(streams).map { it.name })
    }
}
