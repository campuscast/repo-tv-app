package com.campuscast.tvplayer.core.playback

import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.PlaybackState
import com.campuscast.tvplayer.core.model.PlaybackStatus
import com.campuscast.tvplayer.core.model.Publication
import com.campuscast.tvplayer.core.model.PublicationItem
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.ScheduleSlot
import com.campuscast.tvplayer.util.isWithinSchedule
import com.campuscast.tvplayer.util.nowIso
import com.campuscast.tvplayer.util.parseInstant
import java.time.Instant

class PlaybackEvaluator {
    fun evaluate(
        manifest: ReleaseManifest?,
        assetLocalPathLookup: (String) -> String?,
        now: Instant = Instant.now(),
    ): PlaybackState {
        if (manifest == null || manifest.slots.isEmpty()) {
            return PlaybackState(
                status = PlaybackStatus.IDLE,
                releaseId = manifest?.releaseId,
                updatedAtIso = nowIso(),
            )
        }

        val slots = manifest.slots.sortedByDescending { it.priority }
        val activeSlot = slots.firstOrNull { isWithinSchedule(it.startTime, it.endTime, now) }
        val nextSlot = slots
            .filter { slot -> parseInstant(slot.startTime)?.isAfter(now) == true }
            .minByOrNull { parseInstant(it.startTime) ?: Instant.MAX }

        if (activeSlot == null) {
            return PlaybackState(
                status = PlaybackStatus.IDLE,
                releaseId = manifest.releaseId,
                nextSlot = nextSlot,
                updatedAtIso = nowIso(),
            )
        }

        val publication = activeSlot.publicationId?.let { publicationId ->
            manifest.publications.firstOrNull { it.publicationId == publicationId }
        }
        val publicationItem = publication?.let { pickPublicationItem(activeSlot, it, now) }

        val slotAsset = activeSlot.assetId?.let { assetId ->
            manifest.assets.firstOrNull { it.assetId == assetId }
        }

        val publicationAsset = when (publicationItem?.type) {
            "video_asset" -> publicationItem.video?.assetId?.let { videoAssetId ->
                manifest.assets.firstOrNull { it.assetId == videoAssetId }
            }

            "custom_slide" -> publicationItem.slide?.imageAssetId?.let { imageAssetId ->
                manifest.assets.firstOrNull { it.assetId == imageAssetId }
            }

            else -> null
        }

        val selectedAsset = publicationAsset ?: slotAsset
        val localPath = selectedAsset?.let { assetLocalPathLookup(it.assetId) }

        return PlaybackState(
            status = if (selectedAsset == null || !localPath.isNullOrBlank()) {
                PlaybackStatus.PLAYING
            } else {
                PlaybackStatus.LOADING
            },
            currentSlot = activeSlot,
            currentAsset = selectedAsset,
            currentAssetLocalPath = localPath,
            currentPublication = publication,
            currentPublicationItem = publicationItem,
            nextSlot = nextSlot,
            releaseId = manifest.releaseId,
            updatedAtIso = nowIso(),
        )
    }

    private fun pickPublicationItem(
        slot: ScheduleSlot,
        publication: Publication,
        now: Instant,
    ): PublicationItem? {
        if (publication.items.isEmpty()) return null

        val start = parseInstant(slot.startTime) ?: now
        val elapsedMs = maxOf(0, now.toEpochMilli() - start.toEpochMilli())
        val totalDuration = publication.items.sumOf { itemDurationMs(it).toLong() }
        if (totalDuration <= 0) {
            return publication.items.firstOrNull()
        }

        var cursor = elapsedMs % totalDuration
        publication.items.forEach { item ->
            val duration = itemDurationMs(item)
            if (cursor < duration) {
                return item
            }
            cursor -= duration
        }

        return publication.items.firstOrNull()
    }

    private fun itemDurationMs(item: PublicationItem): Int {
        val duration = item.durationMs ?: 0
        return if (duration > 0) duration else 10_000
    }
}
