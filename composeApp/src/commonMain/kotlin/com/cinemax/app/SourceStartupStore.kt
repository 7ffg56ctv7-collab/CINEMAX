package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class StartupSample(
    val mediaKey: String,
    val label: String,
    val durationMillis: Long
)

/**
 * Records only actual startup timings after AVPlayer reports a valid timestamp.
 * Does not store signed video URLs, passwords or network tokens.
 */
class SourceStartupStore(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(StartupSample.serializer())

    fun durations(item: StremioMetaPreview): Map<String, Long> =
        load().filter { it.mediaKey == mediaKey(item) }
            .associate { it.label to it.durationMillis }

    fun recordSuccess(
        item: StremioMetaPreview,
        label: String,
        startEpochMillis: Long,
        firstProgressEpochMillis: Long
    ) {
        val name = safeLabel(label) ?: return
        if (startEpochMillis <= 0L || firstProgressEpochMillis < startEpochMillis) return
        val elapsed = firstProgressEpochMillis - startEpochMillis
        if (elapsed > MAX_STARTUP_MILLIS) return
        val key = mediaKey(item)
        val rows = load().filterNot { it.mediaKey == key && it.label == name }
        val next = (rows + StartupSample(key, name, elapsed)).takeLast(MAX_RECORDS)
        settings.putString(KEY, json.encodeToString(serializer, next))
    }

    fun clear(item: StremioMetaPreview, label: String) {
        val safe = safeLabel(label) ?: return
        settings.putString(KEY, json.encodeToString(
            serializer, load().filterNot { it.mediaKey == mediaKey(item) && it.label == safe }
        ))
    }

    private fun load(): List<StartupSample> = runCatching {
        json.decodeFromString(serializer, settings.getString(KEY, "[]"))
            .filter { it.mediaKey.isNotBlank() && safeLabel(it.label) != null &&
                it.durationMillis in 0L..MAX_STARTUP_MILLIS }
            .distinctBy { it.mediaKey to it.label }
            .takeLast(MAX_RECORDS)
    }.getOrDefault(emptyList())

    private fun mediaKey(item: StremioMetaPreview) = MediaIdentity.storageKey(item)

    private fun safeLabel(raw: String): String? =
        raw.trim().take(256).takeIf {
            it.isNotBlank() && "://" !in it && it.none { c -> c == '\n' || c == '\r' || c == '\u0000' }
        }

    private companion object {
        const val KEY = "raif.stream.lastStartupMillis.v1"
        const val MAX_STARTUP_MILLIS = 120_000L
        const val MAX_RECORDS = 1_000
    }
}
