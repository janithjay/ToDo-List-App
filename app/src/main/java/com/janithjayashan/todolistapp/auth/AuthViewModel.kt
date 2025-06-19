package com.janithjayashan.todolistapp.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.janithjayashan.todolistapp.utils.FirebaseBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unauthenticated)
    val authState: StateFlow<AuthenticationState> = _authState

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        auth.currentUser?.let {
            _authState.value = AuthenticationState.Authenticated
        } ?: run {
            _authState.value = AuthenticationState.Unauthenticated
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthenticationState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthenticationState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthenticationState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthenticationState.Loading
                auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthenticationState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthenticationState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun getContext(): Application {
        return getApplication()
    }

    fun signOut() {
        viewModelScope.launch {
            val backupManager = FirebaseBackupManager(getContext())
            // Backup before signing out
            backupManager.backupToFirebase()
            // Clear local data
            backupManager.clearLocalData()
            // Sign out from Firebase
            auth.signOut()
            _authState.value = AuthenticationState.Unauthenticated
        }
    }
}
