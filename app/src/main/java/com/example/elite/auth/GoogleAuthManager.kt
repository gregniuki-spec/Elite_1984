package com.example.elite.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun signInWithGoogle(onSuccess: (FirebaseUser) -> Unit = {}) {
        scope.launch {
            _isLoading.value = true
            _authError.value = null

            try {
                val serverClientId = context.getString(R.string.default_web_client_id)
                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        _currentUser.value = user
                        onSuccess(user)
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Interactive Google sign-in was cancelled by user.", e)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "CredentialManager sign-in failed", e)
                _authError.value = e.localizedMessage ?: "Google Sign-In failed"
            } catch (e: Exception) {
                Log.e(TAG, "Authentication failed", e)
                _authError.value = e.localizedMessage ?: "Sign-In failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        auth.signOut()
        _currentUser.value = null
        onSignedOut()
    }

    fun cleanup() {
        auth.removeAuthStateListener(authStateListener)
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
    }
}
