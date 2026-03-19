package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.core.model.ContentAsset
import com.campuscast.tvplayer.core.model.PlaybackStatus
import com.campuscast.tvplayer.core.model.Publication
import com.campuscast.tvplayer.core.model.PublicationItem
import com.campuscast.tvplayer.core.model.PublicationVideoPayload
import com.campuscast.tvplayer.core.model.ReleaseManifest
import com.campuscast.tvplayer.core.model.ScheduleSlot
import com.campuscast.tvplayer.core.playback.PlaybackEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PlaybackEvaluatorTest {
    private val evaluator = PlaybackEvaluator()

    @Test
    fun `selects highest priority active slot`() {
        val manifest = ReleaseManifest(
            releaseId = "rel-1",
            scheduleId = "sch-1",
            versionNumber = 1,
            zoneId = "zone-1",
            slots = listOf(
                slot("slot-low", "asset-1", priority = 1),
                slot("slot-high", "asset-2", priority = 10),
            ),
            assets = listOf(asset("asset-1"), asset("asset-2")),
            createdAt = "2026-03-19T09:00:00Z",
        )

        val state = evaluator.evaluate(
            manifest = manifest,
            assetLocalPathLookup = { "/cache/$it" },
            now = Instant.parse("2026-03-19T10:30:00Z"),
        )

        assertEquals("slot-high", state.currentSlot?.slotId)
        assertEquals(PlaybackStatus.PLAYING, state.status)
        assertEquals("/cache/asset-2", state.currentAssetLocalPath)
    }

    @Test
    fun `returns idle when no active slot`() {
        val manifest = ReleaseManifest(
            releaseId = "rel-1",
            scheduleId = "sch-1",
            versionNumber = 1,
            zoneId = "zone-1",
            slots = listOf(
                ScheduleSlot(
                    slotId = "future",
                    assetId = "asset-1",
                    startTime = "2026-03-19T18:00:00Z",
                    endTime = "2026-03-19T19:00:00Z",
                    priority = 1,
                    zoneId = "zone-1",
                )
            ),
            assets = listOf(asset("asset-1")),
            createdAt = "2026-03-19T09:00:00Z",
        )

        val state = evaluator.evaluate(
            manifest = manifest,
            assetLocalPathLookup = { null },
            now = Instant.parse("2026-03-19T10:30:00Z"),
        )

        assertEquals(PlaybackStatus.IDLE, state.status)
        assertNull(state.currentSlot)
        assertNotNull(state.nextSlot)
    }

    @Test
    fun `publication item is selected by elapsed duration`() {
        val manifest = ReleaseManifest(
            releaseId = "rel-1",
            scheduleId = "sch-1",
            versionNumber = 1,
            zoneId = "zone-1",
            slots = listOf(
                ScheduleSlot(
                    slotId = "slot-1",
                    publicationId = "pub-1",
                    startTime = "2026-03-19T10:00:00Z",
                    endTime = "2026-03-19T11:00:00Z",
                    priority = 5,
                    zoneId = "zone-1",
                )
            ),
            assets = listOf(asset("video-1"), asset("video-2")),
            publications = listOf(
                Publication(
                    publicationId = "pub-1",
                    zoneId = "zone-1",
                    title = "Test",
                    type = "slideshow",
                    status = "published",
                    version = 1,
                    items = listOf(
                        PublicationItem(
                            itemId = "item-1",
                            type = "video_asset",
                            durationMs = 5000,
                            video = PublicationVideoPayload(assetId = "video-1"),
                        ),
                        PublicationItem(
                            itemId = "item-2",
                            type = "video_asset",
                            durationMs = 5000,
                            video = PublicationVideoPayload(assetId = "video-2"),
                        ),
                    ),
                )
            ),
            createdAt = "2026-03-19T09:00:00Z",
        )

        val state = evaluator.evaluate(
            manifest = manifest,
            assetLocalPathLookup = { "/cache/$it" },
            now = Instant.parse("2026-03-19T10:00:07Z"),
        )

        assertEquals("item-2", state.currentPublicationItem?.itemId)
        assertEquals("video-2", state.currentAsset?.assetId)
    }

    private fun slot(slotId: String, assetId: String, priority: Int): ScheduleSlot {
        return ScheduleSlot(
            slotId = slotId,
            assetId = assetId,
            startTime = "2026-03-19T10:00:00Z",
            endTime = "2026-03-19T11:00:00Z",
            priority = priority,
            zoneId = "zone-1",
        )
    }

    private fun asset(id: String): ContentAsset {
        return ContentAsset(
            assetId = id,
            filename = "$id.mp4",
            contentType = "video/mp4",
            fileSize = 1,
            sha256Hash = "hash-$id",
            downloadUrl = "https://example.com/$id.mp4",
        )
    }
}
