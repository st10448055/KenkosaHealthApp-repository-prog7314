package com.kenkosa.backend.models

import kotlinx.serialization.Serializable

// Represents a diary note returned by the backend.
@Serializable
data class DiaryNoteResponse(
    // The date the diary note belongs to.
    val date: String,

    // The text content written by the user.
    val noteText: String
)

// Represents the information sent when creating or updating a diary note.
@Serializable
data class UpdateNoteRequest(
    val noteText: String
) {

    // Checks that the diary note does not exceed the maximum
    // allowed length before it is saved.
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Prevents users from entering notes longer than 2000 characters.
        if (noteText.length > 2000) errors.add("Note must be 2000 characters or fewer")

        return errors
    }
}


// ------------------ END OF FILE ------------------ //