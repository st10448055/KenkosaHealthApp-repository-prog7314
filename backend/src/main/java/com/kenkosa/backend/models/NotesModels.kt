package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

@Serializable
data class DiaryNoteResponse(
    val date: String,
    val noteText: String
)

@Serializable
data class UpdateNoteRequest(
    val noteText: String
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (noteText.length > 2000) errors.add("Note must be 2000 characters or fewer")
        return errors
    }
}