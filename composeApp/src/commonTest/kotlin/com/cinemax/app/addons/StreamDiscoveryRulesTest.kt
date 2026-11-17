package com.cinemax.app.addons

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamDiscoveryRulesTest {
    private fun manifest(
        resource: String = "stream",
        types: List<String> = listOf("movie"),
        prefixes: List<String> = listOf("tt")
    ) = StremioManifest(
        id = "test", version = "1", name = "Test",
        resources = listOf(JsonPrimitive(resource)), types = types, idPrefixes = prefixes
    )

    @Test fun allowsCanonicalImdbIdForDeclaredResource() {
        assertTrue(StreamDiscoveryRules.supportsCanonicalDirectStream(manifest(), "movie", "tt1234567"))
    }

    @Test fun canonicalImdbEpisodeIdCanBeLookedUpAcrossAddons() {
        assertTrue(StreamDiscoveryRules.supportsCanonicalDirectStream(
            manifest(types = listOf("series")), "series", "tt1234567:2:12"))
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(
            manifest(types = listOf("series")), "series", "tt1234567:abc:3"))
    }

    @Test fun neverSendsOpaqueIdsAcrossProviders() {
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(manifest(), "movie", "local-1"))
    }

    @Test fun doesNotSendCanonicalIdToUnsupportedType() {
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(manifest(), "series", "tt1234567"))
    }

    @Test fun respectsExplicitAddonPrefixRestrictions() {
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(
            manifest(prefixes = listOf("tmdb:")), "movie", "tt1234567"))
    }

    @Test fun recognizesObjectFormResources() {
        val declared = manifest().copy(resources = listOf(
            JsonObject(mapOf("name" to JsonPrimitive("stream")))))
        assertTrue(StreamDiscoveryRules.supportsCanonicalDirectStream(declared, "movie", "tt1234567"))
    }

    @Test fun catalogOnlyAddonIsNotQueriedForDirectStreams() {
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(
            manifest(resource = "catalog"), "movie", "tt1234567"))
    }

    @Test fun repeatedDirectUrlsAreDeduplicatedAcrossProviders() {
        val streams = listOf(
            StremioStream(name = "A", url = "https://cdn.example/movie.mp4"),
            StremioStream(name = "B", url = "https://cdn.example/movie.mp4"))
        assertEquals(1, StreamDiscoveryRules.rankAndDeduplicate(streams).size)
    }

    @Test fun infohashIsCaseInsensitiveWhileFileIndexStillMatters() {
        val streams = listOf(
            StremioStream(name = "A", infoHash = "ABCD", fileIdx = 1),
            StremioStream(name = "B", infoHash = "abcd", fileIdx = 1),
            StremioStream(name = "C", infoHash = "abcd", fileIdx = 2))
        assertEquals(2, StreamDiscoveryRules.rankAndDeduplicate(streams).size)
    }

    @Test fun opaqueStreamsAreNotMistakenlyMerged() {
        val streams = listOf(StremioStream(name = "Same"), StremioStream(name = "Same"))
        assertEquals(2, StreamDiscoveryRules.rankAndDeduplicate(streams).size)
    }

    @Test fun insecureUrlIsRemovedBeforeRanking() {
        val streams = listOf(
            StremioStream(name = "Unsafe", url = "http://public.example/movie"),
            StremioStream(name = "Safe", url = "https://cdn.example/movie"))
        assertEquals(listOf("Safe"), StreamDiscoveryRules.rankAndDeduplicate(streams).map { it.name })
    }

    @Test fun directStreamsSortByAdvertisedQuality() {
        val streams = listOf(
            StremioStream(name = "720p", url = "https://cdn.example/720"),
            StremioStream(name = "4K", url = "https://cdn.example/4k"),
            StremioStream(name = "1080p", url = "https://cdn.example/1080"))
        assertEquals(listOf("4K", "1080p", "720p"),
            StreamDiscoveryRules.rankAndDeduplicate(streams).map { it.name })
    }
    @Test fun resourceLevelTypesAreRespected() {
        val constrained = manifest().copy(resources = listOf(
            JsonObject(mapOf(
                "name" to JsonPrimitive("stream"),
                "types" to kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("series")))
            ))
        ))
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(constrained, "movie", "tt1234567"))
        assertTrue(StreamDiscoveryRules.supportsCanonicalDirectStream(
            constrained.copy(types = listOf("movie", "series")), "series", "tt1234567"))
    }

    @Test fun resourceLevelIdPrefixesAreRespected() {
        val constrained = manifest(prefixes = emptyList()).copy(resources = listOf(
            JsonObject(mapOf(
                "name" to JsonPrimitive("stream"),
                "idPrefixes" to kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("other:")))
            ))
        ))
        assertFalse(StreamDiscoveryRules.supportsCanonicalDirectStream(constrained, "movie", "tt1234567"))
    }

}
