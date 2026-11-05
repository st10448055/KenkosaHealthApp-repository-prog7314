package com.example.kenkosahealthapp_prog7314.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun get(userId: String, date: String): WaterEntryEntity?

    @Query("SELECT * FROM water_entries WHERE userId = :userId AND pendingSync = 1")
    suspend fun getPending(userId: String): List<WaterEntryEntity>

    @Upsert
    suspend fun upsert(entry: WaterEntryEntity)
}
// References:
// Android Developers (n.d.) Save data in a local database using Room. Available at:
//   https://developer.android.com/training/data-storage/room (Accessed: 5 November 2026).