package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
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
}

/** Local continue-watching history, updated from the native player's real timeline. */
class WatchProgressStore(
    private val settings: Settings = Settings()
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<WatchProgress> = runCatching {
        json.decodeFromString(ListSerializer(WatchProgress.serializer()), settings.getString(KEY, "[]"))
            .filter { it.positionMillis > 0L && it.durationMillis > 0L && it.positionMillis < it.durationMillis }
            .sortedByDescending { it.updatedAtEpochMillis }
    }.getOrDefault(emptyList())

    fun save(progress: WatchProgress) {
        if (progress.durationMillis <= 0L || progress.positionMillis < 0L) return
        val current = load().filterNot { it.item.id == progress.item.id }
        val next = if (progress.positionMillis >= progress.durationMillis - 15_000L) current
            else listOf(progress) + current
        settings.putString(KEY, json.encodeToString(ListSerializer(WatchProgress.serializer()), next.take(MAX_ITEMS)))
    }

    fun remove(id: String) {
        settings.putString(KEY, json.encodeToString(ListSerializer(WatchProgress.serializer()), load().filterNot { it.item.id == id }))
    }

    private companion object {
        const val KEY = "cinemax.watch.progress.v1"
        const val MAX_ITEMS = 30
    }
}
