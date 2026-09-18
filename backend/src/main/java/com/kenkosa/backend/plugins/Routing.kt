package com.kenkosa.backend.plugins

import com.kenkosa.backend.repository.GoalRepository
import com.kenkosa.backend.routes.goalRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val goalRepository = GoalRepository()
    routing {
        goalRoutes(goalRepository)
    }
}