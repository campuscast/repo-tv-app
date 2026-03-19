package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.core.cache.CachePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {
    @Test
    fun `cleanup removes files not in expected set when no missing assets`() {
        val decision = CachePolicy.buildCleanupDecision(
            existingFiles = setOf("a.jpg", "b.mp4", "old.png"),
            expectedFiles = setOf("a.jpg", "b.mp4"),
            missingAssets = 0,
        )

        assertTrue(decision.canCleanup)
        assertEquals(setOf("old.png"), decision.removeFiles)
    }

    @Test
    fun `cleanup does not remove anything when manifest has missing assets`() {
        val decision = CachePolicy.buildCleanupDecision(
            existingFiles = setOf("a.jpg", "b.mp4", "old.png"),
            expectedFiles = setOf("a.jpg", "b.mp4"),
            missingAssets = 1,
        )

        assertFalse(decision.canCleanup)
        assertTrue(decision.removeFiles.isEmpty())
    }
}
