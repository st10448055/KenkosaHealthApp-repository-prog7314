package com.example.kenkosahealthapp_prog7314.data.repository

import android.content.Context
import com.example.kenkosahealthapp_prog7314.KenkosaApp
import com.example.kenkosahealthapp_prog7314.data.local.AppDatabase
import com.example.kenkosahealthapp_prog7314.data.local.MoodDao
import com.example.kenkosahealthapp_prog7314.data.local.MoodEntryEntity
import com.example.kenkosahealthapp_prog7314.data.local.SyncState
import com.example.kenkosahealthapp_prog7314.data.local.toDto
import com.example.kenkosahealthapp_prog7314.data.local.toEntity
import com.example.kenkosahealthapp_prog7314.data.network.CreateMoodRequest
import com.example.kenkosahealthapp_prog7314.data.network.MoodApi
import com.example.kenkosahealthapp_prog7314.data.network.MoodEntryDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateMoodRequest
import com.example.kenkosahealthapp_prog7314.data.sync.NetworkMonitor
import com.example.kenkosahealthapp_prog7314.data.sync.SyncScheduler
import com.example.kenkosahealthapp_prog7314.data.sync.isPermanentClientError
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.util.UUID

// Offline-first repository for mood entries.
// The UI only ever reads from and writes to the local Room database.
// Changes are marked with a sync state and uploaded by syncAll().
class MoodRepository(
    private val api: MoodApi = RetrofitInstance.moodApi,
    private val dao: MoodDao = AppDatabase.get(KenkosaApp.instance).moodDao(),
    private val appContext: Context = KenkosaApp.instance
) {

    private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // ------------------------------------------------------------ reading

    // Live list of moods from the local database.
    fun observeMoods(): Flow<List<MoodEntryDto>> =
        dao.observeAll(uid()).map { list -> list.map { it.toDto() } }

    // Number of changes still waiting to upload.
    fun observePendingCount(): Flow<Int> = dao.observePendingCount(uid())

    // Loads one mood from the local database (works offline).
    suspend fun getMood(moodId: String): ApiResult<MoodEntryDto> {
        val local = dao.getById(moodId)
        return if (local != null) {
            ApiResult.Success(local.toDto())
        } else {
            ApiResult.ServerError(404, "Mood entry not found")
        }
    }

    // ------------------------------------------------------------ writing

    // Saves a new mood locally with a temporary ID and queues it for upload.
    suspend fun createMood(
        mood: String,
        feelings: List<String>,
        loggedAt: Long
    ): ApiResult<MoodEntryDto> {

        if (mood.isBlank()) return ApiResult.ValidationError("Please choose a mood")

        val now = System.currentTimeMillis()
        val entity = MoodEntryEntity(
            moodId = "local-${UUID.randomUUID()}",
            userId = uid(),
            mood = mood,
            feelings = feelings,
            loggedAt = loggedAt,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING_CREATE
        )

        dao.upsert(entity)
        SyncScheduler.syncNow(appContext)
        return ApiResult.Success(entity.toDto())
    }

    // Updates a mood locally and queues the change for upload.
    suspend fun updateMood(
        moodId: String,
        mood: String,
        feelings: List<String>,
        loggedAt: Long
    ): ApiResult<MoodEntryDto> {

        val existing = dao.getById(moodId)
            ?: return ApiResult.ServerError(404, "Mood entry not found")

        // A row that was never uploaded stays a "create"; otherwise it becomes an "update".
        val newState =
            if (existing.syncState == SyncState.PENDING_CREATE) SyncState.PENDING_CREATE
            else SyncState.PENDING_UPDATE

        val updated = existing.copy(
            mood = mood,
            feelings = feelings,
            loggedAt = loggedAt,
            updatedAt = System.currentTimeMillis(),
            syncState = newState
        )

        dao.upsert(updated)
        SyncScheduler.syncNow(appContext)
        return ApiResult.Success(updated.toDto())
    }

    // Deletes a mood locally and queues the deletion for upload.
    suspend fun deleteMood(moodId: String): ApiResult<Unit> {
        val existing = dao.getById(moodId) ?: return ApiResult.Success(Unit)

        if (existing.syncState == SyncState.PENDING_CREATE) {
            // Never reached the server, so just remove it locally.
            dao.deleteById(moodId)
        } else {
            // Hide it now, delete on the server during the next sync.
            dao.upsert(existing.copy(syncState = SyncState.PENDING_DELETE))
            SyncScheduler.syncNow(appContext)
        }
        return ApiResult.Success(Unit)
    }

    // ------------------------------------------------------------ syncing

    // Called by the UI on screen open. Skips quietly if offline or if the sync fails.
    suspend fun refresh() {
        if (!NetworkMonitor.isOnline.value) return
        try {
            syncAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Stay on cached data; the worker will retry.
        }
    }

    // Uploads pending changes, then downloads the latest list.
    // Network errors are thrown so SyncWorker can retry.
    suspend fun syncAll() = syncMutex.withLock {
        if (uid().isEmpty()) return@withLock
        pushPending()
        pullRemote()
    }

    private suspend fun pushPending() {
        val userId = uid()

        // 1. Creates: swap the temporary local ID for the server's ID.
        for (e in dao.getByState(userId, SyncState.PENDING_CREATE)) {
            try {
                val created = api.createMood(CreateMoodRequest(e.mood, e.feelings, e.loggedAt))
                dao.deleteById(e.moodId)
                dao.upsert(created.toEntity(userId, SyncState.SYNCED))
            } catch (ex: HttpException) {
                // Server rejected it for good (e.g. validation): drop it.
                if (ex.isPermanentClientError()) dao.deleteById(e.moodId) else throw ex
            }
        }

        // 2. Updates.
        for (e in dao.getByState(userId, SyncState.PENDING_UPDATE)) {
            try {
                val updated = api.updateMood(
                    e.moodId,
                    UpdateMoodRequest(e.mood, e.feelings, e.loggedAt)
                )
                dao.upsert(updated.toEntity(userId, SyncState.SYNCED))
            } catch (ex: HttpException) {
                // Gone or rejected: remove local copy; pullRemote restores the server version.
                if (ex.isPermanentClientError()) dao.deleteById(e.moodId) else throw ex
            }
        }

        // 3. Deletes.
        for (e in dao.getByState(userId, SyncState.PENDING_DELETE)) {
            try {
                api.deleteMood(e.moodId)
                dao.deleteById(e.moodId)
            } catch (ex: HttpException) {
                if (ex.isPermanentClientError()) dao.deleteById(e.moodId) else throw ex
            }
        }
    }

    // Refreshes local data from the server without overwriting unsynced changes.
    private suspend fun pullRemote() {
        val userId = uid()
        val remote = api.getMoods()

        for (dto in remote) {
            val local = dao.getById(dto.moodId)
            if (local == null || local.syncState == SyncState.SYNCED) {
                dao.upsert(dto.toEntity(userId, SyncState.SYNCED))
            }
        }

        // Remove synced rows that were deleted elsewhere.
        dao.deleteSyncedNotIn(userId, remote.map { it.moodId })
    }

    companion object {
        // Shared across all instances so the worker and the UI never sync at the same time
        // (prevents duplicate uploads).
        private val syncMutex = Mutex()
    }
}
