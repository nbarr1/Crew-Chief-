package com.example.core.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val isFirebaseInitialized: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    private val auth: FirebaseAuth?
        get() = if (isFirebaseInitialized) FirebaseAuth.getInstance() else null

    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        val currentAuth = auth ?: return Result.failure(Exception("Firebase is not configured. Please add google-services.json."))
        try {
            // NOTE: To make this functional, the user must provide a GOOGLE_WEB_CLIENT_ID
            // in their .env file (Secrets Panel) and setup Firebase in their Google Cloud Project.
            val webClientId = try {
                BuildConfig::class.java.getField("GOOGLE_WEB_CLIENT_ID").get(null) as String
            } catch (e: Exception) {
                // Fallback dummy for compilation if missing
                "YOUR_WEB_CLIENT_ID"
            }

            if (webClientId == "YOUR_WEB_CLIENT_ID" || webClientId.isEmpty()) {
                return Result.failure(Exception("Missing GOOGLE_WEB_CLIENT_ID in Secrets."))
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = currentAuth.signInWithCredential(firebaseCredential).await()
                return Result.success(authResult.user!!)
            } else {
                return Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }

        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In failed", e)
            return Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
    }
}
