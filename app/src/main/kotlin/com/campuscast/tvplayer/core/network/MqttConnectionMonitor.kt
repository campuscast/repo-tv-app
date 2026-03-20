package com.campuscast.tvplayer.core.network

import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.core.model.LinkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.net.URI

data class MqttConnectionSnapshot(
    val state: LinkState = LinkState.NOT_INITIALIZED,
    val lastError: String? = null,
)

class MqttConnectionMonitor {
    private val _status = MutableStateFlow(MqttConnectionSnapshot())
    val status: StateFlow<MqttConnectionSnapshot> = _status.asStateFlow()

    private var client: MqttAsyncClient? = null

    @Synchronized
    fun start(config: AppConfig) {
        val brokerUrl = config.mqttBrokerUrl.trim()
        val clientId = config.mqttClientId?.trim().orEmpty()
        if (brokerUrl.isBlank() || clientId.isBlank()) {
            stopInternal(LinkState.NOT_INITIALIZED)
            return
        }

        val serverUri = normalizeBrokerUri(brokerUrl)
        if (serverUri == null) {
            stopInternal(
                state = LinkState.DISCONNECTED,
                error = "Invalid MQTT broker URL: $brokerUrl",
            )
            return
        }

        val existing = client
        if (existing != null && existing.serverURI == serverUri && existing.clientId == clientId) {
            if (existing.isConnected) {
                _status.value = MqttConnectionSnapshot(LinkState.CONNECTED)
            } else {
                _status.value = MqttConnectionSnapshot(LinkState.CONNECTING)
            }
            return
        }

        stopInternal(LinkState.NOT_INITIALIZED)

        val nextClient = MqttAsyncClient(serverUri, clientId)
        client = nextClient
        _status.value = MqttConnectionSnapshot(LinkState.CONNECTING)

        nextClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                _status.value = MqttConnectionSnapshot(LinkState.CONNECTED)
            }

            override fun connectionLost(cause: Throwable?) {
                _status.value = MqttConnectionSnapshot(
                    state = LinkState.DISCONNECTED,
                    lastError = cause?.message ?: "MQTT connection lost",
                )
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) = Unit

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 30
        }

        nextClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                _status.value = MqttConnectionSnapshot(LinkState.CONNECTED)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                _status.value = MqttConnectionSnapshot(
                    state = LinkState.DISCONNECTED,
                    lastError = exception?.message ?: "MQTT connect failed",
                )
            }
        })
    }

    @Synchronized
    fun stop() {
        stopInternal(LinkState.NOT_INITIALIZED)
    }

    @Synchronized
    private fun stopInternal(state: LinkState, error: String? = null) {
        val existing = client
        client = null
        if (existing != null) {
            runCatching {
                if (existing.isConnected) {
                    existing.disconnect().waitForCompletion(3_000)
                }
            }
            runCatching { existing.close() }
        }
        _status.value = MqttConnectionSnapshot(state = state, lastError = error)
    }

    private fun normalizeBrokerUri(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else when (uri.scheme?.lowercase()) {
            "mqtt", "tcp" -> 1883
            "mqtts", "ssl" -> 8883
            else -> 1883
        }
        val scheme = when (uri.scheme?.lowercase()) {
            "mqtt", "tcp" -> "tcp"
            "mqtts", "ssl" -> "ssl"
            else -> return null
        }
        return "$scheme://$host:$port"
    }
}
