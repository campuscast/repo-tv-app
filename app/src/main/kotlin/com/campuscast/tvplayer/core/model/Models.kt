package com.campuscast.tvplayer.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActivationState {
    @SerialName("unregistered")
    UNREGISTERED,

    @SerialName("pending")
    PENDING,

    @SerialName("activated")
    ACTIVATED,
}

@Serializable
data class AppConfig(
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceToken: String? = null,
    val mqttClientId: String? = null,
    val mqttTopicPrefix: String? = null,
    val tokenExpiresAt: String? = null,
    val apiBaseUrl: String = "http://localhost:3000/api/v1",
    val mqttBrokerUrl: String = "mqtt://localhost:1883",
    val activationState: ActivationState = ActivationState.UNREGISTERED,
    val selectedDisplayIds: List<String> = listOf("main"),
    val lastSyncAt: String? = null,
    val zoneId: String? = null,
    val groupId: String? = null,
    val zoneName: String? = null,
    val groupName: String? = null,
    val pendingActivationCode: String? = null,
    val pendingActivationRequestedAt: String? = null,
    val locale: String = "en",
    val theme: String = "dark",
)

@Serializable
data class ActivationCodeResponse(
    @SerialName("activation_code")
    val activationCode: String,
    @SerialName("expires_in")
    val expiresIn: Int,
)

@Serializable
data class DeviceCredentials(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_token")
    val deviceToken: String? = null,
    @SerialName("mqtt_client_id")
    val mqttClientId: String? = null,
    @SerialName("mqtt_topic_prefix")
    val mqttTopicPrefix: String? = null,
    @SerialName("token_expires_at")
    val tokenExpiresAt: String? = null,
    @SerialName("zone_name")
    val zoneName: String? = null,
    @SerialName("group_name")
    val groupName: String? = null,
)

@Serializable
data class DeviceInfo(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("zone_id")
    val zoneId: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("zone_name")
    val zoneName: String,
    @SerialName("group_name")
    val groupName: String,
)

@Serializable
data class Release(
    @SerialName("release_id")
    val releaseId: String,
    @SerialName("schedule_id")
    val scheduleId: String,
    @SerialName("version_number")
    val versionNumber: Int,
    @SerialName("zone_id")
    val zoneId: String,
    @SerialName("manifest_url")
    val manifestUrl: String,
    @SerialName("manifest_signature")
    val manifestSignature: String,
    @SerialName("manifest_key_id")
    val manifestKeyId: String,
    val status: String,
    @SerialName("published_at")
    val publishedAt: String,
)

@Serializable
data class SlotMetadata(
    @SerialName("transition_type")
    val transitionType: String? = null,
    @SerialName("transition_duration_ms")
    val transitionDurationMs: Int? = null,
    @SerialName("video_trim_in_ms")
    val videoTrimInMs: Int? = null,
    @SerialName("video_trim_out_ms")
    val videoTrimOutMs: Int? = null,
    @SerialName("video_mute")
    val videoMute: Boolean? = null,
    @SerialName("video_loop")
    val videoLoop: Boolean? = null,
)

@Serializable
data class ScheduleSlot(
    @SerialName("slot_id")
    val slotId: String,
    @SerialName("asset_id")
    val assetId: String? = null,
    @SerialName("publication_id")
    val publicationId: String? = null,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    val priority: Int = 0,
    @SerialName("zone_id")
    val zoneId: String = "",
    @SerialName("group_id")
    val groupId: String? = null,
    val metadata: SlotMetadata? = null,
)

@Serializable
data class ContentAsset(
    @SerialName("asset_id")
    val assetId: String,
    val filename: String,
    @SerialName("content_type")
    val contentType: String = "application/octet-stream",
    @SerialName("file_size")
    val fileSize: Long = 0,
    @SerialName("sha256_hash")
    val sha256Hash: String,
    @SerialName("download_url")
    val downloadUrl: String,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class PublicationItemTransition(
    val type: String? = null,
    @SerialName("duration_ms")
    val durationMs: Int? = null,
)

@Serializable
data class PublicationSlidePayload(
    val background: String? = null,
    val title: String? = null,
    val body: String? = null,
    @SerialName("image_asset_id")
    val imageAssetId: String? = null,
    @SerialName("logo_asset_id")
    val logoAssetId: String? = null,
    val layout: String? = null,
)

@Serializable
data class PublicationVideoPayload(
    @SerialName("asset_id")
    val assetId: String? = null,
    @SerialName("trim_in_ms")
    val trimInMs: Int? = null,
    @SerialName("trim_out_ms")
    val trimOutMs: Int? = null,
    val mute: Boolean? = null,
    val loop: Boolean? = null,
)

@Serializable
data class PublicationItem(
    @SerialName("item_id")
    val itemId: String? = null,
    val type: String,
    val title: String? = null,
    @SerialName("duration_ms")
    val durationMs: Int? = null,
    val transition: PublicationItemTransition? = null,
    val slide: PublicationSlidePayload? = null,
    val video: PublicationVideoPayload? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class Publication(
    @SerialName("publication_id")
    val publicationId: String,
    @SerialName("zone_id")
    val zoneId: String,
    val title: String,
    val type: String,
    val status: String,
    val version: Int,
    val items: List<PublicationItem> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class ReleaseManifest(
    @SerialName("release_id")
    val releaseId: String,
    @SerialName("schedule_id")
    val scheduleId: String,
    @SerialName("version_number")
    val versionNumber: Int,
    @SerialName("zone_id")
    val zoneId: String,
    val slots: List<ScheduleSlot>,
    val assets: List<ContentAsset>,
    val publications: List<Publication> = emptyList(),
    @SerialName("manifest_hash")
    val manifestHash: String = "",
    @SerialName("created_at")
    val createdAt: String,
)

enum class PlaybackStatus {
    IDLE,
    PLAYING,
    LOADING,
    ERROR,
    OFFLINE,
}

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentSlot: ScheduleSlot? = null,
    val currentAsset: ContentAsset? = null,
    val currentAssetLocalPath: String? = null,
    val currentPublication: Publication? = null,
    val currentPublicationItem: PublicationItem? = null,
    val nextSlot: ScheduleSlot? = null,
    val releaseId: String? = null,
    val errors: List<String> = emptyList(),
    val updatedAtIso: String = "",
)

data class CacheStatus(
    val currentReleaseId: String? = null,
    val totalAssets: Int = 0,
    val availableAssets: Int = 0,
    val missingAssets: Int = 0,
    val lastPrefetchAt: String? = null,
    val lastCleanupAt: String? = null,
    val lastError: String? = null,
)

enum class LinkState {
    CONNECTED,
    CONNECTING,
    NOT_INITIALIZED,
    DISCONNECTED,
}

data class ConnectionStatus(
    val backend: LinkState = LinkState.DISCONNECTED,
    val mqtt: LinkState = LinkState.NOT_INITIALIZED,
    val lastError: String? = null,
)

data class HeartbeatStatus(
    val running: Boolean = false,
    val intervalMs: Long = 30_000,
    val lastAttemptAt: String? = null,
    val lastSuccessAt: String? = null,
    val lastError: String? = null,
)

data class PlayerHealthSnapshot(
    val online: Boolean,
    val backendStatus: LinkState,
    val mqttStatus: LinkState,
    val currentReleaseId: String?,
    val playbackStatus: PlaybackStatus,
    val cache: CacheStatus,
    val heartbeat: HeartbeatStatus,
    val lastError: String?,
)

@Serializable
data class TelemetryDisplay(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class TelemetryPayload(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("current_release_id")
    val currentReleaseId: String?,
    @SerialName("playback_status")
    val playbackStatus: String,
    @SerialName("current_slot_id")
    val currentSlotId: String?,
    val errors: List<String>,
    val displays: List<TelemetryDisplay>,
    @SerialName("selected_displays")
    val selectedDisplays: List<String>,
    val timestamp: String,
    val online: Boolean,
    @SerialName("backend_status")
    val backendStatus: String,
    @SerialName("mqtt_status")
    val mqttStatus: String,
    val cache: TelemetryCache,
    @SerialName("last_error")
    val lastError: String?,
)

@Serializable
data class TelemetryCache(
    @SerialName("current_release_id")
    val currentReleaseId: String?,
    @SerialName("total_assets")
    val totalAssets: Int,
    @SerialName("available_assets")
    val availableAssets: Int,
    @SerialName("missing_assets")
    val missingAssets: Int,
    @SerialName("last_prefetch_at")
    val lastPrefetchAt: String?,
    @SerialName("last_cleanup_at")
    val lastCleanupAt: String?,
    @SerialName("last_error")
    val lastError: String?,
)

enum class DevicePresenceStatus {
    EXISTS,
    MISSING,
    UNKNOWN,
    UNREGISTERED,
}

data class ManifestApplyResult(
    val manifest: ReleaseManifest,
    val usedFallback: Boolean,
    val fallbackReason: String? = null,
)
