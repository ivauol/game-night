package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class RoutingTest : DescribeSpec({

    fun testDb() {
        Database.connect("jdbc:sqlite::memory:", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Users, Games)
        }
    }

    fun setupTestApp(testApplication: TestApplicationBuilder) {
        testApplication.application {
            testDb()
            configureRouting()
        }
    }

    describe("/login") {

    }

    describe("/register") {

    }

    describe("/logout") {

    }

    describe("/game") {

    }

    describe("/search") {

    }

    describe("/move") {

    }

    describe("Session handling") {

    }
})