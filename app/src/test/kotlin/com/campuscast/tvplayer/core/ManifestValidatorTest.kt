package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.ScheduleSlot
import com.campuscast.tvplayer.core.playback.ManifestValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestValidatorTest {
    @Test
    fun `manifest with required fields is usable`() {
        val manifest = sampleManifest()
        assertTrue(ManifestValidator.isUsable(manifest))
    }

    @Test
    fun `manifest without assets is not usable`() {
        val manifest = sampleManifest().copy(assets = emptyList())
        assertFalse(ManifestValidator.isUsable(manifest))
    }

    @Test
    fun `manifest without slots is not usable`() {
        val manifest = sampleManifest().copy(slots = emptyList())
        assertFalse(ManifestValidator.isUsable(manifest))
    }

    private fun sampleManifest(): ReleaseManifest {
        return ReleaseManifest(
            releaseId = "rel-1",
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
