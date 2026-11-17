package com.cinemax.app.addons

/** Deduplicates source entries without altering signed playback URLs. */
object StreamDeduplicator {
    fun unique(streams: List<StremioStream>): List<StremioStream> {
        val seen = mutableSetOf<String>()
        return streams.filter { stream ->
            val hash = stream.infoHash?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            val url = stream.url?.trim()?.takeIf { it.isNotEmpty() }
            val key = when {
                hash != null -> "hash:" + hash + ":" + (stream.fileIdx ?: 0)
                url != null -> "url:" + url
                else -> null
            }
            key == null || seen.add(key)
        }
    }
}
