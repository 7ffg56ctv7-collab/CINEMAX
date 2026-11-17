package com.cinemax.app.core

import com.cinemax.app.addons.StremioAddonClient
import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.addons.StremioStream

enum class CatalogSort { Source, Title, Newest, Oldest, Rating }

/** Provider metadata is untrusted: badges describe advertised formats, not verified codecs. */
object DiscoveryPresentation {
    private val yearPattern = Regex("(?:19|20)[0-9]{2}")
    private val ultraHD = Regex("(^|[^a-z0-9])(4k|2160p|uhd)($|[^a-z0-9])")
    private val quadHD = Regex("(^|[^a-z0-9])(1440p|qhd)($|[^a-z0-9])")
    private val fullHD = Regex("(^|[^a-z0-9])(1080p|fhd|full[ -]?hd)($|[^a-z0-9])")
    private val highDefinition = Regex("(^|[^a-z0-9])(720p|720)($|[^a-z0-9])")
    private val standardDefinition = Regex("(^|[^a-z0-9])(480p|576p|sd)($|[^a-z0-9])")
    private val hevc = Regex("(^|[^a-z0-9])(hevc|h[.]?265|x265)($|[^a-z0-9])")
    private val avc = Regex("(^|[^a-z0-9])(avc|h[.]?264|x264)($|[^a-z0-9])")
    private val av1 = Regex("(^|[^a-z0-9])av1($|[^a-z0-9])")
    private val vp9 = Regex("(^|[^a-z0-9])vp9($|[^a-z0-9])")
    private val dolbyVision = Regex("(^|[^a-z0-9])(dolby[ ._-]?vision|dovi|dv)($|[^a-z0-9])")
    private val hdr10Plus = Regex("(^|[^a-z0-9])hdr10[+]($|[^a-z0-9])")
    private val hdr10 = Regex("(^|[^a-z0-9])hdr10($|[^a-z0-9])")
    private val hdr = Regex("(^|[^a-z0-9])hdr($|[^a-z0-9])")
    private val arabicAudio = Regex("(^|[^a-z0-9])(arabic|ara|ar[ -]?audio)($|[^a-z0-9])")
    private val englishAudio = Regex("(^|[^a-z0-9])(english|eng|en[ -]?audio)($|[^a-z0-9])")
    private val arabicSub = Regex("(^|[^a-z0-9])(ar[ -]?sub|arabic[ -]?sub|arabic subtitles)($|[^a-z0-9])")
    private val englishSub = Regex("(^|[^a-z0-9])(en[ -]?sub|english[ -]?sub|english subtitles)($|[^a-z0-9])")
    private val space = Regex("\\s+")

    private fun normalizeSearch(value: String): String = buildString {
        value.trim().lowercase().forEach { char ->
            when {
                char in '\u064B'..'\u065F' || char == '\u0670' || char == 'ـ' -> Unit
                char == 'أ' || char == 'إ' || char == 'آ' -> append('ا')
                char == 'ى' || char == 'ئ' -> append('ي')
                char == 'ؤ' -> append('و')
                char.isLetterOrDigit() -> append(char)
                else -> append(' ')
            }
        }
    }.trim().split(space).filter { it.isNotEmpty() }.joinToString(" ")

    private fun yearOf(item: StremioMetaPreview): Int? =
        yearPattern.find(item.releaseInfo.orEmpty())?.value?.toIntOrNull()

    private fun ratingOf(item: StremioMetaPreview): Double? =
        item.imdbRating?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it in 0.0..10.0 }

    fun visible(
        items: List<StremioMetaPreview>,
        type: String?,
        text: String,
        order: CatalogSort
    ): List<StremioMetaPreview> {
        val searchTerms = normalizeSearch(text).split(' ').filter { it.isNotBlank() }
        val filtered = items.asSequence()
            .filter { type == null || it.type.equals(type, ignoreCase = true) }
            .filter { item ->
                if (searchTerms.isEmpty()) true
                else {
                    val searchable = normalizeSearch(
                        item.name + " " + item.description.orEmpty() + " " +
                            item.releaseInfo.orEmpty() + " " + item.imdbRating.orEmpty()
                    )
                    searchTerms.all { searchable.contains(it) }
                }
            }
            .distinctBy { MediaIdentity.storageKey(it.type, it.id, it.name, it.releaseInfo) }
            .toList()
        val byTitle = compareBy<StremioMetaPreview> { normalizeSearch(it.name) }
        return when (order) {
            CatalogSort.Source -> filtered
            CatalogSort.Title -> filtered.sortedWith(byTitle)
            CatalogSort.Newest -> filtered.sortedWith(
                compareByDescending<StremioMetaPreview> { yearOf(it) ?: -1 }.then(byTitle)
            )
            CatalogSort.Oldest -> filtered.sortedWith(
                compareBy<StremioMetaPreview> { yearOf(it) ?: Int.MAX_VALUE }.then(byTitle)
            )
            CatalogSort.Rating -> filtered.sortedWith(
                compareByDescending<StremioMetaPreview> { ratingOf(it) ?: -1.0 }.then(byTitle)
            )
        }
    }

    fun qualityLabel(stream: StremioStream): String? {
        val label = (stream.name.orEmpty() + " " + stream.title.orEmpty()).lowercase()
        return when {
            ultraHD.containsMatchIn(label) -> "4K"
            quadHD.containsMatchIn(label) -> "1440p"
            fullHD.containsMatchIn(label) -> "1080p"
            highDefinition.containsMatchIn(label) -> "720p"
            standardDefinition.containsMatchIn(label) -> "SD"
            else -> null
        }
    }

    fun codecLabel(stream: StremioStream): String? {
        val label = (stream.name.orEmpty() + " " + stream.title.orEmpty()).lowercase()
        return when {
            hevc.containsMatchIn(label) -> "H.265"
            avc.containsMatchIn(label) -> "H.264"
            av1.containsMatchIn(label) -> "AV1"
            vp9.containsMatchIn(label) -> "VP9"
            else -> null
        }
    }

    fun hdrLabel(stream: StremioStream): String? {
        val label = (stream.name.orEmpty() + " " + stream.title.orEmpty()).lowercase()
        return when {
            dolbyVision.containsMatchIn(label) -> "Dolby Vision"
            hdr10Plus.containsMatchIn(label) -> "HDR10+"
            hdr10.containsMatchIn(label) -> "HDR10"
            hdr.containsMatchIn(label) -> "HDR"
            else -> null
        }
    }

    fun audioLabel(stream: StremioStream): String? {
        val label = (stream.name.orEmpty() + " " + stream.title.orEmpty()).lowercase()
        return when {
            "مدبلج" in label || "صوت عربي" in label || arabicAudio.containsMatchIn(label) -> "عربي"
            "صوت انجليزي" in label || englishAudio.containsMatchIn(label) -> "English"
            else -> null
        }
    }

    fun subtitleLabel(stream: StremioStream): String? {
        val label = (stream.name.orEmpty() + " " + stream.title.orEmpty()).lowercase()
        return when {
            "مترجم" in label || "ترجمة عربية" in label || arabicSub.containsMatchIn(label) -> "عربية"
            englishSub.containsMatchIn(label) -> "English"
            else -> null
        }
    }

    fun providerLabel(stream: StremioStream): String =
        stream.name?.substringBefore(" • ")?.trim()?.takeIf { it.isNotBlank() } ?: "غير محدد"

    fun availableProviders(streams: List<StremioStream>): List<String> =
        streams.map(::providerLabel).distinct().sorted()

    fun isPlayable(stream: StremioStream): Boolean =
        stream.url?.let { StremioAddonClient.isSafeMediaUrl(it) } == true

    private fun qualityRank(stream: StremioStream): Int = when (qualityLabel(stream)) {
        "4K" -> 5
        "1440p" -> 4
        "1080p" -> 3
        "720p" -> 2
        "SD" -> 1
        else -> 0
    }

    /** Direct HTTPS playback first, then the chosen source label, then quality. */
    fun sortedStreams(
        streams: List<StremioStream>,
        playableOnly: Boolean,
        preferredName: String? = null,
        quality: String? = null,
        provider: String? = null,
        brokenLabels: Set<String> = emptySet()
    ): List<StremioStream> =
        streams.asSequence()
            .filter { !playableOnly || isPlayable(it) }
            .filter { quality == null || qualityLabel(it) == quality }
            .filter { provider == null || providerLabel(it) == provider }
            .sortedWith(
                compareByDescending<StremioStream> { isPlayable(it) }
                    .thenBy { (it.name ?: it.title ?: "مصدر") in brokenLabels }
                    .thenByDescending { !preferredName.isNullOrBlank() && it.name == preferredName }
                    .thenByDescending { qualityRank(it) }
            )
            .toList()
}
