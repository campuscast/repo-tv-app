package com.campuscast.tvplayer.app

import android.content.Context
import com.campuscast.tvplayer.core.cache.ContentCacheManager
import com.campuscast.tvplayer.core.network.BackendClient
import com.campuscast.tvplayer.core.network.MqttConnectionMonitor
import com.campuscast.tvplayer.core.playback.PlaybackEvaluator
import com.campuscast.tvplayer.core.storage.AppConfigStore
import com.campuscast.tvplayer.core.storage.CrashLogStore
import com.campuscast.tvplayer.core.storage.ManifestStore
import com.campuscast.tvplayer.core.telemetry.HeartbeatManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val configStore: AppConfigStore by lazy { AppConfigStore(appContext) }
    val manifestStore: ManifestStore by lazy { ManifestStore(appContext) }
    val crashLogStore: CrashLogStore by lazy { CrashLogStore(appContext) }
    val backendClient: BackendClient by lazy { BackendClient(httpClient) }
    val mqttConnectionMonitor: MqttConnectionMonitor by lazy { MqttConnectionMonitor() }
    val cacheManager: ContentCacheManager by lazy { ContentCacheManager(httpClient, manifestStore) }
    val playbackEvaluator: PlaybackEvaluator by lazy { PlaybackEvaluator() }
    val heartbeatManager: HeartbeatManager by lazy { HeartbeatManager(backendClient) }

    val playerRepository: PlayerRepository by lazy {
        PlayerRepository(
            appContext = appContext,
            configStore = configStore,
            manifestStore = manifestStore,
            crashLogStore = crashLogStore,
            backendClient = backendClient,
            mqttConnectionMonitor = mqttConnectionMonitor,
            cacheManager = cacheManager,
            evaluator = playbackEvaluator,
            heartbeatManager = heartbeatManager,
        )
    }
}
