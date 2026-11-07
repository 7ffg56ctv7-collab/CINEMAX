package com.cinemax.app.addons

import kotlinx.serialization.Serializable

@Serializable
data class InstalledAddon(
    val manifestUrl: String,
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val installedAtEpochMillis: Long
)
