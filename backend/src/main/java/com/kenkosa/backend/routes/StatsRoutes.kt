package com.kenkosa.backend.routes

import com.kenkosa.backend.plugins.FirebaseUserPrincipal
import com.kenkosa.backend.repository.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One day of the weekly statistics. */
@Serializable
data class DayStats(val date: String, val cups: Int, val calories: Double, val sleepMinutes: Int)

/** Falls back to UTC if the phone sends a missing or invalid time zone id. */
private fun zoneOf(raw: String?): ZoneId =
    runCatching { ZoneId.of(raw ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))

fun Route.statsRoutes(
    water: WaterRepository,
    meals: MealDiaryRepository,
    sleep: SleepLogRepository,
    goals: GoalTrackingRepository
) {
    authenticate("firebase-auth") {

        // Last 7 days of water, calories and sleep
        get("/api/stats/weekly") {
            val uid = call.principal<FirebaseUserPrincipal>()!!.uid
            val zone = zoneOf(call.request.queryParameters["tz"])
            val end = call.request.queryParameters["endDate"]
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now(zone)
            val sleeps = sleep.getSleepForUser(uid)

            val days = (6 downTo 0).map { back ->
                val day = end.minusDays(back.toLong())
                val ds = day.toString()
                DayStats(
                    date = ds,
                    cups = water.getTodaysWater(uid, ds).cupsConsumed,
                    calories = meals.getMealsForUser(uid, ds).sumOf { it.calories },
                    // a night counts on the day the user woke up
                    sleepMinutes = sleeps
                        .filter { Instant.ofEpochMilli(it.sleepEnd).atZone(zone).toLocalDate() == day }
                        .sumOf { it.totalDurationMinutes }
                )
            }
            call.respond(HttpStatusCode.OK, days)
        }

        // Fills WATER and SLEEP goal progress from what the user already logged.
        post("/api/stats/sync-goals") {
            val uid = call.principal<FirebaseUserPrincipal>()!!.uid
            val zone = zoneOf(call.request.queryParameters["tz"])
            val date = call.request.queryParameters["date"]
                ?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
                ?: LocalDate.now(zone).toString()
            val day = LocalDate.parse(date)

            val cups = water.getTodaysWater(uid, date).cupsConsumed
            val sleepHours = sleep.getSleepForUser(uid)
                .filter { Instant.ofEpochMilli(it.sleepEnd).atZone(zone).toLocalDate() == day }
                .sumOf { it.totalDurationMinutes } / 60.0

            goals.getGoalsForUser(uid).forEach { g ->
                when (g.category.name) {
                    "WATER" -> goals.logProgress(g.goalId, date, cups.toDouble())
                    "SLEEP" -> goals.logProgress(g.goalId, date, Math.round(sleepHours * 10) / 10.0)
                }
            }
            call.respond(HttpStatusCode.OK, goals.getGoalsForUser(uid))
        }
    }
}
//------------------------------------------END OF FILE-------------------------------------------------