package com.freeapp.founderrooms.api

import com.freeapp.founderrooms.auth.AuthResponse
import com.freeapp.founderrooms.auth.LogInRequest
import com.freeapp.founderrooms.auth.SignUpRequest
import com.freeapp.founderrooms.chat.Message
import com.freeapp.founderrooms.chat.SendMessageRequest
import com.freeapp.founderrooms.core.auth.HttpTokenRefresher
import com.freeapp.founderrooms.core.auth.TokenRefresher
import com.freeapp.founderrooms.core.auth.TokenStore
import com.freeapp.founderrooms.core.auth.installTokenRefresh
import com.freeapp.founderrooms.core.config.NetworkConfig
import com.freeapp.founderrooms.core.data.getEngine
import com.freeapp.founderrooms.items.CreateItemRequest
import com.freeapp.founderrooms.items.Item
import com.freeapp.founderrooms.tiers.TierDto
import com.freeapp.founderrooms.verify.VerifyStatusResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Single entry-point for talking to the Ktor backend.
 *
 * EXTENSION POINT: add new endpoint methods here when adding new entities.
 * Keep one repository per domain rather than overloading this class with
 * unrelated business logic.
 */
class ApiClient(
    private val httpClient: HttpClient = defaultClient(),
) {
    /** Smoke test: hits the server root, no DB dependency. */
    suspend fun pingServer(): Boolean = runCatching {
        httpClient.get(NetworkConfig.BASE_URL).status.value in 200..299
    }.getOrDefault(false)

    suspend fun getItems(): List<Item> =
        httpClient.get("${NetworkConfig.BASE_URL}/items").body()

    suspend fun createItem(name: String): Item =
        httpClient.post("${NetworkConfig.BASE_URL}/items") {
            contentType(ContentType.Application.Json)
            setBody(CreateItemRequest(name))
        }.body()

    suspend fun signUp(email: String, password: String, displayName: String): AuthResponse =
        httpClient.post("${NetworkConfig.BASE_URL}/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody(SignUpRequest(email, password, displayName))
        }.body()

    suspend fun logIn(email: String, password: String): AuthResponse =
        httpClient.post("${NetworkConfig.BASE_URL}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LogInRequest(email, password))
        }.body()

    suspend fun getTiers(): List<TierDto> =
        httpClient.get("${NetworkConfig.BASE_URL}/tiers").body()

    suspend fun getChatHistory(tierId: String): List<Message> =
        httpClient.get("${NetworkConfig.BASE_URL}/chat/$tierId/messages").body()

    suspend fun sendMessage(tierId: String, body: String, linkUrl: String?): Message =
        httpClient.post("${NetworkConfig.BASE_URL}/chat/$tierId/messages") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(body, linkUrl))
        }.body()

    suspend fun startVerification(): VerifyStatusResponse =
        httpClient.post("${NetworkConfig.BASE_URL}/verify/start").body()

    companion object {
        /**
         * EXTENSION POINT: pass a [TokenStore] once the API needs auth. The
         * client then sends the stored bearer token on every request and, on a
         * 401, refreshes it once and retries. With no store the client behaves
         * exactly as an unauthenticated one.
         */
        fun defaultClient(
            tokens: TokenStore? = null,
            refresher: TokenRefresher? = null,
        ): HttpClient = HttpClient(getEngine()) {
            // Needed so a non-2xx (401 on login, 409 on signup, 403 on a
            // locked-tier send) throws a ResponseException that ErrorMapper
            // already knows how to turn into the right ErrorState, instead
            // of `.body()` trying to decode an error payload as the success
            // DTO and surfacing a confusing InvalidResponse.
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging)
        }.apply {
            if (tokens != null) {
                installTokenRefresh(tokens, refresher ?: HttpTokenRefresher())
            }
        }
    }
}
