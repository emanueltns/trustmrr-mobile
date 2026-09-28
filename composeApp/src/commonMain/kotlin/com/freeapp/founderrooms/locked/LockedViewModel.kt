package com.freeapp.founderrooms.locked

import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.BaseViewModel
import com.freeapp.founderrooms.tiers.Tier
import com.freeapp.founderrooms.tiers.TiersRepository

data class LockedUiState(
    val tier: Tier? = null,
    val isLoading: Boolean = true,
    val error: ErrorState? = null,
)

/**
 * Backs `design/mockups/04-locked.html`. Read-only: the tapped tier's own
 * data (emoji, band, member count, lock state), nothing mutated here. Every
 * tap the mockup draws (CTA, back chevron, "back to Public" link) is a plain
 * navigation callback the screen calls directly, the same shape as
 * `GroupsScreen`'s row taps -- so, like [com.freeapp.founderrooms.groups.GroupsViewModel],
 * this has no effect channel of its own (`Nothing` for the effect type).
 *
 * Same parametrized-constructor shape as [com.freeapp.founderrooms.chat.ChatViewModel]
 * and [com.freeapp.founderrooms.verify.VerifyViewModel]: `tierId` and `me`
 * are nav-time values, not Koin singletons.
 */
class LockedViewModel(
    private val tiersRepository: TiersRepository,
    private val tierId: String,
    private val me: Me,
) : BaseViewModel<LockedUiState, Nothing>(LockedUiState()) {

    init {
        loadTier()
    }

    private fun loadTier() = tryToExecute(
        call = { tiersRepository.getTiers(me) },
        onSuccess = { tiers ->
            val tier = tiers.firstOrNull { it.id == tierId }
            updateState { it.copy(tier = tier, isLoading = false) }
        },
        onError = { error -> updateState { it.copy(isLoading = false, error = error) } },
    )
}
