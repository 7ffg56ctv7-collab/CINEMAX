package com.cinemax.app.addons

/**
 * Stable, metadata-only ordering. Never probes or opens a stream URL.
 * Unrecognized quality stays at the end; the provider's order is retained
 * within each quality tier.
 */
object StreamQualityOrder {
    fun qualityLabel(stream: StremioStream): String {
        val text = listOfNotNull(stream.name, stream.title).joinToString(" ").lowercase()
        return when {
            Regex("""(?:^|[^0-9])(?:2160p|4k|uhd)(?:$|[^a-z0-9])""").containsMatchIn(text) -> "4K"
            Regex("""(?:^|[^0-9])1080p?(?:$|[^a-z0-9])""").containsMatchIn(text) -> "1080p"
            Regex("""(?:^|[^0-9])720p?(?:$|[^a-z0-9])""").containsMatchIn(text) -> "720p"
            Regex("""(?:^|[^0-9])480p?(?:$|[^a-z0-9])""").containsMatchIn(text) -> "480p"
            else -> "Unknown"
        }
    }

    fun sorted(streams: List<StremioStream>): List<StremioStream> =
        streams.sortedBy {
            when (qualityLabel(it)) {
                "4K" -> 0
                "1080p" -> 1
                "720p" -> 2
                "480p" -> 3
                else -> 4
            }
        }
}
