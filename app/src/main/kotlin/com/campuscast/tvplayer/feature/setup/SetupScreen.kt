package com.campuscast.tvplayer.feature.setup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuscast.tvplayer.core.i18n.I18n
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.util.normalizeDeviceIdInput

private const val SEGMENT_COUNT = 4
private const val SEGMENT_LENGTH = 4

@Composable
fun SetupScreen(
    config: AppConfig,
    locale: String,
    error: String?,
    onSubmit: (deviceId: String, apiBaseUrl: String, mqttBrokerUrl: String) -> Unit,
    onChangeLocale: (String) -> Unit = {},
) {
    val resolvedLocale = I18n.normalizeLocale(locale)
    val tr = { key: String, params: Map<String, String> -> I18n.t(resolvedLocale, key, params) }
    val t = { key: String -> I18n.t(resolvedLocale, key) }

    var deviceIdSegments by remember(config.deviceId) {
        mutableStateOf(splitDeviceId(config.deviceId))
    }
    var apiBaseUrl by remember(config.apiBaseUrl) { mutableStateOf(config.apiBaseUrl) }
    var mqttBrokerUrl by remember(config.mqttBrokerUrl) { mutableStateOf(config.mqttBrokerUrl) }
    val focusRequesters = remember {
        List(SEGMENT_COUNT) { FocusRequester() }
    }

    fun focusSegment(index: Int) {
        if (index < 0 || index >= SEGMENT_COUNT) return
        focusRequesters[index].requestFocus()
    }

    fun applySegmentsFromIndex(startIndex: Int, rawValue: String) {
        val normalized = normalizeDeviceIdInput(rawValue)
        if (normalized.isBlank()) return

        val next = deviceIdSegments.toMutableList()
        var cursor = startIndex
        var offset = 0
        while (cursor < SEGMENT_COUNT && offset < normalized.length) {
            val chunk = normalized.substring(offset, minOf(offset + SEGMENT_LENGTH, normalized.length))
            next[cursor] = chunk
            offset += SEGMENT_LENGTH
            cursor += 1
        }
        deviceIdSegments = next
        val consumedSegments = maxOf(1, (normalized.length + SEGMENT_LENGTH - 1) / SEGMENT_LENGTH)
        focusSegment(minOf(startIndex + consumedSegments - 1, SEGMENT_COUNT - 1))
    }

    fun applyFullDeviceId(rawValue: String): Boolean {
        val normalized = normalizeDeviceIdInput(rawValue)
        if (normalized.length != SEGMENT_COUNT * SEGMENT_LENGTH) return false
        deviceIdSegments = splitDeviceId(normalized)
        focusSegment(SEGMENT_COUNT - 1)
        return true
    }

    fun onSegmentChanged(index: Int, value: String) {
        val normalized = normalizeDeviceIdInput(value)
        if (applyFullDeviceId(normalized)) return

        if (normalized.length > SEGMENT_LENGTH) {
            val remainingCapacity = (SEGMENT_COUNT - index) * SEGMENT_LENGTH
            val startIndex = if (normalized.length > remainingCapacity) 0 else index
            applySegmentsFromIndex(startIndex, normalized)
            return
        }

        val next = deviceIdSegments.toMutableList()
        next[index] = normalized
        deviceIdSegments = next

        if (normalized.length == SEGMENT_LENGTH && index < SEGMENT_COUNT - 1) {
            focusSegment(index + 1)
        }
    }

    LaunchedEffect(Unit) {
        focusSegment(0)
    }

    val formattedDeviceId = remember(deviceIdSegments) { deviceIdSegments.joinToString("-") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${t("setup.language")}:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, end = 8.dp),
            )
            TextButton(
                onClick = { onChangeLocale("en") },
                enabled = resolvedLocale != "en",
            ) { Text(I18n.t(resolvedLocale, "settings.locale.en")) }
            TextButton(
                onClick = { onChangeLocale("ru") },
                enabled = resolvedLocale != "ru",
            ) { Text(I18n.t(resolvedLocale, "settings.locale.ru")) }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = t("setup.title"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    t("setup.subtitle"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(t("setup.playerId"), style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    deviceIdSegments.forEachIndexed { index, segment ->
                        OutlinedTextField(
                            value = segment,
                            onValueChange = { onSegmentChanged(index, it) },
                            modifier = Modifier
                                .width(150.dp)
                                .focusRequester(focusRequesters[index])
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (event.key) {
                                        Key.Backspace -> {
                                            if (deviceIdSegments[index].isBlank() && index > 0) {
                                                val next = deviceIdSegments.toMutableList()
                                                next[index - 1] = next[index - 1].dropLast(1)
                                                deviceIdSegments = next
                                                focusSegment(index - 1)
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        Key.DirectionLeft -> {
                                            if (index > 0) {
                                                focusSegment(index - 1)
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        Key.DirectionRight -> {
                                            if (index < SEGMENT_COUNT - 1) {
                                                focusSegment(index + 1)
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        else -> false
                                    }
                                },
                            singleLine = true,
                            placeholder = { Text("ABCD") },
                            label = {
                                Text(
                                    tr(
                                        "setup.segment",
                                        mapOf("index" to (index + 1).toString()),
                                    ),
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                            ),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                            ),
                        )
                        if (index < SEGMENT_COUNT - 1) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = tr("setup.formatted", mapOf("value" to formattedDeviceId)),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("setup.apiBaseUrl")) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = mqttBrokerUrl,
                    onValueChange = { mqttBrokerUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t("setup.mqttBrokerUrl")) },
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
                        onClick = { onSubmit(formattedDeviceId, apiBaseUrl, mqttBrokerUrl) },
                    ) {
                        Text(t("setup.continue"))
                    }
                }
            }
        }
    }
}

private fun splitDeviceId(deviceId: String?): List<String> {
    val normalized = normalizeDeviceIdInput(deviceId.orEmpty())
    return List(SEGMENT_COUNT) { index ->
        val start = index * SEGMENT_LENGTH
        normalized.substring(start, minOf(start + SEGMENT_LENGTH, normalized.length))
    }
}
