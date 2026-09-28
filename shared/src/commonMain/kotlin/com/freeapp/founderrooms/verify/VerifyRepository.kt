package com.freeapp.founderrooms.verify

import com.freeapp.founderrooms.api.ApiClient
import kotlinx.serialization.Serializable

/** Wire shape of `POST /verify/start`: always `{"status": "pending"}`. */
@Serializable
data class VerifyStatusResponse(
    val status: String,
)

/**
 * The verification stub, named and commented openly per design doc
 * decision 3: `startVerification` always returns "pending" and NEVER
 * changes `Me.verifiedMrr`. The real Stripe read-only OAuth flow is a later
 * phase that needs owner keys; this call exists only so the Verify screen
 * has something honest to show ("Verification is not live yet, you're still
 * in Public") instead of a spinner that never resolves or a silent no-op.
 */
class VerifyRepository(
    private val api: ApiClient,
) {
    suspend fun startVerification(): String = api.startVerification().status
}
