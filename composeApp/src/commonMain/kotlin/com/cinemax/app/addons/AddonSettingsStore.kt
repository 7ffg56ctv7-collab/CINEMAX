package com.cinemax.app.addons

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Small persistent store backed by the platform's native preferences implementation.
 * Only addon descriptors are stored; credentials and executable plugin code are not.
 */
class AddonSettingsStore(
    private val settings: Settings = Settings(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun load(): List<InstalledAddon> {
        val raw = settings.getStringOrNull(ADDONS_KEY) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(InstalledAddon.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun save(addons: List<InstalledAddon>) {
        settings.putString(
            ADDONS_KEY,
            json.encodeToString(ListSerializer(InstalledAddon.serializer()), addons)
        )
    }

    fun clear() {
        settings.remove(ADDONS_KEY)
    }

    private companion object {
        const val ADDONS_KEY = "cinemax.installed.addons.v1"
    }
}
