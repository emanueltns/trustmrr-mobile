package com.freeapp.founderrooms

import com.freeapp.founderrooms.auth.FounderSeeder
import com.freeapp.founderrooms.auth.JwtService
import com.freeapp.founderrooms.auth.authRoutes
import com.freeapp.founderrooms.chat.chatRoutes
import com.freeapp.founderrooms.config.DatabaseFactory
import com.freeapp.founderrooms.di.serverModule
import com.freeapp.founderrooms.health.healthRoutes
import com.freeapp.founderrooms.items.itemsRoutes
import com.freeapp.founderrooms.tiers.tiersRoutes
import com.freeapp.founderrooms.verify.verifyRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val log = LoggerFactory.getLogger("com.freeapp.founderrooms.Application")

fun Application.module() {
    DatabaseFactory.init(environment.config)

    val jwtSecret = environment.config.property("jwt.secret").getString()

    install(Koin) {
        slf4jLogger()
        modules(serverModule(jwtSecret))
    }
    // Pulled from the Koin container right after install, rather than built
    // a second time here: one JwtService instance backs both the
    // Authentication config below and every other injection site (AuthService,
    // ChatRoutes' websocket auth), all sharing the same secret.
    val jwtService: JwtService by inject()
    val founderSeeder: FounderSeeder by inject()

    install(DefaultHeaders)
    install(CallLogging) { level = Level.INFO }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }
    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "unknown")))
        }
    }
    install(Authentication) {
        jwt("access") {
            verifier(jwtService.accessVerifier)
            validate { credential -> JWTPrincipal(credential.payload) }
        }
        jwt("refresh") {
            verifier(jwtService.refreshVerifier)
            validate { credential -> JWTPrincipal(credential.payload) }
        }
    }
    install(WebSockets)

    // One-time demo seed (see FounderSeeder.kt's doc comment): runs at
    // startup so v1 launches with real members already in the four private
    // rooms. module() itself is not suspend, and this needs to finish before
    // the app starts serving traffic, so runBlocking here (not launched into
    // the background) is the deliberate choice, not an oversight.
    runBlocking { founderSeeder.seedIfNeeded() }

    routing {
        get("/") { call.respondText("kmp-ktor-template server is up") }
        healthRoutes()
        itemsRoutes()
        authRoutes()
        tiersRoutes()
        chatRoutes()
        verifyRoutes()
        // EXTENSION POINT: mount new feature routes here (e.g., booksRoutes()).
    }
}
