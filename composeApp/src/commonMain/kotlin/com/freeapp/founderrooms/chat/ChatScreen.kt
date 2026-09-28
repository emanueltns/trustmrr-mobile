package com.freeapp.founderrooms.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.toDisplayMessage
import com.freeapp.founderrooms.designsystem.FounderRoomsColors

/**
 * Port of `design/mockups/02-chat.html`. Layout only: every value read here
 * comes from [ChatViewModel.state], nothing is computed in this file beyond
 * pure presentation (colors per dark/light, shapes).
 *
 * [viewModel] is passed in already constructed (with `tierId` and `me` bound)
 * rather than resolved with `koinViewModel()` inside this file: those two
 * values come from the navigation route, which is a separate pass per the
 * task boundary. See the delivery report for the exact Koin wiring line.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FounderRoomsColors.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        ChatHeader(
            emoji = state.tierEmoji,
            title = state.tierTitle,
            subtitle = state.tierSubtitle,
            overflowLabel = state.overflowMemberLabel,
            onBack = onBack,
        )

        state.error?.let { ChatErrorBanner(it) }

        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = FounderRoomsColors.muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }
            }
        }

        ChatInputBar(
            draft = state.draft,
            pendingLinkUrl = state.pendingLinkUrl,
            isSending = state.isSending,
            onDraftChange = viewModel::onDraftChange,
            onOpenLinkDialog = viewModel::onOpenLinkDialog,
            onRemovePendingLink = viewModel::onRemovePendingLink,
            onSend = viewModel::onSend,
        )
    }

    if (state.isLinkDialogOpen) {
        LinkDialog(
            onDismiss = viewModel::onDismissLinkDialog,
            onAttach = viewModel::onAttachLink,
        )
    }
}

@Composable
private fun ChatHeader(
    emoji: String,
    title: String,
    subtitle: String,
    overflowLabel: String,
    onBack: () -> Unit,
) {
    Column {
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
                Text("‹", fontSize = 22.sp, color = FounderRoomsColors.ink, fontFamily = FontFamily.Monospace)
            }
            Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FounderRoomsColors.ink,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        " /mo",
                        fontSize = 12.sp,
                        color = FounderRoomsColors.muted,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(subtitle, fontSize = 11.sp, color = FounderRoomsColors.muted, fontFamily = FontFamily.Monospace)
            }
            AvatarStack(overflowLabel)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(FounderRoomsColors.line))
    }
}

@Composable
private fun AvatarStack(overflowLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 7.dp)) {
        listOf(
            FounderRoomsColors.avatarGradient1,
            FounderRoomsColors.avatarGradient2,
            FounderRoomsColors.avatarGradient3,
        ).forEachIndexed { index, gradient ->
            Box(
                modifier = Modifier
                    .offset(x = if (index == 0) 0.dp else (-7).dp * index)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient))
                    .border(2.dp, FounderRoomsColors.background, CircleShape),
            )
        }
        if (overflowLabel.isNotEmpty()) {
            Text(
                overflowLabel,
                fontSize = 11.sp,
                color = FounderRoomsColors.muted,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
    }
}

@Composable
private fun ChatErrorBanner(error: ErrorState) {
    Box(modifier = Modifier.fillMaxWidth().background(FounderRoomsColors.promoIcon).padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(error.toDisplayMessage(), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ChatBubble(message: ChatMessageUi) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (message.showAuthorHeader && message.authorName.isNotBlank()) {
            AuthorAvatar(message.authorId)
            Spacer(modifier = Modifier.width(9.dp))
        } else {
            // Matches the mockup's `margin-left:39px` continuation indent (30dp avatar + 9dp gap).
            Spacer(modifier = Modifier.width(39.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            BubbleCard(message)
        }
    }
}

@Composable
private fun AuthorAvatar(authorId: String) {
    val gradient = if (kotlin.math.abs(authorId.hashCode()) % 2 == 0) {
        FounderRoomsColors.avatarGradientA
    } else {
        FounderRoomsColors.avatarGradientB
    }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient)),
    )
}

@Composable
private fun BubbleCard(message: ChatMessageUi) {
    val background = if (message.isDark) FounderRoomsColors.darkCardBackground else FounderRoomsColors.card
    val border = if (message.isDark) FounderRoomsColors.darkCardBorder else FounderRoomsColors.line
    val nameColor = if (message.isDark) FounderRoomsColors.darkCardName else FounderRoomsColors.accent
    val bodyColor = if (message.isDark) FounderRoomsColors.darkCardBody else FounderRoomsColors.ink
    val timeColor = if (message.isDark) FounderRoomsColors.darkTime else FounderRoomsColors.muted
    val linkColor = if (message.isDark) FounderRoomsColors.darkCardName else FounderRoomsColors.link
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (message.showAuthorHeader && message.authorName.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 5.dp)) {
                Text(message.authorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nameColor, fontFamily = FontFamily.Monospace)
                message.authorMrrBadge?.let { badge ->
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (message.isDark) FounderRoomsColors.darkQuoteBackground else FounderRoomsColors.badgeBackground)
                            .border(
                                1.dp,
                                if (message.isDark) FounderRoomsColors.darkCardBorder else FounderRoomsColors.line,
                                RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    ) {
                        Text(badge, fontSize = 10.sp, color = timeColor, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Decision 7: quotes render read-only, no reply affordance — every quote in v1 is seeded history.
        if (!message.quotedExcerpt.isNullOrBlank()) {
            QuoteBlock(message.quotedAuthor, message.quotedExcerpt, message.isDark)
        }

        if (message.body.isNotBlank()) {
            ExpandableBody(message.body, bodyColor)
        }

        // Decision 6: any message carrying a `linkUrl` renders as a link card, whether it came
        // from the ⊕ dialog or seed data. Message has no icon/color field, so a generic link
        // glyph is used rather than pixel-matching the mockup's per-post icon and red square.
        if (message.linkUrl != null) {
            LinkCard(message.linkUrl, message.linkTitle, linkColor)
        }

        Text(
            message.timeLabel,
            fontSize = 10.sp,
            color = timeColor,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@Composable
private fun QuoteBlock(author: String?, excerpt: String, isDark: Boolean) {
    val accentColor = if (isDark) FounderRoomsColors.darkCardName else FounderRoomsColors.accent
    val backgroundColor = if (isDark) FounderRoomsColors.darkQuoteBackground else FounderRoomsColors.quoteBackground
    val textColor = if (isDark) FounderRoomsColors.darkQuoteText else FounderRoomsColors.quoteText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = 7.dp),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accentColor))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(backgroundColor)
                .padding(start = 9.dp, top = 6.dp, end = 9.dp, bottom = 6.dp),
        ) {
            if (!author.isNullOrBlank()) {
                Text(author, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor, fontFamily = FontFamily.Monospace)
            }
            Text(excerpt, fontSize = 11.5.sp, color = textColor, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ExpandableBody(body: String, color: Color) {
    var expanded by remember(body) { mutableStateOf(false) }
    var isOverflowing by remember(body) { mutableStateOf(false) }
    Column {
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = color,
            fontFamily = FontFamily.Monospace,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> isOverflowing = result.hasVisualOverflow },
        )
        if (isOverflowing && !expanded) {
            Text(
                "Read more",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}

@Composable
private fun LinkCard(url: String, title: String?, accentColor: Color) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { runCatching { uriHandler.openUri(url) } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(FounderRoomsColors.promoIcon),
            contentAlignment = Alignment.Center,
        ) {
            Text("🔗", fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title?.ifBlank { url } ?: url,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                url,
                fontSize = 12.sp,
                color = FounderRoomsColors.muted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    draft: String,
    pendingLinkUrl: String?,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onOpenLinkDialog: () -> Unit,
    onRemovePendingLink: () -> Unit,
    onSend: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp).padding(top = 8.dp, bottom = 22.dp)) {
        pendingLinkUrl?.let { url ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FounderRoomsColors.card)
                    .border(1.dp, FounderRoomsColors.line, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    url,
                    fontSize = 12.sp,
                    color = FounderRoomsColors.link,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.size(24.dp).clickable(onClick = onRemovePendingLink), contentAlignment = Alignment.Center) {
                    Text("✕", fontSize = 13.sp, color = FounderRoomsColors.muted, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FounderRoomsColors.card)
                .border(1.dp, FounderRoomsColors.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(44.dp).clickable(onClick = onOpenLinkDialog), contentAlignment = Alignment.Center) {
                Text("⊕", fontSize = 17.sp, color = FounderRoomsColors.muted, fontFamily = FontFamily.Monospace)
            }
            Box(modifier = Modifier.weight(1f)) {
                if (draft.isEmpty()) {
                    Text("Message", fontSize = 13.sp, color = FounderRoomsColors.muted, fontFamily = FontFamily.Monospace)
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    textStyle = TextStyle(fontSize = 13.sp, color = FounderRoomsColors.ink, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier.size(44.dp).clickable(enabled = !isSending, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(FounderRoomsColors.ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("➤", fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun LinkDialog(
    onDismiss: () -> Unit,
    onAttach: (String) -> Unit,
) {
    var linkText by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste a link", fontFamily = FontFamily.Monospace) },
        text = {
            BasicTextField(
                value = linkText,
                onValueChange = { linkText = it },
                textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = FounderRoomsColors.ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FounderRoomsColors.line, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onAttach(linkText) }) { Text("Attach", fontFamily = FontFamily.Monospace) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontFamily = FontFamily.Monospace) }
        },
    )
}
