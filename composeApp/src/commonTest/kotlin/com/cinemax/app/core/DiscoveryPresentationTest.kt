package com.cinemax.app.core

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.addons.StremioStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryPresentationTest {
    private val library = listOf(
        StremioMetaPreview("id-1", "movie", "Arrival", releaseInfo = "2016"),
        StremioMetaPreview("id-2", "series", "Dark", releaseInfo = "2017"),
        StremioMetaPreview("id-3", "movie", "Dune", releaseInfo = "2021"),
        StremioMetaPreview("id-4", "movie", "Dune", releaseInfo = "2021")
    )

    @Test fun localSearchFiltersMoviesAndNames() {
        val selected = DiscoveryPresentation.visible(library, "movie", "dun", CatalogSort.Source)
        assertEquals(listOf("Dune"), selected.map { it.name })
    }

    @Test fun searchAlsoFindsDescriptionAndIgnoresCase() {
        val items = listOf(StremioMetaPreview("tt1234567", "movie", "Film", description = "Space Adventure"))
        assertEquals(1, DiscoveryPresentation.visible(items, null, "SPACE", CatalogSort.Source).size)
    }

    @Test fun keepsProviderOrderWhenNotSorting() {
        val selected = DiscoveryPresentation.visible(library, null, "", CatalogSort.Source)
        assertEquals(listOf("Arrival", "Dark", "Dune"), selected.map { it.name })
    }

    @Test fun titleSortingWorks() {
        val items = listOf(
            StremioMetaPreview("c", "movie", "Zulu"),
            StremioMetaPreview("b", "movie", "alpha")
        )
        assertEquals(listOf("alpha", "Zulu"), DiscoveryPresentation.visible(items, null, "", CatalogSort.Title).map { it.name })
    }

    @Test fun newestSortingUsesReleaseYear() {
        val sorted = DiscoveryPresentation.visible(library, null, "", CatalogSort.Newest)
        assertEquals(listOf("Dune", "Dark", "Arrival"), sorted.map { it.name })
    }

    @Test fun qualitySortingAndSafePlaybackFilter() {
        val options = listOf(
            StremioStream(name = "720p", url = "https://cdn.example/720"),
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "1080p", url = "http://external.example/1080"),
            StremioStream(name = "4K torrent", infoHash = "aa")
        )
        assertEquals(listOf("4K", "720p"), DiscoveryPresentation.sortedStreams(options, true).map { it.name })
        assertEquals("4K", DiscoveryPresentation.qualityLabel(options[1]))
        assertFalse(DiscoveryPresentation.isPlayable(options[2]))
        assertTrue(DiscoveryPresentation.isPlayable(options[0]))
    }

    @Test fun unknownQualityStaysUnlabeled() {
        assertEquals(null, DiscoveryPresentation.qualityLabel(StremioStream(name = "Server A")))
    }
    @Test fun qualitySupports1440pBetween4kAnd1080p() {
        val options = listOf(
            StremioStream(name = "1080p", url = "https://cdn.example/a"),
            StremioStream(name = "1440p", url = "https://cdn.example/b"),
            StremioStream(name = "4K", url = "https://cdn.example/c")
        )
        assertEquals(listOf("4K", "1440p", "1080p"), DiscoveryPresentation.sortedStreams(options, false).map { it.name })
    }

    @Test fun preferredPlayableStreamAppearsBeforeHigherQuality() {
        val options = listOf(
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "720p", url = "https://cdn.example/720")
        )
        assertEquals("720p", DiscoveryPresentation.sortedStreams(options, false, preferredName = "720p").first().name)
    }

    @Test fun unsafePreferredStreamNeverBeatsSafeStream() {
        val options = listOf(
            StremioStream(name = "720p", url = "https://cdn.example/720"),
            StremioStream(name = "4K", url = "http://public.example/4k")
        )
        assertEquals("720p", DiscoveryPresentation.sortedStreams(options, false, preferredName = "4K").first().name)
    }

    @Test fun qualityFilterIncludesOnlySelectedQuality() {
        val options = listOf(
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "1080p", url = "https://cdn.example/1080")
        )
        assertEquals(listOf("1080p"), DiscoveryPresentation.sortedStreams(options, true, quality = "1080p").map { it.name })
    }

    @Test fun unknownQualityFilterReturnsEmptySafely() {
        assertTrue(DiscoveryPresentation.sortedStreams(
            listOf(StremioStream(name = "Server", url = "https://cdn.example/1")),
            true, quality = "4K"
        ).isEmpty())
    }

}
