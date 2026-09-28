package com.freeapp.founderrooms.verify

import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.BaseViewModel
import com.freeapp.founderrooms.tiers.Tier
import com.freeapp.founderrooms.tiers.TiersRepository

/**
 * The Connect-Stripe button's own small state machine. Kept out of a single
 * `Boolean isLoading` because idle, in-flight and stub-pending each render
 * differently (design doc decision 3 and section 6's runtime sequence).
 */
sealed interface StripeConnectState {
    data object Idle : StripeConnectState
    data object Connecting : StripeConnectState
    data object Pending : StripeConnectState
}

data class VerifyUiState(
    val privateTiers: List<Tier> = emptyList(),
    val isLoadingTiers: Boolean = true,
    val connectState: StripeConnectState = StripeConnectState.Idle,
    val error: ErrorState? = null,
)

sealed interface VerifyEffect

/**
 * ASSUMPTION / compile risk: same `me: Me` note as [com.freeapp.founderrooms.chat.ChatViewModel]
 * — see that file for the full explanation. Not part of this task's declared
 * shared dependencies; assumed to live at `com.freeapp.founderrooms.auth.Me`.
 */
class VerifyViewModel(
    private val tiersRepository: TiersRepository,
    private val verifyRepository: VerifyRepository,
    private val me: Me,
) : BaseViewModel<VerifyUiState, VerifyEffect>(VerifyUiState()) {

    init {
        loadTiers()
    }

    private fun loadTiers() = tryToExecute(
        call = { tiersRepository.getTiers(me) },
        onSuccess = { tiers ->
            // Every private tier from the data, not just the 4 the mockup card drew:
            // the seed table defines 5 (up to 👑 $1M+); dropping one because the
            // mockup's card only had room for 4 rows would hide real tiers.
            val private = tiers.filter { it.mrrFloor > 0.0 }.sortedBy { it.mrrFloor }
            updateState { it.copy(privateTiers = private, isLoadingTiers = false) }
        },
        onError = { error -> updateState { it.copy(isLoadingTiers = false, error = error) } },
    )

    fun onConnectStripe() {
        if (state.value.connectState == StripeConnectState.Connecting) return
        updateState { it.copy(connectState = StripeConnectState.Connecting, error = null) }
        tryToExecute(
            call = { verifyRepository.startVerification() },
            // Decision 3: the stub always returns pending and never changes
            // verifiedMrr, so the returned value itself carries no branch —
            // reaching this callback at all means "pending", stated inline,
            // never a spinner that never resolves (design doc section 6).
            onSuccess = { updateState { it.copy(connectState = StripeConnectState.Pending) } },
            onError = { error -> updateState { it.copy(connectState = StripeConnectState.Idle, error = error) } },
        )
    }
}
