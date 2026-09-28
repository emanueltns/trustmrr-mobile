package com.freeapp.founderrooms.chat

import com.freeapp.founderrooms.api.ApiClient
import com.freeapp.founderrooms.core.auth.TokenStore
import com.freeapp.founderrooms.core.config.NetworkConfig
import com.freeapp.founderrooms.core.data.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * History + live feed + send for one tier room (design doc section 5,
 * "ChatRepository"), one instance shared for the app's lifetime, opening a
 * fresh socket per [live] call.
 *
 * [live] opens its own [HttpClient] rather than reusing [ApiClient]'s
 * private one: [ApiClient]'s client carries `installTokenRefresh`, an
 * interceptor built to retry a failed *REST* request, but a WebSocket
 * upgrade is a one-shot handshake it cannot replay the way TokenRefresh.kt's
 * own doc comment describes for ordinary requests. This client attaches the
 * current access token to the handshake itself and does NOT refresh it on a
 * 401: callers should invoke [history] (through [ApiClient], which does
 * refresh) right before [live], so the token used for the handshake is
 * fresh. A drop mid-session (design doc F5) is the caller's job to notice
 * and reconnect by collecting [live] again; missed messages are backfilled
 * by the next [history] call.
 */
class ChatRepository(
    private val api: ApiClient,
    private val tokenStore: TokenStore,
    private val socketClient: HttpClient = HttpClient(getEngine()) {
        install(WebSockets)
    },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun history(tierId: String): List<Message> = api.getChatHistory(tierId)

    suspend fun send(tierId: String, body: String, linkUrl: String?): Message =
        api.sendMessage(tierId, body, linkUrl)

    /**
     * Pushes every message posted to [tierId] after the socket opens.
     * Closes the socket when the collecting coroutine is cancelled (screen
     * leaves composition), per design doc decision 5.
     */
    fun live(tierId: String): Flow<Message> = callbackFlow {
        val session = socketClient.webSocketSession(urlString = socketUrl(tierId)) {
            tokenStore.read()?.let { header(HttpHeaders.Authorization, "Bearer ${it.accessToken}") }
        }

        val reader = launch {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    runCatching { json.decodeFromString<Message>(frame.readText()) }
                        .onSuccess { trySend(it) }
                }
            }
            close()
        }

        awaitClose {
            reader.cancel()
            session.cancel()
        }
    }

    private fun socketUrl(tierId: String): String =
        NetworkConfig.BASE_URL.replaceFirst("http", "ws") + "/chat/$tierId/socket"
}
