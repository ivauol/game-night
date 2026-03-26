package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.pebble.respondTemplate

import io.ktor.http.Parameters
import io.ktor.server.request.receiveParameters

import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.http.*

import io.ktor.server.pebble.*
import io.ktor.server.response.*

fun Application.configureRouting() {
    val gameManager = GameManager()
    routing {
        get("/") {
            //call.displayHome()
            call.displayBoard()
            call.respondText("Hello World!")
        }

        //load the game
        get("/game"){
            //test ids
            val gameId = 3
            val playerId = 1

            //make the game
            val game = gameManager.createGame(gameId, playerId)
            if (game == null){return@get call.respond(HttpStatusCode.BadRequest, "Game not found")}

            //create a 2d array of strings for the board
            val board2D = Array(8) { x ->
                Array(8) { y ->
                    game.board[x][y]?.let {
                        if (it.king) it.colour[0].uppercase() else it.colour[0].lowercase()
                    } ?: " "
                }.toList()
            }.toList()

            call.respond(PebbleContent("test.peb", mapOf("board" to board2D, "game_id" to gameId, "player_id" to playerId)))
        }

        //to make a move on a board
        post("/move"){
            //get the values required
            val params = call.receiveParameters()
            val gameId = params["game_id"]?.toIntOrNull() ?: return@post call.respondText("Invalid game_id")
            val playerId = params["player_id"]?.toIntOrNull() ?: return@post call.respondText("Invalid player_id")
            val moveInput = params["move"] ?: return@post call.respondText("Move not provided")

            //convert from standard move notation to a list of positions
            val moveList = moveInput.split(",").map { square ->
                val col = square[0] - 'A'
                val row = square[1].digitToInt() - 1
                arrayOf(row, col)
            }.toMutableList()

            val response = GameManager().makeMove(gameId, playerId, moveList)

            //reload the board if a success
            if (response.success) {
                call.respondRedirect("/game")
            } else {
                call.respondText(response.message)
            }
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