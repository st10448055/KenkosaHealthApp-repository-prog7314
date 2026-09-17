package com.example.kenkosahealthapp_prog7314.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context, webClientId: String) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

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

    fun getGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("No user returned after Google sign in")
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? =
        firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut()
    }
}