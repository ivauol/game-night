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
import io.ktor.server.http.content.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import io.ktor.util.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.coroutines.delay
import kotlinx.coroutines.*

//store player session
@Serializable
data class PlayerSession(val playerId: Int)

fun Application.configureRouting() {
    val gameManager = GameManager()

    routing {
        staticResources("/images", "static/images")
        staticResources("/js", "static/js")

        get("/") {
            call.respondTemplate("base.peb", mapOf())
            //call.displayHome()
        }

        //load the login page 
        get("/login"){
            val session = call.sessions.get<PlayerSession>()

            if (session != null && session.playerId != 0){
                return@get call.respondRedirect("/gamecenter")
            }

            call.displayLogIn()
        }

        get("/register"){
            val session = call.sessions.get<PlayerSession>()

            if (session != null && session.playerId != 0){
                return@get call.respondRedirect("/gamecenter")
            }

            call.displayRegister()
        }

        get("/stats"){
            val session = call.sessions.get<PlayerSession>()
            ?: return@get call.respondRedirect("/login")
            val playerId = session.playerId

            val (username, email) = transaction {
                //var userDetails = Users.selectAll()
                //.firstOrNull{it[Users.id].value == playerId}
                var userDetails = Users.selectAll().where{Users.id eq playerId}.firstOrNull()          

                if (userDetails != null){
                    Pair(userDetails[Users.username], userDetails[Users.email])
                }
                else{
                    Pair("", "")
                }
            }
            if (username == ""){
                return@get call.respondRedirect("/gamecenter")
            }

            val (totalGames, activeGames, wonGames) = transaction{
                //var userGames = Games.selectAll()
                //.filter{ it[Games.black_id].value == playerId || it[Games.white_id].value == playerId }
                var userGames = Games.selectAll().where{(Games.black_id eq playerId) or (Games.white_id eq playerId)}

                var total = 0
                var active = 0
                var won = 0
                for (game in userGames){
                    total += 1
                    if (game[Games.status] == "active"){
                        active += 1
                    }
                    if (game[Games.status] == "ended" && game[Games.winner_id] == playerId){
                        won += 1
                    }
                }
                Triple(total, active, won)
            }

            val winRate: Double
            if (totalGames == activeGames){
                winRate = 0.0
            }
            else{
                winRate = wonGames.toDouble()/(totalGames.toDouble()-activeGames.toDouble())
            }

            call.respondTemplate("stats.peb", mapOf(
                "username" to username, 
                "email" to email,
                "totalGames" to totalGames,
                "activeGames" to activeGames,
                "winRate" to "%.2f%%".format(winRate * 100)
                ) as Map<String, Any>
            )
        }

        get("/gamecenter"){
            val session = call.sessions.get<PlayerSession>()

            var login = false
            if (session == null || session.playerId == 0){
                login = true
            }

            call.displayGameCenter(login)
        }

        get ("/welcome"){
            call.displayHome()
        }

        //load the menu of options
        get("/menu") {
            val session = call.sessions.get<PlayerSession>()
            ?: return@get call.respondRedirect("/login")
            if (session.playerId == 0){
                return@get call.respondRedirect("/login")
            }

            call.respondTemplate("menu.peb", mapOf())
        }

        //wait for an opponent in matchmaking
        get("/wait"){
            val session = call.sessions.get<PlayerSession>()
            ?: return@get call.respondRedirect("/login")
            if (session.playerId == 0){
                return@get call.respondRedirect("/login")
            }
            call.respondTemplate("wait.peb", mapOf())
        }

        //search through all games for a specific player
        get("/search") {
            //get necessary parameters
            val session = call.sessions.get<PlayerSession>()
            ?: return@get call.respondRedirect("/login")

            //decide whether looking for active games or ended games
            val status = call.request.queryParameters["status"]

            val playerId = session.playerId
            if (playerId == 0){
                return@get call.respondRedirect("/login")
            }
            val playerGames = gameManager.getGames(playerId, status)

            call.respondTemplate("search.peb",mapOf("games" to playerGames)
            )
        }

        //load the game
        get("/game"){
            //get necessary parameters
            val session = call.sessions.get<PlayerSession>()
            ?: return@get call.respondRedirect("/login")

            val playerId = session.playerId
            if (playerId == 0){
                return@get call.respondRedirect("/login")
            }
            val gameId = call.request.queryParameters["gameId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing gameId")

            //make the game
            val game = gameManager.createGame(gameId, playerId)
            if (game == null){return@get call.respond(HttpStatusCode.BadRequest, "Game not found")}

            call.respondTemplate("game.peb", mapOf("gameId" to gameId, "boardString" to game.boardState,
            "black" to game.black_id, "white" to game.white_id, "playerId" to playerId, "current" to game.current, "winner" to game.winCheck(game.current)))
        }

        //to set the player for testing
        post("/player"){
            //get the player id
            val params = call.receiveParameters()
            val playerId = params["playerId"]?.toIntOrNull()
            ?: return@post call.respondText("Invalid player ID")

            call.sessions.set(PlayerSession(playerId))

            call.respondRedirect("/gamecenter")
        }

        post("/logout"){
            call.sessions.clear<PlayerSession>()
            call.respondRedirect("/gamecenter")
        }

        post("/join"){
            //get necessary values
            val session = call.sessions.get<PlayerSession>()
            ?: return@post call.respondRedirect("/login")

            val gameId = call.receiveParameters()["gameId"]?.toIntOrNull()
            ?: return@post call.respondText("Invalid game ID")

            call.respondRedirect("/game?gameId=$gameId")
        }

        //to make a move on a board
        post("/move"){
            //get the user session
            val params = call.receiveParameters()
            val session = call.sessions.get<PlayerSession>()
            ?: return@post call.respondText("No session", status = HttpStatusCode.BadRequest)

            //get the values required
            val gameId = params["gameId"]?.toIntOrNull()
            ?: return@post call.respondText("Missing gameId", status = HttpStatusCode.BadRequest)
            val playerId = session.playerId
            val moveInput = params["move"] ?: return@post call.respondText("Move not provided")

            //convert to a list of positions
            val moveList = moveInput.split(",").map { square ->
                val row = square[0].digitToInt() - 1
                val col = square[1].digitToInt() - 1
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
                call.respond(HttpStatusCode.BadRequest, response.message)
            }
        }

        //set up the websocket for sync between opponents
        webSocket("/gamews") {
            //get game and player id
            val session = call.sessions.get<PlayerSession>()
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No session"))

            val playerId = session.playerId
            if (playerId == 0) {
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Not logged in"))
            }
            val gameId = call.request.queryParameters["gameId"]?.toIntOrNull()
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No gameId"))
            val game = gameManager.createGame(gameId, playerId)
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Player not in game"))

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
            val session = call.sessions.get<PlayerSession>()
            ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No session"))

            val playerId = session.playerId
            if (playerId == 0) {
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Not logged in"))
            }
            matchmakingQueue.sessions[playerId] = this

            try{
                while (true) {
                    val opponentId = matchmakingQueue.match(playerId)

                    if (opponentId != null) {
                        //if opponent found, add new game to database
                        val gameId = transaction{
                            Games.insert {
                                it[white_id] = opponentId
                                it[black_id] = playerId
                                it[board] = ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b"
                                it[history] = "{}"
                                it[current] = "black"
                                it[start_time] = System.currentTimeMillis()
                                it[status] = "active"
                            } get Games.id
                        }

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
 
private suspend fun ApplicationCall.displayHome() {
    respondTemplate("welcomepage.peb", mapOf())
}

private suspend fun ApplicationCall.displayLogIn(){
    respondTemplate("login-page.peb", mapOf()) // link to userdatabase
}

private suspend fun ApplicationCall.displayRegister() {
    respondTemplate("accountcreate.peb", mapOf()) // link to userdatabase
}

private suspend fun ApplicationCall.displayGameCenter(login: Boolean){
    val image = "/workspaces/game-night/ktor-sample/src/main/resources/static/images/checkers-cover.png"
    respondTemplate("gamecenter.peb", mapOf("checkersImageUrl" to image, "login" to login))
}
