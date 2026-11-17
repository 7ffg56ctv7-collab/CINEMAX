package com.cinemax.app.core

import com.cinemax.app.addons.StremioMetaPreview
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpisodeCatalogTest {
    private fun episode(id: String, season: Int?, number: Int?, title: String?): JsonObject =
        JsonObject(buildMap {
            put("id", JsonPrimitive(id))
            if (season != null) put("season", JsonPrimitive(season))
            if (number != null) put("episode", JsonPrimitive(number))
            if (title != null) put("title", JsonPrimitive(title))
        })

    @Test fun sortsEpisodesBySeasonAndNumber() {
        val episodes = EpisodeCatalog.parse(listOf(
            episode("show:2:1", 2, 1, "Second"),
            episode("show:1:2", 1, 2, "Later"),
            episode("show:1:1", 1, 1, "First")
        ))
        assertEquals(listOf("show:1:1", "show:1:2", "show:2:1"), episodes.map { it.id })
        assertEquals("S01E01 • First", episodes[0].label)
    }

    @Test fun ignoresInvalidAndDuplicateEpisodeIds() {
        val parsed = EpisodeCatalog.parse(listOf(
            episode("", 1, 1, "Bad"),
            episode("series:1:1", 1, 1, "First"),
            episode("series:1:1", 1, 1, "Duplicate")
        ))
        assertEquals(1, parsed.size)
        assertEquals("First", parsed.single().title)
    }

    @Test fun episodeWithoutSeasonUsesTitleAsFallback() {
        val parsed = EpisodeCatalog.parse(listOf(episode("special", null, null, "Trailer")))
        assertEquals("فيديو • Trailer", parsed.single().label)
    }

    @Test fun filtersEpisodesBySeason() {
        val parsed = EpisodeCatalog.parse(listOf(
            episode("a", 1, 1, "One"),
            episode("b", 2, 1, "Two")
        ))
        assertEquals(listOf(1, 2), EpisodeCatalog.seasons(parsed))
        assertEquals(listOf("b"), EpisodeCatalog.forSeason(parsed, 2).map { it.id })
    }

    @Test fun returnsEmptyForNoVideos() {
        assertTrue(EpisodeCatalog.parse(emptyList()).isEmpty())
    }

    @Test fun individualEpisodeProgressKeysDoNotCollide() {
        val show = StremioMetaPreview("tt1234567", "series", "Show", releaseInfo = "2025")
        val first = EpisodeCatalog.playbackPreview(show, StremioEpisode("tt1234567:1:1", "Pilot", 1, 1))
        val second = EpisodeCatalog.playbackPreview(show, StremioEpisode("tt1234567:1:2", "Next", 1, 2))
        assertEquals("tt1234567:1:1", first.id)
        assertTrue(MediaIdentity.storageKey(first.type, first.id, first.name, first.releaseInfo) !=
            MediaIdentity.storageKey(second.type, second.id, second.name, second.releaseInfo))
    }

    @Test fun capsExcessiveProviderEpisodeLists() {
        val entries = (1..600).map { episode("show:1:" + it, 1, it, "Episode") }
        assertEquals(500, EpisodeCatalog.parse(entries).size)
    }
    @Test fun keepsSafeEmbeddedEpisodeStreamsAndRejectsInsecureOnes() {
        val safe = JsonObject(mapOf(
            "name" to JsonPrimitive("1080p"),
            "url" to JsonPrimitive("https://cdn.example/video.mp4")
        ))
        val unsafe = JsonObject(mapOf(
            "name" to JsonPrimitive("Unsafe"),
            "url" to JsonPrimitive("http://public.example/video.mp4")
        ))
        val video = JsonObject(mapOf(
            "id" to JsonPrimitive("tt1234567:1:1"),
            "season" to JsonPrimitive(1),
            "episode" to JsonPrimitive(1),
            "streams" to kotlinx.serialization.json.JsonArray(listOf(unsafe, safe))
        ))
        val streams = EpisodeCatalog.parse(listOf(video)).single().embeddedStreams
        assertEquals(1, streams.size)
        assertEquals("https://cdn.example/video.mp4", streams.single().url)
    }

}
