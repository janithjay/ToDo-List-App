package com.janithjayashan.todolistapp.auth

sealed class AuthenticationState {
    object Loading : AuthenticationState()
    object Authenticated : AuthenticationState()
    object Unauthenticated : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}
