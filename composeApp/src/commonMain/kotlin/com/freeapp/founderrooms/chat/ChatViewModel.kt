package com.freeapp.founderrooms.chat

import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.BaseViewModel
import com.freeapp.founderrooms.designsystem.formatClockTime
import com.freeapp.founderrooms.tiers.TiersRepository

/**
 * One bubble, already flattened for the composable: no [Message] knowledge,
 * no author-grouping decision, no dark/light decision left to make at render
 * time. `authorId` is kept (even though the composable never prints it)
 * because [ChatViewModel] needs it to re-derive [showAuthorHeader] whenever
 * the list is rebuilt.
 */
data class ChatMessageUi(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorMrrBadge: String?,
    val body: String,
    val linkUrl: String?,
    val linkTitle: String?,
    val quotedAuthor: String?,
    val quotedExcerpt: String?,
    val timeLabel: String,
    val isDark: Boolean,
    val showAuthorHeader: Boolean,
)

data class ChatUiState(
    val tierEmoji: String = "",
    val tierTitle: String = "",
    val tierSubtitle: String = "",
    val overflowMemberLabel: String = "",
    val messages: List<ChatMessageUi> = emptyList(),
    val isLoading: Boolean = true,
    val draft: String = "",
    val pendingLinkUrl: String? = null,
    val isLinkDialogOpen: Boolean = false,
    val isSending: Boolean = false,
    val error: ErrorState? = null,
)

/** No effects raised yet. Kept as a real sealed type, not `Unit`, so a future nav/snackbar case has somewhere to go. */
sealed interface ChatEffect

/**
 * ASSUMPTION / compile risk, flagged for the navigation-wiring pass: `me: Me`
 * is not one of this task's declared shared dependencies (only `ChatRepository`,
 * `Message`, `TiersRepository`, `Tier`, `VerifyRepository` were specified). No
 * `Me` type exists anywhere in this repo yet. `TiersRepository.getTiers(me)`
 * is fixed by the calling task's spec, so this assumes `Me` lands at
 * `com.freeapp.founderrooms.auth.Me` with at least `id: String` and
 * `verifiedMrr: Double?`. If the real Auth domain names or places it
 * differently, only the import above and the two `me.` references below need
 * to change.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val tiersRepository: TiersRepository,
    private val tierId: String,
    private val me: Me,
) : BaseViewModel<ChatUiState, ChatEffect>(ChatUiState()) {

    // Ids already rendered, so history's tail and the socket's first pushes
    // (the server broadcasts to the sender's own connection too) can overlap
    // without producing a duplicate bubble.
    private val seenMessageIds = mutableSetOf<String>()

    init {
        loadTierHeader()
        loadHistoryThenLive()
    }

    private fun loadTierHeader() = tryToExecute(
        call = { tiersRepository.getTiers(me) },
        onSuccess = onSuccess@{ tiers ->
            val tier = tiers.firstOrNull { it.id == tierId } ?: return@onSuccess
            val isPublic = tier.mrrFloor <= 0.0
            val overflow = (tier.memberCount - 3).coerceAtLeast(0)
            updateState {
                it.copy(
                    tierEmoji = tier.emoji,
                    // Only the Public case is drawn in the approved mockup ("$0" / "Public");
                    // a private room's header is extrapolated since no mockup covers it.
                    tierTitle = if (isPublic) "$0" else tier.label,
                    tierSubtitle = if (isPublic) "Public" else "Founders",
                    overflowMemberLabel = if (overflow > 0) "+$overflow" else "",
                )
            }
        },
        onError = { error -> updateState { it.copy(error = error) } },
    )

    private fun loadHistoryThenLive() = tryToExecute(
        call = { chatRepository.history(tierId) },
        onSuccess = { history ->
            val ui = history.map { it.toUi() }
            ui.forEach { seenMessageIds += it.id }
            updateState { it.copy(messages = groupByAuthorRun(ui), isLoading = false) }
            collectLive()
        },
        onError = { error -> updateState { it.copy(isLoading = false, error = error) } },
    )

    private fun collectLive() = tryToCollect(
        flow = { chatRepository.live(tierId) },
        onEach = { message -> appendIfNew(message) },
        onError = { error -> updateState { it.copy(error = error) } },
    )

    private fun appendIfNew(message: Message) {
        if (!seenMessageIds.add(message.id.toString())) return
        updateState { it.copy(messages = groupByAuthorRun(it.messages + message.toUi())) }
    }

    fun onDraftChange(value: String) = updateState { it.copy(draft = value) }

    fun onOpenLinkDialog() = updateState { it.copy(isLinkDialogOpen = true) }

    fun onDismissLinkDialog() = updateState { it.copy(isLinkDialogOpen = false) }

    /** Decision 6: the ⊕ dialog's one field becomes `linkUrl` on send, nothing else — no image path exists in v1. */
    fun onAttachLink(url: String) {
        val trimmed = url.trim()
        updateState { it.copy(pendingLinkUrl = trimmed.ifEmpty { null }, isLinkDialogOpen = false) }
    }

    fun onRemovePendingLink() = updateState { it.copy(pendingLinkUrl = null) }

    fun onSend() {
        val body = state.value.draft.trim()
        val link = state.value.pendingLinkUrl
        if ((body.isEmpty() && link == null) || state.value.isSending) return
        updateState { it.copy(isSending = true) }
        tryToExecute(
            call = { chatRepository.send(tierId, body, link) },
            onSuccess = { sent ->
                appendIfNew(sent)
                updateState { it.copy(isSending = false, draft = "", pendingLinkUrl = null, error = null) }
            },
            onError = { error -> updateState { it.copy(isSending = false, error = error) } },
        )
    }

    private fun Message.toUi(): ChatMessageUi = ChatMessageUi(
        id = id.toString(),
        authorId = authorId.toString(),
        authorName = authorName,
        authorMrrBadge = authorMrrBadge,
        body = body,
        linkUrl = linkUrl,
        linkTitle = linkTitle,
        quotedAuthor = quotedAuthor,
        quotedExcerpt = quotedExcerpt,
        timeLabel = formatClockTime(createdAt),
        // Deviation (see delivery report): the mockup's one dark bubble
        // (Julian Wagner's) has no field on Message marking it as special —
        // it is seeded demo styling. The closest deterministic, real rule is
        // "this device's own messages get the dark/own-message treatment",
        // which doubles as visible feedback for an optimistic send.
        isDark = authorId == me.id,
        showAuthorHeader = true,
    )

    /** Consecutive messages by the same author collapse their header, matching the mockup's grouped runs. */
    private fun groupByAuthorRun(messages: List<ChatMessageUi>): List<ChatMessageUi> {
        var previousAuthorId: String? = null
        return messages.map { message ->
            val show = message.authorId != previousAuthorId
            previousAuthorId = message.authorId
            message.copy(showAuthorHeader = show)
        }
    }
}
