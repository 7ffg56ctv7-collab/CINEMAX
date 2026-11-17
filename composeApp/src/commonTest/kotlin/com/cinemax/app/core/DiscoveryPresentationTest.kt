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

    @Test fun arabicAlefVariantsMatchWithoutExactSpelling() {
        val items = listOf(StremioMetaPreview("tt1234567", "movie", "إشارة"))
        assertEquals(1, DiscoveryPresentation.visible(items, null, "اشارة", CatalogSort.Source).size)
    }

    @Test fun arabicDiacriticsAreIgnoredInSearch() {
        val items = listOf(StremioMetaPreview("tt1234567", "movie", "المُسافِر"))
        assertEquals(1, DiscoveryPresentation.visible(items, null, "المسافر", CatalogSort.Source).size)
    }

    @Test fun alefMaqsuraAndYaaMatch() {
        val items = listOf(StremioMetaPreview("tt1234567", "movie", "ليلى"))
        assertEquals(1, DiscoveryPresentation.visible(items, null, "ليلي", CatalogSort.Source).size)
    }

    @Test fun searchNeedsAllTermsAcrossMetadataFields() {
        val items = listOf(StremioMetaPreview("tt1234567", "movie", "Arrival", description = "First contact", releaseInfo = "2016"))
        assertEquals(1, DiscoveryPresentation.visible(items, null, "arrival 2016", CatalogSort.Source).size)
        assertEquals(0, DiscoveryPresentation.visible(items, null, "arrival 2022", CatalogSort.Source).size)
    }

    @Test fun searchByYearAloneUsesReleaseInfo() {
        val items = listOf(StremioMetaPreview("tt1234567", "movie", "Arrival", releaseInfo = "2016"))
        assertEquals(1, DiscoveryPresentation.visible(items, null, "2016", CatalogSort.Source).size)
    }

    @Test fun oldestSortPutsUnknownYearsLast() {
        val items = listOf(
            StremioMetaPreview("a", "movie", "Unknown"),
            StremioMetaPreview("b", "movie", "Old", releaseInfo = "1999"),
            StremioMetaPreview("c", "movie", "New", releaseInfo = "2023")
        )
        assertEquals(listOf("Old", "New", "Unknown"), DiscoveryPresentation.visible(items, null, "", CatalogSort.Oldest).map { it.name })
    }

    @Test fun ratingSortHandlesMissingAndInvalidRatings() {
        val items = listOf(
            StremioMetaPreview("a", "movie", "Poor", imdbRating = "2.5"),
            StremioMetaPreview("b", "movie", "Strong", imdbRating = "9,1"),
            StremioMetaPreview("c", "movie", "Unrated", imdbRating = "N/A")
        )
        assertEquals(listOf("Strong", "Poor", "Unrated"), DiscoveryPresentation.visible(items, null, "", CatalogSort.Rating).map { it.name })
    }

    @Test fun recognizesAdvertisedVideoCodecs() {
        assertEquals("H.265", DiscoveryPresentation.codecLabel(StremioStream(name = "2160p HEVC")))
        assertEquals("H.264", DiscoveryPresentation.codecLabel(StremioStream(name = "1080p x264")))
        assertEquals("AV1", DiscoveryPresentation.codecLabel(StremioStream(name = "1080p AV1")))
        assertEquals("VP9", DiscoveryPresentation.codecLabel(StremioStream(name = "720p VP9")))
        assertEquals(null, DiscoveryPresentation.codecLabel(StremioStream(name = "Server")))
    }

    @Test fun recognizesAdvertisedHdrFormatsWithoutConfusingHigherPriority() {
        assertEquals("Dolby Vision", DiscoveryPresentation.hdrLabel(StremioStream(name = "2160p DV HDR10")))
        assertEquals("HDR10+", DiscoveryPresentation.hdrLabel(StremioStream(title = "HDR10+ 4K")))
        assertEquals("HDR10", DiscoveryPresentation.hdrLabel(StremioStream(name = "HDR10")))
        assertEquals("HDR", DiscoveryPresentation.hdrLabel(StremioStream(name = "HDR")))
    }

    @Test fun recognizesAudioAndSubtitleLanguagesAsLabelsOnly() {
        assertEquals("عربي", DiscoveryPresentation.audioLabel(StremioStream(title = "صوت عربي")))
        assertEquals("English", DiscoveryPresentation.audioLabel(StremioStream(name = "EN Audio 1080p")))
        assertEquals("عربية", DiscoveryPresentation.subtitleLabel(StremioStream(name = "مترجم")))
        assertEquals("English", DiscoveryPresentation.subtitleLabel(StremioStream(name = "English Subtitles")))
    }

    @Test fun providerFiltersDoNotMixStreamNames() {
        val streams = listOf(
            StremioStream(name = "Cinema • 4K", url = "https://cdn.example/4k"),
            StremioStream(name = "Other • 720p", url = "https://cdn.example/720"),
            StremioStream(name = "Cinema • 1080p", url = "https://cdn.example/1080")
        )
        assertEquals(listOf("Cinema", "Other"), DiscoveryPresentation.availableProviders(streams))
        assertEquals(2, DiscoveryPresentation.sortedStreams(streams, true, provider = "Cinema").size)
    }

    @Test fun sourceFiltersComposeAcrossProviderAndQuality() {
        val streams = listOf(
            StremioStream(name = "Cinema • 4K", url = "https://cdn.example/4k"),
            StremioStream(name = "Cinema • 1080p", url = "https://cdn.example/1080"),
            StremioStream(name = "Other • 1080p", url = "https://cdn.example/other")
        )
        assertEquals(listOf("Cinema • 1080p"),
            DiscoveryPresentation.sortedStreams(streams, true, quality = "1080p", provider = "Cinema").map { it.name })
    }

    @Test fun manuallyBrokenSourcesAppearAfterHealthyAlternatives() {
        val streams = listOf(
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "720p", url = "https://cdn.example/720")
        )
        assertEquals(
            listOf("720p", "4K"),
            DiscoveryPresentation.sortedStreams(
                streams, true, preferredName = "4K", brokenLabels = setOf("4K")
            ).map { it.name }
        )
    }

    @Test fun subtitleOnlySourcesAreNotLabeledAsDubbedAudio() {
        assertEquals(null, DiscoveryPresentation.audioLabel(
            StremioStream(name = "Arabic subtitles")
        ))
        assertEquals(null, DiscoveryPresentation.audioLabel(
            StremioStream(name = "English subtitles")
        ))
    }

    @Test fun advertisedAudioAndSubtitleFiltersCompose() {
        val options = listOf(
            StremioStream(name = "Primary • Arabic Audio 1080p", url = "https://cdn.example/ar"),
            StremioStream(name = "Secondary • English subtitles", url = "https://cdn.example/sub"),
            StremioStream(name = "Other • English Audio", url = "https://cdn.example/en")
        )
        assertEquals(listOf("Primary • Arabic Audio 1080p"),
            DiscoveryPresentation.sortedStreams(options, true, audio = "عربي").map { it.name })
        assertEquals(listOf("Secondary • English subtitles"),
            DiscoveryPresentation.sortedStreams(options, true, subtitle = "English").map { it.name })
    }

    @Test fun recordedFastStartupRanksBeforeUnmeasuredHighQualitySource() {
        val sources = listOf(
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "720p", url = "https://cdn.example/720")
        )
        assertEquals(listOf("720p", "4K"),
            DiscoveryPresentation.sortedStreams(
                sources, true, startupMillis = mapOf("720p" to 1000L)
            ).map { it.name })
    }

    @Test fun brokenSourceStillRanksAfterFastButHealthySource() {
        val sources = listOf(
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "720p", url = "https://cdn.example/720")
        )
        assertEquals(listOf("720p", "4K"),
            DiscoveryPresentation.sortedStreams(
                sources, true, preferredName = "4K", brokenLabels = setOf("4K"),
                startupMillis = mapOf("4K" to 100L, "720p" to 4000L)
            ).map { it.name })
    }

}
