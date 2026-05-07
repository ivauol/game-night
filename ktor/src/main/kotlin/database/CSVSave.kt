package com.example

import java.io.File
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

//save database changes directly to the CSV
fun saveToCSV() {
    //get all games
    val games = transaction { Games.selectAll().toList() }

    val header = "game_id,black_id,white_id,board,history,current,start_time,end_time,status,winner_id"
    val file = File("src/main/resources/data/games.csv")
    file.printWriter().use { out ->
        out.println(header)
        //write each game line by line
        games.forEach { row ->
            val line = listOf(
                row[Games.id],
                row[Games.black_id],
                row[Games.white_id],
                row[Games.board],
                //so that there are no trip ups from " in bad places
                row[Games.history].replace("\"", "$").replace(",", "|"),
                row[Games.current],
                row[Games.start_time],
                row[Games.end_time] ?: "",
                row[Games.status],
                row[Games.winner_id] ?: ""
            ).joinToString(",")
            out.println(line)
        }
    }
}