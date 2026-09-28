package com.freeapp.founderrooms.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {
        post("/signup") {
            val body = call.receive<SignupRequest>()
            if (body.email.isBlank() || body.password.isBlank() || body.displayName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email, password and displayName are required"))
                return@post
            }
            when (val result = authService.signup(body)) {
                is SignupResult.EmailTaken ->
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "email already in use"))
                is SignupResult.Success ->
                    call.respond(HttpStatusCode.Created, AuthResponse(tokens = result.tokens, me = result.me))
            }
        }

        post("/login") {
            val body = call.receive<LoginRequest>()
            when (val result = authService.login(body)) {
                is LoginResult.InvalidCredentials ->
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid email or password"))
                is LoginResult.Success ->
                    call.respond(HttpStatusCode.OK, AuthResponse(tokens = result.tokens, me = result.me))
            }
        }

        post("/refresh") {
            // The refresh token travels in the JSON body, not an Authorization
            // header: HttpTokenRefresher (shared/.../core/auth/TokenRefresh.kt)
            // posts {refreshToken} on a bare client with no bearer header set.
            // So this route validates the token directly against JwtService's
            // refresh verifier instead of sitting behind authenticate("refresh").
            // The "refresh" authentication provider is still installed in
            // Application.kt, against this same verifier, for any future route
            // that does carry the refresh token as a bearer header.
            val body = call.receive<RefreshRequest>()
            when (val result = authService.refresh(body.refreshToken)) {
                is RefreshResult.InvalidToken ->
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid or stale refresh token"))
                is RefreshResult.Success ->
                    // Flat AuthTokens, no {tokens: ...} wrapper: HttpTokenRefresher
                    // decodes this response straight into AuthTokens.
                    call.respond(HttpStatusCode.OK, result.tokens)
            }
        }
    }
}
