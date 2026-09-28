package com.freeapp.founderrooms.designsystem

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Hand-rolled formatting for the two things the FounderRooms screens need to
 * print. commonMain has no `NumberFormat` / `String.format("%,d")` / JVM
 * locale API, so both are built from scratch instead of reached for.
 */

/** "10:00 AM" from an ISO-8601 instant string such as `Message.createdAt`. Empty if it does not parse. */
fun formatClockTime(isoInstant: String): String {
    val instant = runCatching { Instant.parse(isoInstant) }.getOrNull() ?: return ""
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val minute = local.minute.toString().padStart(2, '0')
    val amPm = if (local.hour < 12) "AM" else "PM"
    val hour12 = when (val h = local.hour % 12) {
        0 -> 12
        else -> h
    }
    return "$hour12:$minute $amPm"
}

/** "1,083" from 1083. Groups every three digits from the right with an ASCII comma. */
fun formatGroupedCount(value: Int): String {
    val digits = value.toString()
    val negative = digits.startsWith("-")
    val unsigned = if (negative) digits.substring(1) else digits
    val grouped = unsigned.reversed().chunked(3).joinToString(",").reversed()
    return if (negative) "-$grouped" else grouped
}
