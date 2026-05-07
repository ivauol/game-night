package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DatabaseTest :
    DescribeSpec({
        describe("hydrateUsers") {
            val userPath = "src/test/resources/data/testusers.csv"
            val tested = hydrateUsers(userPath)
            it("Should get correct csv data") {
                tested.size shouldBe 2
                tested[0].username shouldBe "black"
                tested[0].email shouldBe "black@example.com"
            }
            it("Should contain hashed passwords") {
                Hasher.verifyPassword("blackPassword", tested[0].password) shouldBe true
            }
        }

        describe("hydrateGames") {
            val gamePath = "src/test/resources/data/testgames.csv"
            val tested = hydrateGames(gamePath)
            it("Should get correct csv data") {
                tested.size shouldBe 2
                tested[0].black_id shouldBe 1
                tested[0].white_id shouldBe 2
                tested[0].board shouldBe ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b"
            }
            it("Should get null fields") {
                tested[0].end_time shouldBe null
                tested[0].winner_id shouldBe null

                tested[1].end_time shouldBe 1680000000200
                tested[1].winner_id shouldBe 2
            }
        }
    })
