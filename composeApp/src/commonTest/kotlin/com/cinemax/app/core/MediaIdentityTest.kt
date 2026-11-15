package com.cinemax.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun refusesEmptyType() {
        assertNull(MediaIdentity.key(type = "movie", title = "Dune", releaseInfo = "2021").let {
            MediaIdentity.key(type = " ", title = "Dune", releaseInfo = "2021")
        })
    }
}
