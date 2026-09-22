package com.kenkosa.backend.repository

import com.google.firebase.cloud.FirestoreClient
import com.kenkosa.backend.models.DiaryNoteResponse

private const val NOTES_COLLECTION = "diaryNotes"

class NotesRepository {

    private val db get() = FirestoreClient.getFirestore()

    private fun docId(userId: String, date: String) = "${userId}_$date"

    suspend fun getTodaysNote(userId: String, date: String): DiaryNoteResponse {
        val doc = db.collection(NOTES_COLLECTION).document(docId(userId, date)).get().await()

        return if (doc.exists()) {
            DiaryNoteResponse(date = date, noteText = doc.getString("noteText") ?: "")
        } else {
            DiaryNoteResponse(date = date, noteText = "")
        }
    }

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