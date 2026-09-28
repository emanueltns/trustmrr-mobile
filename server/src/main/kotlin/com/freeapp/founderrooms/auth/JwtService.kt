package com.freeapp.founderrooms.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date

private const val ISSUER = "founderrooms"
private const val CLAIM_TYP = "typ"
private const val CLAIM_VER = "ver"
private const val TYP_ACCESS = "access"
private const val TYP_REFRESH = "refresh"
private const val ACCESS_TTL_MILLIS = 15L * 60 * 1000
private const val REFRESH_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000

/**
 * Mints and verifies the two JWT shapes decision 4 needs: a short-lived
 * access token (`typ=access`) and a rotating refresh token (`typ=refresh`,
 * carrying `ver` so a replayed old refresh token can be told apart from the
 * live one). One instance, one secret: built once in Application.module()
 * from `jwt.secret`, then reused by the Ktor Authentication install, by
 * AuthService, and by the websocket auth provider.
 */
class JwtService(secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)

    val accessVerifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withClaim(CLAIM_TYP, TYP_ACCESS)
        .build()

    val refreshVerifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withClaim(CLAIM_TYP, TYP_REFRESH)
        .build()

    fun issueAccessToken(userId: Long): String = JWT.create()
        .withIssuer(ISSUER)
        .withSubject(userId.toString())
        .withClaim(CLAIM_TYP, TYP_ACCESS)
        .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TTL_MILLIS))
        .sign(algorithm)

    fun issueRefreshToken(userId: Long, version: Int): String = JWT.create()
        .withIssuer(ISSUER)
        .withSubject(userId.toString())
        .withClaim(CLAIM_TYP, TYP_REFRESH)
        .withClaim(CLAIM_VER, version)
        .withExpiresAt(Date(System.currentTimeMillis() + REFRESH_TTL_MILLIS))
        .sign(algorithm)

    /** Null on any failure (bad signature, expired, wrong `typ`); callers map that to 401. */
    fun verifyAccessToken(token: String): DecodedJWT? = runCatching { accessVerifier.verify(token) }.getOrNull()

    /** Same as [verifyAccessToken] for the refresh claim. Does not check `ver`; AuthService does that against the DB. */
    fun verifyRefreshToken(token: String): DecodedJWT? = runCatching { refreshVerifier.verify(token) }.getOrNull()

    fun userIdOf(decoded: DecodedJWT): Long = decoded.subject.toLong()

    fun refreshVersionOf(decoded: DecodedJWT): Int = decoded.getClaim(CLAIM_VER).asInt()
}
