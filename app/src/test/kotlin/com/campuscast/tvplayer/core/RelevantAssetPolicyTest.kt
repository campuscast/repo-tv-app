package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.core.cache.PREFETCH_LOOKAHEAD_MS
import com.campuscast.tvplayer.core.cache.RelevantAssetPolicy
import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.Publication
import com.campuscast.tvplayer.core.model.PublicationItem
import com.campuscast.tvplayer.core.model.PublicationVideoPayload
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.ScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RelevantAssetPolicyTest {
    private fun manifest(): ReleaseManifest {
        return ReleaseManifest(
            releaseId = "release-1",
            scheduleId = "schedule-1",
            versionNumber = 1,
            zoneId = "zone-1",
            createdAt = "2026-03-31T00:00:00.000Z",
            manifestHash = "hash",
            slots = listOf(
                ScheduleSlot(
                    slotId = "past",
                    assetId = "asset-past",
                    startTime = "2026-03-31T09:00:00.000Z",
                    endTime = "2026-03-31T09:30:00.000Z",
                    priority = 1,
                    zoneId = "zone-1",
                    groupId = "group-1",
                ),
                ScheduleSlot(
                    slotId = "active",
                    assetId = "asset-active",
                    publicationId = "publication-1",
                    startTime = "2026-03-31T10:00:00.000Z",
                    endTime = "2026-03-31T11:00:00.000Z",
                    priority = 1,
                    zoneId = "zone-1",
                    groupId = "group-1",
                ),
                ScheduleSlot(
                    slotId = "near",
                    assetId = "asset-near",
                    startTime = "2026-03-31T10:03:00.000Z",
                    endTime = "2026-03-31T10:30:00.000Z",
                    priority = 1,
                    zoneId = "zone-1",
                    groupId = "group-1",
                ),
                ScheduleSlot(
                    slotId = "tomorrow",
                    assetId = "asset-tomorrow",
                    startTime = "2026-04-01T06:00:00.000Z",
                    endTime = "2026-04-01T07:00:00.000Z",
                    priority = 1,
                    zoneId = "zone-1",
                    groupId = "group-1",
                ),
            ),
            assets = listOf(
                ContentAsset(
                    assetId = "asset-past",
                    filename = "past.mp4",
                    contentType = "video/mp4",
                    fileSize = 1,
                    sha256Hash = "a",
                    downloadUrl = "https://example.test/past.mp4",
                ),
                ContentAsset(
                    assetId = "asset-active",
                    filename = "active.mp4",
                    contentType = "video/mp4",
                    fileSize = 1,
                    sha256Hash = "b",
                    downloadUrl = "https://example.test/active.mp4",
                ),
                ContentAsset(
                    assetId = "asset-publication",
                    filename = "publication.mp4",
                    contentType = "video/mp4",
                    fileSize = 1,
                    sha256Hash = "c",
                    downloadUrl = "https://example.test/publication.mp4",
                ),
                ContentAsset(
                    assetId = "asset-near",
                    filename = "near.mp4",
                    contentType = "video/mp4",
                    fileSize = 1,
                    sha256Hash = "d",
                    downloadUrl = "https://example.test/near.mp4",
                ),
                ContentAsset(
                    assetId = "asset-tomorrow",
                    filename = "tomorrow.mp4",
                    contentType = "video/mp4",
                    fileSize = 1,
                    sha256Hash = "e",
                    downloadUrl = "https://example.test/tomorrow.mp4",
                ),
            ),
            publications = listOf(
                Publication(
                    publicationId = "publication-1",
                    zoneId = "zone-1",
                    title = "Publication",
                    type = "playlist",
                    status = "published",
                    version = 1,
                    items = listOf(
                        PublicationItem(
                            itemId = "item-1",
                            type = "video_asset",
                            video = PublicationVideoPayload(assetId = "asset-publication"),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `selects active and near-term assets`() {
        val selection = RelevantAssetPolicy.selectAssets(
            manifest = manifest(),
            now = Instant.parse("2026-03-31T10:00:00.000Z"),
            lookAheadMs = PREFETCH_LOOKAHEAD_MS,
        )

        assertEquals(listOf("active", "near"), selection.slots.map { it.slotId })
        assertEquals(
            listOf("asset-active", "asset-publication", "asset-near"),
            selection.assets.map { it.assetId },
        )
    }

    @Test
    fun `keeps nearest future slot when no slot is active`() {
        val selection = RelevantAssetPolicy.selectAssets(
            manifest = manifest(),
            now = Instant.parse("2026-03-31T12:00:00.000Z"),
            lookAheadMs = PREFETCH_LOOKAHEAD_MS,
        )

        assertEquals(listOf("tomorrow"), selection.slots.map { it.slotId })
        assertEquals(listOf("asset-tomorrow"), selection.assets.map { it.assetId })
    }
}
