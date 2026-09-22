package com.example.kenkosahealthapp_prog7314.data.network


 // Data Transfer Object (DTO) representing a diary note returned by the backend.
 // The note is associated with a specific date and contains the text
// entered by the user.
data class DiaryNoteDto(

    // Date that the diary note belongs to.
    // This is expected to use the date format used by the backend.
    val date: String,

    // Text content of the user's diary note.
    val noteText: String
)


 // Request object used when updating an existing diary note.
 // Only the note text needs to be sent because the backend determines
 // which user's note is being updated through authentication and the
 // associated date/request context.
data class UpdateNoteRequest(

    // New text that should be stored for the diary note.
    val noteText: String
)

// ------------------ END OF FILE ------------------ //