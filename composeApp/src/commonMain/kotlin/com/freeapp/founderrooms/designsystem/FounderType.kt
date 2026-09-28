package com.freeapp.founderrooms.designsystem

import androidx.compose.ui.text.font.FontFamily

/**
 * Every mockup HTML file under `design/mockups` sets `font-family: ui-monospace,
 * 'SF Mono', Menlo, Consolas, monospace` at its root: FounderRooms' type
 * system is monospace throughout, not a fallback for one screen.
 *
 * `Text` picks this up automatically once a screen wraps its content in
 * `ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = FounderFontFamily))`.
 * `BasicTextField` does not read the ambient text style, so its own
 * `textStyle` must set `fontFamily = FounderFontFamily` explicitly.
 */
val FounderFontFamily: FontFamily = FontFamily.Monospace
