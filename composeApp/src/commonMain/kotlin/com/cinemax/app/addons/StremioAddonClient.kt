package com.cinemax.app.addons

import io.ktor.http.Url
import io.ktor.http.encodeURLPathPart

/**
 * Protocol boundary for Stremio-compatible HTTP addons.
 * Remote addons must use HTTPS. Plain HTTP is allowed only for loopback development.
 * Addon responses are data and downloaded addon code is never executed.
 */
object StremioAddonClient {
    fun manifestUrl(input: String): String? {
        val value = input.trim().removeSuffix("/")
        val parsed = try {
            Url(value)
        } catch (_: Throwable) {
            return null
        }
        val secure = parsed.protocol.name == "https"
        val localDevelopment = parsed.protocol.name == "http" &&
            parsed.host in setOf("127.0.0.1", "localhost", "::1")
        if (!secure && !localDevelopment) return null
        if (parsed.user.isNotEmpty() || parsed.password.isNotEmpty()) return null
        return if (value.endsWith("/manifest.json")) value else "$value/manifest.json"
    }

    fun resourceUrl(baseManifestUrl: String, resource: String, type: String, id: String): String {
        require(isSupportedResource(resource)) { "Unsupported Stremio resource" }
        val base = baseManifestUrl.removeSuffix("/manifest.json").removeSuffix("/")
        return "$base/${resource.encodeURLPathPart()}/${type.encodeURLPathPart()}/${id.encodeURLPathPart()}.json"
    }

    fun isSupportedResource(resource: String): Boolean =
        resource in setOf("catalog", "meta", "stream", "subtitles")
}
