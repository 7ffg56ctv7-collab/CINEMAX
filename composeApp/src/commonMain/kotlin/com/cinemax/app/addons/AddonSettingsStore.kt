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
            sanitize(json.decodeFromString(ListSerializer(InstalledAddon.serializer()), raw))
        }.getOrDefault(emptyList())
    }

    fun save(addons: List<InstalledAddon>) {
        settings.putString(
            ADDONS_KEY,
            json.encodeToString(ListSerializer(InstalledAddon.serializer()), sanitize(addons))
        )
    }

    fun clear() {
        settings.remove(ADDONS_KEY)
    }

    /** Reject broken or insecure old entries and retain only the latest version per addon ID. */
    private fun sanitize(addons: List<InstalledAddon>): List<InstalledAddon> =
        addons.mapNotNull { addon ->
            val manifest = StremioAddonClient.manifestUrl(addon.manifestUrl)
            if (manifest == null || addon.id.isBlank() || addon.name.isBlank() || addon.version.isBlank()) null
            else addon.copy(manifestUrl = manifest, installedAtEpochMillis = addon.installedAtEpochMillis.coerceAtLeast(0L))
        }.asReversed().distinctBy { it.id }.asReversed().takeLast(MAX_ADDONS)

    private companion object {
        const val ADDONS_KEY = "cinemax.installed.addons.v1"
        const val MAX_ADDONS = 100
    }
}
