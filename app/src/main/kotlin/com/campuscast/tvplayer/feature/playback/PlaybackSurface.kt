package com.campuscast.tvplayer.feature.playback

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.core.playback.mapPlaybackMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

@Composable
fun PlaybackSurface(state: PlaybackState) {
    val metadata = mapPlaybackMetadata(state.currentSlot?.metadata, state.currentPublicationItem)
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(if (metadata.transitionType == "fade") metadata.transitionDurationMs else 0),
        label = "playback_transition_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color.Black),
    ) {
        when {
            state.currentPublicationItem?.type == "custom_slide" -> {
                CustomSlideSurface(state = state)
            }

            state.currentAsset?.contentType?.startsWith("image/") == true -> {
                AssetImageSurface(path = state.currentAssetLocalPath)
            }

            state.currentAsset?.contentType?.startsWith("video/") == true -> {
                VideoSurface(
                    uri = state.currentAssetLocalPath?.let { Uri.fromFile(File(it)) }
                        ?: state.currentAsset?.downloadUrl?.let(Uri::parse),
                    trimInMs = metadata.trimInMs,
                    trimOutMs = metadata.trimOutMs,
                    muted = metadata.muted,
                    loop = metadata.loop,
                )
            }

            else -> {
                EmptyPlayableSurface("No playable content")
            }
        }
    }
}

@Composable
private fun CustomSlideSurface(state: PlaybackState) {
    val slide = state.currentPublicationItem?.slide
    val bgColor = remember(slide?.background) {
        runCatching { Color(android.graphics.Color.parseColor(slide?.background ?: "#111827")) }
            .getOrDefault(Color(0xFF111827))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 96.dp, vertical = 70.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        if (!slide?.title.isNullOrBlank()) {
            Text(
                text = slide?.title.orEmpty(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        if (!slide?.body.isNullOrBlank()) {
            Text(
                text = slide?.body.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFF4F6FA),
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        if (!state.currentAssetLocalPath.isNullOrBlank()) {
            val bitmap = remember(state.currentAssetLocalPath) {
                BitmapFactory.decodeFile(state.currentAssetLocalPath)?.asImageBitmap()
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = state.currentAsset?.filename ?: "slide image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .padding(top = 28.dp),
                )
            }
        }
    }
}

@Composable
private fun AssetImageSurface(path: String?) {
    if (path.isNullOrBlank()) {
        EmptyPlayableSurface("Image is not cached yet")
        return
    }

    val bitmap = remember(path) {
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    }

    if (bitmap == null) {
        EmptyPlayableSurface("Failed to decode image")
        return
    }

    Image(
        bitmap = bitmap,
        contentDescription = "Playback image",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun VideoSurface(
    uri: Uri?,
    trimInMs: Int,
    trimOutMs: Int,
    muted: Boolean,
    loop: Boolean,
) {
    if (uri == null) {
        EmptyPlayableSurface("Video is unavailable")
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = if (loop && trimOutMs <= 0) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(player, trimInMs) {
        if (trimInMs > 0) {
            player.seekTo(trimInMs.toLong())
        }
    }

    LaunchedEffect(player, trimInMs, trimOutMs, loop) {
        if (trimOutMs <= 0) return@LaunchedEffect
        while (isActive) {
            val duration = player.duration
            if (duration > 0 && player.currentPosition >= (duration - trimOutMs)) {
                if (loop) {
                    player.seekTo(trimInMs.toLong())
                    player.play()
                } else {
                    player.pause()
                }
            }
            delay(250)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun EmptyPlayableSurface(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF8B919D),
        )
    }
}
