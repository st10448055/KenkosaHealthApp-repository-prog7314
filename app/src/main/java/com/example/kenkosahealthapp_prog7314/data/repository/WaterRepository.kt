package com.example.kenkosahealthapp_prog7314.data.repository

import android.content.Context
import com.example.kenkosahealthapp_prog7314.KenkosaApp
import com.example.kenkosahealthapp_prog7314.data.local.AppDatabase
import com.example.kenkosahealthapp_prog7314.data.local.WaterDao
import com.example.kenkosahealthapp_prog7314.data.local.WaterEntryEntity
import com.example.kenkosahealthapp_prog7314.data.local.toDto
import com.example.kenkosahealthapp_prog7314.data.network.RetrofitInstance
import com.example.kenkosahealthapp_prog7314.data.network.UpdateWaterRequest
import com.example.kenkosahealthapp_prog7314.data.network.WaterApi
import com.example.kenkosahealthapp_prog7314.data.network.WaterIntakeDto
import com.example.kenkosahealthapp_prog7314.data.sync.SyncScheduler
import com.example.kenkosahealthapp_prog7314.data.sync.isPermanentClientError
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

// Offline-first repository for daily water intake.
// Conflict rule: the most recent local change wins.
class WaterRepository(
    private val api: WaterApi = RetrofitInstance.waterApi,
    private val dao: WaterDao = AppDatabase.get(KenkosaApp.instance).waterDao(),
    private val appContext: Context = KenkosaApp.instance
) {

    private fun uid(): String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private fun today(): String = LocalDate.now().toString()

    // Today's water intake. Uses the server when reachable, otherwise the cached copy.
    suspend fun getTodaysWater(): ApiResult<WaterIntakeDto> {
        val userId = uid()
        val local = dao.get(userId, today())

        // Unsynced local changes are newer than the server, so show those.
        if (local?.pendingSync == true) {
            SyncScheduler.syncNow(appContext)
            return ApiResult.Success(local.toDto())
        }

        return when (val result = safeCall { api.getTodaysWater() }) {
            is ApiResult.Success -> {
                // Cache the latest server value.
                dao.upsert(
                    WaterEntryEntity(
                        userId = userId,
                        date = today(),
                        cupsConsumed = result.data.cupsConsumed,
                        targetCups = result.data.targetCups,
                        pendingSync = false
                    )
                )
                result
            }

            // Offline: fall back to the cached value if there is one.
            is ApiResult.NetworkError ->
                if (local != null) ApiResult.Success(local.toDto()) else result

            else -> result
        }
    }

    // Saves the new total locally and queues it for upload.
    suspend fun updateWater(cupsConsumed: Int): ApiResult<WaterIntakeDto> {

        if (cupsConsumed < 0) {
            return ApiResult.ValidationError("Cups consumed cannot be negative")
        }
        if (cupsConsumed > 20) {
            return ApiResult.ValidationError("That seems like an unusually high number of cups")
        }

        val userId = uid()
        val existing = dao.get(userId, today())

        val entity = WaterEntryEntity(
            userId = userId,
            date = today(),
            cupsConsumed = cupsConsumed,
            targetCups = existing?.targetCups ?: DEFAULT_TARGET,
            pendingSync = true
        )

        dao.upsert(entity)
        SyncScheduler.syncNow(appContext)
        return ApiResult.Success(entity.toDto())
    }

    // Uploads unsynced water changes. Network errors are thrown so SyncWorker can retry.
    suspend fun syncAll() = syncMutex.withLock {
        val userId = uid()
        if (userId.isEmpty()) return@withLock

        for (entry in dao.getPending(userId)) {

            // The server's PATCH always writes to the server's "today", so a change
            // from an earlier day cannot be uploaded safely and is dropped.
            if (entry.date != today()) {
                dao.upsert(entry.copy(pendingSync = false))
                continue
            }

            try {
                val saved = api.updateWater(UpdateWaterRequest(entry.cupsConsumed))
                dao.upsert(
                    entry.copy(
                        cupsConsumed = saved.cupsConsumed,
                        targetCups = saved.targetCups,
                        pendingSync = false
                    )
                )
            } catch (ex: HttpException) {
                if (ex.isPermanentClientError()) {
                    dao.upsert(entry.copy(pendingSync = false))
                } else {
                    throw ex
                }
            }
        }
    }

    // Executes an API operation safely and converts exceptions
    // into the appropriate ApiResult type.
    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string() ?: e.message()
        ApiResult.ServerError(e.code(), message ?: "Server error")
    } catch (e: IOException) {
        ApiResult.NetworkError("Couldn't reach the server. Check your connection.")
    } catch (e: Exception) {
        ApiResult.ServerError(0, e.message ?: "Unknown error")
    }

    companion object {
        private const val DEFAULT_TARGET = 8
        private val syncMutex = Mutex()
    }
}
