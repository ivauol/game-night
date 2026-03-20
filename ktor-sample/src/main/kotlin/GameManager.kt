import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID

import com.example.Games

class GameManager(){
    //make a move in a game
    fun makeMove(game_id: Int, player_id: Int, move: MutableList<Array<Int>>){
        var game = this.createGame(game_id, player_id)
        //if game doesn't exist
        if (game == null){return}
        //decide what player colour sent the move
        var playerColour = ""
        if (game.black_id == player_id){
            playerColour = "black"
        }
        else{
            playerColour = "white"
        }
        //if the wrong player made a move
        if (game.current != playerColour){return}
        //if an invalid move is played
        if (!game.validateMove(move, playerColour)){return}
        game.makeMove(move)
        this.updateGame(game, game_id)
    }

    //to load a game from the database and turn it into a Game object
    fun createGame(game_id: Int, player_id: Int): Game?{
        return transaction {
            //select the game with the right id and ensure the player is in the game
            val query = Games.select(
                (Games.id eq game_id) and
                ((Games.black_id eq player_id) or (Games.white_id eq player_id))
            ).firstOrNull()

            query?.let {
                //turn the history string into a JSON
                val historyMap: MutableMap<String, Int> = it[Games.history]
                ?.takeIf { it.isNotEmpty() }
                ?.let { Json.decodeFromString(it) }
                ?: mutableMapOf()

                Game(it[Games.black_id].value, it[Games.white_id].value, it[Games.current], it[Games.board], historyMap)
            }
        }
    }

    //update an existing game after a move
    fun updateGame(game: Game, game_id: Int){
        val history = Json.encodeToString(game.history)
        transaction {
            Games.update({Games.id eq game_id}){
                it[Games.board] = game.boardState
                it[Games.history] = history
                it[Games.current] = game.current
            }
        }
    }
}