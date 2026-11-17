package com.cinemax.app.addons

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddonRepositoryIntegrationTest {
    private val selected = StremioMetaPreview(
        id = "tt1234567", type = "movie", name = "Film", releaseInfo = "2021"
    )
    private val primary = InstalledAddon(
        manifestUrl = "https://primary.example/manifest.json",
        id = "primary", name = "Primary", version = "1", installedAtEpochMillis = 0L
    )
    private val secondary = InstalledAddon(
        manifestUrl = "https://secondary.example/manifest.json",
        id = "secondary", name = "Secondary", version = "1", installedAtEpochMillis = 0L
    )
    private val manifest = """
        {"id":"secondary","name":"Secondary","version":"1",
         "types":["movie"],"resources":["stream"],"idPrefixes":["tt"],"catalogs":[]}
    """.trimIndent()
    private val header = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun discoversAnotherAddonsCanonicalStreamWithoutCatalogs() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.host) {
                "primary.example" -> respond(
                    """{"streams":[{"name":"720p","url":"https://cdn.example/first.mp4"}]}""",
                    headers = header
                )
                "secondary.example" -> when (request.url.encodedPath) {
                    "/manifest.json" -> respond(manifest, headers = header)
                    "/stream/movie/tt1234567.json" -> respond(
                        """{"streams":[{"name":"1080p","url":"https://cdn.example/second.mp4"}]}""",
                        headers = header
                    )
                    else -> respond("{}", status = HttpStatusCode.NotFound)
                }
                else -> respond("{}", status = HttpStatusCode.NotFound)
            }
        }) { expectSuccess = true }
        try {
            val collected = AddonRepository(client).loadStreamsAcrossAddons(
                listOf(primary, secondary), selected, "primary"
            )
            assertEquals(2, collected.size)
            assertTrue(collected.any { it.name?.startsWith("Primary") == true })
            assertTrue(collected.any { it.name?.startsWith("Secondary") == true })
        } finally {
            client.close()
        }
    }

    @Test fun otherProviderFailureDoesNotDiscardPreferredPlayback() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            when (request.url.host) {
                "primary.example" -> respond(
                    """{"streams":[{"name":"720p","url":"https://cdn.example/main.mp4"}]}""",
                    headers = header
                )
                else -> respond("Unavailable", status = HttpStatusCode.ServiceUnavailable)
            }
        }) { expectSuccess = true }
        try {
            val collected = AddonRepository(client).loadStreamsAcrossAddons(
                listOf(primary, secondary), selected, "primary"
            )
            assertEquals(1, collected.size)
            assertTrue(collected.single().name?.startsWith("Primary") == true)
        } finally {
            client.close()
        }
    }

    @Test fun cachedPreferredStreamsAreUsedWithoutRefetching() = runBlocking {
        var preferredRequests = 0
        val client = HttpClient(MockEngine { request ->
            if (request.url.host == "primary.example") preferredRequests++
            respond("{}", status = HttpStatusCode.NotFound)
        }) { expectSuccess = true }
        try {
            val cached = listOf(StremioStream(name = "4K", url = "https://cdn.example/main.mp4"))
            val collected = AddonRepository(client).loadStreamsAcrossAddons(
                listOf(primary), selected, "primary", cached
            )
            assertEquals(0, preferredRequests)
            assertEquals(1, collected.size)
            assertEquals("Primary • 4K", collected.single().name)
        } finally {
            client.close()
        }
    }
}
