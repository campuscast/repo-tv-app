package com.campuscast.tvplayer.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.campuscast.tvplayer.core.i18n.I18n
import com.campuscast.tvplayer.core.model.ActivationCodeResponse
import com.campuscast.tvplayer.core.model.ActivationState
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ConnectionStatus
import com.campuscast.tvplayer.core.model.HeartbeatStatus
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.core.model.PlayerHealthSnapshot
import com.campuscast.tvplayer.util.formatDeviceId
import com.campuscast.tvplayer.util.isValidDeviceId
import com.campuscast.tvplayer.util.nowIso
import com.campuscast.tvplayer.util.parseInstant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

private const val ACTIVATION_CODE_TTL_SECONDS = 15 * 60

class PlayerViewModel(
    private val repository: PlayerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var activationPollJob: Job? = null
    private var activationCountdownJob: Job? = null

    private fun t(key: String, params: Map<String, String> = emptyMap()): String {
        return I18n.t(repository.config.value.locale, key, params)
    }

    init {
        observeRepository()
        bootstrap()
    }

    private fun observeRepository() {
        viewModelScope.launch {
            repository.config.collect { config ->
                _uiState.value = _uiState.value.copy(config = config)
            }
        }
        viewModelScope.launch {
            repository.connection.collect { connection ->
                _uiState.value = _uiState.value.copy(connection = connection)
            }
        }
        viewModelScope.launch {
            repository.playback.collect { playback ->
                _uiState.value = _uiState.value.copy(playback = playback)
            }
        }
        viewModelScope.launch {
            repository.cache.collect { cache ->
                _uiState.value = _uiState.value.copy(cache = cache)
            }
        }
        viewModelScope.launch {
            repository.heartbeat.collect { heartbeat ->
                _uiState.value = _uiState.value.copy(heartbeat = heartbeat)
            }
        }
        viewModelScope.launch {
            repository.recentErrors.collect { errors ->
                _uiState.value = _uiState.value.copy(recentErrors = errors)
            }
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(screen = AppScreen.Boot, isBusy = true, setupError = null)

            runCatching {
                repository.bootstrap()
                val config = repository.config.value

                if (config.deviceId.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(screen = AppScreen.Setup, isBusy = false)
                    return@runCatching
                }

                if (config.activationState != ActivationState.ACTIVATED) {
                    _uiState.value = _uiState.value.copy(screen = AppScreen.Activation, isBusy = false)
                    tryResumePendingActivation()
                    return@runCatching
                }

                repository.startRuntime(viewModelScope)
                repository.revalidateDevice()
                repository.syncReleaseAndManifest()
                _uiState.value = _uiState.value.copy(screen = AppScreen.Playback, isBusy = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    screen = AppScreen.Setup,
                    isBusy = false,
                    setupError = error.message ?: t("playback.error"),
                )
            }
        }
    }

    fun submitSetup(deviceIdRaw: String, apiBaseUrl: String, mqttBrokerUrl: String) {
        viewModelScope.launch {
            val formatted = formatDeviceId(deviceIdRaw)
            if (!isValidDeviceId(formatted)) {
                _uiState.value = _uiState.value.copy(setupError = t("setup.errorFormat"))
                return@launch
            }

            _uiState.value = _uiState.value.copy(isBusy = true, setupError = null)
            repository.saveSetup(formatted, apiBaseUrl.trim(), mqttBrokerUrl.trim())
            _uiState.value = _uiState.value.copy(screen = AppScreen.Activation, isBusy = false)
            requestActivationCode()
        }
    }

    fun requestActivationCode() {
        viewModelScope.launch {
            val deviceId = repository.config.value.deviceId
            if (deviceId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(activationError = t("activation.noDeviceId"))
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                activationPhase = ActivationPhase.LOADING,
                activationError = null,
            )

            runCatching {
                repository.requestActivationCode(deviceId)
            }.onSuccess { code ->
                applyActivationCode(code)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    activationPhase = ActivationPhase.ERROR,
                    activationError = error.message ?: t("activation.failed"),
                )
            }
        }
    }

    private fun tryResumePendingActivation() {
        val config = repository.config.value
        val pendingCode = config.pendingActivationCode
        val pendingAt = parseInstant(config.pendingActivationRequestedAt)
        if (pendingCode.isNullOrBlank() || pendingAt == null) {
            requestActivationCode()
            return
        }

        val elapsed = Instant.now().epochSecond - pendingAt.epochSecond
        val remaining = ACTIVATION_CODE_TTL_SECONDS - elapsed.toInt()
        if (remaining <= 0) {
            requestActivationCode()
            return
        }

        _uiState.value = _uiState.value.copy(
            activationCode = pendingCode,
            activationExpiresSeconds = remaining,
            activationPhase = ActivationPhase.POLLING,
            activationError = null,
        )

        startActivationPolling(pendingCode)
        startActivationCountdown(remaining)
    }

    private fun applyActivationCode(code: ActivationCodeResponse) {
        _uiState.value = _uiState.value.copy(
            activationCode = code.activationCode,
            activationExpiresSeconds = code.expiresIn,
            activationPhase = ActivationPhase.POLLING,
            activationError = null,
        )
        startActivationPolling(code.activationCode)
        startActivationCountdown(code.expiresIn)
    }

    private fun startActivationPolling(code: String) {
        activationPollJob?.cancel()
        activationPollJob = viewModelScope.launch {
            while (true) {
                val deviceId = repository.config.value.deviceId
                if (deviceId.isNullOrBlank()) break

                val credentials = runCatching {
                    repository.pollCredentials(deviceId, code)
                }.getOrNull()

                if (credentials?.deviceToken != null) {
                    activationCountdownJob?.cancel()
                    _uiState.value = _uiState.value.copy(activationPhase = ActivationPhase.ACTIVATED)
                    repository.startRuntime(viewModelScope)
                    runCatching { repository.syncReleaseAndManifest() }
                    delay(800)
                    _uiState.value = _uiState.value.copy(
                        screen = AppScreen.Playback,
                        activationError = null,
                    )
                    break
                }

                delay(3_000)
            }
        }
    }

    private fun startActivationCountdown(initialSeconds: Int) {
        activationCountdownJob?.cancel()
        activationCountdownJob = viewModelScope.launch {
            var left = initialSeconds
            while (left >= 0) {
                _uiState.value = _uiState.value.copy(activationExpiresSeconds = left)
                if (left == 0) {
                    _uiState.value = _uiState.value.copy(
                        activationPhase = ActivationPhase.ERROR,
                        activationError = t("activation.expired"),
                    )
                    break
                }
                delay(1_000)
                left -= 1
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val result = repository.syncReleaseAndManifest()
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                transientMessage = when {
                    result == null -> t("status.noRelease")
                    result.usedFallback -> t("status.cachedManifest")
                    else -> t("status.syncComplete")
                }
            )
        }
    }

    fun openStatus() {
        _uiState.value = _uiState.value.copy(screen = AppScreen.Status)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(screen = AppScreen.Settings)
    }

    fun backToPlayback() {
        _uiState.value = _uiState.value.copy(screen = AppScreen.Playback)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(transientMessage = null)
    }

    fun saveConnectionSettings(apiBaseUrl: String, mqttBrokerUrl: String) {
        viewModelScope.launch {
            repository.saveConfig {
                it.copy(apiBaseUrl = apiBaseUrl.trim(), mqttBrokerUrl = mqttBrokerUrl.trim())
            }
            _uiState.value = _uiState.value.copy(transientMessage = t("settings.savedToast"))
        }
    }

    fun changeLocale(locale: String) {
        viewModelScope.launch {
            repository.saveConfig { it.copy(locale = I18n.normalizeLocale(locale)) }
            _uiState.value = _uiState.value.copy(config = repository.config.value)
        }
    }

    fun resetPlayer() {
        viewModelScope.launch {
            repository.resetActivation()
            _uiState.value = PlayerUiState(
                screen = AppScreen.Setup,
                config = repository.config.value,
                playback = repository.playback.value,
                connection = repository.connection.value,
                cache = repository.cache.value,
                heartbeat = repository.heartbeat.value,
            )
        }
    }

    fun openSetup() {
        _uiState.value = _uiState.value.copy(screen = AppScreen.Setup)
    }

    suspend fun healthSnapshot(): PlayerHealthSnapshot = repository.getHealthSnapshot()

    override fun onCleared() {
        super.onCleared()
        repository.stopRuntime()
    }
}

class PlayerViewModelFactory(
    private val repository: PlayerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return PlayerViewModel(repository) as T
    }
}

enum class AppScreen {
    Boot,
    Setup,
    Activation,
    Playback,
    Status,
    Settings,
}

enum class ActivationPhase {
    LOADING,
    POLLING,
    ACTIVATED,
    ERROR,
}

data class PlayerUiState(
    val screen: AppScreen = AppScreen.Boot,
    val isBusy: Boolean = false,
    val isSyncing: Boolean = false,
    val setupError: String? = null,
    val activationError: String? = null,
    val activationCode: String? = null,
    val activationExpiresSeconds: Int = 0,
    val activationPhase: ActivationPhase = ActivationPhase.LOADING,
    val transientMessage: String? = null,
    val config: AppConfig = AppConfig(),
    val playback: PlaybackState = PlaybackState(updatedAtIso = nowIso()),
    val connection: ConnectionStatus = ConnectionStatus(),
    val cache: CacheStatus = CacheStatus(),
    val heartbeat: HeartbeatStatus = HeartbeatStatus(),
    val recentErrors: List<String> = emptyList(),
)
