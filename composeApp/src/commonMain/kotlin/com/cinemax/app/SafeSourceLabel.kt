package com.cinemax.app

/**
 * Provider source labels are untrusted display text. Never persist URLs or strings
 * which resemble credentials, even when the dangerous part appears after 256 chars.
 */
object SafeSourceLabel {
    private val sensitiveQuery = Regex(
        "(?:^|[?&\\s])(?:token|access_token|api_key|apikey|password|secret|signature|sig|auth|authorization)=",
        RegexOption.IGNORE_CASE
    )

    fun normalize(raw: String): String? {
        val label = raw.trim()
        if (label.isBlank() || "://" in label || "%3a%2f%2f" in label.lowercase()) return null
        if (sensitiveQuery.containsMatchIn(label)) return null
        if (label.any { char ->
                val c = char.code
                c in 0..31 || c in 0x7F..0x9F || c in 0x202A..0x202E || c in 0x2066..0x2069
            }) return null
        return label.take(MAX_LENGTH)
    }

    private const val MAX_LENGTH = 256
}
