package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger

private const val URL = "jdbc:sqlite:./src/main/resources/data/checkers.db"
private const val DRIVER = "org.sqlite.JDBC"

fun Application.configureDatabase() {
    val db = File("./src/main/resources/data/checkers.db")

    Database.connect(URL, driver = DRIVER)

    transaction {
        if (!db.exists()){
            addLogger(StdOutSqlLogger)
            //create the tables
            SchemaUtils.create(Users, Games)
            //insert all users into exposed database
            hydrateUsers().forEach { record ->
                Users.insert {
                    it[username] = record.username
                    it[password] = record.password
                    it[email] = record.email
                }
            }
            //insert all games into exposed database
            hydrateGames().forEach { record ->
                Games.insert {
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
        }
    }
}

//users table
object Users : IntIdTable() {
    val username = varchar("username", 32).uniqueIndex()
    val password = varchar("password", 128)
    val email = varchar("email", 50).uniqueIndex()
}

//games table
object Games : IntIdTable() {
    val black_id = integer("black_id")
    val white_id = integer("white_id")
    val board = varchar("board", 65)
    val history = text("history")
    val current = varchar("current", 5)
    val start_time = long("start_time")
    val end_time = long("end_time").nullable()
    val status = varchar("status", 10)
    val winner_id = integer("winner_id").nullable()
}