package com.cinemax.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
                onProgress((positionSeconds * 1000.0).toLong(), (durationSeconds * 1000.0).toLong())
            }
            delay(5_000L)
        }
    }
    DisposableEffect(player) {
        onDispose { player.pause() }
    }
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = true
            }
        },
        modifier = modifier
    )
}
