package com.freeapp.founderrooms

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.freeapp.founderrooms.auth.AuthRepository
import com.freeapp.founderrooms.auth.AuthScreen
import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.chat.ChatScreen
import com.freeapp.founderrooms.chat.ChatViewModel
import com.freeapp.founderrooms.core.ui.SystemBackHandler
import com.freeapp.founderrooms.groups.GroupsScreen
import com.freeapp.founderrooms.locked.LockedScreen
import com.freeapp.founderrooms.locked.LockedViewModel
import com.freeapp.founderrooms.verify.VerifyScreen
import com.freeapp.founderrooms.verify.VerifyViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The FounderRooms nav graph. Plain Compose state, not a navigation library:
 * none is in `gradle/libs.versions.toml` and this pass's brief says not to
 * add one, so a `sealed class Screen` plus a `when` stands in for a back
 * stack. The Android hardware back button is not wired to it -- a known v1
 * gap, not something this pass attempts.
 */
private sealed class Screen {
    data object Auth : Screen()
    data object Groups : Screen()
    data class Chat(val tierId: String, val me: Me) : Screen()
    data class Locked(val tierId: String, val me: Me) : Screen()
    data class Verify(val me: Me) : Screen()
}

// Mirrors shared's `tiers.PUBLIC_TIER_ID`, which is `internal` to that module
// and so not visible here; see that file's own doc comment for the seed-data
// assumption this id rests on.
private const val PUBLIC_TIER_ID = "public"

/**
 * Entry point, unchanged in signature: `MainActivity.setContent { App() }`
 * still calls this with zero arguments.
 */
@Composable
@Preview
fun App() {
    MaterialTheme {
        val authRepository: AuthRepository = koinInject()

        // KNOWN GAP, flagged for the delivery report rather than worked around:
        // AuthRepository.currentSession() is in-memory only (see its own doc
        // comment) -- there is no `GET /me` and TokenStore's persisted session
        // is never rehydrated into a `Me`. So this only sends a returning user
        // straight to Groups while the process itself stayed alive (backgrounding,
        // a configuration change); a true cold start after the process was
        // killed always lands on Auth even with a valid refresh token sitting in
        // TokenStore. Fixing that needs a change in `shared/`, out of this
        // pass's scope (composeApp/commonMain only).
        var screen by remember {
            mutableStateOf<Screen>(
                if (authRepository.currentSession() != null) Screen.Groups else Screen.Auth
            )
        }

        when (val current = screen) {
            is Screen.Auth -> AuthScreen(onAuthenticated = { screen = Screen.Groups })

            is Screen.Groups -> GroupsScreen(
                onOpenChat = { tierId ->
                    // `me` is resolved here, inside the tap handler, not read
                    // during composition: it becomes part of the new Screen
                    // value rather than something re-fetched (and possibly
                    // mutated into `screen`) mid-recomposition.
                    screen = authRepository.currentSession()?.let { Screen.Chat(tierId, it) } ?: Screen.Auth
                },
                onOpenLocked = { tierId ->
                    screen = authRepository.currentSession()?.let { Screen.Locked(tierId, it) } ?: Screen.Auth
                },
                onOpenVerify = {
                    screen = authRepository.currentSession()?.let { Screen.Verify(it) } ?: Screen.Auth
                },
                onLogOut = {
                    authRepository.logOut()
                    screen = Screen.Auth
                },
            )

            is Screen.Chat -> {
                SystemBackHandler(onBack = { screen = Screen.Groups })
                val owner = rememberScreenViewModelStoreOwner(current)
                val chatViewModel: ChatViewModel = koinViewModel(
                    viewModelStoreOwner = owner,
                    parameters = { parametersOf(current.tierId, current.me) },
                )
                ChatScreen(viewModel = chatViewModel, onBack = { screen = Screen.Groups })
            }

            is Screen.Locked -> {
                SystemBackHandler(onBack = { screen = Screen.Groups })
                val owner = rememberScreenViewModelStoreOwner(current)
                val lockedViewModel: LockedViewModel = koinViewModel(
                    viewModelStoreOwner = owner,
                    parameters = { parametersOf(current.tierId, current.me) },
                )
                LockedScreen(
                    viewModel = lockedViewModel,
                    onVerify = { screen = Screen.Verify(current.me) },
                    onBack = { screen = Screen.Groups },
                    // Design doc section 5 ("LockedScreen.kt ... back link
                    // returns to Public chat") and FLOWS.md step 4/8 are
                    // explicit that this text link is a distinct affordance
                    // from the header's back chevron above: it jumps straight
                    // into the Public tier's chat rather than popping to
                    // Groups, so it reuses `current.me` rather than [onBack].
                    onOpenPublic = { screen = Screen.Chat(PUBLIC_TIER_ID, current.me) },
                )
            }

            is Screen.Verify -> {
                SystemBackHandler(onBack = { screen = Screen.Groups })
                val owner = rememberScreenViewModelStoreOwner(current)
                val verifyViewModel: VerifyViewModel = koinViewModel(
                    viewModelStoreOwner = owner,
                    parameters = { parametersOf(current.me) },
                )
                VerifyScreen(viewModel = verifyViewModel, onBack = { screen = Screen.Groups })
            }
        }
    }
}

/**
 * A fresh [ViewModelStore] per nav entry, keyed on the [Screen] value itself,
 * instead of `koinViewModel()`'s default (the Activity-wide store `setContent`
 * installs). That default caches a ViewModel by class/qualifier only, not by
 * the `parametersOf` passed to it: a second visit to Chat (or Locked, or
 * Verify) with a different `tierId` would silently hand back the FIRST
 * instance -- wrong tier's data -- and, since the first instance's
 * `onCleared()` would never run, its `viewModelScope` (and, for Chat, the
 * open `ChatRepository.live()` websocket) would never cancel either. Scoping
 * to a store that is cleared when this composable leaves the `when` (the
 * user navigates away) fixes both: a correct new instance per tier, and the
 * old instance's coroutines actually cancelled.
 */
@Composable
private fun rememberScreenViewModelStoreOwner(key: Screen): ViewModelStoreOwner {
    val owner = remember(key) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    return owner
}
