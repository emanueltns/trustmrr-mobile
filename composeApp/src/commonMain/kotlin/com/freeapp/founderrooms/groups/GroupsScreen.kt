package com.freeapp.founderrooms.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.core.ui.toDisplayMessage
import com.freeapp.founderrooms.designsystem.FounderColors
import com.freeapp.founderrooms.designsystem.FounderFontFamily
import com.freeapp.founderrooms.tiers.Tier
import org.koin.compose.viewmodel.koinViewModel

private val ErrorRed = Color(0xFFC62828)
private val AvatarGradient = Brush.linearGradient(listOf(Color(0xFFF2B56B), Color(0xFFD96A4B)))
private val EmojiBubble = Color(0xFFF1F1F4)
private val ActiveRowBorder = Color(0xFFBCBCC6)
private val ActiveRowBackground = Color(0xFFFBFBFD)

/**
 * `design/mockups/01-groups.html`, ported 1:1: wordmark + own badge/avatar top
 * bar, the dismissable verify banner, the six tier rows, the footer tagline.
 * The mockup's device-chrome status bar ("9:41", battery glyphs) is not
 * rendered: that is mockup scaffolding standing in for the real OS status
 * bar, not app content.
 *
 * Which row is "Public" is read from list position (index 0), not from
 * `tier.id`: `GetTiersUseCase` (design doc section 5) returns tiers sorted
 * ascending by `mrrFloor` with Public always first (FLOWS.md #1), so this
 * does not depend on `Tier.id`'s type or value, which this file does not
 * otherwise know.
 *
 * Navigation is owned by the caller: tapping an unlocked row calls
 * [onOpenChat] with that tier's id (stringified so this compiles regardless
 * of whether `Tier.id` ends up as a `String` or a numeric type), a locked row
 * calls [onOpenLocked], and the banner's "Verify" calls [onOpenVerify].
 */
@Composable
fun GroupsScreen(
    onOpenChat: (String) -> Unit,
    onOpenLocked: (String) -> Unit,
    onOpenVerify: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = FounderFontFamily)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(FounderColors.background)
                .safeDrawingPadding(),
        ) {
            TopBar(me = state.me, onProfileTap = { showSignOutDialog = true })

            if (showSignOutDialog) {
                AlertDialog(
                    onDismissRequest = { showSignOutDialog = false },
                    title = { Text("Sign out of TrustMRR?") },
                    text = { state.me?.let { Text("Signed in as ${it.displayName}") } },
                    confirmButton = {
                        TextButton(onClick = { showSignOutDialog = false; onLogOut() }) {
                            Text("Sign out", color = FounderColors.ink, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSignOutDialog = false }) {
                            Text("Cancel", color = FounderColors.muted)
                        }
                    },
                )
            }

            if (state.showVerifyBanner) {
                VerifyBanner(onVerify = onOpenVerify, onDismiss = viewModel::onDismissVerifyBanner)
            }

            state.error?.let { error ->
                Text(
                    text = error.toDisplayMessage(),
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.isLoading && state.tiers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FounderColors.ink)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(state.tiers, key = { _, tier -> tier.id }) { index, tier ->
                        val isPublic = index == 0
                        TierRow(
                            tier = tier,
                            isPublic = isPublic,
                            onClick = {
                                val tierId = tier.id.toString()
                                if (tier.locked) onOpenLocked(tierId) else onOpenChat(tierId)
                            },
                        )
                    }
                }
            }

            Footer()
        }
    }
}

@Composable
private fun TopBar(me: Me?, onProfileTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "★ ", color = FounderColors.blue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "TrustMRR",
                color = FounderColors.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }
        // The whole trailing cluster (name + badge + avatar) is one tap target
        // opening the sign-out dialog; kept at the 44dp floor via heightIn.
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onProfileTap)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            me?.displayName?.let { name ->
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FounderColors.ink,
                )
            }
            // Only "unverified" is drawn in the mockup (S2); "verified" is this
            // file's own complement for the state the mockup does not show.
            Text(
                text = if (me?.verifiedMrr != null) "verified" else "unverified",
                fontSize = 11.sp,
                color = FounderColors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, FounderColors.line, RoundedCornerShape(999.dp))
                    .background(FounderColors.card)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AvatarGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = me?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun VerifyBanner(onVerify: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FounderColors.card)
            .border(1.dp, FounderColors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Verify your revenue",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FounderColors.ink,
            )
            Text(
                text = "Unlock the private room that matches your MRR.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = FounderColors.ink,
            )
        }
        // The pill is drawn compact (9dp vertical padding); wrapped so the tap
        // target still clears the 44dp floor (design doc section 8) without
        // growing the visible pill.
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onVerify),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .background(FounderColors.ink, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(text = "Verify", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clickable(onClick = onDismiss)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", color = FounderColors.muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TierRow(tier: Tier, isPublic: Boolean, onClick: () -> Unit) {
    val borderColor = if (isPublic) ActiveRowBorder else FounderColors.line
    val backgroundColor = if (isPublic) ActiveRowBackground else FounderColors.card

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EmojiBubble),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = tier.emoji, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = tier.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FounderColors.ink)
                Text(text = " /mo", fontSize = 12.sp, color = FounderColors.muted)
            }
            Text(
                text = if (isPublic) "Public · everyone welcome" else memberCountLabel(tier.memberCount.toLong()),
                fontSize = 12.sp,
                color = FounderColors.muted,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        if (tier.locked) {
            Text(text = "🔒", fontSize = 15.sp, color = FounderColors.muted)
        } else {
            Text(text = "›", fontSize = 16.sp, color = FounderColors.muted)
        }
    }
}

@Composable
private fun Footer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, start = 32.dp, end = 32.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "A public chat and private groups for founders with verified revenue.",
            fontSize = 11.sp,
            color = FounderColors.muted,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
        )
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Text(text = "by ", fontSize = 11.sp, color = FounderColors.muted)
            Text(text = "★", fontSize = 11.sp, color = FounderColors.blue)
            Text(text = " TrustMRR", fontSize = 11.sp, color = FounderColors.muted)
        }
    }
}

private fun memberCountLabel(count: Long): String =
    "${formatMemberCount(count)} member${if (count == 1L) "" else "s"}"

private fun formatMemberCount(count: Long): String {
    val digits = count.toString()
    val grouped = StringBuilder()
    for ((index, char) in digits.reversed().withIndex()) {
        if (index != 0 && index % 3 == 0) grouped.append(',')
        grouped.append(char)
    }
    return grouped.reverse().toString()
}
