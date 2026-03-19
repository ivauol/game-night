package com.example

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.pebble.Pebble
import io.pebbletemplates.pebble.loader.ClasspathLoader

import io.ktor.server.application.*
import io.ktor.server.pebble.*
 

fun Application.configureTemplates() {
    install(Pebble) {
        loader(ClasspathLoader().apply {
            prefix = "templates"
        })
    }
}