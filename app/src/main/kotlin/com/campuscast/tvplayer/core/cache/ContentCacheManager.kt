package com.campuscast.tvplayer.core.cache

import com.campuscast.tvplayer.core.model.CacheStatus
import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.storage.ManifestStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ContentCacheManager(
    private val httpClient: OkHttpClient,
    private val manifestStore: ManifestStore,
) {
    suspend fun prefetchManifestAssets(
        manifest: ReleaseManifest,
        deviceToken: String,
    ): PrefetchResult {
        val failed = mutableListOf<String>()
        var available = 0
        var downloaded = 0

        manifest.assets.forEach { asset ->
            val cachedPath = localPath(asset)
            if (cachedPath.exists()) {
                available += 1
                return@forEach
            }

            runCatching {
                download(asset, deviceToken)
            }.onSuccess {
                available += 1
                downloaded += 1
            }.onFailure { err ->
                failed += "${asset.assetId}: ${err.message ?: "download failed"}"
            }
        }

        return PrefetchResult(
            total = manifest.assets.size,
            available = available,
            downloaded = downloaded,
            failed = failed,
        )
    }

    suspend fun verifyManifestAssets(manifest: ReleaseManifest): VerificationResult {
        return withContext(Dispatchers.IO) {
            val missing = manifest.assets.filterNot { localPath(it).exists() }.map { it.assetId }
            VerificationResult(
                total = manifest.assets.size,
                available = manifest.assets.size - missing.size,
                missing = missing.size,
                missingAssetIds = missing,
            )
        }
    }

    suspend fun cleanupUnusedAssets(manifest: ReleaseManifest): Int {
        return withContext(Dispatchers.IO) {
            val keepFiles = manifest.assets.map { localPath(it).name }.toSet()
            val contentDir = manifestStore.contentDir()
            val existingFiles = contentDir.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?.toSet()
                ?: emptySet()
            val decision = CachePolicy.buildCleanupDecision(
                existingFiles = existingFiles,
                expectedFiles = keepFiles,
                missingAssets = 0,
            )
            var removed = 0
            contentDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                if (file.name !in decision.removeFiles) return@forEach
                if (file.delete()) {
                    removed += 1
                }
            }
            removed
        }
    }

    suspend fun download(asset: ContentAsset, deviceToken: String): File {
        return withContext(Dispatchers.IO) {
            val target = localPath(asset)
            if (target.exists()) return@withContext target

            val request = Request.Builder()
                .url(asset.downloadUrl)
                .addHeader("Authorization", "Bearer $deviceToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Download failed: HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("Empty download body")
                target.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
                return@withContext target
            }
        }
    }

    suspend fun getLocalPath(asset: ContentAsset): String? {
        return withContext(Dispatchers.IO) {
            val file = localPath(asset)
            if (file.exists()) file.absolutePath else null
        }
    }

    fun localPath(asset: ContentAsset): File {
        val ext = mimeToExt(asset.contentType, asset.downloadUrl)
        return File(manifestStore.contentDir(), "${asset.assetId}$ext")
    }

    suspend fun cacheStatusFor(
        manifest: ReleaseManifest,
        nowIso: String,
        lastError: String? = null,
    ): CacheStatus {
        val verify = verifyManifestAssets(manifest)
        return CacheStatus(
            currentReleaseId = manifest.releaseId,
            totalAssets = verify.total,
            availableAssets = verify.available,
            missingAssets = verify.missing,
            lastPrefetchAt = nowIso,
            lastCleanupAt = if (verify.missing == 0) nowIso else null,
            lastError = lastError ?: if (verify.missing > 0) "Missing ${verify.missing}/${verify.total} assets" else null,
        )
    }

    private fun mimeToExt(mime: String, fallbackUrl: String): String {
        val map = mapOf(
            "image/jpeg" to ".jpg",
            "image/png" to ".png",
            "image/gif" to ".gif",
            "image/webp" to ".webp",
            "image/svg+xml" to ".svg",
            "video/mp4" to ".mp4",
            "video/webm" to ".webm",
            "video/ogg" to ".ogv",
            "text/html" to ".html",
            "application/pdf" to ".pdf",
        )
        map[mime]?.let { return it }
        val path = runCatching { java.net.URI(fallbackUrl).path.orEmpty() }.getOrDefault("")
        val ext = path.substringAfterLast('.', missingDelimiterValue = "")
        return if (ext.isBlank()) ".bin" else ".$ext"
    }
}

data class PrefetchResult(
    val total: Int,
    val available: Int,
    val downloaded: Int,
    val failed: List<String>,
)

data class VerificationResult(
    val total: Int,
    val available: Int,
    val missing: Int,
    val missingAssetIds: List<String>,
)
