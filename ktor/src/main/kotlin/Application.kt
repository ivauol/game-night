package com.example

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import java.security.SecureRandom

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(WebSockets){}
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
