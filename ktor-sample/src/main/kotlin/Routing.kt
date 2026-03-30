package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.pebble.respondTemplate
import io.ktor.http.Parameters
import io.ktor.server.request.receiveParameters
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.*
import io.ktor.server.websocket.*

//store all online players for syncing
object WSConnections {
    private val sessions = mutableMapOf<Int, MutableList<DefaultWebSocketServerSession>>()

    //add a new websocket connection to a game
    fun add(gameId: Int, session: DefaultWebSocketServerSession) {
        val list = sessions.getOrPut(gameId) { mutableListOf() }
        list.add(session)
    }

    //remove a websocket connection from a game
    fun remove(gameId: Int, session: DefaultWebSocketServerSession) {
        sessions[gameId]?.remove(session)
        if (sessions[gameId]?.isEmpty() == true) {
            sessions.remove(gameId)
        }
    }

    //send a message to all online players in a game
    suspend fun broadcast(gameId: Int, message: String) {
        sessions[gameId]?.forEach {
            it.send(Frame.Text(message))
        }
    }
}

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

            call.respondTemplate("test.peb", mapOf("boardString" to game.boardState, "gameId" to gameId, "playerId" to playerId))
        }

        //to make a move on a board
        post("/move"){
            //get the values required
            val params = call.receiveParameters()
            val gameId = params["gameId"]?.toIntOrNull() ?: return@post call.respondText("Invalid game id")
            val playerId = params["playerId"]?.toIntOrNull() ?: return@post call.respondText("Invalid player id")
            val moveInput = params["move"] ?: return@post call.respondText("Move not provided")

            //convert from standard move notation to a list of positions
            val moveList = moveInput.split(",").map { square ->
                val col = square[0] - 'A'
                val row = square[1].digitToInt() - 1
                arrayOf(row, col)
            }.toMutableList()

            val response = gameManager.makeMove(gameId, playerId, moveList)

            //reload the board if a success
            if (response.success) {
                val game = gameManager.createGame(gameId, playerId) ?: return@post call.respond(HttpStatusCode.BadRequest)
                WSConnections.broadcast(gameId, game.boardState)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respondText(response.message)
            }
        }

        //set up the websocket for sync between players
        webSocket("/ws/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull() ?: return@webSocket close(
                CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid gameId")
            )

            println("Client connected!")
            WSConnections.add(gameId, this)

            try {
                for (frame in incoming) {
                    // empty as no messages sent by client
                }
            }
            finally {
                println("Client disconnected!")
                WSConnections.remove(gameId, this)
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