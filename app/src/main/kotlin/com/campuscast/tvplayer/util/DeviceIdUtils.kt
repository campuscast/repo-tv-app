package com.campuscast.tvplayer.util

private const val SEGMENT_COUNT = 4
private const val SEGMENT_LENGTH = 4

fun normalizeDeviceIdInput(raw: String): String {
    return raw
        .uppercase()
        .replace(Regex("[^0-9A-F]"), "")
        .take(SEGMENT_COUNT * SEGMENT_LENGTH)
}

fun formatDeviceId(raw: String): String {
    val normalized = normalizeDeviceIdInput(raw)
    val chunks = normalized.chunked(SEGMENT_LENGTH)
    return chunks.joinToString("-")
}

fun isValidDeviceId(deviceId: String): Boolean {
    val regex = Regex("^[0-9A-F]{4}(-[0-9A-F]{4}){3}$")
    return regex.matches(deviceId)
}
