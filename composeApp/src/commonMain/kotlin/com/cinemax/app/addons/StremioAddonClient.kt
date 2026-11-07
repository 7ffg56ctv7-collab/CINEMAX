package com.cinemax.app.addons

/**
 * Protocol boundary for Stremio-compatible HTTP addons.
 * Keep URLs HTTPS-only outside local development and never execute addon code.
 */
object StremioAddonClient {
    fun manifestUrl(input: String): String? {
        val value = input.trim().removeSuffix("/")
        if (!value.startsWith("https://") && !value.startsWith("http://127.0.0.1")) return null
        return if (value.endsWith("/manifest.json")) value else "$value/manifest.json"
    }

    fun resourceUrl(baseManifestUrl: String, resource: String, type: String, id: String): String {
        val base = baseManifestUrl.removeSuffix("/manifest.json").removeSuffix("/")
        return "$base/$resource/$type/$id.json"
    }

    fun isSupportedResource(resource: String): Boolean =
        resource in setOf("catalog", "meta", "stream", "subtitles")
}
