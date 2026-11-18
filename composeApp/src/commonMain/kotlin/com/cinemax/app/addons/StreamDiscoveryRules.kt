package com.cinemax.app.addons

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import com.cinemax.app.core.DiscoveryPresentation

/**
 * Conservative provider matching: never send opaque, addon-local IDs to unrelated addons.
 * Canonical IMDb identifiers are safe to query directly only when a provider advertises
 * the relevant stream resource and identifier prefix.
 */
object StreamDiscoveryRules {
    private val imdb = Regex("tt[0-9]{7,10}(?::[0-9]{1,3}:[0-9]{1,4})?")

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
                    val types = (resource["types"] as? JsonArray)
                        ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    val prefixes = (resource["idPrefixes"] as? JsonArray)
                        ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    name.equals("stream", true) &&
                        (types.isNullOrEmpty() || types.any { it.equals(type, true) }) &&
                        (prefixes.isNullOrEmpty() || prefixes.any {
                            mediaId.startsWith(it, ignoreCase = true)
                        })
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
            .filter { it.url.isNullOrBlank() || StremioAddonClient.isSafeMediaUrl(it.url.trim()) }
            .sortedWith(
                compareByDescending<StremioStream> { it.url?.startsWith("https://", true) == true }
                    .thenByDescending { qualityRank(it) }
            )
        val seenUrls = mutableSetOf<String>()
        val seenHashes = mutableSetOf<String>()
        return ranked.filter { stream ->
            val url = stream.url?.trim().orEmpty()
            val hash = stream.infoHash?.trim()?.lowercase().orEmpty()
            val fileIdentity = if (hash.isNotEmpty()) hash + ":" + (stream.fileIdx ?: 0) else ""
            val repeated = (url.isNotEmpty() && url in seenUrls) ||
                (fileIdentity.isNotEmpty() && fileIdentity in seenHashes)
            if (!repeated) {
                if (url.isNotEmpty()) seenUrls.add(url)
                if (fileIdentity.isNotEmpty()) seenHashes.add(fileIdentity)
            }
            !repeated
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
