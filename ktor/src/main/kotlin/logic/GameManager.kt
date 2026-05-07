package com.example

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class GameManager {
    // make a move in a game
    fun makeMove(
        game_id: Int,
        player_id: Int,
        move: MutableList<Array<Int>>,
    ): MoveResponse {
        var game = this.createGame(game_id, player_id)
        // if game doesn't exist
        if (game == null) {
            return MoveResponse(false, "Game not found")
        }
        // decide what player colour sent the move
        var playerColour = ""
        if (game.black_id == player_id) {
            playerColour = "black"
        } else {
            playerColour = "white"
        }
        // if the wrong player made a move
        if (game.current != playerColour) {
            return MoveResponse(false, "Not your turn")
        }
        // if an invalid move is played
        if (!game.validateMove(move, playerColour)) {
            return MoveResponse(false, "Invalid move")
        }
        game.makeMove(move)
        this.updateGame(game, game_id)
        return MoveResponse(true, "Move successful", game.boardState, game.winCheck(game.current))
    }

    // to load a game from the database and turn it into a Game object
    fun createGame(
        game_id: Int,
        player_id: Int,
    ): Game? {
        return transaction {
            // select the game with the right id and ensure the player is in the game
            val query =
                Games
                    .selectAll()
                    .where {
                        (Games.id eq game_id) and (
                            (Games.black_id eq player_id) or
                                (Games.white_id eq player_id)
                        )
                    }.firstOrNull()
            // .firstOrNull{(it[Games.id].value == game_id) && (it[Games.black_id].value == player_id || it[Games.white_id].value == player_id)}
            if (query == null) {
                return@transaction null
            }

            // convert the string in the database to a Json object, then to a mutable map
            val historyJson = query[Games.history]
            val historyMap: MutableMap<String, Int> =
                historyJson
                    .takeIf { it.isNotEmpty() }
                    ?.replace("$", "\"")
                    ?.replace("|", ",")
                    ?.let { Json.decodeFromString(it) }
                    ?: mutableMapOf()

            Game(query[Games.black_id], query[Games.white_id], query[Games.current], query[Games.board], historyMap)
        }
    }

    // update an existing game after a move
    fun updateGame(
        game: Game,
        game_id: Int,
    ) {
        val history = Json.encodeToString(game.history)

        val winner = game.winCheck(game.current)
        var status = "active"
        var winner_id: Int? = null
        var win_time: Long? = null
        if (winner != "") {
            status = "ended"
            win_time = System.currentTimeMillis()
            if (winner == "black") {
                winner_id =
                    transaction {
                        // val query = Games.selectAll().first{it[Games.id] == game_id}
                        val query = Games.selectAll().where { Games.id eq game_id }.first()
                        query[Games.black_id]
                    }
            } else {
                winner_id =
                    transaction {
                        // val query = Games.selectAll().first{it[Games.id] == game_id}
                        val query = Games.selectAll().where { Games.id eq game_id }.first()
                        query[Games.white_id]
                    }
            }
        }

        transaction {
            Games.update({ Games.id eq game_id }) {
                it[Games.board] = game.boardState
                it[Games.history] = history
                it[Games.current] = game.current
                it[Games.status] = status
                it[Games.winner_id] = winner_id
                it[Games.end_time] = win_time
            }
        }
    }

    // get every game containing a specific player
    fun getGames(
        playerId: Int,
        status: String?,
    ): MutableList<Int> =
        transaction {
            // var query = Games.selectAll().filter{ it[Games.black_id] == playerId || it[Games.white_id] == playerId }
            var query = Games.selectAll().where { (Games.black_id eq playerId) or (Games.white_id eq playerId) }

            // filter by the correct status if necessary
            if (status != null) { // query = query.filter{ it[Games.status] == status }
                query.andWhere { Games.status eq status }
            }

            var results = mutableListOf<Int>()
            for (game in query) {
                results.add(game[Games.id].value)
            }
            results
        }
}
