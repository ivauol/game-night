package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.staticResources
import io.ktor.server.pebble.respondTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.util.getOrFail
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

// store player session
@Serializable
data class PlayerSession(
    val playerId: Int,
)

// Configuring all the routes we use throughout

fun Application.configureRouting() {
    val gameManager = GameManager()

    routing {
        staticResources("/images", "static/images")
        staticResources("/js", "static/js")

        get("/") {
            return@get call.respondRedirect("/welcome")
        }

        get("/welcome") {
            call.respondTemplate("welcomepage.peb", mapOf())
        }

        get("/login") {
            // The message is only displayed when login is invalid
            val message = call.request.queryParameters["message"] ?: ""
            call.respondTemplate("login.peb", mapOf("message" to message))
        }

        post("/login") {
            val params = call.receiveParameters()
            val username = params.getOrFail("username").lowercase()
            val password = params.getOrFail("password")

            var user: User? = null // create user before transaction block
            transaction {
                user =
                    Users
                        .selectAll()
                        .where { Users.username eq username }
                        .singleOrNull()
                        ?.let {
                            User(
                                id = it[Users.id],
                                username = it[Users.username],
                                password = it[Users.password],
                            )
                        }
            }

            // Checking for valid user, pass, etc. If anything fails we redirect with a message.
            if (user == null) {
                return@post call.respondRedirect("/login?message=Invalid%20user")
            }

            if (!Hasher.verifyPassword(password, user!!.password)) {
                return@post call.respondRedirect("/login?message=Invalid%20user")
            }

            call.sessions.set(PlayerSession(playerId = user!!.id.value))
            return@post call.respondRedirect("/gamecenter")
        }

        /**
         * After the user has logged in (or registered) they'll have access to the rest of the site.
         * For the majority of the remaining routes we check for a session
         * And redirect to login if one isn't found.
         */

        get("/register") {
            // The message is only displayed when registration is invalid
            val message = call.request.queryParameters["message"] ?: ""
            call.respondTemplate("register.peb", mapOf("message" to message))
        }

        post("/register") {
            val params = call.receiveParameters()
            val givenEmail = params.getOrFail("email")
            val givenUsername = params.getOrFail("username").lowercase()
            val givenPass = params.getOrFail("password")

            // Checking for any empty fields/invalid pass length
            if (givenEmail.trim() == "" || givenUsername.trim() == "" || givenPass.trim() == "") {
                return@post call.respondRedirect("/register?message=Please%20fill%20all%20fields.")
            }
            if (givenPass.length < 6) {
                return@post call.respondRedirect("/register?message=Password%20not%20long%20enough.")
            }

            var userExists = false
            var userId: EntityID<Int>? = null

            // Entering new user info into the database
            transaction {
                val user = Users.selectAll().where { Users.username eq givenUsername }.singleOrNull()

                if (user != null) {
                    userExists = true
                    return@transaction
                }

                userId = Users.insert {
                    it[username] = givenUsername
                    it[password] = Hasher.hashPassword(givenPass)
                    it[email] = givenEmail
                } get Users.id
            }

            if (userExists) {
                return@post call.respondRedirect("/register?message=User%20exists.")
            }

            call.sessions.set(PlayerSession(playerId = userId!!.value))
            return@post call.respondRedirect("/gamecenter")
        }

        /**
         * Profile page with user info
         * Displays stats from the Users/Games databases
         */
        get("/stats") {
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@get call.respondRedirect("/login")
            val playerId = session.playerId

            val (username, email) =
                transaction {
                    var userDetails = Users.selectAll().where { Users.id eq playerId }.firstOrNull()

                    if (userDetails != null) {
                        Pair(userDetails[Users.username], userDetails[Users.email])
                    } else {
                        Pair("", "")
                    }
                }
            if (username == "") {
                return@get call.respondRedirect("/gamecenter")
            }

            val (totalGames, activeGames, wonGames) =
                transaction {
                    var userGames =
                        Games.selectAll().where { (Games.black_id eq playerId) or (Games.white_id eq playerId) }

                    var total = 0
                    var active = 0
                    var won = 0
                    for (game in userGames) {
                        total += 1
                        if (game[Games.status] == "active") {
                            active += 1
                        }
                        if (game[Games.status] == "ended" && game[Games.winner_id] == playerId) {
                            won += 1
                        }
                    }
                    Triple(total, active, won)
                }

            val winRate: Double
            if (totalGames == activeGames) {
                winRate = 0.0
            } else {
                winRate = wonGames.toDouble() / (totalGames.toDouble() - activeGames.toDouble())
            }

            call.respondTemplate(
                "stats.peb",
                mapOf(
                    "username" to username,
                    "email" to email,
                    "totalGames" to totalGames,
                    "activeGames" to activeGames,
                    "winRate" to "%.2f%%".format(winRate * 100),
                ) as Map<String, Any>,
            )
        }

        // Clearing player session and back to main menu
        get("/logout") {
            call.sessions.clear<PlayerSession>()
            call.respondRedirect("/welcome")
        }

        get("/gamecenter") {
            val session = call.sessions.get<PlayerSession>() ?: return@get call.respondRedirect("/login")
            call.displayGameCenter()
        }

        // load the menu of options
        get("/menu") {
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@get call.respondRedirect("/login")

            if (session.playerId == 0) {
                return@get call.respondRedirect("/login")
            }

            call.respondTemplate("menu.peb", mapOf())
        }

        // wait for an opponent in matchmaking
        get("/wait") {
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@get call.respondRedirect("/login")
            if (session.playerId == 0) {
                return@get call.respondRedirect("/login")
            }
            call.respondTemplate("wait.peb", mapOf())
        }

        // search through all games for a specific player
        get("/search") {
            // get necessary parameters
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@get call.respondRedirect("/login")

            // decide whether looking for active games or ended games
            val status = call.request.queryParameters["status"]

            val playerId = session.playerId
            if (playerId == 0) {
                return@get call.respondRedirect("/login")
            }
            val playerGames = gameManager.getGames(playerId, status)

            if (playerGames.isEmpty()) {
                return@get call.respondTemplate("search.peb", mapOf("games" to 0))
            } else {
                call.respondTemplate("search.peb", mapOf("games" to playerGames))
            }
        }

        // load the game
        get("/game") {
            // get necessary parameters
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@get call.respondRedirect("/login")

            val playerId = session.playerId
            if (playerId == 0) {
                return@get call.respondRedirect("/login")
            }

            val username =
                transaction {
                    var userDetails = Users.selectAll().where { Users.id eq playerId }.firstOrNull()

                    if (userDetails != null) {
                        userDetails[Users.username]
                    } else {
                        ""
                    }
                }
            val gameId =
                call.request.queryParameters["gameId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing gameId")

            // make the game
            val game = gameManager.createGame(gameId, playerId)
            if (game == null) {
                return@get call.respond(HttpStatusCode.BadRequest, "Game not found")
            }

            call.respondTemplate(
                "game.peb",
                mapOf(
                    "gameId" to gameId,
                    "boardString" to game.boardState,
                    "blackId" to game.black_id,
                    "whiteId" to game.white_id,
                    "playerId" to playerId,
                    "current" to game.current,
                    "winner" to game.winCheck(game.current),
                    "username" to username,
                ),
            )
        }

        // Placeholder route for future development
        get("/chess") {
            val session = call.sessions.get<PlayerSession>() ?: return@get call.respondRedirect("/login")
            call.respondTemplate("chess.peb", mapOf())
        }

        // to set the player for testing
        post("/player") {
            // get the player id
            val params = call.receiveParameters()
            val playerId =
                params["playerId"]?.toIntOrNull()
                    ?: return@post call.respondText("Invalid player ID")

            call.sessions.set(PlayerSession(playerId))

            call.respondRedirect("/gamecenter")
        }

        post("/logout") {
            call.sessions.clear<PlayerSession>()
            call.respondRedirect("/gamecenter")
        }

        post("/join") {
            // get necessary values
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@post call.respondRedirect("/login")

            val gameId =
                call.receiveParameters()["gameId"]?.toIntOrNull()
                    ?: return@post call.respondText("Invalid game ID")

            call.respondRedirect("/game?gameId=$gameId")
        }

        // to make a move on a board
        post("/move") {
            // get the user session
            val params = call.receiveParameters()
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@post call.respondText("No session", status = HttpStatusCode.BadRequest)

            // get the values required
            val gameId =
                params["gameId"]?.toIntOrNull()
                    ?: return@post call.respondText("Missing gameId", status = HttpStatusCode.BadRequest)
            val playerId = session.playerId
            val moveInput = params["move"] ?: return@post call.respondText("Move not provided")

            // convert to a list of positions
            val moveList =
                moveInput
                    .split(",")
                    .map { square ->
                        val row = square[0].digitToInt() - 1
                        val col = square[1].digitToInt() - 1
                        arrayOf(row, col)
                    }.toMutableList()

            val response = gameManager.makeMove(gameId, playerId, moveList)

            // reload the board if a success
            if (response.success) {
                val game = gameManager.createGame(gameId, playerId) ?: return@post call.respond(HttpStatusCode.BadRequest)
                val message = "${game.boardState},${response.winner},${game.current}"
                WSConnections.broadcast(gameId, message)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest, response.message)
            }
        }

        // set up the websocket for sync between opponents
        webSocket("/gamews") {
            // get game and player id
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No session"))

            val playerId = session.playerId
            if (playerId == 0) {
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Not logged in"))
            }
            val gameId =
                call.request.queryParameters["gameId"]?.toIntOrNull()
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No gameId"))
            val game =
                gameManager.createGame(gameId, playerId)
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Player not in game"))

            println("Player $playerId connected to game $gameId")
            WSConnections.add(gameId, this)

            try {
                for (frame in incoming) {
                    // no messages sent
                }
            } finally {
                println("Player $playerId disconnected from game $gameId")
                WSConnections.remove(gameId, this)
            }
        }

        // wait in the queue until an opponent is chosen for a new game
        webSocket("/waitws") {
            val session =
                call.sessions.get<PlayerSession>()
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "No session"))

            val playerId = session.playerId
            if (playerId == 0) {
                return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Not logged in"))
            }
            MatchmakingQueue.sessions[playerId] = this

            try {
                while (true) {
                    val opponentId = MatchmakingQueue.match(playerId)

                    if (opponentId != null) {
                        // if opponent found, add new game to database
                        val gameId =
                            transaction {
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

                        // send the new game id to both players
                        MatchmakingQueue.notify(playerId, "$gameId")
                        MatchmakingQueue.notify(opponentId, "$gameId")

                        break
                    }
                    delay(500)
                }
            } finally {
                MatchmakingQueue.remove(playerId)
            }
        }
    }
}

private suspend fun ApplicationCall.displayGameCenter() {
    val image = "/workspaces/game-night/ktor/src/main/resources/static/images/checkers-cover.png"
    respondTemplate("gamecenter.peb", mapOf("checkersImageUrl" to image))
}
