package com.kenkosa.backend.plugins


import com.kenkosa.backend.repository.MealDiaryRepository
import com.kenkosa.backend.repository.MedicationRepository
import com.kenkosa.backend.repository.MoodRepository
import com.kenkosa.backend.repository.OpenFoodFactsClient

import com.kenkosa.backend.repository.UsdaFoodClient
import com.kenkosa.backend.routes.diaryRoutes
import com.kenkosa.backend.routes.medicationRoutes
import com.kenkosa.backend.routes.moodRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.kenkosa.backend.repository.FastRepository
import com.kenkosa.backend.routes.fastRoutes
import com.kenkosa.backend.repository.GoalTrackingRepository
import com.kenkosa.backend.repository.NotesRepository
import com.kenkosa.backend.routes.goalTrackingRoutes
import com.kenkosa.backend.repository.SleepLogRepository
import com.kenkosa.backend.repository.WaterRepository
import com.kenkosa.backend.routes.sleepLogRoutes
import com.kenkosa.backend.repository.WgerClient
import com.kenkosa.backend.routes.notesRoutes
import com.kenkosa.backend.routes.waterRoutes
import com.kenkosa.backend.routes.workoutRoutes


fun Application.configureRouting() {
    val goalTrackingRepository = GoalTrackingRepository()
    val sleepLogRepository = SleepLogRepository()
    val medicationRepository = MedicationRepository()
    val moodRepository = MoodRepository()
    val mealRepository = MealDiaryRepository()
    val openFoodFactsClient = OpenFoodFactsClient()
    val usdaFoodClient = UsdaFoodClient()
    val fastRepository = FastRepository()
    val wgerClient = WgerClient()
    val waterRepository = WaterRepository()
    val notesRepository = NotesRepository()

    routing {
        goalTrackingRoutes(goalTrackingRepository)
        sleepLogRoutes(sleepLogRepository)
        medicationRoutes(medicationRepository)
        moodRoutes(moodRepository)
        diaryRoutes(mealRepository,openFoodFactsClient,usdaFoodClient)
        fastRoutes(fastRepository)
        workoutRoutes(wgerClient)
        waterRoutes(waterRepository)
        notesRoutes(notesRepository)



    }
}