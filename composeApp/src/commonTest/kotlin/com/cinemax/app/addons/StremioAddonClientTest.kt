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
        assertNull(StremioAddonClient.manifestUrl("javascript:alert(1)"))
    }

    @Test
    fun permitsOnlyKnownResources() {
        assertTrue(StremioAddonClient.isSupportedResource("catalog"))
        assertTrue(StremioAddonClient.isSupportedResource("stream"))
        assertFalse(StremioAddonClient.isSupportedResource("execute"))
    }
}
