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
        val value = input.trim().substringBefore('#')
        if (value.isBlank() || value.length > MAX_MANIFEST_LENGTH || value.any {
            it == '\n' || it == '\r' || it == '\u0000'
        }) return null
        val parsed = try {
            Url(value)
        } catch (_: Exception) {
            return null
        }
        val secure = parsed.protocol.name == "https"
        val localDevelopment = parsed.protocol.name == "http" &&
            parsed.host in setOf("127.0.0.1", "localhost", "::1")
        if (!secure && !localDevelopment) return null
        if (parsed.host.isBlank()) return null
        if (!parsed.user.isNullOrEmpty() || !parsed.password.isNullOrEmpty()) return null
        val queryIndex = value.indexOf('?').takeIf { it >= 0 } ?: value.length
        // Remove the slash from the path, not the complete URL; configured
        // addons commonly append ?token=... after a trailing path slash.
        val path = value.substring(0, queryIndex).removeSuffix("/")
        val query = value.substring(queryIndex)
        return if (path.endsWith("/manifest.json")) path + query else "$path/manifest.json$query"
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
        require(type.isNotBlank() && type.length <= MAX_TYPE_LENGTH) { "Invalid media type" }
        require(id.isNotBlank() && id.length <= MAX_MEDIA_ID_LENGTH) { "Invalid media ID" }
        require(extra.size <= MAX_EXTRA_ARGS) { "Too many resource arguments" }
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
            extra.entries.sortedBy { it.key }.forEach { (key, value) ->
                require(key.isNotBlank() && key.length <= MAX_EXTRA_KEY_LENGTH) {
                    "Invalid resource argument key"
                }
                require(value.length <= MAX_EXTRA_VALUE_LENGTH) { "Resource argument too long" }
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
        val value = input.trim()
        if (value.isBlank() || value.length > MAX_MEDIA_URL_LENGTH ||
            value.contains('#') || value.any {
                it == '\n' || it == '\r' || it == '\u0000'
            }) return false
        val parsed = try {
            Url(value)
        } catch (_: Exception) {
            return false
        }
        val secure = parsed.protocol.name == "https"
        val localDevelopment = parsed.protocol.name == "http" &&
            parsed.host in setOf("127.0.0.1", "localhost", "::1")
        return (secure || localDevelopment) && parsed.host.isNotBlank() &&
            parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty()
    }

    private const val MAX_MANIFEST_LENGTH = 8_192
    private const val MAX_MEDIA_URL_LENGTH = 16_384
    private const val MAX_TYPE_LENGTH = 128
    private const val MAX_MEDIA_ID_LENGTH = 1_024
    private const val MAX_EXTRA_ARGS = 24
    private const val MAX_EXTRA_KEY_LENGTH = 128
    private const val MAX_EXTRA_VALUE_LENGTH = 4_096

    fun isSupportedResource(resource: String): Boolean =
        resource in setOf("catalog", "meta", "stream", "subtitles")
}
