package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

import io.ktor.client.request.*
import io.ktor.http.*

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

import io.ktor.client.plugins.cookies.*

@Suppress("unused")
class ApplicationTest: DescribeSpec({

    fun testDb() {
        // use this for tests instead of original database?
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

    describe("/") {
        it("Should yield the home page") {
            testApplication {
                application { module() }
                val client = createClient {
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
            // check for if logged in already?
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.get("/login")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("Post request with correct login") {
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/gamecenter"
            }
        }

        it("Post request with incorrect login") {
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Mallory", "password" to "mallory123").formUrlEncode())
                }
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login?message=Invalid%20user"
            }

        }
    }

    describe("/register") {
        // check for if logged in already?
        it("Should yield the register page") {
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.get("/register")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("Register attempt with non preexisting user") {
            testApplication {
                application { module() }
                testDb()
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.post("/register") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Fake", "email" to "fake@example.com", "password" to "fake123").formUrlEncode())
                }
                // these post requests are persisting :( fix!!
                // response.status shouldBe HttpStatusCode.Found
                // response.headers[HttpHeaders.Location] shouldBe "/gamecenter"
            }
        }

        it("Register attempt with preexisting user") {
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.post("/register") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Alice", "email" to "alice@example.com", "password" to "alice123").formUrlEncode())
                }
                //  these post requests are persisting :( fix!!
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/register?message=User%20exists."
            }
        }
    }

    describe("/stats") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient {
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
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                val response = client.get("/stats")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/logout") {
        it("Should yield the logout") {
            testApplication {
                application { module() }
                val client = createClient {
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
                application { module() }
                val client = createClient {
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
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                val response = client.get("/gamecenter")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/menu") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient {
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
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                val response = client.get("/menu")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/wait") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient {
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
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                val response = client.get("/wait")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/search") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient {
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
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                val response = client.get("/search")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/game") {
        it("Should redirect to login page without a session") {
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                val response = client.get("/game")
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe "/login"
            }
        }

        it("Should yield the game page with a session") {
            // OK actually it shouldn't
            // Redirect to the gamecenter if there's no game ID?
            testApplication {
                application { module() }
                val client = createClient {
                    followRedirects = false
                    install(HttpCookies)
                }
                client.post("/login") {
                    header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("username" to "Bob", "password" to "bob123").formUrlEncode())
                    // somehow do this with "fake" data
                }
                val response = client.get("/game")
                //response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
