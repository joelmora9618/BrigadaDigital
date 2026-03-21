package com.jem.brigadadigital.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.AuthRepositoryImpl
import com.jem.brigadadigital.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun checkCurrentUser() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            _authState.value = AuthState.Success(userId)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithEmail(email, pass)
            result.onSuccess { uid ->
                _authState.value = AuthState.Success(uid)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUpWithEmail(email, pass)
            result.onSuccess { uid ->
                _authState.value = AuthState.Success(uid)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { uid ->
                _authState.value = AuthState.Success(uid)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logOut() {
        authRepository.signOut()
        _authState.value = AuthState.Idle
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(repository) as T
    }
}