package com.cinemax.app.core

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
        val normalizedType = type.trim().lowercase()
        if (normalizedType.isEmpty()) return null

        val imdb = normalizeImdbId(imdbId)
            ?: normalizeImdbId(id)
        if (imdb != null) return "$normalizedType:imdb:$imdb"

        val tmdb = normalizeTmdbId(tmdbId)
            ?: normalizeTmdbId(id)
        if (tmdb != null) return "$normalizedType:tmdb:$tmdb"

        val normalizedTitle = normalizeTitle(title.orEmpty())
        val year = YEAR_REGEX.find(releaseInfo.orEmpty())?.value
        if (normalizedTitle.isEmpty() || year == null) return null

        return "$normalizedType:title:$normalizedTitle:$year"
    }

    private fun normalizeImdbId(value: String?): String? {
        val candidate = value?.trim()?.lowercase() ?: return null
        return IMDB_REGEX.matchEntire(candidate)?.value
    }

    private fun normalizeTmdbId(value: String?): String? {
        val candidate = value?.trim()?.lowercase() ?: return null
        val match = TMDB_REGEX.matchEntire(candidate) ?: return null
        return match.groupValues[1]
    }

    private fun normalizeTitle(value: String): String =
        value.trim()
            .lowercase()
            .replace(ARABIC_DIACRITICS_REGEX, "")
            .replace("ـ", "")
            .replace(TITLE_PUNCTUATION_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()

    private val IMDB_REGEX = Regex("tt[0-9]{7,10}")
    private val TMDB_REGEX = Regex("(?:tmdb|themoviedb)[:/_-]?([0-9]{1,12})")
    private val YEAR_REGEX = Regex("(?:19|20)[0-9]{2}")
    private val ARABIC_DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670]")
    private val TITLE_PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE_REGEX = Regex("\\s+")
}
