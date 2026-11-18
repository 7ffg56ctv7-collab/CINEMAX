package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WatchProgress(
    val item: StremioMetaPreview,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAtEpochMillis: Long
) {
    val progressFraction: Float
        get() = if (durationMillis > 0L) (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f) else 0f

    /** Use a proportional completion window for short videos rather than always 15 seconds. */
    val isComplete: Boolean
        get() = durationMillis > 0L &&
            positionMillis >= durationMillis - minOf(15_000L, (durationMillis / 4L).coerceAtLeast(1L))
}

/** Local continue-watching history, updated from the native player's real timeline. */
class WatchProgressStore(
    private val settings: Settings = Settings()
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<WatchProgress> = runCatching {
        json.decodeFromString(ListSerializer(WatchProgress.serializer()), settings.getString(KEY, "[]"))
            .filter { it.positionMillis > 0L && it.durationMillis > 0L && it.positionMillis < it.durationMillis && !it.isComplete && it.updatedAtEpochMillis >= 0L }
            .sortedByDescending { it.updatedAtEpochMillis }
            .distinctBy { identityKey(it.item) }
            .take(MAX_ITEMS)
    }.getOrDefault(emptyList())

    fun save(progress: WatchProgress) {
        if (progress.durationMillis <= 0L || progress.positionMillis <= 0L || progress.updatedAtEpochMillis < 0L) return
        val key = identityKey(progress.item)
        val loaded = load()
        if (loaded.any { identityKey(it.item) == key && it.updatedAtEpochMillis > progress.updatedAtEpochMillis }) return
        val current = loaded.filterNot { identityKey(it.item) == key }
        val next = if (progress.isComplete) current
            else listOf(progress) + current
        settings.putString(KEY, json.encodeToString(ListSerializer(WatchProgress.serializer()), next.take(MAX_ITEMS)))
    }

    fun clear() {
        settings.remove(KEY)
    }

    fun remove(item: StremioMetaPreview) {
        val key = identityKey(item)
        settings.putString(
            KEY,
            json.encodeToString(ListSerializer(WatchProgress.serializer()), load().filterNot { identityKey(it.item) == key })
        )
    }

    /** Legacy ID-only removal retained for existing callers. */
    fun remove(id: String) {
        settings.putString(KEY, json.encodeToString(ListSerializer(WatchProgress.serializer()), load().filterNot { it.item.id == id }))
    }

    private fun identityKey(item: StremioMetaPreview) =
        MediaIdentity.storageKey(item)

    private companion object {
        const val KEY = "cinemax.watch.progress.v1"
        const val MAX_ITEMS = 30
    }
}
