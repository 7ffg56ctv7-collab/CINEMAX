package com.cinemax.app.plugins

/**
 * Nuvio provider plugins are executable JavaScript, unlike remote Stremio addons.
 * This interface deliberately does not execute downloaded code.
 *
 * A later runtime implementation must provide:
 * - a disposable JS sandbox (QuickJS or equivalent),
 * - no access to app secrets / filesystem / arbitrary native APIs,
 * - bounded network requests, timeouts, memory and execution limits,
 * - user consent, repository provenance and per-provider enable/disable.
 */
data class NuvioRepositoryManifest(val scrapers: List<NuvioScraperDescriptor> = emptyList())

data class NuvioScraperDescriptor(
    val id: String,
    val name: String,
    val filename: String,
    val version: String? = null,
    val language: String? = null
)

interface NuvioProviderRuntime {
    suspend fun getStreams(
        providerId: String,
        mediaId: String,
        mediaType: String,
        season: Int? = null,
        episode: Int? = null
    ): List<ProviderStream>
}

data class ProviderStream(val name: String, val url: String, val quality: String? = null)
