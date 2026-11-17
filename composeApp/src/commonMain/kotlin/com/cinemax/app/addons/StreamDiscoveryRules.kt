package com.cinemax.app.addons

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.cinemax.app.core.DiscoveryPresentation

/**
 * Conservative provider matching: never send opaque, addon-local IDs to unrelated addons.
 * Canonical IMDb identifiers are safe to query directly only when a provider advertises
 * the relevant stream resource and identifier prefix.
 */
object StreamDiscoveryRules {
    private val imdb = Regex("tt[0-9]{7,10}")

    fun supportsCanonicalDirectStream(
        manifest: StremioManifest,
        type: String,
        mediaId: String
    ): Boolean {
        if (!imdb.matches(mediaId.lowercase())) return false
        if (manifest.types.isNotEmpty() && manifest.types.none { it.equals(type, ignoreCase = true) }) return false
        if (manifest.idPrefixes.isNotEmpty() && manifest.idPrefixes.none {
            mediaId.startsWith(it, ignoreCase = true)
        }) return false
        return manifest.resources.any { resource ->
            when (resource) {
                is JsonPrimitive -> resource.isString && resource.content.equals("stream", true)
                is JsonObject -> {
                    val name = (resource["name"] as? JsonPrimitive)?.content
                    name.equals("stream", true)
                }
                else -> false
            }
        }
    }

    /**
     * A video file is the same source when the absolute URL is byte-identical,
     * or when infohash and selected file index match.
     * An opaque entry without either identifier must not be collapsed by title alone.
     */
    fun rankAndDeduplicate(sources: List<StremioStream>): List<StremioStream> {
        val ranked = sources
            .filter { it.url == null || StremioAddonClient.isSafeMediaUrl(it.url) }
            .sortedWith(
                compareByDescending<StremioStream> { it.url?.startsWith("https://", true) == true }
                    .thenByDescending { qualityRank(it) }
            )
        val identifiers = mutableSetOf<String>()
        return ranked.filter { stream ->
            val url = stream.url?.trim().orEmpty()
            val hash = stream.infoHash?.trim()?.lowercase().orEmpty()
            when {
                url.isNotEmpty() -> identifiers.add("url:" + url)
                hash.isNotEmpty() -> identifiers.add("torrent:" + hash + ":" + (stream.fileIdx ?: 0))
                else -> true
            }
        }
    }

    private fun qualityRank(stream: StremioStream): Int =
        when (DiscoveryPresentation.qualityLabel(stream)) {
            "4K" -> 5
            "1440p" -> 4
            "1080p" -> 3
            "720p" -> 2
            "SD" -> 1
            else -> 0
        }
}
