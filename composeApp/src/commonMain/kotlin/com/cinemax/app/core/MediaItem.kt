package com.cinemax.app.core

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val type: String,
    val title: String,
    val posterUrl: String? = null,
    val description: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val sourceAddonId: String? = null
)
