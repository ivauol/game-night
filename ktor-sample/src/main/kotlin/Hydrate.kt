package com.example

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.io.readCSV

data class GameRecord(
    val game_id: Int,
    val black_id: Int,
    val white_id: Int,
    val board: String,
    val history: String,
    val current: String,
    val start_time: Int,
    val end_time: Int,
    val status: String,
    val winner_id: Int
)

data class UserRecord(
    val user_id: Int,
    val username: String,
    val password: String,
    val email: String
)

fun Any?.parseCell(): String = this?.toString()
    ?.trim()
    .orEmpty()

fun hydrateGames(): List<GameRecord> {
    val filePath = "src/main/resources/templates/games.csv"
    val df = DataFrame.readCSV(filePath) // map of isbn to the multple entries
    val grouped = df.rows()
        .map { row ->
            GameRecord(
                game_id = row["game_id"].parseCell().toInt(),
                black_id = row["black_id"].parseCell().toInt(),
                white_id = row["white_id"].parseCell().toInt(),
                board = row["board"].parseCell(),
                history = row["history"].parseCell(),
                current = row["current"].parseCell(),
                start_time = row["start_time"].parseCell().toInt(),
                end_time = row["end_time"].parseCell().toInt(),
                status = row["status"].parseCell(),
                winner_id = row["winner_id"].parseCell().toInt()
                )
        }

    println("Loaded ${grouped.size} records")
    return grouped
}

fun hydrateUsers(): List<UserRecord> {
    val filePath = "src/main/resources/templates/users.csv"
    val df = DataFrame.readCSV(filePath) // map of isbn to the multple entries
    val grouped = df.rows()
        .map { row ->
            UserRecord(
                user_id = row["user_id"].parseCell().toInt(),
                username = row["username"].parseCell(),
                password = row["password"].parseCell(),
                email = row["email"].parseCell(),
                )
        }

    println("Loaded ${grouped.size} records")
    return grouped
}

fun main() {
    val records = hydrateGames()
    for (record in records) {
        println("Record: $record")
    }
}