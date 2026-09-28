package com.freeapp.founderrooms.chat

import com.freeapp.founderrooms.auth.AuthService
import com.freeapp.founderrooms.auth.Me
import com.freeapp.founderrooms.auth.userId
import com.freeapp.founderrooms.tiers.Tier
import com.freeapp.founderrooms.tiers.TiersRepository
import com.freeapp.founderrooms.tiers.unlocks
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.chatRoutes() {
    val tiersRepository by inject<TiersRepository>()
    val authService by inject<AuthService>()
    val chatService by inject<ChatService>()
    val sessions by inject<ChatSessionRegistry>()
    val json = Json

    route("/chat/{tierId}") {
        authenticate("access") {
            get("/messages") {
                val tierId = call.parameters["tierId"]!!
                val tier = tiersRepository.findById(tierId)
                if (tier == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "no such tier"))
                    return@get
                }
                val me = authService.me(call.principal<JWTPrincipal>()!!.userId)
                if (me == null || !unlocks(tier, me.verifiedMrr)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "not unlocked for this tier"))
                    return@get
                }
                call.respond(chatService.history(tierId))
            }

            post("/messages") {
                val tierId = call.parameters["tierId"]!!
                val tier = tiersRepository.findById(tierId)
                if (tier == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "no such tier"))
                    return@post
                }
                val me = authService.me(call.principal<JWTPrincipal>()!!.userId)
                // F4 (design doc): re-checked here regardless of what the
                // client shows, even though a locked room is hidden client-side.
                if (me == null || !unlocks(tier, me.verifiedMrr)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "not unlocked for this tier"))
                    return@post
                }
                val body = call.receive<SendMessageRequest>()
                if (body.body.isBlank() && body.linkUrl.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "body or linkUrl is required"))
                    return@post
                }
                val message = chatService.send(tierId, me.id, body.body, body.linkUrl)
                sessions.broadcast(tierId, json.encodeToString(Message.serializer(), message))
                call.respond(HttpStatusCode.Created, message)
            }

            webSocket("/socket") {
                val tierId = call.parameters["tierId"]!!
                val tier: Tier? = tiersRepository.findById(tierId)
                val me: Me? = call.principal<JWTPrincipal>()?.let { authService.me(it.userId) }

                if (tier == null || me == null || !unlocks(tier, me.verifiedMrr)) {
                    // Nothing registered yet, so `finally` below never runs
                    // for this connection: close and return before join().
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "not unlocked"))
                    return@webSocket
                }

                sessions.join(tierId, this)
                try {
                    // Push-only channel: ChatRepository.live() on the client
                    // never sends frames here, so this loop only exists to
                    // suspend until the client disconnects.
                    for (frame in incoming) {
                        // no-op: incoming frames are drained, not acted on
                    }
                } finally {
                    sessions.leave(tierId, this)
                }
            }
        }
    }
}
