package com.campuscast.tvplayer.core.telemetry

import android.util.Log
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ConnectionStatus
import com.campuscast.tvplayer.core.model.HeartbeatStatus
import com.campuscast.tvplayer.core.model.LinkState
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.core.model.TelemetryCache
import com.campuscast.tvplayer.core.model.TelemetryDisplay
import com.campuscast.tvplayer.core.model.TelemetryPayload
import com.campuscast.tvplayer.core.network.BackendClient
import com.campuscast.tvplayer.core.preview.ScreenCaptureService
import com.campuscast.tvplayer.util.nowIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "HeartbeatManager"

class HeartbeatManager(
    private val backendClient: BackendClient,
    private val screenCaptureService: ScreenCaptureService,
) {
    private var job: Job? = null

    private val _status = MutableStateFlow(HeartbeatStatus())
    val status: StateFlow<HeartbeatStatus> = _status

    fun start(
        scope: CoroutineScope,
        intervalMs: Long = 30_000,
        payloadProvider: suspend () -> HeartbeatInputs,
    ) {
        stop()
        _status.value = _status.value.copy(running = true, intervalMs = intervalMs, lastError = null)

        job = scope.launch {
            while (true) {
                sendOne(payloadProvider)
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _status.value = _status.value.copy(running = false)
    }

    private suspend fun sendOne(provider: suspend () -> HeartbeatInputs) {
        val input = provider()
        val config = input.config
        if (config.deviceId.isNullOrBlank() || config.deviceToken.isNullOrBlank()) return

        val now = nowIso()
        _status.value = _status.value.copy(lastAttemptAt = now)

        val payload = TelemetryPayload(
            deviceId = config.deviceId,
            currentReleaseId = input.playbackState.releaseId,
            playbackStatus = input.playbackState.status.name.lowercase(),
            currentSlotId = input.playbackState.currentSlot?.slotId,
            currentPublicationId = input.playbackState.currentPublication?.publicationId,
            currentPublicationTitle = input.playbackState.currentPublication?.title,
            currentPublicationItemId = input.playbackState.currentPublicationItem?.itemId,
            currentPublicationItemTitle = input.playbackState.currentPublicationItem?.title,
            errors = input.playbackState.errors,
            displays = listOf(
                TelemetryDisplay(
                    id = "main",
                    label = "Main TV Display",
                    width = input.displayWidth,
                    height = input.displayHeight,
                )
            ),
            selectedDisplays = config.selectedDisplayIds,
            timestamp = now,
            online = input.connectionStatus.backend == LinkState.CONNECTED || input.connectionStatus.mqtt == LinkState.CONNECTED,
            backendStatus = input.connectionStatus.backend.name.lowercase(),
            mqttStatus = input.connectionStatus.mqtt.name.lowercase(),
            cache = TelemetryCache(
                currentReleaseId = input.cacheStatus.currentReleaseId,
                totalAssets = input.cacheStatus.totalAssets,
                availableAssets = input.cacheStatus.availableAssets,
                missingAssets = input.cacheStatus.missingAssets,
                lastPrefetchAt = input.cacheStatus.lastPrefetchAt,
                lastCleanupAt = input.cacheStatus.lastCleanupAt,
                lastError = input.cacheStatus.lastError,
            ),
            lastError = input.connectionStatus.lastError ?: input.playbackState.errors.lastOrNull(),
        )

        runCatching {
            backendClient.sendTelemetry(config.apiBaseUrl, config.deviceToken, payload)
        }.onSuccess { response ->
            _status.value = _status.value.copy(lastSuccessAt = nowIso(), lastError = null)
            runCatching {
                val preview = screenCaptureService.capturePreview(response.screenshotRequest)
                backendClient.uploadPreview(config.apiBaseUrl, config.deviceToken, preview)
            }.onFailure { error ->
                Log.w(TAG, "Preview upload failed", error)
            }
        }.onFailure { error ->
            _status.value = _status.value.copy(lastError = error.message)
            Log.w(TAG, "Telemetry send failed", error)
        }
    }
}

data class HeartbeatInputs(
    val config: AppConfig,
    val playbackState: PlaybackState,
    val cacheStatus: CacheStatus,
    val connectionStatus: ConnectionStatus,
    val displayWidth: Int,
    val displayHeight: Int,
)
