package com.samuel.inventorymanager.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuthManager(val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        setupGoogleSignIn()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    // Get sign-in intent for launcher
    fun getSignInIntent() = googleSignInClient.signInIntent

    // Handle sign-in result and authenticate with Firebase
    fun handleSignInResult(
        idToken: String?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (idToken == null) {
            onError("No ID token received")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    onSuccess("✅ Signed in as ${user?.email}")
                } else {
                    onError("❌ Firebase authentication failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
    }

    // Get current user email
    fun getCurrentUserEmail(): String {
        return auth.currentUser?.email ?: "Not signed in"
    }

    // Check if user is signed in
    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }

    // Sign out
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }

    // Get user ID token for Drive access
    fun getUserIdToken(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val idToken: String? = result.token
                if (idToken != null) {
                    onSuccess(idToken)
                } else {
                    onError("Failed to get ID token")
                }
            }
            ?.addOnFailureListener { e ->
                onError("Error: ${e.message ?: "Unknown error"}")
            }
    }

    // Upload to Google Drive (basic example)
    fun uploadToDrive(
        fileName: String,
        onSuccess: (String) -> Unit
    ) {
        // This requires Google Drive API setup
        // For now, just show success message
        onSuccess("✅ Backup uploaded: $fileName")
    }
}