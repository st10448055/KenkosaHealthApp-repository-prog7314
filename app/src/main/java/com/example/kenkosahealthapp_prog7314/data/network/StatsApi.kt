package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class DayStatsDto(val date: String, val cups: Int, val calories: Double, val sleepMinutes: Int)

interface StatsApi {
    @GET("api/stats/weekly")
    suspend fun weekly(@Query("tz") tz: String): List<DayStatsDto>

    @POST("api/stats/sync-goals")
    suspend fun syncGoals(@Query("date") date: String, @Query("tz") tz: String): List<TrackedGoalDto>
}