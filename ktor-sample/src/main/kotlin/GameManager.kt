class GameManager(){
    fun createGame(game_id: Int, player_id: Int){
        val query = Games.selectAll()
        game_id.let {
            query.andWhere { Games.id eq it }
        }
        player_id.let {
            query.andWhere { Games.black_id eq it or Games.white_id eq it }
        }
        query.prepareSQL(this)
        val record = query[0]
        return Game(record.black_id, record.white_id, record.current, record.board, record.history)
    }
}