package com.freeapp.founderrooms.locked

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeapp.founderrooms.core.ui.toDisplayMessage
import com.freeapp.founderrooms.designsystem.FounderFontFamily
import com.freeapp.founderrooms.designsystem.FounderRoomsColors
import com.freeapp.founderrooms.designsystem.formatGroupedCount
import com.freeapp.founderrooms.tiers.Tier

private val ErrorRed = Color(0xFFC62828)

/**
 * Port of `design/mockups/04-locked.html`: back chevron, the tier's big
 * emoji with a lock badge overlaid, the MRR band as title, member count,
 * body copy with the revenue clause bolded, the primary CTA, and the
 * "back to Public" text link.
 *
 * [viewModel] is passed in already constructed, same pattern as
 * `ChatScreen` and `VerifyScreen`: `tierId` and `me` are nav-time values
 * Koin's plain viewModel DSL cannot supply on its own.
 */
@Composable
fun LockedScreen(
    viewModel: LockedViewModel,
    onVerify: () -> Unit,
    onBack: () -> Unit,
    onOpenPublic: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(FounderRoomsColors.background).safeDrawingPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 22.sp, color = FounderRoomsColors.ink, fontFamily = FounderFontFamily)
            }
        }

        val tier = state.tier
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator(color = FounderRoomsColors.ink)
                tier == null -> Text(
                    state.error?.toDisplayMessage() ?: "That room could not be found.",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontFamily = FounderFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 34.dp),
                )
                else -> LockedContent(tier = tier, onVerify = onVerify, onOpenPublic = onOpenPublic)
            }
        }
    }
}

@Composable
private fun LockedContent(tier: Tier, onVerify: () -> Unit, onOpenPublic: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp).padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(FounderRoomsColors.card)
                    .border(1.dp, FounderRoomsColors.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(tier.emoji, fontSize = 40.sp, fontFamily = FounderFontFamily)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(FounderRoomsColors.ink)
                    .border(3.dp, FounderRoomsColors.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔒", fontSize = 15.sp, color = Color.White, fontFamily = FounderFontFamily)
            }
        }

        Row(modifier = Modifier.padding(top = 20.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                tier.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = FounderRoomsColors.ink,
                letterSpacing = (-0.4).sp,
                fontFamily = FounderFontFamily,
            )
            Text(" /mo", fontSize = 14.sp, color = FounderRoomsColors.muted, fontFamily = FounderFontFamily)
        }

        Text(
            memberCountLabel(tier.memberCount),
            fontSize = 12.sp,
            color = FounderRoomsColors.muted,
            fontFamily = FounderFontFamily,
            modifier = Modifier.padding(top = 6.dp),
        )

        // Exact mockup copy (04-locked.html's `.copy`), not the task brief's
        // paraphrase: "Verify once and it opens on its own", not "request access".
        Text(
            text = buildAnnotatedString {
                append("This room is for founders with ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = FounderRoomsColors.ink)) {
                    append("verified ${tier.label} monthly revenue")
                }
                append(". Verify once and it opens on its own.")
            },
            fontSize = 13.sp,
            color = FounderRoomsColors.muted,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            fontFamily = FounderFontFamily,
            modifier = Modifier.padding(top = 16.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FounderRoomsColors.ink)
                .clickable(onClick = onVerify)
                .padding(vertical = 15.dp, horizontal = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Verify my revenue →",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FounderFontFamily,
            )
        }

        // Decision: the mockup's "back to 🌱 Public chat" link is a distinct
        // affordance from the header's back chevron (that one pops to
        // Groups). Design doc section 5 spells this out literally --
        // "LockedScreen.kt ... back link returns to Public chat" -- so this
        // routes straight into the Public tier's chat via [onOpenPublic],
        // matching FLOWS.md step 4/8's intent rather than re-using [onBack].
        Text(
            "back to 🌱 Public chat",
            fontSize = 12.sp,
            color = FounderRoomsColors.muted,
            textDecoration = TextDecoration.Underline,
            fontFamily = FounderFontFamily,
            modifier = Modifier
                .padding(top = 14.dp)
                .heightIn(min = 44.dp)
                .clickable(onClick = onOpenPublic)
                .padding(vertical = 8.dp),
        )
    }
}

private fun memberCountLabel(count: Int): String =
    "${formatGroupedCount(count)} member${if (count == 1) "" else "s"} inside"
