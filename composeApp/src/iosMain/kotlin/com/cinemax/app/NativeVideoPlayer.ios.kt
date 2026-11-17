package com.cinemax.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeVideoPlayer(
    url: String,
    modifier: Modifier,
    initialPositionMillis: Long,
    onProgress: (positionMillis: Long, durationMillis: Long) -> Unit
) {
    val latestOnProgress = rememberUpdatedState(onProgress)
    val player = remember(url) {
        AVPlayer(uRL = NSURL(string = url)).apply {
            if (initialPositionMillis > 0L) {
                seekToTime(CMTimeMakeWithSeconds(initialPositionMillis.toDouble() / 1000.0, 1000))
            }
            play()
        }
    }
    LaunchedEffect(player) {
        while (isActive) {
            val positionSeconds = CMTimeGetSeconds(player.currentTime())
            val durationSeconds = player.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            if (positionSeconds.isFinite() && durationSeconds.isFinite() &&
                positionSeconds > 0.0 && durationSeconds > 0.0
            ) {
                latestOnProgress.value((positionSeconds * 1000.0).toLong(), (durationSeconds * 1000.0).toLong())
            }
            delay(5_000L)
        }
    }
    DisposableEffect(player) {
        onDispose {
            val positionSeconds = CMTimeGetSeconds(player.currentTime())
            val durationSeconds = player.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: 0.0
            if (positionSeconds.isFinite() && durationSeconds.isFinite() &&
                positionSeconds > 0.0 && durationSeconds > 0.0
            ) {
                latestOnProgress.value(
                    (positionSeconds * 1000.0).toLong(),
                    (durationSeconds * 1000.0).toLong()
                )
            }
            player.pause()
        }
    }
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = true
            }
        },
        // Compose may reuse the same AVPlayerViewController when a new URL is
        // selected. The controller must be rebound to the new AVPlayer; changing
        // remember(url) alone does not update the native view controller.
        update = { controller ->
            if (controller.player !== player) {
                controller.player = player
            }
        },
        modifier = modifier
    )
}
