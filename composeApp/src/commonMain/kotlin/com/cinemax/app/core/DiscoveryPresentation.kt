package com.cinemax.app.core

import com.cinemax.app.addons.StremioAddonClient
import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.addons.StremioStream

enum class CatalogSort { Source, Title, Newest }

/** Shared presentation rules. Filtering never modifies the provider's original catalog. */
object DiscoveryPresentation {
    private val yearPattern = Regex("(?:19|20)[0-9]{2}")
    private val ultraHD = Regex("(^|[^a-z0-9])(4k|2160p|uhd)($|[^a-z0-9])")
    private val fullHD = Regex("(^|[^a-z0-9])(1080p|fhd|full[ -]?hd)($|[^a-z0-9])")
    private val highDefinition = Regex("(^|[^a-z0-9])(720p|720)($|[^a-z0-9])")
    private val standardDefinition = Regex("(^|[^a-z0-9])(480p|576p|sd)($|[^a-z0-9])")

    fun visible(
        items: List<StremioMetaPreview>,
        type: String?,
        text: String,
        order: CatalogSort
    ): List<StremioMetaPreview> {
        val query = text.trim()
        val filtered = items.asSequence()
            .filter { type == null || it.type.equals(type, ignoreCase = true) }
            .filter {
                query.isEmpty() || it.name.contains(query, ignoreCase = true) ||
                    it.description?.contains(query, ignoreCase = true) == true
            }
            .distinctBy { MediaIdentity.storageKey(it.type, it.id, it.name, it.releaseInfo) }
            .toList()
        return when (order) {
            CatalogSort.Source -> filtered
            CatalogSort.Title -> filtered.sortedBy { it.name.lowercase() }
            CatalogSort.Newest -> filtered.sortedWith(
                compareByDescending<StremioMetaPreview> {
                    yearPattern.find(it.releaseInfo.orEmpty())?.value?.toIntOrNull() ?: 0
                }.thenBy { it.name.lowercase() }
            )
        }
    }

    fun qualityLabel(stream: StremioStream): String? {
        val label = (stream.name.orEmpty() + " " + stream.title.orEmpty()).lowercase()
        return when {
            ultraHD.containsMatchIn(label) -> "4K"
            fullHD.containsMatchIn(label) -> "1080p"
            highDefinition.containsMatchIn(label) -> "720p"
            standardDefinition.containsMatchIn(label) -> "SD"
            else -> null
        }
    }

    fun isPlayable(stream: StremioStream): Boolean =
        stream.url?.let { StremioAddonClient.isSafeMediaUrl(it) } == true

    private fun qualityRank(stream: StremioStream): Int = when (qualityLabel(stream)) {
        "4K" -> 4
        "1080p" -> 3
        "720p" -> 2
        "SD" -> 1
        else -> 0
    }

    /** Put directly playable sources first, then the best advertised resolution. */
    fun sortedStreams(streams: List<StremioStream>, playableOnly: Boolean): List<StremioStream> =
        streams.asSequence()
            .filter { !playableOnly || isPlayable(it) }
            .sortedWith(
                compareByDescending<StremioStream> { isPlayable(it) }
                    .thenByDescending { qualityRank(it) }
            )
            .toList()
}
