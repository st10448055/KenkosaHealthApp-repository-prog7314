package com.example.kenkosahealthapp_prog7314.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Authentication. Google sign-in uses Android's Credential Manager
 * (the replacement for the deprecated GoogleSignIn API).
 *
 * @param webClientId the *web* OAuth client id from the Firebase console (used as the server client id).
 */
class AuthRepository(context: Context, private val webClientId: String) {

    private val appContext: Context = context.applicationContext
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> = try {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("No user returned after sign up")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = try {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("No user returned after sign in")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Shows the Google account picker and returns a Google ID token.
     * Credential Manager needs an *Activity* context to draw its bottom sheet.
     */
    suspend fun requestGoogleIdToken(activityContext: Context): Result<String> = try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)   // show every Google account on the phone
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
            .credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.d(TAG, "Google credential received")
            Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential.type}")
            Result.failure(IllegalStateException("Unexpected credential type"))
        }
    } catch (e: GetCredentialException) {
        Log.w(TAG, "Credential Manager error: ${e.javaClass.simpleName}")
        Result.failure(e)
    } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Could not parse Google ID token", e)
        Result.failure(e)
    }

    /** Exchanges the Google ID token for a Firebase session. */
    suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("No user returned after Google sign in")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Firebase emails the user a reset link. Same result whether or not the address has an account. */
    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Log.d(TAG, "Password reset requested")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Password reset failed", e)
        Result.failure(e)
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? =
        firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    /** Signs out of Firebase and clears Credential Manager's remembered Google sign-in. */
    suspend fun signOut() {
        firebaseAuth.signOut()
        runCatching {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { Log.w(TAG, "Could not clear credential state", it) }
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}