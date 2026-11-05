package com.example.kenkosahealthapp_prog7314.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto

// Tracks whether a local row matches the server or has changes waiting to upload.
object SyncState {
    const val SYNCED = "SYNCED"
    const val PENDING_CREATE = "PENDING_CREATE"
    const val PENDING_UPDATE = "PENDING_UPDATE"
    const val PENDING_DELETE = "PENDING_DELETE"
}

// Local copy of a mood entry.
// userId keeps different accounts' data separate on the same device.
@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey val moodId: String,
    val userId: String,
    val mood: String,
    val feelings: List<String>,
    val loggedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: String
)

fun MoodEntryEntity.toDto() = MoodEntryDto(
    moodId = moodId,
    userId = userId,
    mood = mood,
    feelings = feelings,
    loggedAt = loggedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MoodEntryDto.toEntity(userId: String, syncState: String) = MoodEntryEntity(
    moodId = moodId,
    userId = userId,
    mood = mood,
    feelings = feelings,
    loggedAt = loggedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncState = syncState
)
// References:
// Android Developers (n.d.) Save data in a local database using Room. Available at:
//   https://developer.android.com/training/data-storage/room (Accessed: 5 November 2026).
// Android Developers (n.d.) Build an offline-first app. Available at:
//   https://developer.android.com/topic/architecture/data-layer/offline-first (Accessed: 5 November 2026).