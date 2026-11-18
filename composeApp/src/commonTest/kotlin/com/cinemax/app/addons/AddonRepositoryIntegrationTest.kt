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
    @Test fun mapsEpisodeToAnotherProvidersIdThroughParentSeriesCatalog() = runBlocking {
        val selectedShow = StremioMetaPreview("main-local", "series", "Show", releaseInfo = "2025")
        val savedEpisode = com.cinemax.app.core.EpisodeCatalog.playbackPreview(
            selectedShow, com.cinemax.app.core.StremioEpisode("main-local:1:2", "Two", 1, 2)
        )
        val other = secondary.copy(id = "other", name = "Other")
        val manifest = """
            {"id":"other","name":"Other","version":"1","types":["series"],
            "resources":["catalog","meta","stream"],"idPrefixes":["other"],
            "catalogs":[{"type":"series","id":"top"}]}
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            when {
                request.url.host == "secondary.example" && path.endsWith("/manifest.json") ->
                    respond(manifest, headers = header)
                request.url.host == "secondary.example" && "/catalog/series/" in path ->
                    respond("""{"metas":[{"id":"other-series","type":"series","name":"Show","releaseInfo":"2025"}]}""", headers = header)
                request.url.host == "secondary.example" && "/meta/series/" in path ->
                    respond("""{"meta":{"id":"other-series","type":"series","name":"Show",
                        "videos":[{"id":"other:1:2","season":1,"episode":2,"title":"Episode Two"}]}}""", headers = header)
                request.url.host == "secondary.example" && "/stream/series/" in path ->
                    respond("""{"streams":[{"name":"1080p","url":"https://cdn.example/second.mp4"}]}""", headers = header)
                else -> respond("Unavailable", status = HttpStatusCode.NotFound)
            }
        }) { expectSuccess = true }
        try {
            val sources = AddonRepository(client).loadStreamsAcrossAddons(
                listOf(other), savedEpisode, preferredAddonId = "absent"
            )
            assertEquals(1, sources.size)
            assertEquals("Other • 1080p", sources.single().name)
        } finally {
            client.close()
        }
    }

    @Test fun refusesToPlayAnotherEpisodeWhenSeasonsDoNotMatch() = runBlocking {
        val original = StremioMetaPreview("local-main", "series", "Show", releaseInfo = "2025")
        val selectedEpisode = com.cinemax.app.core.EpisodeCatalog.playbackPreview(
            original, com.cinemax.app.core.StremioEpisode("main:2:3", "Third", 2, 3)
        )
        var streamRequests = 0
        val manifest = """
            {"id":"secondary","name":"Secondary","version":"1","types":["series"],
             "resources":["catalog","meta","stream"],"catalogs":[{"type":"series","id":"top"}]}
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/manifest.json") -> respond(manifest, headers = header)
                "/catalog/series/" in path ->
                    respond("""{"metas":[{"id":"other","type":"series","name":"Show","releaseInfo":"2025"}]}""", headers = header)
                "/meta/series/" in path ->
                    respond("""{"meta":{"id":"other","type":"series","name":"Show",
                        "videos":[{"id":"other:1:3","season":1,"episode":3}]}}""", headers = header)
                "/stream/series/" in path -> {
                    streamRequests++
                    respond("""{"streams":[{"url":"https://cdn.example/wrong.mp4"}]}""", headers = header)
                }
                else -> respond("{}", status = HttpStatusCode.NotFound)
            }
        }) { expectSuccess = true }
        try {
            val results = AddonRepository(client).loadStreamsAcrossAddons(
                listOf(secondary), selectedEpisode, preferredAddonId = "absent"
            )
            assertTrue(results.isEmpty())
            assertEquals(0, streamRequests)
        } finally {
            client.close()
        }
    }

    @Test fun embeddedEpisodeStreamAvoidsUnnecessaryNetworkCall() = runBlocking {
        val original = StremioMetaPreview("local-main", "series", "Show", releaseInfo = "2025")
        val selected = com.cinemax.app.core.EpisodeCatalog.playbackPreview(
            original, com.cinemax.app.core.StremioEpisode("main:1:2", "Two", 1, 2))
        var streamRequests = 0
        val manifest = """
            {"id":"secondary","name":"Secondary","version":"1","types":["series"],
             "resources":["catalog","meta"],"catalogs":[{"type":"series","id":"top"}]}
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/manifest.json") -> respond(manifest, headers = header)
                "/catalog/series/" in path ->
                    respond("""{"metas":[{"id":"other","type":"series","name":"Show","releaseInfo":"2025"}]}""", headers = header)
                "/meta/series/" in path ->
                    respond("""{"meta":{"id":"other","type":"series","name":"Show",
                        "videos":[{"id":"other:1:2","season":1,"episode":2,
                        "streams":[{"url":"https://cdn.example/embedded.mp4","name":"720p"}]}]}}""", headers = header)
                "/stream/series/" in path -> {
                    streamRequests++
                    respond("{}", status = HttpStatusCode.NotFound)
                }
                else -> respond("{}", status = HttpStatusCode.NotFound)
            }
        }) { expectSuccess = true }
        try {
            val streams = AddonRepository(client).loadStreamsAcrossAddons(
                listOf(secondary), selected, preferredAddonId = "absent")
            assertEquals("https://cdn.example/embedded.mp4", streams.single().url)
            assertEquals(0, streamRequests)
        } finally {
            client.close()
        }
    }

    @Test fun invalidManifestNameIsRejectedBeforeInstallation() = runBlocking {
        val client = HttpClient(MockEngine {
            respond("""{"id":"provider","name":" ","version":"1"}""", headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).loadManifest("https://primary.example/manifest.json")
            }
        } finally { client.close() }
        // Kotlin/Native XCTest requires test functions to return Unit.
        Unit
    }

    @Test fun duplicateCatalogDefinitionsAreSanitized() = runBlocking {
        val client = HttpClient(MockEngine {
            respond("""{"id":"provider","name":"Test","version":"1",
                "catalogs":[{"type":"movie","id":"top"},{"type":"movie","id":"top"},
                {"type":"movie","id":" "},{"type":"series","id":"fresh"}],
                "types":["movie","movie","series",""]}""", headers = header)
        })
        try {
            val manifest = AddonRepository(client).loadManifest("https://primary.example/manifest.json")
            assertEquals(2, manifest.catalogs.size)
            assertEquals(listOf("movie","series"), manifest.types)
        } finally { client.close() }
    }

    @Test fun oversizedSearchIsRejectedWithoutNetworkRequest() = runBlocking {
        var count = 0
        val client = HttpClient(MockEngine {
            count++
            respond("""{"metas":[]}""", headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).searchCatalog(
                    "https://primary.example/manifest.json", "movie", "top", "a".repeat(161))
            }
            assertEquals(0, count)
        } finally { client.close() }
    }

    @Test fun excessivePaginationOffsetIsRejectedWithoutNetworkRequest() = runBlocking {
        var count = 0
        val client = HttpClient(MockEngine {
            count++
            respond("""{"metas":[]}""", headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).searchCatalog(
                    "https://primary.example/manifest.json", "movie", "top", "a", skip = 1_000_001)
            }
            assertEquals(0, count)
        } finally { client.close() }
    }

    @Test fun remoteStreamListsAreBoundedAndInvalidUrlsExcluded() = runBlocking {
        val streams = (1..260).joinToString(",") {
            """{"name":"Stream","url":"https://cdn.example/$it.mp4"}"""
        }
        val client = HttpClient(MockEngine {
            respond("""{"streams":[$streams]}""", headers = header)
        })
        try {
            val loaded = AddonRepository(client).loadStreams(
                "https://primary.example/manifest.json","movie","tt1234567")
            assertEquals(250, loaded.size)
        } finally { client.close() }
    }

    @Test fun corruptCatalogEntriesAreDroppedBeforeDisplay() = runBlocking {
        val client = HttpClient(MockEngine {
            respond("""{"metas":[
                {"id":"","type":"movie","name":"Missing ID"},
                {"id":"good","type":"movie","name":"Valid"},
                {"id":"good","type":"movie","name":"Duplicate"},
                {"id":"bad","type":"movie","name":" "}
            ]}""", headers = header)
        })
        try {
            val loaded = AddonRepository(client).loadCatalog(
                "https://primary.example/manifest.json","movie","top")
            assertEquals(1, loaded.size)
            assertEquals("Valid", loaded.single().name)
        } finally { client.close() }
    }

    @Test fun metadataEpisodeListsCannotGrowWithoutLimit() = runBlocking {
        val episodes = (1..1002).joinToString(",") {
            """{"id":"video-$it","episode":$it}"""
        }
        val client = HttpClient(MockEngine {
            respond("""{"meta":{"id":"tt1234567","type":"series","name":"Show",
                "videos":[$episodes]}}""", headers = header)
        })
        try {
            val meta = AddonRepository(client).loadMeta(
                "https://primary.example/manifest.json","series","tt1234567")
            assertEquals(1000, meta?.videos?.size)
        } finally { client.close() }
    }

    @Test fun literalZeroSearchStringIsNotMistakenForZeroSkip() = runBlocking {
        var requested = ""
        val client = HttpClient(MockEngine { request ->
            requested = request.url.toString()
            respond("""{"metas":[]}""", headers = header)
        }) { expectSuccess = true }
        try {
            AddonRepository(client).searchCatalog(
                "https://primary.example/manifest.json", "movie", "top", "0"
            )
            assertTrue("search=0" in requested)
            assertTrue("skip=" !in requested)
        } finally { client.close() }
    }

    @Test fun hugeManifestIsRejectedBeforeJsonDecoding() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(" ".repeat(256 * 1024 + 1), headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).loadManifest("https://primary.example/manifest.json")
            }
        } finally { client.close() }
    }

    @Test fun hugeCatalogIsRejectedBeforeJsonDecoding() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(" ".repeat(2 * 1024 * 1024 + 1), headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).loadCatalog("https://primary.example/manifest.json", "movie", "top")
            }
        } finally { client.close() }
    }

    @Test fun hugeMetadataIsRejectedBeforeJsonDecoding() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(" ".repeat(4 * 1024 * 1024 + 1), headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).loadMeta("https://primary.example/manifest.json", "movie", "tt1234567")
            }
        } finally { client.close() }
    }

    @Test fun hugeStreamListIsRejectedBeforeJsonDecoding() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(" ".repeat(2 * 1024 * 1024 + 1), headers = header)
        })
        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AddonRepository(client).loadStreams("https://primary.example/manifest.json", "movie", "tt1234567")
            }
        } finally { client.close() }
    }

}
