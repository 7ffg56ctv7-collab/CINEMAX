package com.cinemax.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun NativeVideoPlayer(
    url: String,
    modifier: Modifier,
    initialPositionMillis: Long,
    onProgress: (positionMillis: Long, durationMillis: Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            if (initialPositionMillis > 0L) seekTo(initialPositionMillis)
            prepare()
            playWhenReady = true
        }
    }
    LaunchedEffect(player) {
        while (isActive) {
            val position = player.currentPosition.coerceAtLeast(0L)
            val duration = player.duration
            if (position > 0L && duration > 0L && duration != C.TIME_UNSET) {
                onProgress(position, duration)
            }
            delay(5_000L)
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
            }
        },
        update = { it.player = player },
        modifier = modifier
    )
}
