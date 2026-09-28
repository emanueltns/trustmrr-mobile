package com.freeapp.founderrooms.verify

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Deliberate permanent stub (design doc decision 3), not a TODO to fill in:
 * the real Stripe read-only OAuth flow needs owner API keys and is a later
 * phase. This never reads or writes `verifiedMrr`: no VerifyService, no
 * table, on purpose, so there is nothing here that could accidentally start
 * granting tier access.
 */
fun Route.verifyRoutes() {
    route("/verify") {
        authenticate("access") {
            post("/start") { call.respond(mapOf("status" to "pending")) }
        }
    }
}
