package com.campuscast.tvplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campuscast.tvplayer.feature.activation.ActivationScreen
import com.campuscast.tvplayer.feature.boot.BootScreen
import com.campuscast.tvplayer.feature.playback.PlaybackScreen
import com.campuscast.tvplayer.feature.settings.SettingsScreen
import com.campuscast.tvplayer.feature.setup.SetupScreen
import com.campuscast.tvplayer.feature.status.StatusScreen
import com.campuscast.tvplayer.ui.theme.CampusCastTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels {
        val app = application as CampusCastTvApplication
        PlayerViewModelFactory(app.container.playerRepository)
    }
    private val appContainer by lazy { (application as CampusCastTvApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer.screenCaptureService.attach(this)

        setContent {
            CampusCastTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlayerRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        appContainer.screenCaptureService.detach(this)
        super.onDestroy()
    }
}

@Composable
private fun PlayerRoot(viewModel: PlayerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.transientMessage) {
        uiState.transientMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = uiState.screen == AppScreen.Playback) {
        viewModel.openStatus()
    }

    BackHandler(enabled = uiState.screen == AppScreen.Status || uiState.screen == AppScreen.Settings) {
        viewModel.backToPlayback()
    }

    BackHandler(enabled = uiState.screen == AppScreen.Activation) {
        viewModel.openSetup()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.screen) {
            AppScreen.Boot -> BootScreen(locale = uiState.config.locale)
            AppScreen.Setup -> SetupScreen(
                config = uiState.config,
                locale = uiState.config.locale,
                error = uiState.setupError,
                onSubmit = viewModel::submitSetup,
                onChangeLocale = viewModel::changeLocale,
            )

            AppScreen.Activation -> ActivationScreen(
                locale = uiState.config.locale,
                deviceId = uiState.config.deviceId,
                code = uiState.activationCode,
                expiresSeconds = uiState.activationExpiresSeconds,
                phase = uiState.activationPhase,
                error = uiState.activationError,
                onRetry = viewModel::requestActivationCode,
            )

            AppScreen.Playback -> PlaybackScreen(
                locale = uiState.config.locale,
                state = uiState.playback,
                onOpenStatus = viewModel::openStatus,
            )

            AppScreen.Status -> StatusScreen(
                locale = uiState.config.locale,
                config = uiState.config,
                connection = uiState.connection,
                playback = uiState.playback,
                cache = uiState.cache,
                heartbeat = uiState.heartbeat,
                recentErrors = uiState.recentErrors,
                latestCrash = uiState.latestCrash,
                isSyncing = uiState.isSyncing,
                onSyncNow = viewModel::syncNow,
                onOpenSettings = viewModel::openSettings,
                onBackToPlayback = viewModel::backToPlayback,
            )

            AppScreen.Settings -> SettingsScreen(
                locale = uiState.config.locale,
                config = uiState.config,
                onSave = viewModel::saveConnectionSettings,
                onChangeLocale = viewModel::changeLocale,
                onResetPlayer = viewModel::resetPlayer,
                onBack = viewModel::backToPlayback,
            )
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}
