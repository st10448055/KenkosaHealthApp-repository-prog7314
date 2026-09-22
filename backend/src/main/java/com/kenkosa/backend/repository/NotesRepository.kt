package com.kenkosa.backend.repository

import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.DiaryNoteResponse

// Firestore collection used to store diary notes.
private const val NOTES_COLLECTION = "diaryNotes"

class NotesRepository {

    // Gets the Firestore database instance.
    private val db get() = FirestoreClient.getFirestore()

    // Creates a unique document ID using the user and date.
    private fun docId(userId: String, date: String) = "${userId}_$date"

    // Retrieves the diary note for a specific user and date.
    suspend fun getTodaysNote(userId: String, date: String): DiaryNoteResponse {
        val doc = db.collection(NOTES_COLLECTION).document(docId(userId, date)).get().await()

        // Return the saved note, or an empty note if none exists.
        return if (doc.exists()) {
            DiaryNoteResponse(date = date, noteText = doc.getString("noteText") ?: "")
        } else {
            DiaryNoteResponse(date = date, noteText = "")
        }
    }

    // Creates or updates the diary note for the specified date.
    suspend fun updateNote(userId: String, date: String, noteText: String): DiaryNoteResponse {
        val data = mapOf(
            "userId" to userId,
            "date" to date,
            "noteText" to noteText
        )

        db.collection(NOTES_COLLECTION).document(docId(userId, date)).set(data).await()

        return DiaryNoteResponse(date = date, noteText = noteText)
    }
}

// ------------------ END OF FILE ------------------ //