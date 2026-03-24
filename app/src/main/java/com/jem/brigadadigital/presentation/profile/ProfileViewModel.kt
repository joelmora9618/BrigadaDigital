package com.jem.brigadadigital.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.UserRepositoryImpl
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _rawAvailablePersonnelList = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _activeResponderUids = MutableStateFlow<Set<String>>(emptySet())

    val availablePersonnelList: StateFlow<List<UserProfile>> = combine(
        _rawAvailablePersonnelList,
        _activeResponderUids
    ) { rawList, activeUids ->
        rawList.filter { it.uid !in activeUids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availablePersonnelCount: StateFlow<Int> = availablePersonnelList
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var observeAvailableJob: kotlinx.coroutines.Job? = null

    fun checkUserProfile(uid: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!isRefresh) {
                    _profileState.value = ProfileState.Loading
                }
                _isRefreshing.value = true
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
                    if (!isRefresh) {
                        _profileState.value = ProfileState.Error(e.message ?: "Error al cargar perfil")
                    }
                }
            } catch (e: Exception) {
                if (!isRefresh) {
                    _profileState.value = ProfileState.Error(e.message ?: "Error critico")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun refresh(uid: String) {
        _isRefreshing.value = true
        try {
            // Ponemos un timeout de 5 segundos para que el indicador no se quede pegado si falla la red
            kotlinx.coroutines.withTimeout(5000) {
                val result = userRepository.getUserProfile(uid)
                result.onSuccess { profile ->
                    if (profile != null) {
                        _profileState.value = ProfileState.Loaded(profile)
                        observeProfile(uid)
                        observeAvailablePersonnel(profile.cuartelId)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Refresh", "Timeout o error en refresh", e)
        } finally {
            _isRefreshing.value = false
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
            userRepository.observeAvailablePersonnelList(cuartelId).collect { result ->
                result.onSuccess { list ->
                    _rawAvailablePersonnelList.value = list
                }
            }
        }
    }

    fun setActiveResponders(uids: Set<String>) {
        _activeResponderUids.value = uids
    }

    fun resetState() {
        observeAvailableJob?.cancel()
        _profileState.value = ProfileState.Idle
        _rawAvailablePersonnelList.value = emptyList()
        _activeResponderUids.value = emptySet()
    }

    fun updateFcmToken(uid: String, token: String) {
        viewModelScope.launch {
            userRepository.updateFcmToken(uid, token)
        }
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
