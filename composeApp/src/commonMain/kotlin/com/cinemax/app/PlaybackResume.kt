package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity

/** Single policy for native-player resume; the setting must affect seeking too. */
object PlaybackResume {
    fun initialPosition(
        item: StremioMetaPreview?,
        entries: List<WatchProgress>,
        enabled: Boolean
    ): Long {
        if (!enabled || item == null) return 0L
        val key = MediaIdentity.storageKey(item)
        val progress = entries.firstOrNull {
            MediaIdentity.storageKey(it.item) == key &&
                it.positionMillis > 0L && it.durationMillis > 0L &&
                it.positionMillis < it.durationMillis && !it.isComplete
        } ?: return 0L
        return progress.positionMillis
    }
}
