package com.example

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

@Suppress("unused")
class ApplicationTest: DescribeSpec({

    describe("/") {
        it("Should yield the home page") {
            testApplication {
                application { module() }
                val response = client.get("/")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/login") {
        it("Should yield the login page") {
            testApplication {
                application { module() }
                val response = client.get("/login")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/register") {
        it("Should yield the register page") {
            testApplication {
                application { module() }
                val response = client.get("/register")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/stats") {
        it("Should yield the stats page") {
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
        it("Should yield the game centre page") {
            testApplication {
                application { module() }
                val response = client.get("/gamecenter")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/menu") {
        it("Should yield the menu page") {
            testApplication {
                application { module() }
                val response = client.get("/menu")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/wait") {
        it("Should yield the wait page") {
            testApplication {
                application { module() }
                val response = client.get("/wait")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/search") {
        it("Should yield the search page") {
            testApplication {
                application { module() }
                val response = client.get("/search")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    describe("/game") {
        it("Should yield the game page") {
            testApplication {
                application { module() }
                val response = client.get("/game")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
