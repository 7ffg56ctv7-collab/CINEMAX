package com.cinemax.app.core

import com.cinemax.app.addons.StremioMetaPreview

/**
 * Stable identity for the same movie/series returned by different catalogs or addons.
 *
 * Prefer globally meaningful provider IDs. Only fall back to title + release year when
 * both are present; title-only matching is intentionally avoided to prevent false merges.
 */
object MediaIdentity {
    fun key(
        type: String,
        id: String? = null,
        imdbId: String? = null,
        tmdbId: String? = null,
        title: String? = null,
        releaseInfo: String? = null
    ): String? {
        val normalizedType = when (type.trim().lowercase()) {
            "film", "movies" -> "movie"
            "tv", "show", "shows", "tvshow", "tvseries" -> "series"
            else -> type.trim().lowercase()
        }
        if (normalizedType.isEmpty()) return null

        val imdbEpisode = normalizeImdbEpisodeId(id)
        if (imdbEpisode != null && normalizedType == "series") {
            return "$normalizedType:imdb-episode:$imdbEpisode"
        }

        val imdb = normalizeImdbId(imdbId)
            ?: normalizeImdbId(id)
        if (imdb != null) return "$normalizedType:imdb:$imdb"

        val tmdb = normalizeTmdbId(tmdbId)
            ?: normalizeTmdbId(id)
        if (tmdb != null) return "$normalizedType:tmdb:$tmdb"

        val normalizedTitle = normalizeTitle(title.orEmpty())
        val year = YEAR_REGEX.find(releaseInfo.orEmpty())?.value
            ?: YEAR_REGEX.find(title.orEmpty())?.value
        if (normalizedTitle.isEmpty() || year == null) return null

        val titleWithoutYear = normalizeTitle(title.orEmpty().replace(YEAR_REGEX, " "))
        if (titleWithoutYear.isEmpty()) return null
        return "$normalizedType:title:$titleWithoutYear:$year"
    }

    /** Stable persistence key; opaque IDs are namespaced with title to avoid cross-addon collisions. */
    fun storageKey(type: String, id: String, title: String, releaseInfo: String? = null): String {
        return key(type = type, id = id, title = title, releaseInfo = releaseInfo)
            ?: "${type.trim().lowercase()}:local:${id.trim().lowercase()}:${normalizeTitle(title)}"
    }

    private fun normalizeImdbEpisodeId(value: String?): String? {
        val candidate = value?.trim()?.lowercase()?.removePrefix("imdb:") ?: return null
        return IMDB_EPISODE_REGEX.matchEntire(candidate)?.value
    }

    /** Separate each episode, including addon-local IDs, from its parent series. */
    fun storageKey(item: StremioMetaPreview): String {
        if (item.type.equals("series", ignoreCase = true) && !item.seriesId.isNullOrBlank()) {
            normalizeImdbEpisodeId(item.id)?.let { return "series:imdb-episode:$it" }
            val parent = key(type = item.type, id = item.seriesId,
                title = item.seriesName, releaseInfo = item.releaseInfo)
                ?: ("series:local-parent:" + item.seriesId.trim().lowercase() + ":" +
                    normalizeTitle(item.seriesName.orEmpty()))
            if (item.episodeSeason != null && item.episodeNumber != null &&
                item.episodeSeason in 0..999 && item.episodeNumber in 0..9999) {
                return "$parent:episode:" + item.episodeSeason + ":" + item.episodeNumber
            }
            return "$parent:episode-id:" + item.id.trim().lowercase()
        }
        return storageKey(item.type, item.id, item.name, item.releaseInfo)
    }

    private fun normalizeImdbId(value: String?): String? {
        val candidate = value?.trim()?.lowercase()?.removePrefix("imdb:") ?: return null
        return IMDB_REGEX.matchEntire(candidate)?.value
    }

    private fun normalizeTmdbId(value: String?): String? {
        val candidate = value?.trim()?.lowercase() ?: return null
        val match = TMDB_REGEX.matchEntire(candidate) ?: return null
        return match.groupValues[1].trimStart('0').ifEmpty { "0" }
    }

    /**
     * Avoid Unicode character-class regexes here: Kotlin/Native's regex engine does not
     * support the same property escapes on every target. Char classification is portable.
     */
    private fun normalizeTitle(value: String): String = buildString {
        value.trim().lowercase().forEach { char ->
            when {
                char in '\u064B'..'\u065F' || char == '\u0670' || char == 'ـ' -> Unit
                char == 'أ' || char == 'إ' || char == 'آ' -> append('ا')
                char == 'ى' -> append('ي')
                char.isLetterOrDigit() -> append(char)
                else -> append(' ')
            }
        }
    }.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }.joinToString(" ")

    private val IMDB_REGEX = Regex("tt[0-9]{7,10}")
    private val IMDB_EPISODE_REGEX = Regex("tt[0-9]{7,10}:[0-9]{1,3}:[0-9]{1,4}")
    private val TMDB_REGEX = Regex("(?:tmdb|themoviedb)[:/_-]?([0-9]{1,12})")
    private val YEAR_REGEX = Regex("(?:19|20)[0-9]{2}")
    private val WHITESPACE_REGEX = Regex("\\s+")
}
