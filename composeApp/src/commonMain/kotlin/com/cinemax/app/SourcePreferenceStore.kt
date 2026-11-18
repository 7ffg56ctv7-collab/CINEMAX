package com.cinemax.app

import com.cinemax.app.addons.StremioMetaPreview
import com.cinemax.app.core.MediaIdentity
import com.russhwolf.settings.Settings

/** Stores the last manually selected provider label without retaining signed media URLs. */
class SourcePreferenceStore(private val settings: Settings = Settings()) {
    private fun key(item: StremioMetaPreview): String =
        "raif.media.preferredSource." +
            MediaIdentity.storageKey(item)

    fun load(item: StremioMetaPreview): String? =
        SafeSourceLabel.normalize(settings.getString(key(item), ""))

    fun save(item: StremioMetaPreview, sourceName: String) {
        val label = SafeSourceLabel.normalize(sourceName) ?: return
        settings.putString(key(item), label)
    }

    fun clear(item: StremioMetaPreview) {
        settings.remove(key(item))
    }
}
