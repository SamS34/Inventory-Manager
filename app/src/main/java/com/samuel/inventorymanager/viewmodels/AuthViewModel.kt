package com.samuel.inventorymanager.viewmodels

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch

    private lateinit var oneTapClient: SignInClient
    private val auth = FirebaseAuth.getInstance()

    fun initialize(context: Context) {
        oneTapClient = Identity.getSignInClient(context)
        checkAuthState(context)
    }

    private fun checkAuthState(context: Context) {
        viewModelScope.launch {
            try {
                // Check if user has completed onboarding
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val hasCompletedOnboarding = prefs.getBoolean("completed_onboarding", false)
                _isFirstLaunch.value = !hasCompletedOnboarding

                // Check if user is signed in
                val currentUser = auth.currentUser
                if (currentUser != null && hasCompletedOnboarding) {
                    _authState.value = AuthState.Authenticated(currentUser.email ?: "User")
                } else if (hasCompletedOnboarding) {
                    _authState.value = AuthState.NotAuthenticated
                } else {
                    _authState.value = AuthState.ShowOnboarding
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun completeOnboarding(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("completed_onboarding", true).apply()
        _isFirstLaunch.value = false
        _authState.value = AuthState.NotAuthenticated
    }

    fun startGoogleSignIn(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            // Replace with your actual Web Client ID from Firebase Console
                            .setServerClientId("604064044455-7rb4vc1arekkbi59999aprp8hamrtkhq.apps.googleusercontent.com")
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()

                val result = oneTapClient.beginSignIn(signInRequest).await()
                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                launcher.launch(intentSenderRequest)

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign in failed: ${e.message}")
            }
        }
    }

    fun handleSignInResult(intent: Intent?) {
        viewModelScope.launch {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(intent)
                val idToken = credential.googleIdToken

                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user

                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user.email ?: "User")
                    } else {
                        _authState.value = AuthState.Error("Authentication failed")
                    }
                } else {
                    _authState.value = AuthState.Error("No ID token received")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign in error: ${e.message}")
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            auth.signOut()
            oneTapClient.signOut().await()
            _authState.value = AuthState.NotAuthenticated
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object ShowOnboarding : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}