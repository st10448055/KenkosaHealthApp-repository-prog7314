package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

interface NotesApi {
    @GET("api/notes")
    suspend fun getTodaysNote(@Query("date") date: String? = null): DiaryNoteDto

    @PATCH("api/notes")
    suspend fun updateNote(@Body request: UpdateNoteRequest): DiaryNoteDto
}