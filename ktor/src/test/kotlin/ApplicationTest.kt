package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.WebSockets
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom

@Suppress("unused")
class ApplicationTest :
    DescribeSpec({

        /**
         * Set up a database with example data for testing.
         * Before every test we clear and populate the database with the user/game data.
         * We then run this through our test application setup.
         */

        fun testDb() {
            Database.connect("jdbc:sqlite:./src/test/resources/data/testusers.db", driver = "org.sqlite.JDBC")

            transaction {
                SchemaUtils.create(Users, Games)
            }

            transaction {
                Users.deleteAll()
                Users.insert {
                    it[id] = 1
                    it[username] = "eve"
                    it[email] = "eve@example.com"
                    it[password] = Hasher.hashPassword("eve123")
                }
                Users.insert {
                    it[id] = 2
                    it[username] = "frank"
                    it[email] = "frank@example.com"
                    it[password] = Hasher.hashPassword("frank123")
                }
                Games.insert {
                    it[white_id] = 2
                    it[black_id] = 1
                    it[board] = ".b.b.b.bb.b.b.b..b.b.b.b................w.w.w.w..w.w.w.ww.w.w.w.b"
                    it[history] = "{}"
                    it[current] = "black"
                    it[start_time] = 16
                    it[end_time] = null
                    it[status] = "active"
                    it[winner_id] = null
                }
            }
        }

        /**
         * Set up a test module for running each test.
         * testModule() is essentially the same as the original except we link to a testDb() instead.
         */

        fun Application.testModule() {
            install(WebSockets) {}
            install(Sessions) {
                cookie<PlayerSession>("player_session") {
                    cookie.path = "/"
                    cookie.httpOnly = true
                    val key = ByteArray(32)
                    SecureRandom().nextBytes(key)
                    transform(SessionTransportTransformerMessageAuthentication(key))
                }
            }
            testDb()
            configureTemplates()
            configureRouting()
        }

        describe("/") {
            it("Should yield the home page") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/welcome"
                }
            }
        }

        describe("/login") {
            it("Should yield the login page") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/login")
                    response.status shouldBe HttpStatusCode.OK
                }
            }

            it("Post request with correct login") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response =
                        client.post("/login") {
                            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                            setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                        }
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/gamecenter"
                }
            }

            it("Post request with incorrect login") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response =
                        client.post("/login") {
                            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                            setBody(listOf("username" to "Mallory", "password" to "mallory123").formUrlEncode())
                        }
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login?message=Invalid%20user"
                }
            }
        }

        describe("/register") {
            it("Should yield the register page") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/register")
                    response.status shouldBe HttpStatusCode.OK
                }
            }

            it("Register attempt with non preexisting user") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response =
                        client.post("/register") {
                            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                            setBody(listOf("username" to "Fake", "email" to "fake@example.com", "password" to "fake123").formUrlEncode())
                        }
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/gamecenter"
                }
            }

            it("Register attempt with preexisting user") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response =
                        client.post("/register") {
                            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                            setBody(listOf("username" to "Eve", "email" to "eve@example.com", "password" to "eve123").formUrlEncode())
                        }
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/register?message=User%20exists."
                }
            }
        }

        describe("/chess") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/chess")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }
            it("Should yield the chess page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/chess")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }

        describe("/stats") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/stats")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }

            it("Should yield the stats page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/stats")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }

        describe("/logout") {
            it("Should yield the logout") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/logout")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/welcome"
                }
            }
        }

        describe("/gamecenter") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/gamecenter")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }

            it("Should yield the game centre page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/gamecenter")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }

        describe("/menu") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/menu")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }

            it("Should yield the menu page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/menu")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }

        describe("/wait") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/wait")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }

            it("Should yield the wait page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/wait")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }

        describe("/search") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/search")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }

            it("Should yield the search page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/search")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }

        describe("/game") {
            it("Should redirect to login page without a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    val response = client.get("/game")
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe "/login"
                }
            }

            it("Should yield the game page with a session") {
                testApplication {
                    application { testModule() }
                    val client =
                        createClient {
                            followRedirects = false
                            install(HttpCookies)
                        }
                    client.post("/login") {
                        header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        setBody(listOf("username" to "Eve", "password" to "eve123").formUrlEncode())
                    }
                    val response = client.get("/game?gameId=1")
                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }
    })
