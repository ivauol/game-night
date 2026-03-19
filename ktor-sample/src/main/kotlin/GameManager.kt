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

class GameManager(){
    fun createGame(game_id: Int, player_id: Int): Game{
        transaction {
            val query = Games.selectAll()
            game_id.let {
                query.andWhere { Games.id eq it }
            }
            player_id.let {
                query.andWhere { Games.black_id eq it or Games.white_id eq it }
            }
            query.prepareSQL(this)
            val record = query[0]
        }
        return Game(record.black_id, record.white_id, record.current, record.board, record.history)
    }
}