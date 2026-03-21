package com.jem.brigadadigital.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.UserRepositoryImpl
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    fun checkUserProfile(uid: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = userRepository.getUserProfile(uid)
            result.onSuccess { profile ->
                if (profile != null) {
                    _profileState.value = ProfileState.Loaded(profile)
                    observeProfile(uid)
                } else {
                    _profileState.value = ProfileState.NotFound
                }
            }.onFailure { e ->
                _profileState.value = ProfileState.Error(e.message ?: "Error al cargar perfil")
            }
        }
    }

    private fun observeProfile(uid: String) {
        viewModelScope.launch {
            userRepository.observeUserProfile(uid).collect { result ->
                result.onSuccess { profile ->
                    if (profile != null) {
                        _profileState.value = ProfileState.Loaded(profile)
                    } else {
                        _profileState.value = ProfileState.NotFound
                    }
                }.onFailure { e ->
                    _profileState.value = ProfileState.Error(e.message ?: "Error de sincronización")
                }
            }
        }
    }

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = userRepository.saveUserProfile(profile)
            result.onSuccess {
                _profileState.value = ProfileState.Saved(profile.uid)
                // Start observing changes after saving
                observeProfile(profile.uid)
            }.onFailure { e ->
                _profileState.value = ProfileState.Error(e.message ?: "Error al guardar perfil")
            }
        }
    }

    fun toggleAvailability(uid: String, isAvailable: Boolean) {
        viewModelScope.launch {
            userRepository.updateAvailability(uid, isAvailable)
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}

class ProfileViewModelFactory(
    private val repository: UserRepository = UserRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(repository) as T
    }
}
