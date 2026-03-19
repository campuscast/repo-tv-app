package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.core.model.PublicationItem
import com.campuscast.tvplayer.core.model.PublicationItemTransition
import com.campuscast.tvplayer.core.model.PublicationVideoPayload
import com.campuscast.tvplayer.core.model.SlotMetadata
import com.campuscast.tvplayer.core.playback.mapPlaybackMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMetadataTest {
    @Test
    fun `publication metadata overrides slot metadata`() {
        val slot = SlotMetadata(
            transitionType = "fade",
            transitionDurationMs = 400,
            videoTrimInMs = 1000,
            videoTrimOutMs = 3000,
            videoMute = true,
            videoLoop = false,
        )
        val item = PublicationItem(
            type = "video_asset",
            transition = PublicationItemTransition(type = "cut", durationMs = 120),
            video = PublicationVideoPayload(
                trimInMs = 200,
                trimOutMs = 500,
                mute = false,
                loop = true,
            ),
        )

        val result = mapPlaybackMetadata(slot, item)

        assertEquals("cut", result.transitionType)
        assertEquals(120, result.transitionDurationMs)
        assertEquals(200, result.trimInMs)
        assertEquals(500, result.trimOutMs)
        assertFalse(result.muted)
        assertTrue(result.loop)
    }

    @Test
    fun `defaults are applied when metadata is empty`() {
        val result = mapPlaybackMetadata(null, null)

        assertEquals("cut", result.transitionType)
        assertEquals(300, result.transitionDurationMs)
        assertEquals(0, result.trimInMs)
        assertEquals(0, result.trimOutMs)
        assertTrue(result.muted)
        assertTrue(result.loop)
    }
}
