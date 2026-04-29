package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll

class GameManagerTest: DescribeSpec({
    val gameManager = GameManager()
    beforeSpec {
        Database.connect("jdbc:sqlite::memory:", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Users, Games)
        }
    }
    beforeEach {
        transaction {
            Users.deleteAll()
            Games.deleteAll()
        }
    }

    describe("makeMove"){
        
    }
})