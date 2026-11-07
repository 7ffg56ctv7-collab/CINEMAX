package com.cinemax.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@Composable
actual fun NativeVideoPlayer(
    url: String,
    modifier: Modifier
) {
    val player = remember(url) {
        AVPlayer(uRL = NSURL(string = url)).apply { play() }
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
