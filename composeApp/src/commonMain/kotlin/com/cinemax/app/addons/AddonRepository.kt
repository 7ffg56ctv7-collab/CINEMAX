package com.cinemax.app.addons

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.cinemax.app.core.MediaIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import io.ktor.http.isSuccess

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
        require(response.status.isSuccess()) { "Addon manifest request failed: ${response.status.value}" }
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
        require(response.status.isSuccess()) { "Addon catalog request failed: ${response.status.value}" }
        return json.decodeFromString<StremioCatalogResponse>(response.bodyAsText()).metas
            .filter { it.id.isNotBlank() && it.type.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { "${it.type.lowercase()}:${it.id}" }
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
        require(response.status.isSuccess()) { "Addon metadata request failed: ${response.status.value}" }
        return json.decodeFromString<StremioMetaResponse>(response.bodyAsText()).meta
    }

    suspend fun loadStreams(manifestUrl: String, type: String, mediaId: String): List<StremioStream> {
        val url = StremioAddonClient.resourceUrl(validatedBase(manifestUrl), "stream", type, mediaId)
        val response = http.get(url)
        require(response.status.isSuccess()) { "Addon streams request failed: ${response.status.value}" }
        return json.decodeFromString<StremioStreamResponse>(response.bodyAsText()).streams
            .filter { it.url.isNullOrBlank() || StremioAddonClient.isSafeMediaUrl(it.url.trim()) }
            .map { stream -> stream.copy(url = stream.url?.trim()?.takeIf { it.isNotEmpty() }) }
    }

    /**
     * Discover the preferred addon's sources by its own ID and other providers
     * by canonical IMDb ID when advertised, or by title/year catalog matching.
     * Each failing provider is isolated so the others remain available.
     */
    suspend fun loadStreamsAcrossAddons(
        addons: List<InstalledAddon>,
        selected: StremioMetaPreview,
        preferredAddonId: String? = null,
        cachedPreferredStreams: List<StremioStream>? = null
    ): List<StremioStream> = coroutineScope {
        val preferred = addons.firstOrNull { it.id == preferredAddonId }
        val ordered = (listOfNotNull(preferred) + addons.filterNot { it.id == preferredAddonId })
            .distinctBy { it.id to it.manifestUrl }
        val semaphore = Semaphore(MAX_PARALLEL_ADDONS)
        val batches = ordered.map { addon ->
            async {
                semaphore.withPermit {
                    // An unresponsive third-party addon must not block every source.
                    withTimeoutOrNull(ADDON_DISCOVERY_TIMEOUT_MS) {
                        try {
                            val raw = if (addon.id == preferredAddonId) {
                                cachedPreferredStreams ?: loadStreams(addon.manifestUrl, selected.type, selected.id)
                            } else {
                                discoverOtherProviderStreams(addon, selected)
                            }
                            raw.map { stream ->
                                stream.copy(
                                    name = addon.name + " • " +
                                        (stream.name ?: stream.title ?: "مصدر")
                                )
                            }
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            emptyList()
                        }
                    } ?: emptyList()
                }
            }
        }
        StreamDiscoveryRules.rankAndDeduplicate(batches.awaitAll().flatten())
    }

    private suspend fun discoverOtherProviderStreams(
        addon: InstalledAddon,
        selected: StremioMetaPreview
    ): List<StremioStream> {
        val manifest = loadManifest(addon.manifestUrl)
        if (StreamDiscoveryRules.supportsCanonicalDirectStream(manifest, selected.type, selected.id)) {
            try {
                val direct = loadStreams(addon.manifestUrl, selected.type, selected.id)
                if (direct.isNotEmpty()) return direct
            } catch (error: Exception) {
                if (error is CancellationException) throw error
            }
        }

        val selectedKey = MediaIdentity.key(
            type = selected.type, id = selected.id,
            title = selected.name, releaseInfo = selected.releaseInfo
        ) ?: return emptyList()
        val matches = linkedMapOf<String, StremioMetaPreview>()
        manifest.catalogs
            .asSequence()
            .filter { it.type.equals(selected.type, ignoreCase = true) }
            .take(MAX_CATALOGS_PER_ADDON)
            .forEach { catalog ->
                val candidates = linkedMapOf<String, StremioMetaPreview>()
                if (catalog.extra.any { it.name.equals("search", ignoreCase = true) }) {
                    try {
                        searchCatalog(addon.manifestUrl, catalog.type, catalog.id, selected.name)
                            .forEach { candidates[it.type + ":" + it.id] = it }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                    }
                }
                try {
                    loadCatalog(addon.manifestUrl, catalog.type, catalog.id)
                        .forEach { candidates[it.type + ":" + it.id] = it }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                }
                for (candidate in candidates.values) {
                    val key = MediaIdentity.key(
                        type = candidate.type, id = candidate.id,
                        title = candidate.name, releaseInfo = candidate.releaseInfo
                    )
                    if (key == selectedKey) {
                        matches[candidate.type + ":" + candidate.id] = candidate
                    }
                }
            }
        return matches.values.flatMap { matched ->
            try {
                loadStreams(addon.manifestUrl, matched.type, matched.id)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                emptyList()
            }
        }
    }

    private companion object {
        const val MAX_PARALLEL_ADDONS = 3
        const val MAX_CATALOGS_PER_ADDON = 8
        const val ADDON_DISCOVERY_TIMEOUT_MS = 15_000L
    }

    private fun validatedBase(manifestUrl: String): String =
        StremioAddonClient.manifestUrl(manifestUrl)
            ?: throw IllegalArgumentException("رابط الإضافة غير صالح")
}
