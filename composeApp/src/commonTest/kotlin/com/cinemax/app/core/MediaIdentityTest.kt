package com.cinemax.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaIdentityTest {
    @Test
    fun imdbIdHasPriorityOverTmdbAndTitle() {
        assertEquals(
            "movie:imdb:tt1234567",
            MediaIdentity.key(
                type = "movie",
                id = "tt1234567",
                tmdbId = "tmdb:987",
                title = "Different title",
                releaseInfo = "2020"
            )
        )
    }

    @Test
    fun tmdbIdsNormalizeAcrossCommonPrefixes() {
        assertEquals(
            MediaIdentity.key(type = "series", tmdbId = "tmdb:42"),
            MediaIdentity.key(type = "series", id = "themoviedb/42")
        )
    }

    @Test
    fun titleFallbackUsesNormalizedTitleAndYear() {
        assertEquals(
            MediaIdentity.key(type = "movie", title = "The.Matrix!", releaseInfo = "1999–2000"),
            MediaIdentity.key(type = "movie", title = " the matrix ", releaseInfo = "1999")
        )
    }

    @Test
    fun typeRemainsPartOfIdentity() {
        val movie = MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "2021")
        val series = MediaIdentity.key(type = "series", title = "Dune", releaseInfo = "2021")
        kotlin.test.assertNotEquals(movie, series)
    }

    @Test
    fun refusesTitleOnlyFallbackWithoutYear() {
        assertNull(MediaIdentity.key(type = "movie", title = "Dune"))
    }

    @Test
    fun opaqueAddonIdsDoNotOverrideDifferentTitles() {
        val first = MediaIdentity.key(type = "movie", id = "catalog-item-7", title = "Dune", releaseInfo = "2021")
        val second = MediaIdentity.key(type = "movie", id = "catalog-item-7", title = "Arrival", releaseInfo = "2016")
        kotlin.test.assertNotEquals(first, second)
    }

    @Test
    fun canonicalImdbIdMatchesEvenWhenTitlesDiffer() {
        assertEquals(
            MediaIdentity.key(type = "movie", id = "tt1234567", title = "Dune", releaseInfo = "2021"),
            MediaIdentity.key(type = "movie", id = "tt1234567", title = "Different label", releaseInfo = "2020")
        )
    }

    @Test
    fun acceptsImdbPrefix() {
        assertEquals("movie:imdb:tt1234567", MediaIdentity.key(type = "movie", imdbId = "IMDb:TT1234567"))
    }

    @Test
    fun removesTmdbLeadingZeros() {
        assertEquals(MediaIdentity.key(type = "movie", tmdbId = "tmdb:00042"), MediaIdentity.key(type = "movie", tmdbId = "tmdb:42"))
    }

    @Test
    fun normalizesArabicHamza() {
        assertEquals(MediaIdentity.key(type = "movie", title = "أمل", releaseInfo = "2020"), MediaIdentity.key(type = "movie", title = "امل", releaseInfo = "2020"))
    }

    @Test
    fun normalizesArabicAlefMaqsura() {
        assertEquals(MediaIdentity.key(type = "movie", title = "ليلى", releaseInfo = "2020"), MediaIdentity.key(type = "movie", title = "ليلي", releaseInfo = "2020"))
    }

    @Test
    fun acceptsReleaseYearInTitle() {
        assertEquals(MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "2021"), MediaIdentity.key(type = "movie", title = "Dune (2021)"))
    }

    @Test
    fun movieAliasesShareIdentity() {
        assertEquals(MediaIdentity.key(type = "film", title = "Dune", releaseInfo = "2021"), MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "2021"))
    }

    @Test
    fun seriesAliasesShareIdentity() {
        assertEquals(MediaIdentity.key(type = "tv", title = "Lost", releaseInfo = "2004"), MediaIdentity.key(type = "series", title = "Lost", releaseInfo = "2004"))
    }

    @Test
    fun refusesNumericOnlyTitle() {
        assertNull(MediaIdentity.key(type = "movie", title = "2021"))
    }

    @Test
    fun normalizesArabicDiacritics() {
        assertEquals(MediaIdentity.key(type = "movie", title = "السَلام", releaseInfo = "2020"), MediaIdentity.key(type = "movie", title = "السلام", releaseInfo = "2020"))
    }

    @Test
    fun distinctReleaseYearsRemainDistinct() {
        kotlin.test.assertNotEquals(MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "1984"), MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "2021"))
    }

    @Test
    fun refusesEmptyType() {
        assertNull(MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "2021").let {
            MediaIdentity.key(type = " ", title = "Dune", releaseInfo = "2021")
        })
    }
    @Test fun canonicalImdbEpisodeHasStableIdentityDespiteRename() {
        val english = MediaIdentity.key(type = "series", id = "tt1234567:1:2", title = "Episode 2")
        val arabic = MediaIdentity.key(type = "series", id = "TT1234567:1:2", title = "الحلقة الثانية")
        assertEquals("series:imdb-episode:tt1234567:1:2", english)
        assertEquals(english, arabic)
    }

    @Test fun imdbEpisodeDoesNotMergeIntoParentSeries() {
        kotlin.test.assertNotEquals(MediaIdentity.key(type = "series", id = "tt1234567"),
            MediaIdentity.key(type = "series", id = "tt1234567:1:1"))
    }

    @Test fun localEpisodeIdsWithSharedParentAndNumberMerge() {
        val sourceA = com.cinemax.app.addons.StremioMetaPreview(
            "local-a", "series", "Show - Episode", seriesId = "tt1234567",
            seriesName = "Show", episodeSeason = 1, episodeNumber = 2
        )
        val sourceB = sourceA.copy(id = "local-b", name = "الحلقة الثانية")
        assertEquals(MediaIdentity.storageKey(sourceA), MediaIdentity.storageKey(sourceB))
    }

    @Test fun differentSeasonsRemainSeparateEvenWhenLocalIdIsSame() {
        val one = com.cinemax.app.addons.StremioMetaPreview(
            "same", "series", "Episode 1", seriesId = "tt1234567",
            seriesName = "Show", episodeSeason = 1, episodeNumber = 1
        )
        kotlin.test.assertNotEquals(MediaIdentity.storageKey(one),
            MediaIdentity.storageKey(one.copy(episodeSeason = 2)))
    }

}

class StremioAddonClientSecurityTest {
    @Test
    fun acceptsHttpsManifestAndAddsManifestPath() {
        assertEquals(
            "https://addons.example.org/manifest.json",
            com.cinemax.app.addons.StremioAddonClient.manifestUrl("https://addons.example.org")
        )
    }

    @Test
    fun rejectsInsecureRemoteAndCredentialUrls() {
        assertNull(com.cinemax.app.addons.StremioAddonClient.manifestUrl("http://addons.example.org"))
        assertNull(com.cinemax.app.addons.StremioAddonClient.manifestUrl("https://user:password@addons.example.org"))
        assertNull(com.cinemax.app.addons.StremioAddonClient.manifestUrl("file:///etc/passwd"))
    }

    @Test
    fun allowsLoopbackHttpOnlyForDevelopment() {
        assertEquals(
            "http://127.0.0.1:7000/manifest.json",
            com.cinemax.app.addons.StremioAddonClient.manifestUrl("http://127.0.0.1:7000")
        )
        assertNull(com.cinemax.app.addons.StremioAddonClient.manifestUrl("http://192.168.1.20:7000"))
    }

    @Test
    fun mediaUrlsMustBeHttpsOrLoopbackWithoutCredentials() {
        assertTrue(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("https://cdn.example.org/video.m3u8"))
        assertTrue(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("http://localhost:8080/video.mp4"))
        assertFalse(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("http://cdn.example.org/video.mp4"))
        assertFalse(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("https://user:pass@cdn.example.org/video.mp4"))
        assertFalse(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("javascript:alert(1)"))
    }

    @Test
    fun stripsManifestFragmentBeforeBuildingUrl() {
        assertEquals(
            "https://addons.example.org/manifest.json",
            com.cinemax.app.addons.StremioAddonClient.manifestUrl("https://addons.example.org/#ignored")
        )
    }

    @Test
    fun rejectsFragmentInPlaybackUrl() {
        assertFalse(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("https://cdn.example.org/video.mp4#fragment"))
    }

    @Test
    fun rejectsEmptyManifestAndMediaUrls() {
        assertNull(com.cinemax.app.addons.StremioAddonClient.manifestUrl("   "))
        assertFalse(com.cinemax.app.addons.StremioAddonClient.isSafeMediaUrl("   "))
    }
}
