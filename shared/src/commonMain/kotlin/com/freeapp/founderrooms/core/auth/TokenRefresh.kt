package com.freeapp.founderrooms.core.auth

import com.freeapp.founderrooms.core.config.NetworkConfig
import com.freeapp.founderrooms.core.data.getEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Body of the refresh call. */
@Serializable
private data class RefreshTokenRequest(val refreshToken: String)

/** Exchanges a refresh token for a new pair, or null if the session is over. */
interface TokenRefresher {
    suspend fun refresh(refreshToken: String): AuthTokens?
}

/**
 * Calls the server's refresh endpoint.
 *
 * It owns a bare HttpClient on purpose: the refresh call must not travel
 * through the interceptor that triggered it, or a dead refresh token produces
 * an endless 401 loop.
 *
 * EXTENSION POINT: point [endpoint] at your own route, or replace this class
 * entirely if refreshing needs different credentials.
 */
class HttpTokenRefresher(
    private val endpoint: String = "${NetworkConfig.BASE_URL}/auth/refresh",
    private val client: HttpClient = HttpClient(getEngine()) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) : TokenRefresher {

    override suspend fun refresh(refreshToken: String): AuthTokens? = runCatching {
        client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }.body<AuthTokens>()
    }.getOrNull()
}

/**
 * Attaches the stored access token to every request and, on a 401, refreshes
 * the pair once and retries the original request once.
 *
 * A second 401 after a fresh token is a real 401: it is returned to the caller,
 * where the base ViewModel maps it to `ErrorState.Unauthorized` and the app
 * sends the user back to login.
 *
 * Constraint: the retry replays the request builder, so a request with a
 * one-shot streaming body cannot be retried this way. Requests carrying an
 * upload stream should be sent on a client without this interceptor.
 */
fun HttpClient.installTokenRefresh(
    tokens: TokenStore,
    refresher: TokenRefresher,
) {
    val refreshLock = Mutex()

    plugin(HttpSend).intercept { request ->
        val stored = tokens.read()
        if (stored != null) {
            request.headers[HttpHeaders.Authorization] = "Bearer ${stored.accessToken}"
        }

        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call

        val refreshed = refreshLock.withLock {
            val current = tokens.read() ?: return@withLock null
            // Parallel requests can pile up behind one 401. If another of them
            // already refreshed while this one waited for the lock, reuse that
            // result rather than spending the refresh token a second time.
            if (stored != null && current.accessToken != stored.accessToken) {
                current
            } else {
                refresher.refresh(current.refreshToken)?.also(tokens::save)
            }
        }

        if (refreshed == null) {
            tokens.clear()
            return@intercept call
        }

        request.headers[HttpHeaders.Authorization] = "Bearer ${refreshed.accessToken}"
        execute(request)
    }
}
