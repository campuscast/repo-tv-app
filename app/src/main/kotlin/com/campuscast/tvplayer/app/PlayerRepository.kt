package com.campuscast.tvplayer.app

import android.content.Context
import android.util.Log
import com.campuscast.tvplayer.core.cache.ContentCacheManager
import com.campuscast.tvplayer.core.model.ActivationCodeResponse
import com.campuscast.tvplayer.core.model.ActivationState
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ConnectionStatus
import com.campuscast.tvplayer.core.model.DeviceCredentials
import com.campuscast.tvplayer.core.model.DevicePresenceStatus
import com.campuscast.tvplayer.core.model.HeartbeatStatus
import com.campuscast.tvplayer.core.model.LinkState
import com.campuscast.tvplayer.core.model.ManifestApplyResult
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.core.model.PlaybackStatus
import com.campuscast.tvplayer.core.model.PlayerHealthSnapshot
import com.campuscast.tvplayer.core.model.Release
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.network.BackendClient
import com.campuscast.tvplayer.core.network.MqttConnectionMonitor
import com.campuscast.tvplayer.core.playback.ManifestValidator
import com.campuscast.tvplayer.core.playback.PlaybackEvaluator
import com.campuscast.tvplayer.core.storage.AppConfigStore
import com.campuscast.tvplayer.core.storage.ManifestStore
import com.campuscast.tvplayer.core.telemetry.HeartbeatInputs
import com.campuscast.tvplayer.core.telemetry.HeartbeatManager
import com.campuscast.tvplayer.util.nowIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PlayerRepository"

class PlayerRepository(
    private val appContext: Context,
    private val configStore: AppConfigStore,
    private val manifestStore: ManifestStore,
    private val backendClient: BackendClient,
    private val mqttConnectionMonitor: MqttConnectionMonitor,
    private val cacheManager: ContentCacheManager,
    private val evaluator: PlaybackEvaluator,
    private val heartbeatManager: HeartbeatManager,
) {
    private val syncMutex = Mutex()
    private var playbackTickerJob: Job? = null
    private var syncLoopJob: Job? = null
    private var mqttStatusJob: Job? = null
    private var lastManifest: ReleaseManifest? = null
    private val downloadInFlight = ConcurrentHashMap.newKeySet<String>()

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val _manifest = MutableStateFlow<ReleaseManifest?>(null)
    val manifest: StateFlow<ReleaseManifest?> = _manifest.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackState(updatedAtIso = nowIso()))
    val playback: StateFlow<PlaybackState> = _playback.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionStatus())
    val connection: StateFlow<ConnectionStatus> = _connection.asStateFlow()

    private val _cache = MutableStateFlow(CacheStatus())
    val cache: StateFlow<CacheStatus> = _cache.asStateFlow()

    val heartbeat: StateFlow<HeartbeatStatus> = heartbeatManager.status

    private val _recentErrors = MutableStateFlow<List<String>>(emptyList())
    val recentErrors: StateFlow<List<String>> = _recentErrors.asStateFlow()

    suspend fun bootstrap() {
        _config.value = configStore.getConfig()
        _manifest.value = manifestStore.getCurrentManifest()
        lastManifest = _manifest.value
        _cache.value = manifestStore.getCacheStatus()

        val evaluated = evaluator.evaluate(_manifest.value, ::lookupLocalAsset)
        _playback.value = evaluated
    }

    fun startRuntime(scope: CoroutineScope) {
        startPlaybackTicker(scope)
        if (_config.value.activationState == ActivationState.ACTIVATED) {
            startSyncLoop(scope)
            startHeartbeat(scope)
            startMqttMonitoring(scope)
        }
    }

    fun stopRuntime() {
        playbackTickerJob?.cancel()
        syncLoopJob?.cancel()
        mqttStatusJob?.cancel()
        mqttConnectionMonitor.stop()
        heartbeatManager.stop()
    }

    suspend fun saveSetup(deviceId: String, apiBaseUrl: String, mqttBrokerUrl: String): AppConfig {
        val updated = configStore.saveConfig {
            it.copy(
                deviceId = deviceId,
                deviceToken = null,
                mqttClientId = null,
                mqttTopicPrefix = null,
                tokenExpiresAt = null,
                apiBaseUrl = apiBaseUrl,
                mqttBrokerUrl = mqttBrokerUrl,
                activationState = ActivationState.PENDING,
                lastSyncAt = null,
                zoneId = null,
                groupId = null,
                zoneName = null,
                groupName = null,
                pendingActivationCode = null,
                pendingActivationRequestedAt = null,
            )
        }
        _config.value = updated
        return updated
    }

    suspend fun resetActivation(): AppConfig {
        val updated = configStore.saveConfig {
            it.copy(
                deviceId = null,
                deviceName = null,
                deviceToken = null,
                mqttClientId = null,
                mqttTopicPrefix = null,
                tokenExpiresAt = null,
                activationState = ActivationState.UNREGISTERED,
                lastSyncAt = null,
                zoneId = null,
                groupId = null,
                zoneName = null,
                groupName = null,
                pendingActivationCode = null,
                pendingActivationRequestedAt = null,
            )
        }
        _config.value = updated
        _connection.value = ConnectionStatus()
        mqttConnectionMonitor.stop()
        heartbeatManager.stop()
        return updated
    }

    suspend fun revalidateDevice(): DevicePresenceStatus {
        val cfg = _config.value
        val deviceId = cfg.deviceId
        val deviceToken = cfg.deviceToken
        if (cfg.activationState != ActivationState.ACTIVATED || deviceId.isNullOrBlank() || deviceToken.isNullOrBlank()) {
            return DevicePresenceStatus.UNREGISTERED
        }

        _connection.value = _connection.value.copy(backend = LinkState.CONNECTING)
        val existence = backendClient.checkDeviceExists(cfg.apiBaseUrl, deviceId)
        when (existence) {
            DevicePresenceStatus.MISSING -> {
                resetActivation()
                _connection.value = _connection.value.copy(
                    backend = LinkState.DISCONNECTED,
                    lastError = "Device was removed from CMS",
                )
            }

            DevicePresenceStatus.EXISTS -> {
                _connection.value = _connection.value.copy(backend = LinkState.CONNECTED, lastError = null)
                runCatching {
                    backendClient.fetchDeviceInfo(cfg.apiBaseUrl, deviceToken, deviceId)
                }.onSuccess { info ->
                    val updated = configStore.saveConfig {
                        it.copy(
                            zoneId = info.zoneId,
                            groupId = info.groupId,
                            zoneName = info.zoneName,
                            groupName = info.groupName,
                        )
                    }
                    _config.value = updated
                }
            }

            DevicePresenceStatus.UNKNOWN -> {
                _connection.value = _connection.value.copy(
                    backend = LinkState.DISCONNECTED,
                    lastError = "Could not verify device in CMS",
                )
            }

            DevicePresenceStatus.UNREGISTERED -> Unit
        }

        return existence
    }

    suspend fun requestActivationCode(deviceId: String): ActivationCodeResponse {
        val cfg = _config.value
        val response = backendClient.requestActivationCode(cfg.apiBaseUrl, deviceId)
        val updated = configStore.saveConfig {
            it.copy(
                pendingActivationCode = response.activationCode,
                pendingActivationRequestedAt = nowIso(),
            )
        }
        _config.value = updated
        return response
    }

    suspend fun pollCredentials(deviceId: String, code: String): DeviceCredentials? {
        val cfg = _config.value
        val credentials = backendClient.pollCredentials(cfg.apiBaseUrl, deviceId, code) ?: return null
        if (credentials.deviceToken.isNullOrBlank()) return null

        val updated = configStore.saveConfig {
            it.copy(
                deviceToken = credentials.deviceToken,
                mqttClientId = credentials.mqttClientId,
                mqttTopicPrefix = credentials.mqttTopicPrefix,
                tokenExpiresAt = credentials.tokenExpiresAt,
                activationState = ActivationState.ACTIVATED,
                zoneName = credentials.zoneName,
                groupName = credentials.groupName,
                pendingActivationCode = null,
                pendingActivationRequestedAt = null,
            )
        }
        _config.value = updated
        _connection.value = _connection.value.copy(
            backend = LinkState.CONNECTED,
            mqtt = if (credentials.mqttClientId.isNullOrBlank()) {
                LinkState.NOT_INITIALIZED
            } else {
                LinkState.CONNECTING
            },
            lastError = null,
        )
        return credentials
    }

    suspend fun syncReleaseAndManifest(): ManifestApplyResult? {
        return syncMutex.withLock {
            val cfg = _config.value
            val deviceId = cfg.deviceId ?: return@withLock null
            val deviceToken = cfg.deviceToken ?: return@withLock null
            if (deviceId.isBlank() || deviceToken.isBlank()) return@withLock null

            _connection.value = _connection.value.copy(backend = LinkState.CONNECTING, lastError = null)
            try {
                val release = backendClient.fetchRelease(cfg.apiBaseUrl, deviceToken, deviceId)
                if (release == null) {
                    val fallback = lastKnownGood("No active release from backend")
                    return@withLock fallback
                }

                val result = applyReleaseManifest(cfg, release)
                _connection.value = _connection.value.copy(
                    backend = if (_cache.value.missingAssets == 0) LinkState.CONNECTED else LinkState.DISCONNECTED,
                    lastError = if (_cache.value.missingAssets == 0) null else "Manifest downloaded with missing assets",
                )
                return@withLock result
            } catch (error: Throwable) {
                val fallback = lastKnownGood(error.message ?: "Backend unreachable")
                if (fallback != null) {
                    _connection.value = _connection.value.copy(
                        backend = LinkState.DISCONNECTED,
                        lastError = "Using cached manifest (${error.message})",
                    )
                    return@withLock fallback
                }
                _connection.value = _connection.value.copy(
                    backend = LinkState.DISCONNECTED,
                    lastError = error.message,
                )
                appendError(error.message ?: "Release sync failed")
                return@withLock null
            }
        }
    }

    private suspend fun applyReleaseManifest(cfg: AppConfig, release: Release): ManifestApplyResult {
        val token = requireNotNull(cfg.deviceToken) { "No token" }
        val manifest = backendClient.fetchManifest(cfg.apiBaseUrl, token, release.releaseId)
        require(ManifestValidator.isUsable(manifest)) { "Manifest payload is invalid" }

        val prefetch = cacheManager.prefetchManifestAssets(manifest, token)
        val verify = cacheManager.verifyManifestAssets(manifest)
        val now = nowIso()

        if (prefetch.failed.isNotEmpty()) {
            val fallback = lastKnownGood(
                "Manifest prefetch incomplete (${prefetch.failed.size} failed asset downloads)",
            )
            if (fallback != null) {
                return fallback
            }
        }

        manifestStore.saveCurrentManifest(manifest)
        lastManifest = manifest
        _manifest.value = manifest

        val updatedConfig = configStore.saveConfig {
            it.copy(lastSyncAt = now)
        }
        _config.value = updatedConfig

        var lastCleanupAt = _cache.value.lastCleanupAt
        if (verify.missing == 0) {
            cacheManager.cleanupUnusedAssets(manifest)
            lastCleanupAt = now
        }

        val cacheStatus = CacheStatus(
            currentReleaseId = manifest.releaseId,
            totalAssets = verify.total,
            availableAssets = verify.available,
            missingAssets = verify.missing,
            lastPrefetchAt = now,
            lastCleanupAt = lastCleanupAt,
            lastError = prefetch.failed.firstOrNull()
                ?: if (verify.missing > 0) "Missing ${verify.missing}/${verify.total} assets" else null,
        )
        manifestStore.saveCacheStatus(cacheStatus)
        _cache.value = cacheStatus

        return ManifestApplyResult(manifest = manifest, usedFallback = false)
    }

    private suspend fun lastKnownGood(reason: String): ManifestApplyResult? {
        val fallback = manifestStore.getLastKnownGoodManifest() ?: return null
        val verify = cacheManager.verifyManifestAssets(fallback)
        val now = nowIso()
        val status = CacheStatus(
            currentReleaseId = fallback.releaseId,
            totalAssets = verify.total,
            availableAssets = verify.available,
            missingAssets = verify.missing,
            lastPrefetchAt = now,
            lastCleanupAt = _cache.value.lastCleanupAt,
            lastError = reason,
        )
        manifestStore.saveCacheStatus(status)
        _cache.value = status
        _manifest.value = fallback
        lastManifest = fallback
        appendError("Fallback to last-known-good manifest: $reason")
        return ManifestApplyResult(
            manifest = fallback,
            usedFallback = true,
            fallbackReason = reason,
        )
    }

    private fun startPlaybackTicker(scope: CoroutineScope) {
        playbackTickerJob?.cancel()
        playbackTickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                runCatching {
                    val manifest = _manifest.value
                    val nextState = evaluator.evaluate(manifest, ::lookupLocalAsset)
                    _playback.value = if (_connection.value.backend == LinkState.DISCONNECTED && nextState.status == PlaybackStatus.PLAYING) {
                        nextState.copy(status = PlaybackStatus.OFFLINE)
                    } else {
                        nextState
                    }

                    val current = _playback.value.currentAsset
                    if (current != null) {
                        maybeDownloadCurrentAsset(scope, current.assetId)
                    }
                }.onFailure { error ->
                    appendError("Playback ticker failed: ${error.message ?: "unknown error"}")
                }
                delay(1_000)
            }
        }
    }

    private fun maybeDownloadCurrentAsset(scope: CoroutineScope, assetId: String) {
        val manifest = _manifest.value ?: return
        val cfg = _config.value
        val token = cfg.deviceToken ?: return
        val asset = manifest.assets.firstOrNull { it.assetId == assetId } ?: return
        if (lookupLocalAsset(assetId) != null) return
        if (!downloadInFlight.add(assetId)) return

        scope.launch(Dispatchers.IO) {
            runCatching {
                cacheManager.download(asset, token)
            }.onFailure {
                Log.w(TAG, "Current asset download failed: $assetId", it)
            }
            downloadInFlight.remove(assetId)
        }
    }

    private fun startSyncLoop(scope: CoroutineScope) {
        syncLoopJob?.cancel()
        syncLoopJob = scope.launch {
            while (isActive) {
                runCatching {
                    syncReleaseAndManifest()
                }.onFailure { error ->
                    appendError("Sync loop failed: ${error.message ?: "unknown error"}")
                }
                delay(30_000)
            }
        }
    }

    private fun startHeartbeat(scope: CoroutineScope) {
        heartbeatManager.start(scope) {
            HeartbeatInputs(
                config = _config.value,
                playbackState = _playback.value,
                cacheStatus = _cache.value,
                connectionStatus = _connection.value,
                displayWidth = appContext.resources.displayMetrics.widthPixels,
                displayHeight = appContext.resources.displayMetrics.heightPixels,
            )
        }
    }

    private fun startMqttMonitoring(scope: CoroutineScope) {
        mqttStatusJob?.cancel()
        mqttStatusJob = scope.launch {
            mqttConnectionMonitor.status.collect { snapshot ->
                val nextError = when {
                    snapshot.lastError != null -> snapshot.lastError
                    _connection.value.backend == LinkState.CONNECTED -> null
                    else -> _connection.value.lastError
                }
                _connection.value = _connection.value.copy(
                    mqtt = snapshot.state,
                    lastError = nextError,
                )
            }
        }
        mqttConnectionMonitor.start(_config.value)
    }

    fun lookupLocalAsset(assetId: String): String? {
        val manifest = _manifest.value ?: return null
        val asset = manifest.assets.firstOrNull { it.assetId == assetId } ?: return null
        val local = cacheManager.localPath(asset)
        return if (local.exists()) local.absolutePath else null
    }

    suspend fun getHealthSnapshot(): PlayerHealthSnapshot {
        val conn = _connection.value
        val playbackState = _playback.value
        val cacheStatus = _cache.value

        return PlayerHealthSnapshot(
            online = conn.backend == LinkState.CONNECTED || conn.mqtt == LinkState.CONNECTED,
            backendStatus = conn.backend,
            mqttStatus = conn.mqtt,
            currentReleaseId = playbackState.releaseId ?: _manifest.value?.releaseId,
            playbackStatus = playbackState.status,
            cache = cacheStatus,
            heartbeat = heartbeat.value,
            lastError = conn.lastError ?: playbackState.errors.lastOrNull() ?: cacheStatus.lastError,
        )
    }

    fun setConfig(config: AppConfig) {
        _config.value = config
    }

    suspend fun saveConfig(update: (AppConfig) -> AppConfig): AppConfig {
        val updated = configStore.saveConfig(update)
        _config.value = updated
        if (updated.activationState == ActivationState.ACTIVATED) {
            mqttConnectionMonitor.start(updated)
        } else {
            mqttConnectionMonitor.stop()
            _connection.value = _connection.value.copy(mqtt = LinkState.NOT_INITIALIZED)
        }
        return updated
    }

    fun setConnectionStatus(connectionStatus: ConnectionStatus) {
        _connection.value = connectionStatus
    }

    private fun appendError(message: String) {
        val next = (_recentErrors.value + message).takeLast(50)
        _recentErrors.value = next
        _playback.value = _playback.value.copy(
            errors = (_playback.value.errors + message).takeLast(20),
            updatedAtIso = nowIso(),
        )
    }
}
