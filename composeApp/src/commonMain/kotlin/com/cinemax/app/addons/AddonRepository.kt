package com.cinemax.app.addons

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.cinemax.app.core.MediaIdentity
import kotlinx.coroutines.CancellationException

/**
 * Small, testable HTTP repository for remote Stremio-compatible addons.
 * It validates the manifest URL before making requests and keeps addon content as data.
 */
class AddonRepository(
    private val http: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = false }
) {
    suspend fun loadManifest(input: String): StremioManifest {
        val url = StremioAddonClient.manifestUrl(input)
            ?: throw IllegalArgumentException("رابط الإضافة يجب أن يبدأ بـ https://")
        val response = http.get(url)
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun loadCatalog(
        manifestUrl: String,
        type: String,
        catalogId: String,
        extra: Map<String, String> = emptyMap()
    ): List<StremioMetaPreview> {
        val base = validatedBase(manifestUrl)
        val url = StremioAddonClient.resourceUrl(base, "catalog", type, catalogId, extra)
        val response = http.get(url)
        return json.decodeFromString<StremioCatalogResponse>(response.bodyAsText()).metas
    }

    suspend fun searchCatalog(
        manifestUrl: String,
        type: String,
        catalogId: String,
        query: String,
        skip: Int = 0
    ): List<StremioMetaPreview> {
        require(query.isNotBlank()) { "Search query cannot be blank" }
        require(skip >= 0) { "Skip cannot be negative" }
        return loadCatalog(
            manifestUrl, type, catalogId,
            mapOf("search" to query.trim(), "skip" to skip.toString())
                .filterValues { it != "0" }
        )
    }

    suspend fun loadMeta(manifestUrl: String, type: String, mediaId: String): StremioMeta? {
        val url = StremioAddonClient.resourceUrl(validatedBase(manifestUrl), "meta", type, mediaId)
        val response = http.get(url)
        return json.decodeFromString<StremioMetaResponse>(response.bodyAsText()).meta
    }

    suspend fun loadStreams(manifestUrl: String, type: String, mediaId: String): List<StremioStream> {
        val url = StremioAddonClient.resourceUrl(validatedBase(manifestUrl), "stream", type, mediaId)
        val response = http.get(url)
        return json.decodeFromString<StremioStreamResponse>(response.bodyAsText()).streams
    }

    /**
     * Match catalog entries by canonical identity, then query each addon's own ID.
     * A matching title can have different IDs in different addons.
     */
    suspend fun loadStreamsAcrossAddons(
        addons: List<InstalledAddon>,
        selected: StremioMetaPreview,
        preferredAddonId: String? = null
    ): List<StremioStream> {
        val selectedKey = MediaIdentity.key(
            type = selected.type, id = selected.id,
            title = selected.name, releaseInfo = selected.releaseInfo
        )
        val matches = linkedMapOf<String, Pair<InstalledAddon, StremioMetaPreview>>()
        addons.forEach { addon ->
            try {
                val manifest = loadManifest(addon.manifestUrl)
                manifest.catalogs
                    .filter { it.type.equals(selected.type, ignoreCase = true) }
                    .forEach { catalog ->
                        val candidates = linkedMapOf<String, StremioMetaPreview>()
                        // Search-capable catalogs can find entries beyond their first page.
                        if (catalog.extra.any { it.name.equals("search", ignoreCase = true) }) {
                            try {
                                searchCatalog(addon.manifestUrl, catalog.type, catalog.id, selected.name)
                                    .forEach { candidates["${it.type}:${it.id}"] = it }
                            } catch (error: Exception) {
                                if (error is CancellationException) throw error
                                // Continue with the catalog's normal first page if search is unavailable.
                            }
                        }
                        try {
                            // Keep first-page entries too: some addons advertise search but implement it poorly.
                            loadCatalog(addon.manifestUrl, catalog.type, catalog.id)
                                .forEach { candidates["${it.type}:${it.id}"] = it }
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                        }
                        candidates.values.forEach { candidate ->
                            val candidateKey = MediaIdentity.key(
                                type = candidate.type, id = candidate.id,
                                title = candidate.name, releaseInfo = candidate.releaseInfo
                            )
                            // Never trust opaque addon-local IDs across providers: equal IDs can describe unrelated titles.
                            val sameMedia = selectedKey != null && candidateKey == selectedKey
                            if (sameMedia) {
                                matches["${addon.id}:${candidate.type}:${candidate.id}"] = addon to candidate
                            }
                        }
                    }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
            }
        }
        // Keep the selected addon usable even when its item is beyond the first catalog page.
        addons.firstOrNull { it.id == preferredAddonId }?.let { addon ->
            if (matches.values.none { it.first.id == addon.id } && selected.id.isNotBlank()) {
                matches["${addon.id}:${selected.type}:${selected.id}"] = addon to selected
            }
        }
        val collected = matches.values.flatMap { (addon, media) ->
            try {
                loadStreams(addon.manifestUrl, media.type, media.id).map { stream ->
                    stream.copy(name = "${addon.name} • ${stream.name ?: stream.title ?: "مصدر"}")
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                emptyList()
            }
        }
        // Multiple addons can expose the same direct media URL.
        val seenUrls = mutableSetOf<String>()
        return collected.filter { stream ->
            val url = stream.url?.trim()
            url.isNullOrEmpty() || seenUrls.add(url)
        }
    }
    private fun validatedBase(manifestUrl: String): String =
        StremioAddonClient.manifestUrl(manifestUrl)
            ?: throw IllegalArgumentException("رابط الإضافة غير صالح")
}
