package com.cinemax.app.core

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.addons.StremioStream
import com.cinemax.app.addons.StremioAddonClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class StremioEpisode(
    val id: String,
    val title: String?,
    val season: Int?,
    val number: Int?,
    val embeddedStreams: List<StremioStream> = emptyList()
) {
    val label: String
        get() {
            val position = if (season != null && number != null) {
                "S" + season.toString().padStart(2, '0') +
                    "E" + number.toString().padStart(2, '0')
            } else "فيديو"
            return title?.takeIf { it.isNotBlank() }?.let { position + " • " + it }
                ?: position
        }
}

/** Safe mapping of Stremio's metadata videos into watchable TV episodes. */
object EpisodeCatalog {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(videos: List<JsonObject>): List<StremioEpisode> =
        videos.asSequence()
            .take(1_000) // Bound untrusted provider metadata before allocating episode records.
            .mapNotNull { video ->
                val id = (video["id"] as? JsonPrimitive)?.content?.trim().orEmpty()
                if (id.isEmpty() || id.length > 512) return@mapNotNull null
                val season = (video["season"] as? JsonPrimitive)?.content
                    ?.toIntOrNull()?.takeIf { it in 0..999 }
                val number = (video["episode"] as? JsonPrimitive)?.content
                    ?.toIntOrNull()?.takeIf { it in 0..9999 }
                val title = (video["title"] as? JsonPrimitive)?.content?.trim()
                    ?.takeIf { it.isNotBlank() }?.take(150)
                val embedded = (video["streams"] as? JsonArray)
                    ?.take(20)?.mapNotNull { value ->
                        runCatching { json.decodeFromString<StremioStream>(value.toString()) }.getOrNull()
                    }?.filter { it.url == null || StremioAddonClient.isSafeMediaUrl(it.url) }
                    .orEmpty()
                StremioEpisode(id, title, season, number, embedded)
            }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<StremioEpisode> { it.season ?: Int.MAX_VALUE }
                    .thenBy { it.number ?: Int.MAX_VALUE }
                    .thenBy { it.title.orEmpty() }
            )
            .take(MAX_EPISODES)
            .toList()

    fun seasons(episodes: List<StremioEpisode>): List<Int> =
        episodes.mapNotNull { it.season }.distinct().sorted()

    fun forSeason(episodes: List<StremioEpisode>, season: Int?): List<StremioEpisode> =
        if (season == null) episodes else episodes.filter { it.season == season }

    /** The episode gets a distinct progress key instead of sharing the entire show's key. */
    fun playbackPreview(series: StremioMetaPreview, episode: StremioEpisode): StremioMetaPreview =
        series.copy(
            id = episode.id,
            name = series.name + " • " + episode.label,
            seriesId = series.id,
            seriesName = series.name,
            episodeSeason = episode.season,
            episodeNumber = episode.number,
            episodeTitle = episode.title
        )

    /** A saved episode always opens the parent series metadata screen. */
    fun restoreSelection(saved: StremioMetaPreview): Pair<StremioMetaPreview, StremioEpisode>? {
        val parentId = saved.seriesId?.takeIf { it.isNotBlank() } ?: return null
        val parentName = saved.seriesName?.takeIf { it.isNotBlank() } ?: return null
        if (saved.id.isBlank() || !saved.type.equals("series", ignoreCase = true)) return null
        val parent = saved.copy(
            id = parentId, name = parentName,
            seriesId = null, seriesName = null,
            episodeSeason = null, episodeNumber = null, episodeTitle = null
        )
        val episode = StremioEpisode(saved.id, saved.episodeTitle,
            saved.episodeSeason, saved.episodeNumber)
        return parent to episode
    }

    /** Never guess episodes by title; match season and episode or exact ID. */
    fun matchingEpisode(candidates: List<StremioEpisode>, target: StremioEpisode): StremioEpisode? {
        // Exact provider IDs are stronger evidence than human-readable titles.
        candidates.firstOrNull { it.id == target.id }?.let { return it }
        if (target.season != null && target.number != null) {
            return candidates.firstOrNull { it.season == target.season && it.number == target.number }
        }
        return null
    }

    fun nextEpisode(episodes: List<StremioEpisode>, current: StremioEpisode): StremioEpisode? {
        val ordered = episodes.sortedWith(
            compareBy<StremioEpisode> { it.season ?: Int.MAX_VALUE }
                .thenBy { it.number ?: Int.MAX_VALUE }
        )
        val index = ordered.indexOfFirst { it.id == current.id }
        return if (index < 0) null else ordered.getOrNull(index + 1)
    }

    private const val MAX_EPISODES = 500
}
