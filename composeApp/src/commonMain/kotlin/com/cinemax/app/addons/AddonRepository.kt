package com.cinemax.app.addons

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

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

    suspend fun loadCatalog(manifestUrl: String, type: String, catalogId: String): List<StremioMetaPreview> {
        val base = StremioAddonClient.manifestUrl(manifestUrl)
            ?: throw IllegalArgumentException("رابط الإضافة غير صالح")
        val url = StremioAddonClient.resourceUrl(base, "catalog", type, catalogId)
        val response = http.get(url)
        return json.decodeFromString<StremioCatalogResponse>(response.bodyAsText()).metas
    }

    suspend fun loadStreams(manifestUrl: String, type: String, mediaId: String): List<StremioStream> {
        val base = StremioAddonClient.manifestUrl(manifestUrl)
            ?: throw IllegalArgumentException("رابط الإضافة غير صالح")
        val url = StremioAddonClient.resourceUrl(base, "stream", type, mediaId)
        val response = http.get(url)
        return json.decodeFromString<StremioStreamResponse>(response.bodyAsText()).streams
    }
}
