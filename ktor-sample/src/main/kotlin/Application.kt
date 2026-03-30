package com.example

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(WebSockets){}
    configureDatabase()
    configureTemplates()
    configureRouting()
}
