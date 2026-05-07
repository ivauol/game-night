import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

// store all online players for syncing
object WSConnections {
    private val sessions = mutableMapOf<Int, MutableList<DefaultWebSocketServerSession>>()

    // add a new websocket connection to a game
    fun add(
        gameId: Int,
        session: DefaultWebSocketServerSession,
    ) {
        val list = sessions.getOrPut(gameId) { mutableListOf() }
        list.add(session)
    }

    // remove a websocket connection from a game
    fun remove(
        gameId: Int,
        session: DefaultWebSocketServerSession,
    ) {
        sessions[gameId]?.remove(session)
        if (sessions[gameId]?.isEmpty() == true) {
            sessions.remove(gameId)
        }
    }

    // send a message to all online players in a game
    suspend fun broadcast(
        gameId: Int,
        message: String,
    ) {
        sessions[gameId]?.forEach {
            it.send(Frame.Text(message))
        }
    }
}

// queue designed for matchmaking
object matchmakingQueue {
    private val players = mutableListOf<Int>()
    val sessions = mutableMapOf<Int, DefaultWebSocketServerSession>()

    fun match(playerId: Int): Int? {
        synchronized(this) {
            val opponent = players.firstOrNull { it != playerId }
            if (opponent != null) {
                players.remove(playerId)
                players.remove(opponent)

                return opponent
            } else {
                if (!players.contains(playerId)) {
                    players.add(playerId)
                }
                return null
            }
        }
    }

    suspend fun notify(
        playerId: Int,
        message: String,
    ) {
        sessions[playerId]?.send(Frame.Text(message))
        sessions.remove(playerId)
    }

    fun remove(playerId: Int) {
        synchronized(this) {
            players.remove(playerId)
            sessions.remove(playerId)
        }
    }
}
