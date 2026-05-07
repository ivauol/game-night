package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class GameManagerTest :
    DescribeSpec({
        val gameManager = GameManager()
        beforeSpec {
            Database.connect("jdbc:sqlite:./src/test/resources/data/testcheckers.db", driver = "org.sqlite.JDBC")
            transaction {
                SchemaUtils.create(Games)
            }
        }

        beforeEach {
            transaction {
                Games.deleteAll()
                Games.insert {
                    it[id] = 1
                    it[white_id] = 2
                    it[black_id] = 1
                    it[board] = ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b"
                    it[history] = "{}"
                    it[current] = "black"
                    it[start_time] = 1680000000100
                    it[end_time] = null
                    it[status] = "active"
                    it[winner_id] = null
                }
                Games.insert {
                    it[id] = 2
                    it[white_id] = 2
                    it[black_id] = 1
                    it[board] = ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b"
                    it[history] =
                        "{$.b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b$:1|$.b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.w$:2}"
                    it[current] = "black"
                    it[start_time] = 1680000000100
                    it[end_time] = null
                    it[status] = "active"
                    it[winner_id] = null
                }
            }
        }

        describe("createGame") {
            it("Should return null if game id doesn't exist") {
                val game = gameManager.createGame(5, 1)
                game shouldBe null
            }
            it("Should return null if player id not in game") {
                val game = gameManager.createGame(1, 3)
                game shouldBe null
            }
            it("Should return the correct game with valid input") {
                val game = gameManager.createGame(1, 2)
                game?.black_id shouldBe 1
            }
            it("Should parse empty history") {
                val game = gameManager.createGame(1, 2)
                game?.history shouldBe mutableMapOf()
            }
            it("Should parse non-empty history") {
                val game = gameManager.createGame(2, 2)
                game?.history shouldBe
                    mutableMapOf(
                        ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b" to 1,
                        ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.w" to 2,
                    )
            }
        }

        describe("makeMove") {
            it("Should explain if the game doesn't exist") {
                val tested = gameManager.makeMove(10, 5, mutableListOf(arrayOf(1, 2), arrayOf(2, 3)))
                tested.message shouldBe "Game not found"
            }
            it("Should explain if the wrong player tries to move") {
                val tested = gameManager.makeMove(1, 2, mutableListOf(arrayOf(5, 6), arrayOf(4, 5)))
                tested.message shouldBe "Not your turn"
            }
            it("Should explain if a move is invalid") {
                val tested = gameManager.makeMove(1, 1, mutableListOf(arrayOf(5, 6), arrayOf(4, 5)))
                tested.message shouldBe "Invalid move"
            }
            it("Should correctly move a piece with a valid move entered") {
                val tested = gameManager.makeMove(1, 1, mutableListOf(arrayOf(2, 1), arrayOf(3, 2)))
                tested.message shouldBe "Move successful"
                tested.board shouldBe ".b.b.b.bb.b.b.b....b.b.b..b.............w.w.w.w..w.w.w.ww.w.w.w.w"
            }
            it("Should update database with a valid move") {
                val tested = gameManager.makeMove(1, 1, mutableListOf(arrayOf(2, 1), arrayOf(3, 2)))
                transaction {
                    val query = Games.selectAll().where { Games.id eq 1 }.first()
                    query[Games.board] shouldBe ".b.b.b.bb.b.b.b....b.b.b..b.............w.w.w.w..w.w.w.ww.w.w.w.w"
                    query[Games.current] shouldBe "white"
                    query[Games.history] shouldBe """{".b.b.b.bb.b.b.b....b.b.b..b.............w.w.w.w..w.w.w.ww.w.w.w.w":1}"""
                }
            }
        }
    })
