package com.example

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.io.readCSV

//game layout
data class GameRecord(
    val game_id: Int,
    val black_id: Int,
    val white_id: Int,
    val board: String,
    val history: String,
    val current: String,
    val start_time: Long,
    val end_time: Long?,
    val status: String,
    val winner_id: Int?
)

//user layout
data class UserRecord(
    val user_id: Int,
    val username: String,
    val password: String,
    val email: String
)

//convert cell value to a proper string
fun Any?.parseCell(): String? = this?.toString()?.trim()?.takeIf { it.isNotEmpty() }

//get all games from the CSV
fun hydrateGames(): List<GameRecord> {
    val filePath = "src/main/resources/templates/games.csv"
    val df = DataFrame.readCSV(filePath)
    //get all the existing games
    val grouped = df.rows()
        .map { row ->
            GameRecord(
                game_id = row["game_id"].parseCell()!!.toInt(),
                black_id = row["black_id"].parseCell()!!.toInt(),
                white_id = row["white_id"].parseCell()!!.toInt(),
                board = row["board"].parseCell()!!,
                history = row["history"].parseCell()!!,
                current = row["current"].parseCell()!!,
                start_time = row["start_time"].parseCell()!!.toLong(),
                end_time = row["end_time"].parseCell()?.toLong(),
                status = row["status"].parseCell()!!,
                winner_id = row["winner_id"].parseCell()?.toInt()
                )
        }

    return grouped
}

//get all users from the CSV
fun hydrateUsers(): List<UserRecord> {
    val filePath = "src/main/resources/templates/users.csv"
    val df = DataFrame.readCSV(filePath)
    //get all existing users
    val grouped = df.rows()
        .map { row ->
            UserRecord(
                user_id = row["user_id"].parseCell()!!.toInt(),
                username = row["username"].parseCell()!!,
                password = row["password"].parseCell()!!,
                email = row["email"].parseCell()!!,
                )
        }

    return grouped
}

fun main() {
    val records = hydrateGames()
    for (record in records) {
        println("Record: $record")
    }
}