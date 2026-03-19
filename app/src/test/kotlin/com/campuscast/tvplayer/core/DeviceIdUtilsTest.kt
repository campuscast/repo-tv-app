package com.campuscast.tvplayer.core

import com.campuscast.tvplayer.util.formatDeviceId
import com.campuscast.tvplayer.util.isValidDeviceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdUtilsTest {
    @Test
    fun `formats raw input into segmented player id`() {
        assertEquals("ABCD-EF12-3456-7890", formatDeviceId("abcd ef12 3456 7890"))
    }

    @Test
    fun `validates proper player id format`() {
        assertTrue(isValidDeviceId("ABCD-EF12-3456-7890"))
        assertFalse(isValidDeviceId("ABCD-EF12-3456"))
    }
}
