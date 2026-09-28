package com.freeapp.founderrooms.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Palette locked by the approved FounderRooms mockups (`design/APPROVED.md`):
 * `02-chat.html`, `03-verify.html`, `04-locked.html`. These are the visual
 * contract, not a starting point: do not retune a value against "what looks
 * right" without re-approving the mockup it came from.
 *
 * EXTENSION POINT: add a color here only when a screen other than these three
 * needs it too. A one-off color belongs at its call site.
 */
object FounderRoomsColors {
    val background = Color(0xFFF5F5F7)
    val card = Color(0xFFFFFFFF)
    val line = Color(0xFFE3E3E8)
    val ink = Color(0xFF1A1A1E)
    val muted = Color(0xFF8A8A92)
    val accent = Color(0xFFE05D38)
    val link = Color(0xFF2F6FED)

    // The one dark-styled bubble variant in 02-chat.html.
    val darkCardBackground = Color(0xFF17171B)
    val darkCardBorder = Color(0xFF2C2C34)
    val darkCardName = Color(0xFF7FB3FF)
    val darkCardBody = Color(0xFFE8E8EC)
    val darkQuoteBackground = Color(0xFF20202A)
    val darkQuoteText = Color(0xFFB9B9C4)
    val darkTime = Color(0xFF6F6F7A)

    val quoteBackground = Color(0xFFFAF5F3)
    val quoteText = Color(0xFF6D6D75)
    val badgeBackground = Color(0xFFFAFAFB)
    val promoIcon = Color(0xFFEE3333)

    val verifiedGreen = Color(0xFF1D9D5A)
    val privacyBackground = Color(0xFFF0F6F1)
    val privacyBorder = Color(0xFFD8E8DC)
    val privacyText = Color(0xFF3C5C46)

    // Avatar stack gradients (01/02 mockups): three "other members" tones
    // plus the two per-author tones used on message avatars.
    val avatarGradient1 = listOf(Color(0xFF9DB4F0), Color(0xFF5A76C8))
    val avatarGradient2 = listOf(Color(0xFFF0C89D), Color(0xFFC88A5A))
    val avatarGradient3 = listOf(Color(0xFFA8D8B0), Color(0xFF5AC87A))
    val avatarGradientA = listOf(Color(0xFF8FB7F7), Color(0xFF3D63C9))
    val avatarGradientB = listOf(Color(0xFF2C2C34), Color(0xFF57575F))
}
