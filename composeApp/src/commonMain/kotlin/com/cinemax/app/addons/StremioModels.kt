package com.cinemax.app.addons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class StremioManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val resources: List<JsonElement> = emptyList(),
    val types: List<String> = emptyList(),
    val catalogs: List<StremioCatalog> = emptyList(),
    @SerialName("idPrefixes") val idPrefixes: List<String> = emptyList()
)

@Serializable
data class StremioCatalog(
    val type: String,
    val id: String,
    val name: String? = null
)

@Serializable
data class StremioMetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null
)

@Serializable
data class StremioCatalogResponse(
    val metas: List<StremioMetaPreview> = emptyList()
)

@Serializable
data class StremioStream(
    val name: String? = null,
    val title: String? = null,
    val url: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val behaviorHints: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class StremioStreamResponse(
    val streams: List<StremioStream> = emptyList()
)
