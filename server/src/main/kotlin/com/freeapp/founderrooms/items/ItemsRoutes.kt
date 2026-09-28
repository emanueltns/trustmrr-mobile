package com.freeapp.founderrooms.items

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.itemsRoutes() {
    val repo by inject<ItemsRepository>()

    route("/items") {
        get { call.respond(repo.all()) }

        post {
            val body = call.receive<CreateItemRequest>()
            if (body.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "name is required"))
                return@post
            }
            val created = repo.create(body.name.trim())
            call.respond(HttpStatusCode.Created, created)
        }
    }
}
