package com.example.kenkosahealthapp_prog7314.data.local

import androidx.room.Entity
import com.example.kenkosahealthapp_prog7314.data.network.WaterIntakeDto

// One row per user per day.
// pendingSync = true means the value was changed offline and still needs uploading.
@Entity(tableName = "water_entries", primaryKeys = ["userId", "date"])
data class WaterEntryEntity(
    val userId: String,
    val date: String,
    val cupsConsumed: Int,
    val targetCups: Int,
    val pendingSync: Boolean
)

fun WaterEntryEntity.toDto() = WaterIntakeDto(
    date = date,
    cupsConsumed = cupsConsumed,
    targetCups = targetCups
)
// References:
// Android Developers (n.d.) Save data in a local database using Room. Available at:
//   https://developer.android.com/training/data-storage/room (Accessed: 5 November 2026).
// Android Developers (n.d.) Build an offline-first app. Available at:
//   https://developer.android.com/topic/architecture/data-layer/offline-first (Accessed: 5 November 2026).