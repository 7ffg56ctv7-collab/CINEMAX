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

    @Test fun recognizesStremioScheduleFlagWithoutTreatingItAsEpisodes() {
        val raw = """
          {"id":"tv-channel","type":"series","name":"Channel",
           "hasScheduledVideos":true,
           "videos":[{"id":"schedule:slot","title":"News"}]}
        """.trimIndent()
        val meta = kotlinx.serialization.json.Json.decodeFromString<com.cinemax.app.addons.StremioMeta>(raw)
        assertTrue(meta.hasScheduledVideos)
        assertEquals(1, meta.videos.size)
    }

    @Test fun savedEpisodeRestoresItsParentSeriesAndPosition() {
        val series = StremioMetaPreview("tt1234567", "series", "The Show", releaseInfo = "2025")
        val episode = StremioEpisode("tt1234567:1:2", "Episode Two", 1, 2)
        val saved = EpisodeCatalog.playbackPreview(series, episode)
        assertEquals("tt1234567", saved.seriesId)
        assertEquals(1, saved.episodeSeason)
        assertEquals(2, saved.episodeNumber)
        val restored = EpisodeCatalog.restoreSelection(saved)
        assertEquals(series, restored?.first)
        assertEquals(episode.id, restored?.second?.id)
        assertEquals("Episode Two", restored?.second?.title)
    }

    @Test fun seriesWithoutResumeContextDoesNotFalselyRestoreEpisode() {
        assertEquals(null, EpisodeCatalog.restoreSelection(
            StremioMetaPreview("tt1234567", "series", "The Show")))
    }

    @Test fun malformedSavedEpisodeContextIsRejected() {
        val incomplete = StremioMetaPreview("opaque", "series", "Episode", seriesId = "parent")
        assertEquals(null, EpisodeCatalog.restoreSelection(incomplete))
        assertEquals(null, EpisodeCatalog.restoreSelection(incomplete.copy(seriesName = " ")))
    }

    @Test fun matchesAcrossProviderLocalEpisodeIdsBySeasonAndNumber() {
        val a = StremioEpisode("a-episode", "A name", 1, 2)
        val b = StremioEpisode("b-episode", "B name", 1, 2)
        assertEquals(b, EpisodeCatalog.matchingEpisode(listOf(b), a))
    }

    @Test fun mismatchedSeasonOrEpisodeDoesNotReturnWrongVideo() {
        val target = StremioEpisode("a", null, 2, 3)
        assertEquals(null, EpisodeCatalog.matchingEpisode(listOf(
            StremioEpisode("b", null, 1, 3),
            StremioEpisode("c", null, 2, 2)
        ), target))
    }

    @Test fun nextEpisodeReturnsOrderedEpisodeAcrossSeasons() {
        val first = StremioEpisode("a", null, 1, 2)
        val second = StremioEpisode("b", null, 2, 1)
        val third = StremioEpisode("c", null, 2, 2)
        assertEquals("b", EpisodeCatalog.nextEpisode(listOf(third,first,second), first)?.id)
        assertEquals("c", EpisodeCatalog.nextEpisode(listOf(third,first,second), second)?.id)
        assertEquals(null, EpisodeCatalog.nextEpisode(listOf(third,first,second), third))
    }

    @Test fun savedEpisodeMetadataSurvivesSerialization() {
        val series = StremioMetaPreview("tt1234567", "series", "The Show")
        val saved = EpisodeCatalog.playbackPreview(series, StremioEpisode("part-1", "Pilot", 1, 1))
        val json = kotlinx.serialization.json.Json
        val data = json.encodeToString(StremioMetaPreview.serializer(), saved)
        val decoded = json.decodeFromString(StremioMetaPreview.serializer(), data)
        assertEquals(saved, decoded)
        assertEquals("tt1234567", EpisodeCatalog.restoreSelection(decoded)?.first?.id)
    }

}
