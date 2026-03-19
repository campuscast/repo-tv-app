package com.campuscast.tvplayer.feature.setup

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.util.formatDeviceId

@Composable
fun SetupScreen(
    config: AppConfig,
    error: String?,
    onSubmit: (deviceId: String, apiBaseUrl: String, mqttBrokerUrl: String) -> Unit,
) {
    var deviceId by remember(config.deviceId) { mutableStateOf(config.deviceId.orEmpty()) }
    var apiBaseUrl by remember(config.apiBaseUrl) { mutableStateOf(config.apiBaseUrl) }
    var mqttBrokerUrl by remember(config.mqttBrokerUrl) { mutableStateOf(config.mqttBrokerUrl) }
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(18.dp),
                )
                .focusable(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text("Player Setup", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Enter Player ID and backend URLs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(modifier = Modifier.height(22.dp))

                OutlinedTextField(
                    value = deviceId,
                    onValueChange = {
                        deviceId = formatDeviceId(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Player ID") },
                    placeholder = { Text("ABCD-EF12-3456-7890") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Base URL") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = mqttBrokerUrl,
                    onValueChange = { mqttBrokerUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("MQTT Broker URL") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { onSubmit(deviceId, apiBaseUrl, mqttBrokerUrl) },
                    ) {
                        Text("Continue to Activation")
                    }
                }
            }
        }
    }
}
