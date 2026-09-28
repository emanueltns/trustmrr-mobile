package com.freeapp.founderrooms.util

import kotlin.math.roundToLong

/**
 * Pure formatter for the "$2K/mo" style MRR badges shown on chat messages
 * and tier rows (see design/mockups/02-chat.html, 03-verify.html).
 *
 * Rounds to the nearest whole dollar first, then to the nearest K or M, so a
 * value that rounds up into the next magnitude (e.g. 999_999) displays in
 * that magnitude ("$1M/mo") rather than as "$1000K/mo".
 */
object MrrFormatter {

    fun format(mrr: Double): String {
        val dollars = mrr.roundToLong()
        return when {
            dollars < 1_000L -> "\$${dollars}/mo"
            dollars < 1_000_000L -> {
                val thousands = (dollars / 1_000.0).roundToLong()
                if (thousands >= 1_000L) "\$${thousands / 1_000L}M/mo" else "\$${thousands}K/mo"
            }
            else -> "\$${(dollars / 1_000_000.0).roundToLong()}M/mo"
        }
    }
}
