package com.example

import io.ktor.server.application.Application


fun Application.module() {
    configureRouting()
    configureTemplates()
    }


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
} 