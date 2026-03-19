package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

private const val URL = "jdbc:h2:./games"
private const val DRIVER = "org.h2.Driver"

fun Application.configureDatabase() {
    Database.connect(URL, driver = DRIVER)

    transaction {
        SchemaUtils.drop(Games, Users)
        SchemaUtils.create(Games, Users)
        hydrateGames().forEach { record ->
            Games.insert {
                it[id] = record.game_id
                it[white_id] = record.white_id
                it[black_id] = record.black_id
                it[board] = record.board
                it[history] = record.history
                it[current] = record.current
                it[start_time] = record.start_time
                it[end_time] = record.end_time
                it[status] = record.status
                it[winner_id] = record.winner_id
            }
        }
        hydrateUsers().forEach { record ->
            Users.insert {
                it[id] = record.user_id
                it[username] = record.username
                it[password] = record.password
                it[email] = record.email
            }
        }
    }
}

object Users : IntIdTable() {
    val username = varchar("username", 32)
    val password = varchar("password", 32)
    val email = varchar("email", 50).uniqueIndex()
}

object Games : IntIdTable() {
    val black_id = reference("user_id", Users)
    val white_id = reference("user_id", Users)
    val board = varchar("board", 65)
    val history = varchar("history", 50)
    val current = varchar("current", 5)
    val start_time = integer("start_time")
    val end_time = integer("end_time")
    val status = varchar("status", 10)
    val winner_id = reference("user_id", Users)
}