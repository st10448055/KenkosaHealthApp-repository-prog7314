package com.example.kenkosahealthapp_prog7314.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {

    // Live list for the UI. Rows waiting to be deleted are hidden immediately.
    @Query(
        "SELECT * FROM mood_entries " +
        "WHERE userId = :userId AND syncState != 'PENDING_DELETE' " +
        "ORDER BY loggedAt DESC"
    )
    fun observeAll(userId: String): Flow<List<MoodEntryEntity>>

    // Number of changes not yet uploaded.
    @Query("SELECT COUNT(*) FROM mood_entries WHERE userId = :userId AND syncState != 'SYNCED'")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("SELECT * FROM mood_entries WHERE moodId = :moodId LIMIT 1")
    suspend fun getById(moodId: String): MoodEntryEntity?

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND syncState = :state")
    suspend fun getByState(userId: String, state: String): List<MoodEntryEntity>

    // Insert or replace.
    @Upsert
    suspend fun upsert(entry: MoodEntryEntity)

    @Query("DELETE FROM mood_entries WHERE moodId = :moodId")
    suspend fun deleteById(moodId: String)

    // Removes synced rows that no longer exist on the server.
    @Query(
        "DELETE FROM mood_entries " +
        "WHERE userId = :userId AND syncState = 'SYNCED' AND moodId NOT IN (:keepIds)"
    )
    suspend fun deleteSyncedNotIn(userId: String, keepIds: List<String>)
}
// References:
// Android Developers (n.d.) Save data in a local database using Room. Available at:
//   https://developer.android.com/training/data-storage/room (Accessed: 5 November 2026).