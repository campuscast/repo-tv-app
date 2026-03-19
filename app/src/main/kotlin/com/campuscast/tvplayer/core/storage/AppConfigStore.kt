package com.campuscast.tvplayer.core.storage

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.campuscast.tvplayer.core.model.ActivationState
import com.campuscast.tvplayer.core.model.AppConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.configDataStore by preferencesDataStore(name = "campuscast_player_config")

class AppConfigStore(private val context: Context) {
    companion object {
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val DEVICE_NAME = stringPreferencesKey("device_name")
        private val DEVICE_TOKEN = stringPreferencesKey("device_token")
        private val MQTT_CLIENT_ID = stringPreferencesKey("mqtt_client_id")
        private val MQTT_TOPIC_PREFIX = stringPreferencesKey("mqtt_topic_prefix")
        private val TOKEN_EXPIRES_AT = stringPreferencesKey("token_expires_at")
        private val API_BASE_URL = stringPreferencesKey("api_base_url")
        private val MQTT_BROKER_URL = stringPreferencesKey("mqtt_broker_url")
        private val ACTIVATION_STATE = stringPreferencesKey("activation_state")
        private val SELECTED_DISPLAY_IDS = stringPreferencesKey("selected_display_ids")
        private val LAST_SYNC_AT = stringPreferencesKey("last_sync_at")
        private val ZONE_ID = stringPreferencesKey("zone_id")
        private val GROUP_ID = stringPreferencesKey("group_id")
        private val ZONE_NAME = stringPreferencesKey("zone_name")
        private val GROUP_NAME = stringPreferencesKey("group_name")
        private val PENDING_ACTIVATION_CODE = stringPreferencesKey("pending_activation_code")
        private val PENDING_ACTIVATION_REQUESTED_AT = stringPreferencesKey("pending_activation_requested_at")
        private val LOCALE = stringPreferencesKey("locale")
        private val THEME = stringPreferencesKey("theme")
    }

    val configFlow: Flow<AppConfig> = context.configDataStore.data.map(::toConfig)

    suspend fun getConfig(): AppConfig = configFlow.map { it }.firstSync()

    suspend fun saveConfig(update: (AppConfig) -> AppConfig): AppConfig {
        var updatedConfig = AppConfig()
        context.configDataStore.edit { prefs ->
            val current = toConfig(prefs)
            updatedConfig = update(current)
            writeConfig(prefs, updatedConfig)
        }
        return updatedConfig
    }

    private fun toConfig(prefs: Preferences): AppConfig {
        return AppConfig(
            deviceId = prefs[DEVICE_ID],
            deviceName = prefs[DEVICE_NAME],
            deviceToken = prefs[DEVICE_TOKEN],
            mqttClientId = prefs[MQTT_CLIENT_ID],
            mqttTopicPrefix = prefs[MQTT_TOPIC_PREFIX],
            tokenExpiresAt = prefs[TOKEN_EXPIRES_AT],
            apiBaseUrl = prefs[API_BASE_URL] ?: AppConfig().apiBaseUrl,
            mqttBrokerUrl = prefs[MQTT_BROKER_URL] ?: AppConfig().mqttBrokerUrl,
            activationState = prefs[ACTIVATION_STATE]?.let {
                runCatching { ActivationState.valueOf(it) }.getOrNull()
            } ?: ActivationState.UNREGISTERED,
            selectedDisplayIds = prefs[SELECTED_DISPLAY_IDS]?.split(',')?.filter { it.isNotBlank() } ?: listOf("main"),
            lastSyncAt = prefs[LAST_SYNC_AT],
            zoneId = prefs[ZONE_ID],
            groupId = prefs[GROUP_ID],
            zoneName = prefs[ZONE_NAME],
            groupName = prefs[GROUP_NAME],
            pendingActivationCode = prefs[PENDING_ACTIVATION_CODE],
            pendingActivationRequestedAt = prefs[PENDING_ACTIVATION_REQUESTED_AT],
            locale = prefs[LOCALE] ?: "en",
            theme = prefs[THEME] ?: "dark",
        )
    }

    private fun writeConfig(prefs: MutablePreferences, config: AppConfig) {
        putOrRemove(prefs, DEVICE_ID, config.deviceId)
        putOrRemove(prefs, DEVICE_NAME, config.deviceName)
        putOrRemove(prefs, DEVICE_TOKEN, config.deviceToken)
        putOrRemove(prefs, MQTT_CLIENT_ID, config.mqttClientId)
        putOrRemove(prefs, MQTT_TOPIC_PREFIX, config.mqttTopicPrefix)
        putOrRemove(prefs, TOKEN_EXPIRES_AT, config.tokenExpiresAt)
        prefs[API_BASE_URL] = config.apiBaseUrl
        prefs[MQTT_BROKER_URL] = config.mqttBrokerUrl
        prefs[ACTIVATION_STATE] = config.activationState.name
        prefs[SELECTED_DISPLAY_IDS] = config.selectedDisplayIds.joinToString(",")
        putOrRemove(prefs, LAST_SYNC_AT, config.lastSyncAt)
        putOrRemove(prefs, ZONE_ID, config.zoneId)
        putOrRemove(prefs, GROUP_ID, config.groupId)
        putOrRemove(prefs, ZONE_NAME, config.zoneName)
        putOrRemove(prefs, GROUP_NAME, config.groupName)
        putOrRemove(prefs, PENDING_ACTIVATION_CODE, config.pendingActivationCode)
        putOrRemove(prefs, PENDING_ACTIVATION_REQUESTED_AT, config.pendingActivationRequestedAt)
        prefs[LOCALE] = config.locale
        prefs[THEME] = config.theme
    }

    private fun <T> putOrRemove(
        prefs: MutablePreferences,
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) {
            prefs.remove(key)
        } else {
            prefs[key] = value
        }
    }
}

private suspend fun <T> Flow<T>.firstSync(): T {
    return this.first()
}
