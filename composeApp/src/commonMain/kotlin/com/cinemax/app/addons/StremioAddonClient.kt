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
        val value = input.trim().substringBefore('#').removeSuffix("/")
        if (value.isBlank()) return null
        val parsed = try {
            Url(value)
        } catch (_: Throwable) {
            return null
        }
        val secure = parsed.protocol.name == "https"
        val localDevelopment = parsed.protocol.name == "http" &&
            parsed.host in setOf("127.0.0.1", "localhost", "::1")
        if (!secure && !localDevelopment) return null
        if (parsed.host.isBlank()) return null
        if (!parsed.user.isNullOrEmpty() || !parsed.password.isNullOrEmpty()) return null
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
        require(type.isNotBlank() && id.isNotBlank()) { "Resource type and id cannot be blank" }
        val withoutFragment = manifestUrl(baseManifestUrl)
            ?: throw IllegalArgumentException("Invalid addon manifest URL")
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
            extra.entries.sortedBy { it.key }.forEach { (key, value) ->
                require(key.isNotBlank()) { "Extra argument key cannot be blank" }
                append("/")
                append(key.encodeURLPathPart())
                append("=")
                append(value.encodeURLPathPart().replace("&", "%26").replace("=", "%3D"))
            }
            append(".json")
        }
        return path + query
    }

    fun isSafeMediaUrl(input: String): Boolean {
        val parsed = try {
            Url(input.trim())
        } catch (_: Throwable) {
            return false
        }
        val secure = parsed.protocol.name == "https"
        val localDevelopment = parsed.protocol.name == "http" &&
            parsed.host in setOf("127.0.0.1", "localhost", "::1")
        return parsed.host.isNotBlank() && (secure || localDevelopment) &&
            parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty()
    }

    fun isSupportedResource(resource: String): Boolean =
        resource in setOf("catalog", "meta", "stream", "subtitles")
}
