package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.pebble.respondTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.*
import io.ktor.server.websocket.*
import io.ktor.server.http.content.*
import io.ktor.server.sessions.*
import io.ktor.server.util.getOrFail
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import kotlinx.coroutines.delay
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.selectAll

//store player session
@Serializable
data class PlayerSession(val playerId: Int, val gameId: Int, val lastSeen: Long = System.currentTimeMillis())
val playerSessions = mutableMapOf<String, PlayerSession>()

fun Application.configureRouting() {
    val gameManager = GameManager()

    //check every minute if theres any sessions unused for an hour and delete them
    launch {
        while (true) {
            delay(60 * 1000)

            val now = System.currentTimeMillis()
            val timeout = 60 * 60 * 1000

            playerSessions.entries.removeIf { (token, session) ->
                now - session.lastSeen > timeout
            }
        }
    }

    routing {
        staticResources("/images", "static/images")
        staticResources("/js", "static/js")

        get("/ping"){
            val token = call.request.queryParameters["token"]
            val session = playerSessions[token]
            if (session == null){
                return@get call.respondText("Session Expired.")
            }
            return@get call.respond(HttpStatusCode.OK)
        }

        get("/") {
            call.respondTemplate("base.peb", mapOf())
            //call.displayHome()
        }

        get("/home") {
            call.respondTemplate("home.peb.html", mapOf())
        }

        get("/login2") {
            val message = call.request.queryParameters["message"] ?: ""
            call.respondTemplate("login2.peb.html", mapOf("message" to message))
        }

        post("/login2") {
            val params = call.receiveParameters()
            val username = params.getOrFail("username")
            val password = params.getOrFail("password")

            var user: User? = null
            transaction {
                user = Users.selectAll().where { Users.username eq username }.singleOrNull()
                    ?.let {
                        User(
                            id=it[Users.id],
                            username=it[Users.username],
                            password=it[Users.password]
                        )
                    }
            }

            println(user)

            if (user == null){
                return@post call.respondRedirect("/login2?message=Invalid%20user")
            }

            if (!Hasher.verifyPassword(password, user!!.password)) {
                return@post call.respondRedirect("/login2?message=Invalid%20user")
            }

            call.sessions.set(PlayerSession(playerId=user!!.id.value, gameId=1))
            return@post call.respondRedirect("/success")
            // actually redirect to the game centre page
        }

        // temp page for checking
        get("/success") {
            val session = call.sessions.get<PlayerSession>()
            if (session == null) { // if there's no session
                println("No session!")
                return@get call.respondRedirect("/login2")
            }
            call.respondTemplate("success.peb.html", mapOf())
        }

        //load the login page 
        get("/login"){
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondRedirect("/gamecenter")
            val session = playerSessions[token]
            ?: return@get call.respondRedirect("/gamecenter")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

            call.displayLogIn(token)
        }

        get("/register"){
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondRedirect("/gamecenter")
            val session = playerSessions[token]
            ?: return@get call.respondRedirect("/gamecenter")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

            call.displayRegister(token)
        }

        get("/logout") {
            call.sessions.clear<PlayerSession>()
            call.respondRedirect("/loggedout")
        }

        get("/loggedout") {
            call.respondTemplate("loggedout.peb.html", mapOf())
        }

        get("/gamecenter"){
            val token = call.request.queryParameters["token"]
            val session = playerSessions[token]
            if (token == null || session == null){
                val bytes = ByteArray(32)
                SecureRandom().nextBytes(bytes)
                val newToken = bytes.joinToString("") { "%02x".format(it) }
                playerSessions[newToken] = PlayerSession(playerId = 0, gameId = 0)
                return@get call.respondRedirect("/gamecenter?token=$newToken")
            }
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

            var login = false
            if (playerSessions[token]?.playerId == 0){
                login = true
            }

            call.displayGameCenter(token, login)
        }

        //load the menu of options
        get("/menu") {
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondRedirect("/gamecenter")
            val session = playerSessions[token]
            ?: return@get call.respondRedirect("/gamecenter")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

            //reset the gameId if going back from /game
            playerSessions[token] = session.copy(gameId = 0)

            val playerId = session.playerId
            if (playerId == 0){
                return@get call.respondRedirect("/login?token=$token")
            }

            call.respondTemplate("menu.peb", mapOf("token" to token))
        }

        //wait for an opponent in matchmaking
        get("/wait"){
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondRedirect("/gamecenter")
            val session = playerSessions[token]
            ?: return@get call.respondRedirect("/gamecenter")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())
            if (session.playerId == 0){
                return@get call.respondRedirect("/login?token=$token")
            }
            call.respondTemplate("wait.peb", mapOf("token" to token))
        }

        //search through all games for a specific player
        get("/search") {
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondRedirect("/gamecenter")
            val session = playerSessions[token]
            ?: return@get call.respondRedirect("/gamecenter")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

            //decide whether looking for active games or ended games
            val status = call.request.queryParameters["status"]

            //reset the gameId if going back from /game
            playerSessions[token] = session.copy(gameId = 0)

            val playerId = session.playerId
            if (playerId == 0){
                return@get call.respondRedirect("/login?token=$token")
            }
            val playerGames = gameManager.getGames(playerId, status)

            call.respondTemplate("search.peb",mapOf("games" to playerGames, "token" to token)
            )
        }

        //load the game
        get("/game"){
            //get necessary parameters
            val token = call.request.queryParameters["token"]
            ?: return@get call.respondRedirect("/gamecenter")
            val session = playerSessions[token]
            ?: return@get call.respondRedirect("/gamecenter")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

            val playerId = session.playerId
            if (playerId == 0){
                return@get call.respondRedirect("/login?token=$token")
            }
            val gameId = session.gameId

            //make the game
            val game = gameManager.createGame(gameId, playerId)
            if (game == null){return@get call.respond(HttpStatusCode.BadRequest, "Game not found")}

            call.respondTemplate("game.peb", mapOf("boardString" to game.boardState, "session" to token, "black" to game.black_id, "white" to game.white_id, "playerId" to playerId, "current" to game.current, "winner" to game.winCheck(game.current)))
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

            call.respondRedirect("/gamecenter?token=$sessionToken")
        }

        post("/logout"){
            //get necessary values
            val params = call.receiveParameters()
            val sessionToken = params["token"]
            ?: return@post call.respondText("Invalid session token")

            playerSessions[sessionToken] = playerSessions[sessionToken]!!.copy(playerId = 0, lastSeen = System.currentTimeMillis())
            call.respondRedirect("/gamecenter?token=$sessionToken")
        }

        post("/join"){
            //get necessary values
            val params = call.receiveParameters()
            val gameId = params["gameId"]?.toIntOrNull()
            ?: return@post call.respondText("Invalid game ID")
            val sessionToken = params["token"]
            ?: return@post call.respondText("Invalid session token")

            //update session so it references the selected game
            playerSessions[sessionToken] = playerSessions[sessionToken]!!.copy(gameId = gameId, lastSeen = System.currentTimeMillis())
            call.respondRedirect("/game?token=$sessionToken")
        }

        //to make a move on a board
        post("/move"){
            //get the user session
            val params = call.receiveParameters()
            val token = params["token"] ?: return@post call.respondText("No token provided")
            val session = playerSessions[token] ?: return@post call.respondText("Invalid session")
            playerSessions[token] = session.copy(lastSeen = System.currentTimeMillis())

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
                call.respond(HttpStatusCode.BadRequest, response.message)
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
                                it[start_time] = System.currentTimeMillis()
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

private suspend fun ApplicationCall.displayHome() {
    respondTemplate("welcomepage.peb", model=emptyMap())
}

private suspend fun ApplicationCall.displayLogIn(token: String){
    respondTemplate("login-page.peb", mapOf("token" to token) ) // link to userdatabase
}

private suspend fun ApplicationCall.displayRegister(token: String) {
    respondTemplate("accountcreate.peb", mapOf("token" to token)) // link to userdatabase
}


private suspend fun ApplicationCall.displayGameCenter(token: String, login: Boolean){
    val image= "/workspaces/game-night/ktor-sample/src/main/resources/static/images/checkers-cover.png"
    respondTemplate("gamecenter.peb", mapOf("checkersImageUrl" to image, "token" to token, "login" to login))
}
