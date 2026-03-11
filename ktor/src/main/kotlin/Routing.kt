package com.example

import io.ktor.http.Parameters
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.pebble.respondTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.pebble.*
import io.ktor.server.response.*

fun Application.configureRouting(){
    routing {
        get("/") { call.displayForm() }
    }
} 

private suspend fun ApplicationCall.displayForm(){
    respondTemplate("base.peb", model= emptyMap<String, String>())
}
