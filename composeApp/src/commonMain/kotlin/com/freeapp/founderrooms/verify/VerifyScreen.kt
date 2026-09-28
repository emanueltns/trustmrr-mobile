package com.freeapp.founderrooms.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeapp.founderrooms.core.ui.toDisplayMessage
import com.freeapp.founderrooms.designsystem.FounderFontFamily
import com.freeapp.founderrooms.designsystem.FounderRoomsColors
import com.freeapp.founderrooms.tiers.Tier

private val ErrorRed = Color(0xFFC62828)

/**
 * Port of `design/mockups/03-verify.html`. Same passed-in-viewModel shape as
 * [com.freeapp.founderrooms.chat.ChatScreen]: [VerifyViewModel] needs `me`
 * at construction time, a nav-time value, so it is resolved by the caller
 * with Koin's parametrized `viewModel` DSL, not `koinViewModel()` here.
 */
@Composable
fun VerifyScreen(
    viewModel: VerifyViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FounderRoomsColors.background)
            .safeDrawingPadding(),
    ) {
        VerifyHeader(onBack = onBack)

        state.error?.let { error ->
            Text(
                text = error.toDisplayMessage(),
                color = ErrorRed,
                fontSize = 12.sp,
                fontFamily = FounderFontFamily,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            VerifyHero()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FounderRoomsColors.card)
                    .border(1.dp, FounderRoomsColors.line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp),
            ) {
                if (state.isLoadingTiers) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FounderRoomsColors.ink)
                    }
                } else {
                    state.privateTiers.forEachIndexed { index, tier ->
                        VerifyTierRow(tier = tier, isLast = index == state.privateTiers.lastIndex)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FounderRoomsColors.privacyBackground)
                    .border(1.dp, FounderRoomsColors.privacyBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text("🛡", fontSize = 14.sp, fontFamily = FounderFontFamily)
                Text(
                    "We read exactly one number: your MRR total. Never your customers, never your payouts. You can disconnect any time and keep chatting in Public.",
                    fontSize = 11.5.sp,
                    color = FounderRoomsColors.privacyText,
                    lineHeight = 17.sp,
                    fontFamily = FounderFontFamily,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ConnectStripeButton(connectState = state.connectState, onClick = viewModel::onConnectStripe)

            // Mockup's `.skip` link: a real affordance, not decoration -- per
            // design doc section 5 ("skip link pops back to Groups") this is
            // the same destination as the header's back chevron.
            Text(
                "I have no revenue yet, stay in 🌱 Public",
                fontSize = 12.sp,
                color = FounderRoomsColors.muted,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline,
                fontFamily = FounderFontFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onBack)
                    .padding(top = 16.dp, bottom = 22.dp),
            )
        }
    }
}

@Composable
private fun VerifyHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", fontSize = 22.sp, color = FounderRoomsColors.ink, fontFamily = FounderFontFamily)
        }
        Text(
            "Verify revenue",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = FounderRoomsColors.ink,
            fontFamily = FounderFontFamily,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun VerifyHero() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔐", fontSize = 44.sp, fontFamily = FounderFontFamily)
        Text(
            "One number unlocks your room",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = FounderRoomsColors.ink,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center,
            fontFamily = FounderFontFamily,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "Connect your Stripe account read-only. We check your monthly recurring revenue once and place you in the matching group.",
            fontSize = 12.5.sp,
            color = FounderRoomsColors.muted,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            fontFamily = FounderFontFamily,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun VerifyTierRow(tier: Tier, isLast: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(tier.emoji, fontSize = 16.sp, fontFamily = FounderFontFamily)
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    tier.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FounderRoomsColors.ink,
                    fontFamily = FounderFontFamily,
                )
                Text(" /mo", fontSize = 11.sp, color = FounderRoomsColors.muted, fontFamily = FounderFontFamily)
            }
            // `tier.locked` already carries this user's unlock state (computed
            // in shared's `toTiers`), so no extra "is this mine" field is needed
            // to tell "your tier" apart from "🔒 unlocks" here.
            if (tier.locked) {
                Text("🔒 unlocks", fontSize = 11.sp, color = FounderRoomsColors.muted, fontFamily = FounderFontFamily)
            } else {
                Text(
                    "your tier",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FounderRoomsColors.verifiedGreen,
                    fontFamily = FounderFontFamily,
                )
            }
        }
        if (!isLast) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(FounderRoomsColors.line))
        }
    }
}

@Composable
private fun ConnectStripeButton(connectState: StripeConnectState, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FounderRoomsColors.ink)
            .clickable(enabled = connectState == StripeConnectState.Idle, onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (connectState) {
            StripeConnectState.Idle -> Text(
                "Connect Stripe →",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FounderFontFamily,
            )
            StripeConnectState.Connecting -> CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
            // Literal copy from VerifyViewModel's decision-3 doc comment /
            // design doc section 6: stated outcome, never a spinner that
            // never resolves.
            StripeConnectState.Pending -> Text(
                "Verification is not live yet, you're still in Public",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = FounderFontFamily,
            )
        }
    }
}
