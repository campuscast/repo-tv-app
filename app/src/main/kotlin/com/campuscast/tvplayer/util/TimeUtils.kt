package com.campuscast.tvplayer.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun nowIso(): String = Instant.now().toString()

fun parseInstant(iso: String?): Instant? = try {
    if (iso.isNullOrBlank()) null else Instant.parse(iso)
} catch (_: Exception) {
    null
}

fun formatLocalDateTime(iso: String?): String {
    val instant = parseInstant(iso) ?: return "Never"
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

fun isWithinSchedule(startTimeIso: String, endTimeIso: String, now: Instant = Instant.now()): Boolean {
    val start = parseInstant(startTimeIso) ?: return false
    val end = parseInstant(endTimeIso) ?: return false
    return !now.isBefore(start) && now.isBefore(end)
}
