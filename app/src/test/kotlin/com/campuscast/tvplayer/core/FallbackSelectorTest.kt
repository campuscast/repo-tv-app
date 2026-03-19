package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.ScheduleSlot
import com.campuscast.tvplayer.core.playback.FallbackSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FallbackSelectorTest {
    @Test
    fun `returns current when current manifest is usable`() {
        val current = sampleManifest("cur")
        val previous = sampleManifest("prev")

        val selected = FallbackSelector.chooseLastKnownGood(current, previous)

        assertEquals("cur", selected?.releaseId)
    }

    @Test
    fun `returns previous when current is broken`() {
        val current = sampleManifest("cur").copy(assets = emptyList())
        val previous = sampleManifest("prev")

        val selected = FallbackSelector.chooseLastKnownGood(current, previous)

        assertEquals("prev", selected?.releaseId)
    }

    @Test
    fun `returns null when both are broken`() {
        val current = sampleManifest("cur").copy(slots = emptyList())
        val previous = sampleManifest("prev").copy(assets = emptyList())

        val selected = FallbackSelector.chooseLastKnownGood(current, previous)

        assertNull(selected)
    }

    private fun sampleManifest(id: String): ReleaseManifest {
        return ReleaseManifest(
            releaseId = id,
            scheduleId = "sch-1",
            versionNumber = 1,
            zoneId = "zone-1",
            slots = listOf(
                ScheduleSlot(
                    slotId = "slot-1",
                    assetId = "asset-1",
                    startTime = "2026-03-19T10:00:00Z",
                    endTime = "2026-03-19T12:00:00Z",
                    priority = 1,
                    zoneId = "zone-1",
                )
            ),
            assets = listOf(
                ContentAsset(
                    assetId = "asset-1",
                    filename = "video.mp4",
                    contentType = "video/mp4",
                    fileSize = 123,
                    sha256Hash = "hash",
                    downloadUrl = "https://example.com/video.mp4",
                )
            ),
            createdAt = "2026-03-19T09:00:00Z",
        )
    }
}
