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

    private val _availablePersonnelCount = MutableStateFlow(0)
    val availablePersonnelCount: StateFlow<Int> = _availablePersonnelCount.asStateFlow()

    private var observeAvailableJob: kotlinx.coroutines.Job? = null

    fun checkUserProfile(uid: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = userRepository.getUserProfile(uid)
            result.onSuccess { profile ->
                if (profile != null) {
                    _profileState.value = ProfileState.Loaded(profile)
                    observeProfile(uid)
                    observeAvailablePersonnel(profile.cuartelId)
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
                        observeAvailablePersonnel(profile.cuartelId)
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

    fun toggleAvailability(uid: String, disponible: Boolean) {
        viewModelScope.launch {
            val result = userRepository.updateAvailability(uid, disponible)
            result.onFailure { e ->
                // Mostramos explícitamente el error en caso de que sea un problema de Permisos en Firestore
                val errorMsg = e.message ?: "Error desconocido"
                _profileState.value = ProfileState.Error("Fallo al actualizar estado. Verifica reglas Firestore: $errorMsg")
            }
        }
    }

    private fun observeAvailablePersonnel(cuartelId: String) {
        if (cuartelId.isEmpty()) return
        observeAvailableJob?.cancel()
        observeAvailableJob = viewModelScope.launch {
            userRepository.observeAvailablePersonnel(cuartelId).collect { result ->
                result.onSuccess { count ->
                    _availablePersonnelCount.value = count
                }
            }
        }
    }

    fun resetState() {
        observeAvailableJob?.cancel()
        _profileState.value = ProfileState.Idle
        _availablePersonnelCount.value = 0
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
