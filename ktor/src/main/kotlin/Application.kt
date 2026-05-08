package com.example

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.websocket.WebSockets
import java.security.SecureRandom

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
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
    configureDatabase()
    configureTemplates()
    configureRouting()
}
