package com.freeapp.founderrooms.tiers

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.tiersRoutes() {
    val repo by inject<TiersRepository>()

    route("/tiers") {
        authenticate("access") {
            get { call.respond(repo.all()) }
        }
    }
}
