package com.example.kenkosahealthapp_prog7314.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

// Retrofit API interface used to communicate with the backend diary notes endpoints.
interface NotesApi {

    // GET /api/notes
    // Retrieves the diary note for a specific date.
    // If no date is supplied, the backend can use the current date.
    @GET("api/notes")
    suspend fun getTodaysNote(
        @Query("date") date: String? = null
    ): DiaryNoteDto

    // PATCH /api/notes
    // Updates the user's diary note using the supplied request body.
    // Returns the updated diary note.
    @PATCH("api/notes")
    suspend fun updateNote(
        @Body request: UpdateNoteRequest
    ): DiaryNoteDto
}

// ------------------ END OF FILE ------------------ //