package com.cinemax.app.addons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StremioAddonClientTest {
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
    fun permitsOnlyKnownResources() {
        assertTrue(StremioAddonClient.isSupportedResource("catalog"))
        assertTrue(StremioAddonClient.isSupportedResource("stream"))
        assertFalse(StremioAddonClient.isSupportedResource("execute"))
    }
}
