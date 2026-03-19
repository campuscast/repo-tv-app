package com.campuscast.tvplayer.feature.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.core.model.PlaybackStatus
import com.campuscast.tvplayer.util.formatLocalDateTime

@Composable
fun PlaybackScreen(
    state: PlaybackState,
    onOpenStatus: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionUp) {
                    onOpenStatus()
                    true
                } else if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) {
                    onOpenStatus()
                    true
                } else {
                    false
                }
            },
    ) {
        when {
            state.status == PlaybackStatus.IDLE || state.currentSlot == null -> {
                IdlePlaybackView(nextSlotStart = state.nextSlot?.startTime)
            }

            state.status == PlaybackStatus.ERROR -> {
                IdlePlaybackView(nextSlotStart = null, subtitle = state.errors.lastOrNull() ?: "Playback error")
            }

            else -> {
                PlaybackSurface(state)
            }
        }

        if (state.status == PlaybackStatus.OFFLINE) {
            Text(
                text = "Offline - using cached content",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun IdlePlaybackView(nextSlotStart: String?, subtitle: String = "CampusCast Player") {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (nextSlotStart != null) {
            Text(
                text = "Next slot: ${formatLocalDateTime(nextSlotStart)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Text(
            text = "Press UP for service menu",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 22.dp),
        )
    }
}
