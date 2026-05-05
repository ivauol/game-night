package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

import io.ktor.client.request.*
import io.ktor.http.*

import io.ktor.server.routing.*
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class ApplicationTest: DescribeSpec({

    fun testDb() {
        Database.connect("jdbc:sqlite:./src/test/resources/data/testusers.db", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Users)
        }

        transaction{
            Users.deleteAll()
            Users.insert {
                it[id] = 1
                it[username] = "Eve"
                it[email] = "eve@example.com"
                it[password] = "eve123"
            }
        }
    }

    testApplication {
        routing {
            get("/test-login") {
                call.sessions.set(PlayerSession(playerId=1)) // TO-DO: FIX
            }

            get("/clear-session") {
                call.sessions.clear<PlayerSession>()
            }
        }
    }


    describe("/") {
        it("Should yield the home page") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                val response = client.get("/")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/welcome"
            }
        }
    }

    describe("/login") {
        it("Should yield the login page") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                val response = client.get("/login")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/register") {
        it("Should yield the register page") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                val response = client.get("/register")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("POST register check") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                val response = client.post("/register") {
                    contentType(ContentType.Application.Json)
                    setBody("""
                        {
                        "username": "Eve",
                        "email": "eve@example.com",
                        "password": "eve123"
                        }
                    """.trimIndent())
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/stats") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.get("/clear-session")
                val response = client.get("/stats")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the stats page with a session") {
            testApplication {
                application { module() }
                val response = client.get("/stats")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/logout") {
        it("Should yield the logout") {
            testApplication {
                application { module() }
                val response = client.get("/logout")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/gamecenter") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.get("/clear-session")
                val response = client.get("/gamecenter")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the game centre page with a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                val response = client.get("/gamecenter")
                response.status shouldBe HttpStatusCode.Found
                // WRONG
            }
        }
    }

    describe("/menu") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.get("/clear-session")
                val response = client.get("/menu")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the menu page with a session") {
            testApplication {
                application { module() }
                // val client = createClient { followRedirects = false }
                // var response = client.get("/test-login")
                // client.get("/clear-session")
                val response = client.get("/menu")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/wait") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.get("/clear-session")
                val response = client.get("/wait")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the wait page") {
            testApplication {
                application { module() }
                val response = client.get("/wait")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/search") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.get("/clear-session")
                val response = client.get("/search")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the search page with a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.post("/test-login")
                val response = client.get("/search")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/game") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient { followRedirects = false }
                client.get("/clear-session")
                val response = client.get("/game")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the game page") {
            testApplication {
                application { module() }
                val response = client.get("/game")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
