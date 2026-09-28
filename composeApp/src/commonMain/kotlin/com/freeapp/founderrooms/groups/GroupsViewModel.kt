package com.freeapp.founderrooms.groups

import com.freeapp.founderrooms.auth.AuthRepository
import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.BaseViewModel
import com.freeapp.founderrooms.tiers.Tier
import com.freeapp.founderrooms.tiers.TiersRepository
import com.russhwolf.settings.Settings

data class GroupsUiState(
    val me: Me? = null,
    val tiers: List<Tier> = emptyList(),
    val isLoading: Boolean = true,
    val showVerifyBanner: Boolean = false,
    val error: ErrorState? = null,
)

/**
 * Ported from `design/mockups/01-groups.html`. No one-shot effect type here:
 * every row tap is an instant, synchronous navigation the caller already owns
 * (see `GroupsScreen`'s `onOpenChat` / `onOpenLocked` / `onOpenVerify`
 * callbacks) -- unlike Auth's Continue button, which navigates only after an
 * async call resolves and needs the effect channel for that.
 *
 * The verify banner's dismissal is a UI preference, not a domain concept, so
 * it is persisted here directly with `multiplatform-settings`, the same
 * `Settings()` usage as `core.auth.SettingsTokenStore`, rather than routed
 * through [TiersRepository] or [AuthRepository].
 */
class GroupsViewModel(
    private val authRepository: AuthRepository,
    private val tiersRepository: TiersRepository,
    private val settings: Settings = Settings(),
) : BaseViewModel<GroupsUiState, Nothing>(GroupsUiState()) {

    init {
        load()
    }

    fun load() = tryToExecute(
        call = {
            val me = authRepository.currentSession() ?: error("No active session")
            me to tiersRepository.getTiers(me = me)
        },
        onSuccess = { (me, tiers) ->
            updateState {
                it.copy(
                    me = me,
                    tiers = tiers,
                    isLoading = false,
                    showVerifyBanner = shouldShowVerifyBanner(isVerified = me.verifiedMrr != null),
                    error = null,
                )
            }
        },
        onError = { error -> updateState { it.copy(isLoading = false, error = error) } },
    )

    fun onDismissVerifyBanner() {
        settings.putBoolean(KEY_DISMISSED, true)
        updateState { it.copy(showVerifyBanner = false) }
    }

    /**
     * FLOWS.md #6: a dismissal survives an app kill, but only while the user
     * stays unverified. The moment `verifiedMrr` is observed non-null and
     * later reverts to null, the earlier dismissal is cleared so the banner
     * comes back, per that flow's "returns if verifiedMrr becomes null again".
     */
    private fun shouldShowVerifyBanner(isVerified: Boolean): Boolean {
        val wasVerified = settings.getBoolean(KEY_WAS_VERIFIED, false)
        if (wasVerified && !isVerified) {
            settings.putBoolean(KEY_DISMISSED, false)
        }
        settings.putBoolean(KEY_WAS_VERIFIED, isVerified)
        return !isVerified && !settings.getBoolean(KEY_DISMISSED, false)
    }

    private companion object {
        const val KEY_DISMISSED = "groups.verify_banner_dismissed"
        const val KEY_WAS_VERIFIED = "groups.verify_banner_was_verified"
    }
}
