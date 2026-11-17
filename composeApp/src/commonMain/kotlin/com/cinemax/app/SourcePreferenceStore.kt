package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity
import com.russhwolf.settings.Settings

/** Stores the last manually selected provider label without retaining signed media URLs. */
class SourcePreferenceStore(private val settings: Settings = Settings()) {
    private fun key(item: StremioMetaPreview): String =
        "raif.media.preferredSource." +
            MediaIdentity.storageKey(item.type, item.id, item.name, item.releaseInfo)

    fun load(item: StremioMetaPreview): String? =
        settings.getString(key(item), "").takeIf { it.isNotBlank() && "://" !in it && "?token=" !in it.lowercase() }

    fun save(item: StremioMetaPreview, sourceName: String) {
        val label = sourceName.trim().take(256)
        if (label.isNotEmpty() && "://" !in label && "?token=" !in label.lowercase()) settings.putString(key(item), label)
    }

    fun clear(item: StremioMetaPreview) {
        settings.remove(key(item))
    }
}
