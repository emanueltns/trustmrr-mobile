package com.freeapp.founderrooms.auth

import com.freeapp.founderrooms.api.ApiClient
import com.freeapp.founderrooms.core.auth.TokenStore
import kotlinx.coroutines.CancellationException

/**
 * Sign up, log in, hold the session (design doc section 5, "AuthRepository").
 *
 * `signUp`/`logIn` return `Result<Me>` rather than letting the call throw,
 * per the design doc's explicit signature: this is a narrow, deliberate
 * exception to this template's usual rule that repositories let exceptions
 * propagate to the base ViewModel's `tryToExecute` (see core/error/ErrorState.kt).
 * `CancellationException` is rethrown, never wrapped, so cancelling the
 * screen does not look like a failed login.
 */
class AuthRepository(
    private val api: ApiClient,
    private val tokenStore: TokenStore,
) {
    // In-memory only: there is no `GET /me` in v1 (see currentSession below).
    private var cachedMe: Me? = null

    suspend fun signUp(email: String, password: String, displayName: String): Result<Me> =
        authenticate { api.signUp(email, password, displayName) }

    suspend fun logIn(email: String, password: String): Result<Me> =
        authenticate { api.logIn(email, password) }

    fun logOut() {
        tokenStore.clear()
        cachedMe = null
    }

    /**
     * The signed-in user, cached in memory since the last `signUp`/`logIn` in
     * this process.
     *
     * This is NOT re-derived from the server: v1 has no `GET /me` endpoint
     * (design doc section 5 lists only signup/login/refresh), so a cold
     * process restart returns null here even though `TokenStore` still holds
     * a valid session, until the user logs in again. The null check against
     * `tokenStore.read()` only guards against the opposite drift, a stale
     * cached `Me` surviving a logout or a forced-clear after a refresh
     * failure (see core/auth/TokenRefresh.kt); it cannot restore a profile
     * TokenStore never told it about.
     */
    fun currentSession(): Me? = if (tokenStore.read() == null) null else cachedMe

    private suspend fun authenticate(call: suspend () -> AuthResponse): Result<Me> =
        try {
            val response = call()
            tokenStore.save(response.tokens)
            cachedMe = response.me
            Result.success(response.me)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
