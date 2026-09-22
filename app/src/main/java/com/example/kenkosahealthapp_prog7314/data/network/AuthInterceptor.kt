package com.example.kenkosahealthapp_prog7314.data.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

// OkHttp interceptor responsible for adding the authenticated
// Firebase user's ID token to outgoing API requests.
class AuthInterceptor : Interceptor {

    // Intercepts every HTTP request before it is sent to the backend.
    override fun intercept(chain: Interceptor.Chain): Response {

        // Retrieve the currently signed-in Firebase user.
        // If no user is signed in, currentUser will be null.
        val user = FirebaseAuth.getInstance().currentUser

        // Attempt to retrieve the Firebase ID token for the
        // currently authenticated user.
        val token = user?.let {

            try {
                // Request the user's Firebase ID token.
                // getIdToken(false) uses the existing cached token
                // when it is still valid instead of forcing a refresh.
                // Tasks.await() waits for the Firebase task to complete,
                // with a maximum timeout of 10 seconds.
                Tasks.await(
                    it.getIdToken(false),
                    10,
                    TimeUnit.SECONDS
                ).token

            } catch (e: Exception) {

                // If the token cannot be retrieved, return null.
                // The request will then be sent without an Authorization header.
                null
            }
        }

        // Create the HTTP request that will be sent to the backend.
        val request = if (token != null) {

            // If a Firebase ID token was successfully retrieved,
            // add it to the Authorization header using the Bearer scheme.
            // The backend can use this token to verify the user's
            // Firebase identity before processing the request.
            chain.request().newBuilder()
                .addHeader(
                    "Authorization",
                    "Bearer $token"
                )
                .build()

        } else {

            // If no authenticated user or token is available,
            // send the original request without an Authorization header.
            chain.request()
        }

        // Continue the request chain and send the request to the backend.
        return chain.proceed(request)
    }
}

// ------------------ END OF FILE ------------------ //