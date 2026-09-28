package com.freeapp.founderrooms.chat

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Keyed session map (design doc decision 5): pairs each tier room's
 * WebSocket producer (ChatRoutes' POST handler broadcasting a new message)
 * with its consumers (every open socket in that room), so a message sent by
 * one client reaches every other open session without a pub/sub broker. One
 * process-wide instance, registered as a Koin singleton.
 *
 * Backed by a ConcurrentHashMap of concurrent sets rather than a Mutex:
 * Netty dispatches each connection's coroutines on its own worker thread, so
 * join/leave/broadcast can race across sessions, and a lock-free structure
 * avoids serializing broadcasts behind whichever session is joining/leaving
 * at that instant.
 */
class ChatSessionRegistry {
    private val sessionsByTier = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    fun join(tierId: String, session: DefaultWebSocketServerSession) {
        val sessions = sessionsByTier.computeIfAbsent(tierId) {
            Collections.newSetFromMap(ConcurrentHashMap())
        }
        sessions.add(session)
    }

    fun leave(tierId: String, session: DefaultWebSocketServerSession) {
        sessionsByTier[tierId]?.remove(session)
    }

    /**
     * Sends [json] to every session currently in [tierId]'s set. A session
     * whose send fails (client vanished without a clean close) is dropped
     * from the set instead of aborting the loop, so one dead socket cannot
     * block delivery to the rest of the room.
     */
    suspend fun broadcast(tierId: String, json: String) {
        val sessions = sessionsByTier[tierId] ?: return
        for (session in sessions.toList()) {
            runCatching { session.send(Frame.Text(json)) }
                .onFailure { sessions.remove(session) }
        }
    }
}
