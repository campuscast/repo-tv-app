package com.campuscast.tvplayer.feature.setup

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuscast.tvplayer.core.i18n.I18n
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.util.isValidDeviceId
import com.campuscast.tvplayer.util.normalizeDeviceIdInput

private const val SEGMENT_COUNT = 4
private const val SEGMENT_LENGTH = 4
private const val IP_SEGMENT_COUNT = 4

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
    val scrollState = rememberScrollState()

    var deviceIdSegments by remember(config.deviceId) {
        mutableStateOf(splitDeviceId(config.deviceId))
    }
    var showAdvanced by remember(config.apiBaseUrl, config.mqttBrokerUrl) {
        mutableStateOf(
            config.apiBaseUrl != AppConfig().apiBaseUrl || config.mqttBrokerUrl != AppConfig().mqttBrokerUrl,
        )
    }
    var localError by remember { mutableStateOf<String?>(null) }

    var apiEndpoint by remember(config.apiBaseUrl) {
        mutableStateOf(parseEndpointConfig(config.apiBaseUrl, "http", "3000", "/api/v1"))
    }
    var mqttEndpoint by remember(config.mqttBrokerUrl) {
        mutableStateOf(parseEndpointConfig(config.mqttBrokerUrl, "mqtt", "1883", ""))
    }

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
    val apiBaseUrl = remember(apiEndpoint) { apiEndpoint.toUrl() }
    val mqttBrokerUrl = remember(mqttEndpoint) { mqttEndpoint.toUrl() }

    fun submit() {
        localError = when {
            !isValidDeviceId(formattedDeviceId) -> t("setup.errorFormat")
            !apiEndpoint.isValid() -> t("setup.errorApi")
            !mqttEndpoint.isValid() -> t("setup.errorMqtt")
            else -> null
        }
        if (localError == null) {
            onSubmit(formattedDeviceId, apiBaseUrl, mqttBrokerUrl)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 48.dp, vertical = 30.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = t("setup.quickStart"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = t("setup.quickStartDesc"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(18.dp))
                        EndpointEditor(
                            title = t("setup.apiBaseUrl"),
                            endpoint = apiEndpoint,
                            onChange = { apiEndpoint = it },
                            previewLabel = t("setup.urlPreview"),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        EndpointEditor(
                            title = t("setup.mqttBrokerUrl"),
                            endpoint = mqttEndpoint,
                            onChange = { mqttEndpoint = it },
                            previewLabel = t("setup.urlPreview"),
                        )
                    }

                    val effectiveError = localError ?: error
                    if (!effectiveError.isNullOrBlank()) {
                        Text(
                            text = effectiveError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.78f)
                .padding(bottom = 20.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (showAdvanced) t("setup.hideAdvanced") else t("setup.showAdvanced"),
                    )
                }
                Button(
                    onClick = ::submit,
                    modifier = Modifier.weight(1.2f),
                ) {
                    Text(t("setup.continue"))
                }
            }
        }
    }
}

@Composable
private fun EndpointEditor(
    title: String,
    endpoint: EndpointConfig,
    onChange: (EndpointConfig) -> Unit,
    previewLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            endpoint.ipSegments.forEachIndexed { index, segment ->
                OutlinedTextField(
                    value = segment,
                    onValueChange = { value ->
                        val next = endpoint.ipSegments.toMutableList()
                        next[index] = normalizeIpSegment(value)
                        onChange(endpoint.copy(ipSegments = next))
                    },
                    modifier = Modifier.width(92.dp),
                    singleLine = true,
                    label = { Text("${index + 1}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                )
                if (index < IP_SEGMENT_COUNT - 1) {
                    Text(".", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Text(":", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = endpoint.port,
                onValueChange = { onChange(endpoint.copy(port = normalizePort(it))) },
                modifier = Modifier.width(132.dp),
                singleLine = true,
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }

        if (endpoint.path.isNotBlank()) {
            OutlinedTextField(
                value = endpoint.path,
                onValueChange = { onChange(endpoint.copy(path = normalizePath(it))) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Path") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }

        Text(
            text = "$previewLabel: ${endpoint.toUrl()}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class EndpointConfig(
    val scheme: String,
    val ipSegments: List<String>,
    val port: String,
    val path: String = "",
) {
    fun toUrl(): String {
        val host = ipSegments.joinToString(".") { it.ifBlank { "0" } }
        val suffix = when {
            path.isBlank() -> ""
            path.startsWith("/") -> path
            else -> "/$path"
        }
        return "$scheme://$host:$port$suffix"
    }

    fun isValid(): Boolean {
        return ipSegments.size == IP_SEGMENT_COUNT &&
            ipSegments.all { segment ->
                segment.isNotBlank() && segment.toIntOrNull() != null && segment.toInt() in 0..255
            } &&
            port.toIntOrNull() != null &&
            port.toInt() in 1..65535
    }
}

private fun parseEndpointConfig(
    rawUrl: String,
    defaultScheme: String,
    defaultPort: String,
    defaultPath: String,
): EndpointConfig {
    val parsed = Uri.parse(rawUrl)
    val host = parsed.host
    val hostSegments = host
        ?.split('.')
        ?.takeIf { it.size == IP_SEGMENT_COUNT && it.all { part -> part.toIntOrNull() != null } }
        ?: listOf("192", "168", "0", "1")

    return EndpointConfig(
        scheme = parsed.scheme ?: defaultScheme,
        ipSegments = hostSegments,
        port = (if (parsed.port > 0) parsed.port.toString() else defaultPort),
        path = parsed.path?.takeIf { it.isNotBlank() } ?: defaultPath,
    )
}

private fun splitDeviceId(deviceId: String?): List<String> {
    val normalized = normalizeDeviceIdInput(deviceId.orEmpty())
    return List(SEGMENT_COUNT) { index ->
        val start = index * SEGMENT_LENGTH
        if (start >= normalized.length) {
            ""
        } else {
            normalized.substring(start, minOf(start + SEGMENT_LENGTH, normalized.length))
        }
    }
}

private fun normalizeIpSegment(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(3)
    val numeric = digits.toIntOrNull() ?: return digits
    return minOf(numeric, 255).toString()
}

private fun normalizePort(raw: String): String {
    return raw.filter(Char::isDigit).take(5)
}

private fun normalizePath(raw: String): String {
    if (raw.isBlank()) return ""
    return if (raw.startsWith("/")) raw else "/$raw"
}
