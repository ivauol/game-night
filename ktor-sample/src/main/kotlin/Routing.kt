package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.pebble.respondTemplate

import io.ktor.http.Parameters
import io.ktor.server.request.receiveParameters

import io.ktor.server.http.content.*
import io.ktor.server.request.*


fun Application.configureRouting() {
    routing {
        get("/") {
            //call.displayHome()
            call.displayBoard()
            call.respondText("Hello World!")
        }
    }
}
 
private suspend fun ApplicationCall.displayHome() {
    respondTemplate("base.peb", model=emptyMap())
}

private suspend fun ApplicationCall.displayBoard() {
    //val toPrint = boardString()
    val printThis = "hello"
    //val check = getBoardDetails(receiveParameters())
    respondTemplate("board.peb", model = mapOf(
        "printThis" to printThis
    ))
}

private fun getBoardDetails(params: Parameters) = params["string"] ?: error("No board")