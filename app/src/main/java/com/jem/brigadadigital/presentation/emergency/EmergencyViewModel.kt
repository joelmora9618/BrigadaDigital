package com.jem.brigadadigital.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyViewModel(
    private val emergencyRepository: EmergencyRepository
) : ViewModel() {

    private val _emergencyState = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // FASE 11
    private val _suggestions = MutableStateFlow<List<AddressSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AddressSuggestion>> = _suggestions.asStateFlow()

    private val _selectedPosition = MutableStateFlow<org.maplibre.spatialk.geojson.Position?>(null)
    val selectedPosition: StateFlow<org.maplibre.spatialk.geojson.Position?> = _selectedPosition.asStateFlow()

    data class AddressSuggestion(
        val display_name: String,
        val lat: Double,
        val lon: Double
    )

    init {
        observeEmergencies()
    }

    private fun observeEmergencies() {
        viewModelScope.launch {
            emergencyRepository.observeActiveEmergency().collect { result ->
                result.onSuccess { emergency ->
                    if (emergency != null) {
                        _emergencyState.value = EmergencyState.Active(emergency)
                    } else {
                        _emergencyState.value = EmergencyState.Idle
                    }
                }.onFailure { e ->
                    // Mostramos el error en pantalla mediante el Toast
                    _errorMessage.value = "Error al escuchar emergencias: ${e.message}"
                    _emergencyState.value = EmergencyState.Idle
                }
            }
        }
    }

    fun respondToEmergency(emergencyId: String, uid: String, isGoing: Boolean) {
        viewModelScope.launch {
            val result = emergencyRepository.respondToEmergency(emergencyId, uid, isGoing)
            if (result.isSuccess && isGoing) {
                // TODO: In Phase 4, start foreground service for GPS tracking here
            } else if (result.isSuccess && !isGoing) {
                // Return to idle
                _emergencyState.value = EmergencyState.Idle
            } else if (result.isFailure) {
                _errorMessage.value = "Error al responder: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun triggerTestEmergency() {
        viewModelScope.launch {
            val result = emergencyRepository.createTestEmergency()
            result.onFailure { e ->
                _errorMessage.value = "Fallo al crear alerta: ${e.message}"
            }
        }
    }
    
    fun createEmergency(titulo: String, descripcion: String, tipo: String, direccion: String) {
        viewModelScope.launch {
            val event = com.jem.brigadadigital.domain.model.EmergencyEvent(
                id = "", // Automatic ID by Firestore
                titulo = titulo,
                descripcion = descripcion,
                direccion = direccion,
                tipo = tipo,
                isActive = true,
                timestamp = System.currentTimeMillis()
            )
            emergencyRepository.createEmergency(event)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // FASE 11
    fun searchAddress(query: String) {
        if (query.length < 3) {
            _suggestions.value = emptyList()
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1"
                val connection = java.net.URL(urlString).openConnection()
                connection.setRequestProperty("User-Agent", "BrigadaDigitalApp/1.0")
                val responseText = connection.getInputStream().bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(responseText)
                val newList = mutableListOf<AddressSuggestion>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    newList.add(AddressSuggestion(
                        display_name = obj.getString("display_name"),
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon")
                    ))
                }
                _suggestions.value = newList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onLocationSelected(lat: Double, lon: Double, address: String?) {
        _selectedPosition.value = org.maplibre.spatialk.geojson.Position(lon, lat)
        _suggestions.value = emptyList()
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    // Actualizar createEmergency para incluir coordenadas
    fun createEmergency(titulo: String, descripcion: String, tipo: String, direccion: String, lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            val ubicacion = if (lat != null && lon != null) {
                com.google.firebase.firestore.GeoPoint(lat, lon)
            } else {
                null
            }

            val event = com.jem.brigadadigital.domain.model.EmergencyEvent(
                id = "", // Automatic ID by Firestore
                titulo = titulo,
                descripcion = descripcion,
                direccion = direccion,
                tipo = tipo,
                isActive = true,
                timestamp = System.currentTimeMillis(),
                ubicacion = ubicacion
            )
            emergencyRepository.createEmergency(event)
            _selectedPosition.value = null // Reset position after creation
        }
    }
}

class EmergencyViewModelFactory(
    private val repository: EmergencyRepository = EmergencyRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EmergencyViewModel(repository) as T
    }
}
