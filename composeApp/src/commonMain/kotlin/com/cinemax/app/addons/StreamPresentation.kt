package com.cinemax.app.addons

/**
 * Source metadata is untrusted. This classifies presentation labels only;
 * it never follows a URL or interprets addon-supplied executable content.
 */
object StreamPresentation {
    private fun words(stream: StremioStream): String =
        listOfNotNull(stream.name, stream.title).joinToString(" ").lowercase()

    private fun has(text: String, token: String): Boolean =
        Regex("""(?:^|[^a-z0-9])""" + Regex.escape(token) + """(?:$|[^a-z0-9])""").containsMatchIn(text)

    fun videoCodec(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            listOf("av1", "av01").any { has(text, it) } -> "AV1"
            listOf("hevc", "h265", "h.265", "x265").any { has(text, it) } -> "HEVC"
            listOf("avc", "h264", "h.264", "x264").any { has(text, it) } -> "H.264"
            else -> null
        }
    }

    fun hdrFormat(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            listOf("dolby vision", "dovi", "dv").any { has(text, it) } -> "Dolby Vision"
            has(text, "hdr10+") || has(text, "hdr10plus") -> "HDR10+"
            has(text, "hdr10") -> "HDR10"
            has(text, "hdr") -> "HDR"
            else -> null
        }
    }

    fun audioFormat(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            has(text, "truehd") -> "TrueHD"
            has(text, "atmos") -> "Atmos"
            listOf("eac3", "e-ac3", "dd+").any { has(text, it) } -> "E-AC3"
            listOf("ac3", "dd5.1").any { has(text, it) } -> "AC3"
            has(text, "dts") || has(text, "dts-hd") -> "DTS"
            has(text, "aac") -> "AAC"
            else -> null
        }
    }

    fun sourceType(stream: StremioStream): String {
        val url = stream.url?.trim().orEmpty()
        return when {
            !stream.infoHash.isNullOrBlank() -> "Torrent"
            url.startsWith("https://", ignoreCase = true) ||
                url.startsWith("http://", ignoreCase = true) -> "Direct"
            else -> "Unknown"
        }
    }

    fun badges(stream: StremioStream): List<String> =
        listOfNotNull(
            StreamQualityOrder.qualityLabel(stream).takeUnless { it == "Unknown" },
            hdrFormat(stream),
            videoCodec(stream),
            audioFormat(stream)
        )
}
