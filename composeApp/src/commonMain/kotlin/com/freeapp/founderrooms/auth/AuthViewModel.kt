package com.freeapp.founderrooms.auth

import com.freeapp.founderrooms.core.error.ErrorState
import com.freeapp.founderrooms.core.ui.BaseViewModel

/**
 * Ported from `design/mockups/05-auth.html`. The mockup draws the log-in
 * copy ("New here? Create an account"), so [AuthMode.LogIn] is the default;
 * signing up is reached by tapping the switch line (see [onToggleMode]).
 *
 * [AuthRepository] and [Me] live in the `:shared` module, package
 * `com.freeapp.founderrooms.auth` (design doc section 5), built in parallel
 * with this file; resolved here by that shared package name, no import
 * needed. If that module ever moves to a different package this file will
 * fail to compile, which is the point: no silent mismatch.
 */
enum class AuthMode { SignUp, LogIn }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LogIn,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    // Client-side "you left a field blank" hint. Kept separate from [error]
    // so it never collides with server-sourced copy (see AuthScreen's
    // authErrorMessage): one is a local nudge, the other is a real failure.
    val fieldHint: String? = null,
    val error: ErrorState? = null,
)

/** One-shot: fired once sign-up/log-in succeeds. The caller decides where that goes. */
sealed interface AuthEffect {
    data object Authenticated : AuthEffect
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<AuthUiState, AuthEffect>(AuthUiState()) {

    fun onEmailChange(value: String) = updateState { it.copy(email = value, fieldHint = null, error = null) }

    fun onPasswordChange(value: String) = updateState { it.copy(password = value, fieldHint = null, error = null) }

    fun onToggleMode() = updateState {
        it.copy(
            mode = if (it.mode == AuthMode.LogIn) AuthMode.SignUp else AuthMode.LogIn,
            fieldHint = null,
            error = null,
        )
    }

    fun onContinue() {
        val current = state.value
        if (current.isLoading) return

        val email = current.email.trim()
        val password = current.password
        if (email.isEmpty() || password.isEmpty()) {
            // A tap that does nothing visible is a dead button; say what's missing.
            updateState { it.copy(fieldHint = "Enter your email and password.") }
            return
        }

        updateState { it.copy(isLoading = true, fieldHint = null, error = null) }
        tryToExecute(
            call = {
                when (current.mode) {
                    // No display-name field is drawn in 05-auth.html; the signup
                    // contract (design doc section 5) requires one, so it is
                    // derived from the email's local part. Flagged in the
                    // delivery report as an assumption, not asked as a question.
                    AuthMode.SignUp -> authRepository.signUp(
                        email = email,
                        password = password,
                        displayName = displayNameFrom(email),
                    ).getOrThrow()
                    AuthMode.LogIn -> authRepository.logIn(
                        email = email,
                        password = password,
                    ).getOrThrow()
                }
            },
            onSuccess = {
                updateState { it.copy(isLoading = false) }
                sendEffect(AuthEffect.Authenticated)
            },
            onError = { error -> updateState { it.copy(isLoading = false, error = error) } },
        )
    }

    private fun displayNameFrom(email: String): String =
        email.substringBefore('@').ifBlank { "Founder" }
}
