package com.janithjayashan.todolistapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
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

    fun signOut() {
        auth.signOut()
        _authState.value = AuthenticationState.Unauthenticated
    }
}
