package com.jem.brigadadigital.presentation.auth

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val userUid: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
