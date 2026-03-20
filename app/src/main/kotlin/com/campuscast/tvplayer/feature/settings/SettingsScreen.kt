package com.campuscast.tvplayer.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.campuscast.tvplayer.core.i18n.I18n
import com.campuscast.tvplayer.core.model.AppConfig

@Composable
fun SettingsScreen(
    locale: String,
    config: AppConfig,
    onSave: (apiBaseUrl: String, mqttBrokerUrl: String) -> Unit,
    onChangeLocale: (String) -> Unit,
    onResetPlayer: () -> Unit,
    onBack: () -> Unit,
) {
    val resolvedLocale = I18n.normalizeLocale(locale)
    val t = { key: String -> I18n.t(resolvedLocale, key) }

    var apiBaseUrl by remember(config.apiBaseUrl) { mutableStateOf(config.apiBaseUrl) }
    var mqttBrokerUrl by remember(config.mqttBrokerUrl) { mutableStateOf(config.mqttBrokerUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(t("playback.idleSubtitle"), style = MaterialTheme.typography.headlineSmall)
        Text(
            t("settings.title"),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(t("settings.language"), style = MaterialTheme.typography.titleLarge)
                Text(
                    t("settings.languageDesc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onChangeLocale("en") },
                        enabled = resolvedLocale != "en",
                    ) {
                        Text(I18n.t(resolvedLocale, "settings.locale.en"))
                    }
                    OutlinedButton(
                        onClick = { onChangeLocale("ru") },
                        enabled = resolvedLocale != "ru",
                    ) {
                        Text(I18n.t(resolvedLocale, "settings.locale.ru"))
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(t("settings.connection"), style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("settings.apiBaseUrl")) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
                OutlinedTextField(
                    value = mqttBrokerUrl,
                    onValueChange = { mqttBrokerUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("settings.mqttBrokerUrl")) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(t("settings.playerIdentity"), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${t("diagnostics.deviceId")}: ${config.deviceId ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text =
                        "${t("settings.activationState")}: ${
                            t("settings.activation.${config.activationState.name.lowercase()}")
                        }",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onSave(apiBaseUrl, mqttBrokerUrl) },
                modifier = Modifier.weight(1f),
            ) {
                Text(t("settings.save"))
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text(t("settings.back"))
            }
            OutlinedButton(
                onClick = onResetPlayer,
                modifier = Modifier.weight(1f),
            ) {
                Text(t("settings.resetActivation"))
            }
        }
    }
}
