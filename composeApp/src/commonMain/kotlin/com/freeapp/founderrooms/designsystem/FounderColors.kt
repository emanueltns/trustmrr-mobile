package com.freeapp.founderrooms.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The FounderRooms palette, lifted byte-for-byte from the CSS custom
 * properties every mockup HTML file under `design/mockups` declares at its root
 * (`--bg`, `--card`, `--line`, `--ink`, `--mut`, `--acc`, `--blue`). Locked per
 * `design/APPROVED.md`: change a value here only alongside a re-approved
 * mockup, never to match one screen's own preference.
 *
 * EXTENSION POINT: add a color only once a second screen needs it, same rule
 * as [Dimens].
 */
object FounderColors {
    val background = Color(0xFFF5F5F7)
    val card = Color(0xFFFFFFFF)
    val line = Color(0xFFE3E3E8)
    val ink = Color(0xFF1A1A1E)
    val muted = Color(0xFF8A8A92)
    val accent = Color(0xFFE05D38)
    val blue = Color(0xFF2F6FED)
}
