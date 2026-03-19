package com.campuscast.tvplayer.core.playback

import com.campuscast.tvplayer.core.model.ReleaseManifest

object ManifestValidator {
    fun isUsable(manifest: ReleaseManifest?): Boolean {
        if (manifest == null) return false
        if (manifest.releaseId.isBlank()) return false
        if (manifest.scheduleId.isBlank()) return false
        if (manifest.zoneId.isBlank()) return false
        if (manifest.slots.isEmpty()) return false
        if (manifest.assets.isEmpty()) return false
        return true
    }
}
