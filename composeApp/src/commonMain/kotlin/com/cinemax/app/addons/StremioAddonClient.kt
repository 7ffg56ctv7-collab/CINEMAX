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
        val queryIndex = value.indexOf('?').takeIf { it >= 0 } ?: value.length
        val path = value.substring(0, queryIndex)
        val query = value.substring(queryIndex)
        return if (path.endsWith("/manifest.json")) value else "$path/manifest.json$query"
    }

    /**
     * Builds a Stremio resource URL. Extra arguments are path segments in the
     * protocol's key=value form and are encoded independently to prevent path injection.
     */
    fun resourceUrl(
        baseManifestUrl: String,
        resource: String,
        type: String,
        id: String,
        extra: Map<String, String> = emptyMap()
    ): String {
        require(isSupportedResource(resource)) { "Unsupported Stremio resource" }
        val withoutFragment = baseManifestUrl.substringBefore('#')
        val queryIndex = withoutFragment.indexOf('?').takeIf { it >= 0 } ?: withoutFragment.length
        val manifestPath = withoutFragment.substring(0, queryIndex)
        val query = withoutFragment.substring(queryIndex)
        val base = manifestPath.removeSuffix("/manifest.json").removeSuffix("/")
        val path = buildString {
            append(base)
            append("/")
            append(resource.encodeURLPathPart())
            append("/")
            append(type.encodeURLPathPart())
            append("/")
            append(id.encodeURLPathPart())
            extra.toSortedMap().forEach { (key, value) ->
                require(key.isNotBlank()) { "Extra argument key cannot be blank" }
                append("/")
                append(key.encodeURLPathPart())
                append("=")
                append(value.encodeURLPathPart())
            }
            append(".json")
        }
        return path + query
    }

    fun isSupportedResource(resource: String): Boolean =
        resource in setOf("catalog", "meta", "stream", "subtitles")
}
