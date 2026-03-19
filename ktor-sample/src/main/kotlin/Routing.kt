package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.pebble.respondTemplate

import io.ktor.server.http.content.*
import io.ktor.server.request.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.displayHome()
            call.respondText("Hello World!")
        }
    }
}
 
private suspend fun ApplicationCall.displayHome() {
    respondTemplate("base.peb", model=emptyMap())
}