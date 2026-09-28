package com.freeapp.founderrooms.auth

import io.ktor.server.auth.jwt.JWTPrincipal

/** The `sub` claim [JwtService] mints is always the numeric user id. */
val JWTPrincipal.userId: Long
    get() = payload.subject.toLong()
