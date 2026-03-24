package com.jem.brigadadigital.presentation.movil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.MovilRepositoryImpl
import com.jem.brigadadigital.domain.model.Movil
import com.jem.brigadadigital.domain.repository.MovilRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MovilViewModel(
    private val repository: MovilRepository
) : ViewModel() {

    private val _moviles = MutableStateFlow<List<Movil>>(emptyList())
    val moviles: StateFlow<List<Movil>> = _moviles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun observeMoviles(cuartelId: String) {
        if (cuartelId.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            repository.observeMovilesByCuartel(cuartelId)
                .catch { _isLoading.value = false }
                .collect { result ->
                    _isLoading.value = false
                    result.onSuccess { list ->
                        _moviles.value = list
                    }
                }
        }
    }

    fun addMovil(nombre: String, tipo: String, patente: String, cuartelId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val nuevoMovil = Movil(
                nombre = nombre,
                tipo = tipo,
                patente = patente,
                cuartelId = cuartelId
            )
            val result = repository.addMovil(nuevoMovil)
            _isLoading.value = false
            result.onFailure { e ->
                _error.value = "Fallo al guardar móvil: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class MovilViewModelFactory(
    private val repository: MovilRepository = MovilRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MovilViewModel(repository) as T
    }
}
