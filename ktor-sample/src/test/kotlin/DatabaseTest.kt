package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DatabaseTest: DescribeSpec({
    describe("hydrateUsers"){
        val userPath = "src/test/resources/data/testusers.csv"
    }

    describe("hydrateGames"){
        val gamePath = "src/test/resources/data/testgames.csv"
    }
})