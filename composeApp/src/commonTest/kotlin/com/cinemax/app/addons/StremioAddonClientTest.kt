package com.cinemax.app.addons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class StremioAddonClientTest {
    @Test
    fun manifestQueryAndTrailingSlashDoNotCreateDoubleSlashes() {
        assertEquals(
            "https://addon.example/config/manifest.json?token=abc",
            StremioAddonClient.manifestUrl("https://addon.example/config/?token=abc")
        )
        assertEquals(
            "https://addon.example/config/manifest.json?token=abc",
            StremioAddonClient.manifestUrl("https://addon.example/config/manifest.json?token=abc")
        )
        assertEquals(
            "https://addon.example/config/stream/movie/tt123.json?token=abc",
            StremioAddonClient.resourceUrl("https://addon.example/config/?token=abc", "stream", "movie", "tt123")
        )
    }

    @Test
    fun normalizesManifestUrl() {
        assertEquals("https://addon.example/manifest.json", StremioAddonClient.manifestUrl("https://addon.example"))
        assertEquals("https://addon.example/manifest.json", StremioAddonClient.manifestUrl("https://addon.example/manifest.json"))
    }

    @Test
    fun rejectsInsecureRemoteUrls() {
        assertNull(StremioAddonClient.manifestUrl("http://addon.example"))
        assertNull(StremioAddonClient.manifestUrl("http://127.0.0.1.evil.example"))
        assertEquals("http://127.0.0.1:7000/manifest.json", StremioAddonClient.manifestUrl("http://127.0.0.1:7000"))
        assertNull(StremioAddonClient.manifestUrl("javascript:alert(1)"))
    }

    @Test
    fun encodesDynamicResourcePathSegments() {
        val url = StremioAddonClient.resourceUrl("https://addon.example/manifest.json", "stream", "movie", "tt/123")
        assertEquals("https://addon.example/stream/movie/tt%2F123.json", url)
    }

    @Test
    fun encodesCatalogExtraArgumentsAsSeparateSegments() {
        val url = StremioAddonClient.resourceUrl(
            "https://addon.example/manifest.json",
            "catalog",
            "movie",
            "popular",
            mapOf("search" to "batman & robin")
        )
        assertEquals(
            "https://addon.example/catalog/movie/popular/search=batman%20%26%20robin.json",
            url
        )
    }

    @Test
    fun preservesAddonConfigurationQueryParameters() {
        val manifest = StremioAddonClient.manifestUrl("https://addon.example/config?token=a%2Fb")
        assertEquals("https://addon.example/config/manifest.json?token=a%2Fb", manifest)
        assertEquals(
            "https://addon.example/config/stream/movie/tt123.json?token=a%2Fb",
            StremioAddonClient.resourceUrl(manifest!!, "stream", "movie", "tt123")
        )
    }

    @Test
    fun allowsOnlySecureMediaUrls() {
        assertTrue(StremioAddonClient.isSafeMediaUrl("https://cdn.example/video.m3u8"))
        assertTrue(StremioAddonClient.isSafeMediaUrl("http://127.0.0.1:8080/video.mp4"))
        assertFalse(StremioAddonClient.isSafeMediaUrl("http://cdn.example/video.mp4"))
        assertFalse(StremioAddonClient.isSafeMediaUrl("https://user:pass@cdn.example/video.mp4"))
        assertFalse(StremioAddonClient.isSafeMediaUrl("javascript:alert(1)"))
    }

    @Test
    fun permitsOnlyKnownResources() {
        assertTrue(StremioAddonClient.isSupportedResource("catalog"))
        assertTrue(StremioAddonClient.isSupportedResource("stream"))
        assertFalse(StremioAddonClient.isSupportedResource("execute"))
    }
    @Test
    fun resourceBuilderRejectsUnsafeOriginsAndBlankIdentifiers() {
        val invalidOrigins = listOf(
            "http://remote.example/manifest.json",
            "javascript:alert(1)",
            "https://user:pass@addon.example/manifest.json",
            "http://localhost.evil.example/manifest.json",
            ""
        )
        invalidOrigins.forEach { origin ->
            assertFailsWith<IllegalArgumentException>(origin) {
                StremioAddonClient.resourceUrl(origin, "stream", "movie", "tt123")
            }
        }
        assertFailsWith<IllegalArgumentException> {
            StremioAddonClient.resourceUrl("https://addon.example", "stream", "", "tt123")
        }
        assertFailsWith<IllegalArgumentException> {
            StremioAddonClient.resourceUrl("https://addon.example", "stream", "movie", " ")
        }
        assertEquals(
            "https://addon.example/stream/movie/tt123.json",
            StremioAddonClient.resourceUrl("https://addon.example", "stream", "movie", "tt123")
        )
    }

    /**
     * Thirty independent regression cases covering URL normalization, protocol
     * restrictions, resource allowlisting, and safe playback URL selection.
     */
    @Test
    fun regressionMatrixForAddonAndPlaybackUrls() {
        val normalized = listOf(
            "https://addon.example" to "https://addon.example/manifest.json",
            "https://addon.example/" to "https://addon.example/manifest.json",
            " https://addon.example " to "https://addon.example/manifest.json",
            "https://addon.example/manifest.json" to "https://addon.example/manifest.json",
            "https://addon.example/config" to "https://addon.example/config/manifest.json",
            "https://addon.example/config/" to "https://addon.example/config/manifest.json",
            "https://addon.example/config?key=abc" to "https://addon.example/config/manifest.json?key=abc",
            "https://addon.example/manifest.json?key=abc" to "https://addon.example/manifest.json?key=abc",
            "https://addon.example/#section" to "https://addon.example/manifest.json",
            "https://addon.example/config#section" to "https://addon.example/config/manifest.json"
        )
        normalized.forEach { (input, expected) ->
            assertEquals(expected, StremioAddonClient.manifestUrl(input), input)
        }

        val rejectedManifests = listOf(
            "",
            " ",
            "http://addon.example",
            "ftp://addon.example",
            "javascript:alert(1)",
            "https://user:pass@addon.example",
            "http://127.0.0.1.evil.example",
            "http://localhost.evil.example"
        )
        rejectedManifests.forEach { input ->
            assertNull(StremioAddonClient.manifestUrl(input), input)
        }

        val acceptedPlaybackUrls = listOf(
            "https://cdn.example/video.mp4",
            "https://cdn.example/video.m3u8",
            "https://cdn.example/video.mp4?token=abc",
            "http://127.0.0.1:8080/video.mp4",
            "http://localhost:8080/video.mp4",
            "https://cdn.example/path/to/video.mp4"
        )
        acceptedPlaybackUrls.forEach { input ->
            assertTrue(StremioAddonClient.isSafeMediaUrl(input), input)
        }

        val rejectedPlaybackUrls = listOf(
            "",
            "http://cdn.example/video.mp4",
            "ftp://cdn.example/video.mp4",
            "javascript:alert(1)",
            "https://user:pass@cdn.example/video.mp4",
            "http://localhost.evil.example/video.mp4"
        )
        rejectedPlaybackUrls.forEach { input ->
            assertFalse(StremioAddonClient.isSafeMediaUrl(input), input)
        }
    }

}
