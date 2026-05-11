package com.campuscast.tvplayer.core.storage

import android.content.Context
import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.playback.FallbackSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ManifestStore(
    context: Context,
    private val json: Json = appJson,
) {
    private val rootDir = File(context.filesDir, "player-data").apply { mkdirs() }
    private val currentManifestFile = File(rootDir, "current-manifest.json")
    private val previousManifestFile = File(rootDir, "previous-manifest.json")
    private val cacheStatusFile = File(rootDir, "cache-status.json")

    suspend fun saveCurrentManifest(manifest: ReleaseManifest) = withContext(Dispatchers.IO) {
        if (currentManifestFile.exists()) {
            currentManifestFile.copyTo(previousManifestFile, overwrite = true)
        }
        currentManifestFile.writeText(json.encodeToString(manifest))
    }

    suspend fun clearManifests() = withContext(Dispatchers.IO) {
        if (currentManifestFile.exists()) {
            currentManifestFile.delete()
        }
        if (previousManifestFile.exists()) {
            previousManifestFile.delete()
        }
    }

    suspend fun getCurrentManifest(): ReleaseManifest? = withContext(Dispatchers.IO) {
        readManifest(currentManifestFile)
    }

    suspend fun getPreviousManifest(): ReleaseManifest? = withContext(Dispatchers.IO) {
        readManifest(previousManifestFile)
    }

    suspend fun getLastKnownGoodManifest(): ReleaseManifest? {
        val current = getCurrentManifest()
        val previous = getPreviousManifest()
        return FallbackSelector.chooseLastKnownGood(current, previous)
    }

    suspend fun getCacheStatus(): CacheStatus = withContext(Dispatchers.IO) {
        if (!cacheStatusFile.exists()) {
            return@withContext CacheStatus()
        }
        runCatching {
            json.decodeFromString<CacheStatusDto>(cacheStatusFile.readText()).toDomain()
        }.getOrElse { CacheStatus() }
    }

    suspend fun saveCacheStatus(status: CacheStatus) = withContext(Dispatchers.IO) {
        cacheStatusFile.writeText(json.encodeToString(CacheStatusDto.fromDomain(status)))
    }

    fun contentDir(): File {
        return File(rootDir, "content").apply { mkdirs() }
    }

    private fun readManifest(file: File): ReleaseManifest? {
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<ReleaseManifest>(file.readText())
        }.getOrNull()
    }
}

@kotlinx.serialization.Serializable
private data class CacheStatusDto(
    @kotlinx.serialization.SerialName("current_release_id")
    val currentReleaseId: String? = null,
    @kotlinx.serialization.SerialName("total_assets")
    val totalAssets: Int = 0,
    @kotlinx.serialization.SerialName("available_assets")
    val availableAssets: Int = 0,
    @kotlinx.serialization.SerialName("missing_assets")
    val missingAssets: Int = 0,
    @kotlinx.serialization.SerialName("last_prefetch_at")
    val lastPrefetchAt: String? = null,
    @kotlinx.serialization.SerialName("last_cleanup_at")
    val lastCleanupAt: String? = null,
    @kotlinx.serialization.SerialName("last_error")
    val lastError: String? = null,
) {
    fun toDomain(): CacheStatus = CacheStatus(
        currentReleaseId = currentReleaseId,
        totalAssets = totalAssets,
        availableAssets = availableAssets,
        missingAssets = missingAssets,
        lastPrefetchAt = lastPrefetchAt,
        lastCleanupAt = lastCleanupAt,
        lastError = lastError,
    )

    companion object {
        fun fromDomain(value: CacheStatus): CacheStatusDto = CacheStatusDto(
            currentReleaseId = value.currentReleaseId,
            totalAssets = value.totalAssets,
            availableAssets = value.availableAssets,
            missingAssets = value.missingAssets,
            lastPrefetchAt = value.lastPrefetchAt,
            lastCleanupAt = value.lastCleanupAt,
            lastError = value.lastError,
        )
    }
}
