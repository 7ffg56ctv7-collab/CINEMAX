package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class BrokenSourceRecord(val mediaKey: String, val sourceName: String)

/**
 * User-reported broken sources. Never probes or stores playback URLs.
 * Explicitly reported sources can be ranked below alternative working sources.
 */
class SourceHealthStore(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(BrokenSourceRecord.serializer())

    fun brokenLabels(item: StremioMetaPreview): Set<String> =
        load().filter { it.mediaKey == itemKey(item) }.map { it.sourceName }.toSet()

    fun markBroken(item: StremioMetaPreview, sourceName: String) {
        val label = validLabel(sourceName) ?: return
        val current = load().filterNot { it.mediaKey == itemKey(item) && it.sourceName == label }
        settings.putString(KEY, json.encodeToString(
            listSerializer, (current + BrokenSourceRecord(itemKey(item), label)).takeLast(MAX_RECORDS)
        ))
    }

    fun clearBroken(item: StremioMetaPreview, sourceName: String) {
        val label = validLabel(sourceName) ?: return
        settings.putString(
            KEY, json.encodeToString(listSerializer, load().filterNot {
                it.mediaKey == itemKey(item) && it.sourceName == label
            })
        )
    }

    private fun load(): List<BrokenSourceRecord> = runCatching {
        json.decodeFromString(listSerializer, settings.getString(KEY, "[]"))
            .filter { it.mediaKey.isNotBlank() && validLabel(it.sourceName) != null }
            .distinctBy { it.mediaKey to it.sourceName }
            .takeLast(MAX_RECORDS)
    }.getOrDefault(emptyList())

    private fun validLabel(source: String): String? =
        source.trim().take(256).takeIf { it.isNotBlank() && "://" !in it }

    private fun itemKey(item: StremioMetaPreview): String =
        MediaIdentity.storageKey(item.type, item.id, item.name, item.releaseInfo)

    private companion object {
        const val KEY = "raif.sources.userReportedBroken.v1"
        const val MAX_RECORDS = 1_000
    }
}
