package com.campuscast.tvplayer.feature.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campuscast.tvplayer.core.i18n.I18n
import com.campuscast.tvplayer.core.model.AppConfig
import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ConnectionStatus
import com.campuscast.tvplayer.core.model.CrashLogInfo
import com.campuscast.tvplayer.core.model.HeartbeatStatus
import com.campuscast.tvplayer.core.model.LinkState
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.util.formatLocalDateTime

@Composable
fun StatusScreen(
    locale: String,
    config: AppConfig,
    connection: ConnectionStatus,
    playback: PlaybackState,
    cache: CacheStatus,
    heartbeat: HeartbeatStatus,
    recentErrors: List<String>,
    latestCrash: CrashLogInfo?,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToPlayback: () -> Unit,
) {
    val resolvedLocale = I18n.normalizeLocale(locale)
    val t = { key: String -> I18n.t(resolvedLocale, key) }
    val effectiveConnection = when {
        connection.backend == LinkState.CONNECTED || connection.mqtt == LinkState.CONNECTED -> LinkState.CONNECTED
        connection.backend == LinkState.CONNECTING || connection.mqtt == LinkState.CONNECTING -> LinkState.CONNECTING
        else -> LinkState.DISCONNECTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(t("playback.idleSubtitle"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    t("status.serviceStatus"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            ConnectionPill(locale = resolvedLocale, state = effectiveConnection)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactStatusCard(
                title = t("diagnostics.connection"),
                value = stateLabel(resolvedLocale, effectiveConnection),
                secondary = "${t("status.backendMqtt")}: ${stateLabel(resolvedLocale, connection.backend)} / ${stateLabel(resolvedLocale, connection.mqtt)}",
                modifier = Modifier.weight(1f),
            )
            CompactStatusCard(
                title = t("diagnostics.playback"),
                value = playbackStatusLabel(resolvedLocale, playback.status),
                secondary = playback.releaseId ?: t("status.playback.idle"),
                modifier = Modifier.weight(1f),
            )
            CompactStatusCard(
                title = t("diagnostics.displays"),
                value = "${config.selectedDisplayIds.size} / ${config.selectedDisplayIds.size} ${t("diagnostics.selected")}",
                secondary = t("status.tvRuntime"),
                modifier = Modifier.weight(1f),
            )
        }

        StatusCard(title = t("diagnostics.deviceInfo")) {
            ValueRow(t("diagnostics.deviceId"), config.deviceId ?: "-")
            ValueRow(t("diagnostics.zone"), config.zoneName ?: config.zoneId ?: "-")
            ValueRow(t("diagnostics.group"), config.groupName ?: config.groupId ?: "-")
            ValueRow(t("diagnostics.lastSync"), formatLocalDateTime(config.lastSyncAt))
            ValueRow(t("diagnostics.apiUrl"), config.apiBaseUrl)
        }

        StatusCard(
            title = t("status.runtimeHealth"),
            subtitle = t("status.runtimeHealthDesc"),
        ) {
            ValueRow(
                t("status.online"),
                if (effectiveConnection == LinkState.DISCONNECTED) {
                    t("connection.disconnected")
                } else {
                    t("connection.connected")
                },
            )
            ValueRow(
                t("status.backendMqtt"),
                "${stateLabel(resolvedLocale, connection.backend)} / ${
                    stateLabel(
                        resolvedLocale,
                        connection.mqtt,
                    )
                }",
            )
            ValueRow(t("status.currentRelease"), cache.currentReleaseId ?: playback.releaseId ?: "-")
            ValueRow(t("status.playbackStatus"), playbackStatusLabel(resolvedLocale, playback.status))
            ValueRow(t("status.cacheAssets"), "${cache.availableAssets}/${cache.totalAssets}")
            ValueRow(t("status.cacheMissing"), cache.missingAssets.toString())
            ValueRow(t("status.heartbeatSuccess"), formatLocalDateTime(heartbeat.lastSuccessAt))
            ValueRow(t("status.heartbeatAttempt"), formatLocalDateTime(heartbeat.lastAttemptAt))
            ValueRow(t("status.lastError"), connection.lastError ?: cache.lastError ?: recentErrors.lastOrNull() ?: "-")
        }

        StatusCard(title = t("status.cache")) {
            ValueRow(t("status.currentRelease"), cache.currentReleaseId ?: "-")
            ValueRow(t("status.assets"), "${cache.availableAssets}/${cache.totalAssets}")
            ValueRow(t("status.missing"), cache.missingAssets.toString())
            ValueRow(t("status.lastPrefetch"), formatLocalDateTime(cache.lastPrefetchAt))
            ValueRow(t("status.lastCleanup"), formatLocalDateTime(cache.lastCleanupAt))
            ValueRow(t("status.lastCacheError"), cache.lastError ?: "-")
        }

        if (latestCrash != null) {
            StatusCard(title = t("diagnostics.lastCrash")) {
                ValueRow(t("diagnostics.lastCrashAt"), formatLocalDateTime(latestCrash.capturedAtIso))
                ValueRow(t("diagnostics.lastCrashFile"), latestCrash.filePath)
                Text(
                    text = latestCrash.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = latestCrash.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (recentErrors.isNotEmpty()) {
            StatusCard(title = t("diagnostics.recentErrors")) {
                recentErrors.takeLast(10).reversed().forEach { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onSyncNow, modifier = Modifier.weight(1f)) {
                Text(if (isSyncing) t("common.loading") else t("diagnostics.syncNow"))
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text(t("settings.title"))
            }
            OutlinedButton(onClick = onBackToPlayback, modifier = Modifier.weight(1f)) {
                Text(t("status.backToPlayback"))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun CompactStatusCard(
    title: String,
    value: String,
    secondary: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ConnectionPill(locale: String, state: LinkState) {
    Surface(
        color = when (state) {
            LinkState.CONNECTED -> MaterialTheme.colorScheme.tertiary
            LinkState.CONNECTING -> MaterialTheme.colorScheme.primary
            LinkState.NOT_INITIALIZED -> MaterialTheme.colorScheme.secondary
            LinkState.DISCONNECTED -> MaterialTheme.colorScheme.error
        },
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = stateLabel(locale, state),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.34f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.66f),
            fontFamily = if (value.length > 24) FontFamily.Monospace else null,
        )
    }
}

private fun stateLabel(locale: String, state: LinkState): String = when (state) {
    LinkState.CONNECTED -> I18n.t(locale, "connection.connected")
    LinkState.CONNECTING -> I18n.t(locale, "connection.connecting")
    LinkState.NOT_INITIALIZED -> I18n.t(locale, "connection.not_initialized")
    LinkState.DISCONNECTED -> I18n.t(locale, "connection.disconnected")
}

private fun playbackStatusLabel(locale: String, status: com.campuscast.tvplayer.core.model.PlaybackStatus): String =
    when (status) {
        com.campuscast.tvplayer.core.model.PlaybackStatus.IDLE -> I18n.t(locale, "status.playback.idle")
        com.campuscast.tvplayer.core.model.PlaybackStatus.PLAYING -> I18n.t(locale, "status.playback.playing")
        com.campuscast.tvplayer.core.model.PlaybackStatus.LOADING -> I18n.t(locale, "status.playback.loading")
        com.campuscast.tvplayer.core.model.PlaybackStatus.ERROR -> I18n.t(locale, "status.playback.error")
        com.campuscast.tvplayer.core.model.PlaybackStatus.OFFLINE -> I18n.t(locale, "status.playback.offline")
    }
