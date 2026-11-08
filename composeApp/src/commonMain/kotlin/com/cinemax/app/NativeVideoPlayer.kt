package com.cinemax.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun NativeVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    initialPositionMillis: Long = 0L,
    onProgress: (positionMillis: Long, durationMillis: Long) -> Unit = { _, _ -> }
)
