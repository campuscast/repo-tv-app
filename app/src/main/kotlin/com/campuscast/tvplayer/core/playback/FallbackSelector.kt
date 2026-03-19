package com.campuscast.tvplayer.core.playback

import com.campuscast.tvplayer.core.model.ReleaseManifest

object FallbackSelector {
    fun chooseLastKnownGood(
        current: ReleaseManifest?,
        previous: ReleaseManifest?,
    ): ReleaseManifest? {
        return when {
            ManifestValidator.isUsable(current) -> current
            ManifestValidator.isUsable(previous) -> previous
            else -> null
        }
    }
}
