package com.campuscast.tvplayer.core.playback

import com.campuscast.tvplayer.core.model.PublicationItem
import com.campuscast.tvplayer.core.model.SlotMetadata

data class EffectivePlaybackMetadata(
    val transitionType: String,
    val transitionDurationMs: Int,
    val trimInMs: Int,
    val trimOutMs: Int,
    val muted: Boolean,
    val loop: Boolean,
)

fun mapPlaybackMetadata(
    slotMetadata: SlotMetadata?,
    publicationItem: PublicationItem?,
): EffectivePlaybackMetadata {
    return EffectivePlaybackMetadata(
        transitionType = publicationItem?.transition?.type ?: slotMetadata?.transitionType ?: "cut",
        transitionDurationMs = publicationItem?.transition?.durationMs ?: slotMetadata?.transitionDurationMs ?: 300,
        trimInMs = publicationItem?.video?.trimInMs ?: slotMetadata?.videoTrimInMs ?: 0,
        trimOutMs = publicationItem?.video?.trimOutMs ?: slotMetadata?.videoTrimOutMs ?: 0,
        muted = publicationItem?.video?.mute ?: slotMetadata?.videoMute ?: true,
        loop = publicationItem?.video?.loop ?: slotMetadata?.videoLoop ?: true,
    )
}
