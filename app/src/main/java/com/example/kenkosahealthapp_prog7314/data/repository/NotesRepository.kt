package com.example.kenkosahealthapp_prog7314.data.repository

import com.example.kenkosahealthapp_prog7314.data.network.DiaryNoteDto
import com.example.kenkosahealthapp_prog7314.data.network.NotesApi
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateNoteRequest
import retrofit2.HttpException
import java.io.IOException

class NotesRepository(private val api: NotesApi = RetrofitInstance.notesApi) {

    suspend fun getTodaysNote(): ApiResult<DiaryNoteDto> =
        safeCall { api.getTodaysNote() }

    suspend fun updateNote(noteText: String): ApiResult<DiaryNoteDto> {
        if (noteText.length > 2000) return ApiResult.ValidationError("Note must be 2000 characters or fewer")
        return safeCall { api.updateNote(UpdateNoteRequest(noteText)) }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string() ?: e.message()
        ApiResult.ServerError(e.code(), message ?: "Server error")
    } catch (e: IOException) {
        ApiResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.ServerError(0, e.message ?: "Unknown error")
    }
}