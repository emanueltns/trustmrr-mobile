package com.freeapp.founderrooms.auth

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

/**
 * Field names must match the client's existing `AuthTokens` byte for byte:
 * `shared/.../core/auth/TokenStore.kt` deserializes this shape directly, and
 * `HttpTokenRefresher` decodes `POST /auth/refresh`'s response straight into
 * it with no wrapper. Signup and login wrap this inside [AuthResponse]
 * alongside [Me]; refresh returns it bare (see AuthRoutes.kt).
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class Me(
    val id: Long,
    val email: String,
    val displayName: String,
    val verifiedMrr: Double?,
)

@Serializable
data class AuthResponse(
    val tokens: AuthTokens,
    val me: Me,
)
