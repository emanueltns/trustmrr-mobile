package com.freeapp.founderrooms.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * House watermark, one line, every app built through the build-mobile-app
 * skill carries it (skill section 10 + the design phase mockups). The whole
 * row is one tap target opening freeapp.ai, kept at the 44dp floor without
 * growing the visible text.
 */
@Composable
fun MadeWithFreeapp(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = { uriHandler.openUri("https://freeapp.ai") }),
        contentAlignment = Alignment.Center,
    ) {
        ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = FounderFontFamily)) {
            Text(
                text = "Made with freeapp.ai",
                fontSize = 10.sp,
                color = FounderColors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
