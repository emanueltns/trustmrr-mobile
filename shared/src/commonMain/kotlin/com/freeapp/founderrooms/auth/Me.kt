package com.freeapp.founderrooms.auth

import com.freeapp.founderrooms.core.auth.AuthTokens
import kotlinx.serialization.Serializable

/**
 * EXTENSION POINT: mirror this file for new domain entities.
 * Field names must match the server-side auth DTOs byte for byte, exactly
 * like every other DTO in this template (see items/Item.kt).
 *
 * ASSUMPTION: `id` is a Long, matching the `Item.id` convention elsewhere in
 * this template (a server-generated auto-increment primary key).
 */
@Serializable
data class Me(
    val id: Long,
    val email: String,
    val displayName: String,
    val verifiedMrr: Double? = null,
)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LogInRequest(
    val email: String,
    val password: String,
)

/** Wire shape of `POST /auth/signup` and `POST /auth/login`: `{tokens, me}`. */
@Serializable
data class AuthResponse(
    val tokens: AuthTokens,
    val me: Me,
)
