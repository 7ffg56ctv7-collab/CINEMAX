package com.cinemax.app.plugins

/**
 * Describes optional provider repositories without executing downloaded code.
 * This boundary intentionally contains metadata only; it is not a JavaScript runtime.
 */
data class ProviderRepositoryManifest(val providers: List<ProviderDescriptor> = emptyList())

data class ProviderDescriptor(
    val id: String,
    val name: String,
    val filename: String,
    val version: String? = null,
    val language: String? = null
)

interface ProviderRuntime {
    suspend fun getStreams(
        providerId: String,
        mediaId: String,
        mediaType: String,
        season: Int? = null,
        episode: Int? = null
    ): List<ProviderStream>
}

data class ProviderStream(val name: String, val url: String, val quality: String? = null)
