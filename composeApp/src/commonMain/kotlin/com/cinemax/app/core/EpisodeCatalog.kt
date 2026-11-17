package com.cinemax.app.core

import com.cinemax.app.addons.StremioMetaPreview
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class StremioEpisode(
    val id: String,
    val title: String?,
    val season: Int?,
    val number: Int?
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
    fun parse(videos: List<JsonObject>): List<StremioEpisode> =
        videos.asSequence()
            .mapNotNull { video ->
                val id = (video["id"] as? JsonPrimitive)?.content?.trim().orEmpty()
                if (id.isEmpty() || id.length > 512) return@mapNotNull null
                val season = (video["season"] as? JsonPrimitive)?.content
                    ?.toIntOrNull()?.takeIf { it in 0..999 }
                val number = (video["episode"] as? JsonPrimitive)?.content
                    ?.toIntOrNull()?.takeIf { it in 0..9999 }
                val title = (video["title"] as? JsonPrimitive)?.content?.trim()
                    ?.takeIf { it.isNotBlank() }?.take(150)
                StremioEpisode(id, title, season, number)
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
        series.copy(id = episode.id, name = series.name + " • " + episode.label)
    private const val MAX_EPISODES = 500
}
