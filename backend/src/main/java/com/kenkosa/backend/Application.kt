package com.kenkosa.backend

import com.kenkosa.backend.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureFirebase()
    configureSerialization()
    configureAuthentication()
    configureStatusPages()
    configureRouting()
}