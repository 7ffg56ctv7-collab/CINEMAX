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
            .distinctBy { it.item.id }
    }.getOrDefault(emptyList())

    fun save(progress: WatchProgress) {
        if (progress.durationMillis <= 0L || progress.positionMillis < 0L || progress.item.id.isBlank()) return
        val current = load().filterNot { it.item.id == progress.item.id }
        val completionWindow = minOf(15_000L, progress.durationMillis / 2L)
        val finished = progress.positionMillis >= progress.durationMillis - completionWindow
        val next = if (finished || progress.positionMillis == 0L) current
            else (listOf(progress) + current)
                .sortedByDescending { it.updatedAtEpochMillis }
                .take(MAX_ITEMS)
        settings.putString(KEY, json.encodeToString(ListSerializer(WatchProgress.serializer()), next))
    }

    fun remove(id: String) {
        settings.putString(KEY, json.encodeToString(ListSerializer(WatchProgress.serializer()), load().filterNot { it.item.id == id }))
    }

    private companion object {
        const val KEY = "cinemax.watch.progress.v1"
        const val MAX_ITEMS = 30
    }
}
