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

// Repository responsible for handling user authentication.
// Firebase Authentication is used for email/password authentication
// and Google Sign-In. The repository keeps authentication logic
// separate from the application's UI.

class AuthRepository(
    context: Context,
    webClientId: String
) {

    // Firebase Authentication instance used to manage signed-in users.
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Google Sign-In client used to start and manage Google authentication.
    private val googleSignInClient: GoogleSignInClient

    init {

        // Configures the Google Sign-In options.
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )

            // Requests an ID token that can be used to authenticate
            // the user with Firebase.
            .requestIdToken(webClientId)

            // Requests the user's email address.
            .requestEmail()

            .build()

        // Creates the Google Sign-In client using the configured options.
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    // Returns the currently authenticated Firebase user.
    // Returns null when no user is signed in.
    fun currentUser(): FirebaseUser? =
        firebaseAuth.currentUser


    // Creates a new Firebase account using an email address and password.
    suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = try {

        // Sends the registration details to Firebase Authentication.
        val result = firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .await()

        // Retrieves the newly created Firebase user.
        val user = result.user
            ?: throw IllegalStateException(
                "No user returned after sign up"
            )

        // Returns the newly created user as a successful Result.
        Result.success(user)

    } catch (e: Exception) {

        // Returns the Firebase error as a failed Result.
        Result.failure(e)
    }


    // Signs an existing user in using their email and password.
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = try {

        // Sends the login credentials to Firebase Authentication.
        val result = firebaseAuth
            .signInWithEmailAndPassword(email, password)
            .await()

        // Retrieves the authenticated Firebase user.
        val user = result.user
            ?: throw IllegalStateException(
                "No user returned after sign in"
            )

        // Returns the authenticated user as a successful Result.
        Result.success(user)

    } catch (e: Exception) {

        // Returns the authentication error as a failed Result.
        Result.failure(e)
    }


    // Creates the Intent required to launch Google's Sign-In screen.
    fun getGoogleSignInIntent(): Intent =
        googleSignInClient.signInIntent


    // Authenticates with Firebase using a Google ID token.
    suspend fun firebaseAuthWithGoogle(
        idToken: String
    ): Result<FirebaseUser> = try {

        // Converts the Google ID token into a Firebase credential.
        val credential = GoogleAuthProvider.getCredential(
            idToken,
            null
        )

        // Signs into Firebase using the Google credential.
        val result = firebaseAuth
            .signInWithCredential(credential)
            .await()

        // Retrieves the authenticated Firebase user.
        val user = result.user
            ?: throw IllegalStateException(
                "No user returned after Google sign in"
            )

        // Returns the authenticated user as a successful Result.
        Result.success(user)

    } catch (e: Exception) {

        // Returns the authentication error as a failed Result.
        Result.failure(e)
    }

    /**
     * Retrieves the current user's Firebase ID token.
     *
     * The ID token is used by the application's AuthInterceptor
     * when making authenticated requests to the backend API.
     *
     * forceRefresh determines whether Firebase should request
     * a fresh token instead of using the cached token.
     */
    suspend fun getIdToken(
        forceRefresh: Boolean = false
    ): String? =
        firebaseAuth.currentUser
            ?.getIdToken(forceRefresh)
            ?.await()
            ?.token


     // Signs the user out of both Firebase Authentication
     // and Google Sign-In.
    fun signOut() {
        // Signs the user out of Firebase.
        firebaseAuth.signOut()

        // Signs the user out of the Google Sign-In client.
        googleSignInClient.signOut()
    }
}

// ------------------ END OF FILE ------------------ //