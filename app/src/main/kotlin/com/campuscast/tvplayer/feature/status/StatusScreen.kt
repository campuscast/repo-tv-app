package com.campuscast.tvplayer.feature.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ConnectionStatus
import com.campuscast.tvplayer.core.model.HeartbeatStatus
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.util.formatLocalDateTime

@Composable
fun StatusScreen(
    config: AppConfig,
    connection: ConnectionStatus,
    playback: PlaybackState,
    cache: CacheStatus,
    heartbeat: HeartbeatStatus,
    recentErrors: List<String>,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToPlayback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Service Status", style = MaterialTheme.typography.headlineMedium)

        StatusCard(title = "Connection") {
            ValueRow("Backend", connection.backend.name.lowercase())
            ValueRow("MQTT", connection.mqtt.name.lowercase())
            ValueRow("Last error", connection.lastError ?: "-")
        }

        StatusCard(title = "Playback") {
            ValueRow("Status", playback.status.name.lowercase())
            ValueRow("Release", playback.releaseId ?: "-")
            ValueRow("Slot", playback.currentSlot?.slotId ?: "-")
            ValueRow("Asset", playback.currentAsset?.filename ?: "-")
            ValueRow("Updated", formatLocalDateTime(playback.updatedAtIso))
        }

        StatusCard(title = "Cache") {
            ValueRow("Current release", cache.currentReleaseId ?: "-")
            ValueRow("Assets", "${cache.availableAssets}/${cache.totalAssets}")
            ValueRow("Missing", cache.missingAssets.toString())
            ValueRow("Last prefetch", formatLocalDateTime(cache.lastPrefetchAt))
            ValueRow("Last cleanup", formatLocalDateTime(cache.lastCleanupAt))
            ValueRow("Last cache error", cache.lastError ?: "-")
        }

        StatusCard(title = "Heartbeat") {
            ValueRow("Running", heartbeat.running.toString())
            ValueRow("Last attempt", formatLocalDateTime(heartbeat.lastAttemptAt))
            ValueRow("Last success", formatLocalDateTime(heartbeat.lastSuccessAt))
            ValueRow("Last error", heartbeat.lastError ?: "-")
        }

        StatusCard(title = "Device") {
            ValueRow("Device ID", config.deviceId ?: "-")
            ValueRow("Zone", config.zoneName ?: config.zoneId ?: "-")
            ValueRow("Group", config.groupName ?: config.groupId ?: "-")
            ValueRow("Last sync", formatLocalDateTime(config.lastSyncAt))
            ValueRow("API", config.apiBaseUrl)
        }

        if (recentErrors.isNotEmpty()) {
            StatusCard(title = "Recent Errors") {
                recentErrors.takeLast(10).reversed().forEach { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onSyncNow, modifier = Modifier.weight(1f)) {
                Text(if (isSyncing) "Syncing..." else "Sync now")
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text("Settings")
            }
            OutlinedButton(onClick = onBackToPlayback, modifier = Modifier.weight(1f)) {
                Text("Back to playback")
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.34f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.66f),
            fontFamily = if (value.length > 24) FontFamily.Monospace else null,
        )
    }
}
