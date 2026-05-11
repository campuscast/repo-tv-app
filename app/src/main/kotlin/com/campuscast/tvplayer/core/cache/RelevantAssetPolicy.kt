package com.campuscast.tvplayer.core.cache

import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.ScheduleSlot
import com.campuscast.tvplayer.util.parseInstant
import java.time.Instant

const val PREFETCH_LOOKAHEAD_MS: Long = 5 * 60_000
const val NORMAL_SYNC_INTERVAL_MS: Long = 5 * 60_000
const val NEAR_SLOT_SYNC_INTERVAL_MS: Long = 30_000
const val RECOVERY_SYNC_INTERVAL_MS: Long = 60_000

data class RelevantAssetSelection(
    val slots: List<ScheduleSlot> = emptyList(),
    val assets: List<ContentAsset> = emptyList(),
    val nextSlot: ScheduleSlot? = null,
)

object RelevantAssetPolicy {
    fun selectAssets(
        manifest: ReleaseManifest?,
        now: Instant = Instant.now(),
        lookAheadMs: Long = PREFETCH_LOOKAHEAD_MS,
    ): RelevantAssetSelection {
        if (manifest == null || manifest.slots.isEmpty()) {
            return RelevantAssetSelection()
        }

        val windowEnd = now.plusMillis(lookAheadMs)
        val slotsByStart = manifest.slots.sortedBy { parseInstant(it.startTime) ?: Instant.MAX }
        val nextSlot = slotsByStart.firstOrNull { slot ->
            val start = parseInstant(slot.startTime)
            val end = parseInstant(slot.endTime)
            start != null && end != null && start.isAfter(now) && end.isAfter(now)
        }

        val selectedSlots = linkedMapOf<String, ScheduleSlot>()
        slotsByStart.forEach { slot ->
            val start = parseInstant(slot.startTime) ?: return@forEach
            val end = parseInstant(slot.endTime) ?: return@forEach
            if (!end.isAfter(now)) return@forEach

            val isActive = !start.isAfter(now)
            val isWithinLookAhead = start.isAfter(now) && !start.isAfter(windowEnd)
            if (!isActive && !isWithinLookAhead) return@forEach

            selectedSlots.putIfAbsent(slot.slotId, slot)
        }

        if (selectedSlots.isEmpty() && nextSlot != null) {
            val nextStart = parseInstant(nextSlot.startTime)
            if (nextStart != null) {
                slotsByStart.forEach { slot ->
                    if (parseInstant(slot.startTime) == nextStart) {
                        selectedSlots.putIfAbsent(slot.slotId, slot)
                    }
                }
            }
        }

        val publicationsById = manifest.publications.associateBy { it.publicationId }
        val assetIds = linkedSetOf<String>()
        selectedSlots.values.forEach { slot ->
            slot.assetId?.takeIf { it.isNotBlank() }?.let(assetIds::add)
            val publication = slot.publicationId?.let(publicationsById::get)
            publication?.items?.forEach { item ->
                item.video?.assetId?.takeIf { it.isNotBlank() }?.let(assetIds::add)
                item.slide?.imageAssetId?.takeIf { it.isNotBlank() }?.let(assetIds::add)
                item.slide?.logoAssetId?.takeIf { it.isNotBlank() }?.let(assetIds::add)
            }
        }

        return RelevantAssetSelection(
            slots = selectedSlots.values.toList(),
            assets = manifest.assets.filter { it.assetId in assetIds },
            nextSlot = nextSlot,
        )
    }

    fun computeSyncDelayMs(
        manifest: ReleaseManifest?,
        now: Instant = Instant.now(),
        lookAheadMs: Long = PREFETCH_LOOKAHEAD_MS,
    ): Long {
        val nextSlot = selectAssets(manifest, now, lookAheadMs).nextSlot ?: return NORMAL_SYNC_INTERVAL_MS
        val nextStart = parseInstant(nextSlot.startTime) ?: return NORMAL_SYNC_INTERVAL_MS
        return if (nextStart.toEpochMilli() - now.toEpochMilli() <= lookAheadMs) {
            NEAR_SLOT_SYNC_INTERVAL_MS
        } else {
            NORMAL_SYNC_INTERVAL_MS
        }
    }
}
