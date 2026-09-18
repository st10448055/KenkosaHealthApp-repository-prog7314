package com.kenkosa.backend.plugins

import com.kenkosa.backend.repository.GoalRepository
import com.kenkosa.backend.repository.SleepRepository
import com.kenkosa.backend.routes.goalRoutes
import com.kenkosa.backend.routes.sleepRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val goalRepository = GoalRepository()
    val sleepRepository = SleepRepository()
    routing {
        goalRoutes(goalRepository)
        sleepRoutes(sleepRepository)
    }
}