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
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import io.ktor.util.*
import java.security.SecureRandom
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import kotlinx.coroutines.delay

//store player session
@Serializable
data class PlayerSession(val playerId: Int, val gameId: Int)
val playerSessions = mutableMapOf<String, PlayerSession>()

fun Application.configureRouting() {
    val gameManager = GameManager()
    routing {
        //home page loading
        get("/"){
            call.respondTemplate("base.peb", mapOf())
        }

        //load the menu of options
        get("/menu") {
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondText("No token provided")
            val session = playerSessions[token]
            ?: return@get call.respondText("Invalid token")

            //reset the gameId if going back from /game
            playerSessions[token] = session.copy(gameId = 0)

            val playerId = session.playerId

            call.respondTemplate("menu.peb", mapOf("token" to token))
        }

        //wait for an opponent in matchmaking
        get("/wait"){
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondText("No token provided")
            call.respondTemplate("wait.peb", mapOf("token" to token))
        }

        //search through all games for a specific player
        get("/search") {
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondText("No token provided")
            val session = playerSessions[token]
            ?: return@get call.respondText("Invalid token")

            //decide whether looking for active games or ended games
            val status = call.request.queryParameters["status"]

            //reset the gameId if going back from /game
            playerSessions[token] = session.copy(gameId = 0)

            val playerId = session.playerId
            val playerGames = gameManager.getGames(playerId, status)

            call.respondTemplate("search.peb",mapOf("games" to playerGames, "token" to token)
            )
        }

        //load the game
        get("/game"){
            //get necessary parameters
            val sessionToken = call.request.queryParameters["token"]
            ?: return@get call.respondText("No token provided")
            val playerId = playerSessions[sessionToken]?.playerId
            val gameId = playerSessions[sessionToken]?.gameId
            if (playerId == null || gameId == null){return@get call.respond(HttpStatusCode.BadRequest, "Game not found")}

            //make the game
            val game = gameManager.createGame(gameId, playerId)
            if (game == null){return@get call.respond(HttpStatusCode.BadRequest, "Game not found")}

            call.respondTemplate("game.peb", mapOf("boardString" to game.boardState, "session" to sessionToken, "black" to game.black_id, "white" to game.white_id, "current" to game.current, "winner" to game.winCheck(game.current)))
        }

        //to set the player for testing
        post("/player"){
            //get the player id
            val params = call.receiveParameters()
            val playerId = params["playerId"]?.toIntOrNull()
            ?: return@post call.respondText("Invalid player ID")

            //create a random 32 byte hex value for session
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            val sessionToken = bytes.joinToString("") { "%02x".format(it) }
            playerSessions[sessionToken] = PlayerSession(playerId, gameId = 0)

            call.respondRedirect("/menu?token=$sessionToken")
        }

        post("/join"){
            //get necessary values
            val params = call.receiveParameters()
            val gameId = params["gameId"]?.toIntOrNull()
            ?: return@post call.respondText("Invalid game ID")
            val sessionToken = params["token"]
            ?: return@post call.respondText("Invalid session token")

            //update session so it references the selected game
            playerSessions[sessionToken] = playerSessions[sessionToken]!!.copy(gameId = gameId)
            call.respondRedirect("/game?token=$sessionToken")
        }

        //to make a move on a board
        post("/move"){
            //get the user session
            val params = call.receiveParameters()
            val token = params["token"] ?: return@post call.respondText("No token provided")
            val session = playerSessions[token] ?: return@post call.respondText("Invalid session")

            //get the values required
            val gameId = session.gameId
            val playerId = session.playerId
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
                val message = "${game.boardState},${response.winner},${game.current}"
                WSConnections.broadcast(gameId, message)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respondText(response.message)
            }
        }

        //set up the websocket for sync between opponents
        webSocket("/gamews") {
            //get game and player id
            val token = call.request.queryParameters["token"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No token"))
            val session = playerSessions[token]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid token"))

            val gameId = session.gameId
            val playerId = session.playerId

            println("Player $playerId connected to game $gameId")
            WSConnections.add(gameId, this)

            try {
                for (frame in incoming) {
                    //no messages sent 
                }
            } 
            finally {
                println("Player $playerId disconnected from game $gameId")
                WSConnections.remove(gameId, this)
            }
        }

        //wait in the queue until an opponent is chosen for a new game
        webSocket("/waitws"){
            val token = call.request.queryParameters["token"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No token"))
            val session = playerSessions[token]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid token"))

            val playerId = session.playerId
            matchmakingQueue.sessions[playerId] = this

            try{
                while (true) {
                    val opponentId = matchmakingQueue.match(playerId)

                    if (opponentId != null) {
                        //if opponent found, add new game to database
                        val gameId = transaction{
                            Games.insert {
                                it[white_id] = EntityID(opponentId, Users)
                                it[black_id] = EntityID(playerId, Users)
                                it[board] = ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b"
                                it[history] = "{}"
                                it[current] = "black"
                                it[start_time] = 1680000000100
                                it[status] = "active"
                            } get Games.id
                        }
                        saveToCSV()

                        //send the new game id to both players
                        matchmakingQueue.notify(playerId, "$gameId")
                        matchmakingQueue.notify(opponentId, "$gameId")

                        break
                    }
                    delay(500)
                }
            } finally {
                matchmakingQueue.remove(playerId)
            }
        }
    }
}