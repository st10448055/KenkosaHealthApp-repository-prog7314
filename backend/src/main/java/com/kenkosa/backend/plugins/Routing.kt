package com.kenkosa.backend.plugins

import com.kenkosa.backend.repository.GoalRepository
import com.kenkosa.backend.repository.MealDiaryRepository
import com.kenkosa.backend.repository.MedicationRepository
import com.kenkosa.backend.repository.MoodRepository
import com.kenkosa.backend.repository.OpenFoodFactsClient
import com.kenkosa.backend.repository.SleepRepository
import com.kenkosa.backend.repository.UsdaFoodClient
import com.kenkosa.backend.routes.diaryRoutes
import com.kenkosa.backend.routes.goalRoutes
import com.kenkosa.backend.routes.medicationRoutes
import com.kenkosa.backend.routes.moodRoutes
import com.kenkosa.backend.routes.sleepRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.kenkosa.backend.repository.FastRepository
import com.kenkosa.backend.routes.fastRoutes

fun Application.configureRouting() {
    val goalRepository = GoalRepository()
    val sleepRepository = SleepRepository()
    val medicationRepository = MedicationRepository()
    val moodRepository = MoodRepository()
    val mealRepository = MealDiaryRepository()
    val openFoodFactsClient = OpenFoodFactsClient()
    val usdaFoodClient = UsdaFoodClient()
    val fastRepository = FastRepository()

    routing {
        goalRoutes(goalRepository)
        sleepRoutes(sleepRepository)
        medicationRoutes(medicationRepository)
        moodRoutes(moodRepository)
        diaryRoutes(mealRepository,openFoodFactsClient,usdaFoodClient)
        fastRoutes(fastRepository)

    }
}