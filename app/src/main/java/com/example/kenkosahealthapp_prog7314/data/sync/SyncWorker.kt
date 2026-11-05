package com.example.kenkosahealthapp_prog7314.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kenkosahealthapp_prog7314.data.repository.MoodRepository
import com.example.kenkosahealthapp_prog7314.data.repository.WaterRepository
import com.google.firebase.auth.FirebaseAuth
import retrofit2.HttpException
import java.io.IOException
import com.example.kenkosahealthapp_prog7314.notifications.FcmTokenManager

// Background worker that uploads offline changes, then refreshes from the server.
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        // Nobody signed in, so there is nothing to sync.
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        return try {
            MoodRepository().syncAll()
            WaterRepository().syncAll()
            // If the push token could not be registered earlier (offline), do it now.
            if (!FcmTokenManager.syncPending(applicationContext)) return Result.retry()
            Result.success()

        } catch (e: IOException) {
            // Connection dropped mid-sync: try again later with backoff.
            Result.retry()

        } catch (e: HttpException) {
            // Server or auth problem (e.g. 5xx, expired token): try again later.
            Result.retry()

        } catch (e: Exception) {
            Result.failure()
        }
    }
}

// A 4xx error that will never succeed on retry (bad data, record gone).
// 401/403 (auth), 408 (timeout) and 429 (rate limit) are treated as temporary.
fun HttpException.isPermanentClientError(): Boolean =
    code() in 400..499 && code() !in listOf(401, 403, 408, 429)
