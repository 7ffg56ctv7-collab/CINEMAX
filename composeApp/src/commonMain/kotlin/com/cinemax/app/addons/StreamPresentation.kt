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

    fun containerFormat(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            has(text, "mkv") || has(text, "matroska") -> "MKV"
            has(text, "mp4") -> "MP4"
            has(text, "webm") -> "WebM"
            has(text, "avi") -> "AVI"
            else -> null
        }
    }

    fun releaseSource(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            listOf("bluray", "blu-ray", "bdrip", "brrip").any { has(text, it) } -> "Blu-ray"
            listOf("web-dl", "webdl").any { has(text, it) } -> "WEB-DL"
            listOf("webrip", "web-rip").any { has(text, it) } -> "WEBRip"
            has(text, "hdtv") -> "HDTV"
            has(text, "dvdrip") -> "DVDRip"
            else -> null
        }
    }

    fun frameRate(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            listOf("60fps", "60 fps", "59.94fps").any { has(text, it) } -> "60 fps"
            listOf("50fps", "50 fps").any { has(text, it) } -> "50 fps"
            listOf("30fps", "30 fps", "29.97fps").any { has(text, it) } -> "30 fps"
            listOf("24fps", "24 fps", "23.976fps").any { has(text, it) } -> "24 fps"
            else -> null
        }
    }

    fun audioChannels(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            has(text, "7.1") -> "7.1"
            has(text, "5.1") -> "5.1"
            has(text, "2.0") || has(text, "stereo") -> "Stereo"
            has(text, "mono") || has(text, "1.0") -> "Mono"
            else -> null
        }
    }

    fun subtitleLanguage(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            listOf("arabic sub", "arabic subs", "arabic subtitles", "ar sub", "ar subs").any { has(text, it) } -> "Arabic subtitles"
            listOf("english sub", "english subs", "english subtitles", "en sub", "en subs").any { has(text, it) } -> "English subtitles"
            else -> null
        }
    }

    fun audioLanguage(stream: StremioStream): String? {
        val text = words(stream)
        return when {
            listOf("arabic dub", "arabic dubbed", "ar dub").any { has(text, it) } -> "Arabic dub"
            listOf("english dub", "english dubbed", "en dub").any { has(text, it) } -> "English dub"
            else -> null
        }
    }

    fun sourceType(stream: StremioStream): String {
        val url = stream.url?.trim().orEmpty()
        return when {
            !stream.infoHash.isNullOrBlank() -> "Torrent"
            StremioAddonClient.isSafeMediaUrl(url) -> "Direct"
            else -> "Unknown"
        }
    }

    fun badges(stream: StremioStream): List<String> =
        listOfNotNull(
            StreamQualityOrder.qualityLabel(stream).takeUnless { it == "Unknown" },
            hdrFormat(stream),
            videoCodec(stream),
            audioFormat(stream),
            audioChannels(stream),
            releaseSource(stream),
            frameRate(stream),
            subtitleLanguage(stream),
            audioLanguage(stream),
            containerFormat(stream)
        )
}
