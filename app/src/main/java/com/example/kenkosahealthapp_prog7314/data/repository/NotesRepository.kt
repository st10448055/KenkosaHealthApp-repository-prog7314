package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.DiaryNoteDto
import com.example.kenkosahealthapp_prog7314.data.network.NotesApi
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateNoteRequest
import retrofit2.HttpException
import java.io.IOException

// Repository responsible for managing the user's diary notes.
// It communicates with the backend through NotesApi.
class NotesRepository(
    // Uses the shared Retrofit NotesApi instance by default.
    private val api: NotesApi = RetrofitInstance.notesApi
) {

    // Retrieves the user's diary note for the current day.
    suspend fun getTodaysNote(): ApiResult<DiaryNoteDto> =
        safeCall {
            api.getTodaysNote()
        }

    // Updates the user's diary note.
    suspend fun updateNote(
        noteText: String
    ): ApiResult<DiaryNoteDto> {

        // Prevents diary notes from exceeding the maximum
        // allowed length of 2000 characters.
        if (noteText.length > 2000) {
            return ApiResult.ValidationError(
                "Note must be 2000 characters or fewer"
            )
        }

        // Sends the updated note text to the backend.
        return safeCall {
            api.updateNote(
                UpdateNoteRequest(noteText)
            )
        }
    }

    // Executes an API operation safely and converts exceptions
    // into the appropriate ApiResult type.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): ApiResult<T> = try {

        // API request completed successfully.
        ApiResult.Success(block())

    } catch (e: HttpException) {

        // Handles HTTP errors returned by the backend.
        val message = e.response()?.errorBody()?.string()
            ?: e.message()

        // Returns the HTTP status code and server error message.
        ApiResult.ServerError(
            e.code(),
            message ?: "Server error"
        )

    } catch (e: IOException) {

        // Handles network connectivity problems.
        ApiResult.NetworkError(
            "Couldn't reach the server. Check your connection."
        )

    } catch (e: Exception) {

        // Handles unexpected errors.
        ApiResult.ServerError(
            0,
            e.message ?: "Unknown error"
        )
    }
}

// ------------------ END OF FILE ------------------ //