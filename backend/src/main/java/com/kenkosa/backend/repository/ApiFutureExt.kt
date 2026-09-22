package com.kenkosa.backend.repository

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Allows Firebase ApiFuture operations to be used with Kotlin coroutines.
suspend fun <T> ApiFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
    ApiFutures.addCallback(
        this,
        object : ApiFutureCallback<T> {
            // Resume the coroutine when the Firebase operation succeeds.
            override fun onSuccess(result: T) {
                continuation.resume(result)
            }

            // Pass the error back to the coroutine if the operation fails.
            override fun onFailure(t: Throwable) {
                continuation.resumeWithException(t)
            }
        },
        MoreExecutors.directExecutor()
    )
}

// ------------------ END OF FILE ------------------ //