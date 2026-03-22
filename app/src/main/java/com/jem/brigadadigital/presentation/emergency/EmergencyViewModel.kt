package com.jem.brigadadigital.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmergencyViewModel(
    private val emergencyRepository: EmergencyRepository,
    private val userRepository: com.jem.brigadadigital.domain.repository.UserRepository
) : ViewModel() {

    private var currentUser: com.jem.brigadadigital.domain.model.UserProfile? = null
    
    // Jobs to manage observers
    private var observeEmergenciesJob: kotlinx.coroutines.Job? = null
    private var observeAllActiveEmergenciesJob: kotlinx.coroutines.Job? = null
    private var observePastEmergenciesJob: kotlinx.coroutines.Job? = null
    private var observeRespondersJob: kotlinx.coroutines.Job? = null
    private var observeDetailedRespondersJob: kotlinx.coroutines.Job? = null

    private val _emergencyState = MutableStateFlow<EmergencyState>(EmergencyState.Idle)
    val emergencyState: StateFlow<EmergencyState> = _emergencyState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // FASE 11
    private val _suggestions = MutableStateFlow<List<AddressSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AddressSuggestion>> = _suggestions.asStateFlow()

    private val _selectedPosition = MutableStateFlow<org.maplibre.spatialk.geojson.Position?>(null)
    val selectedPosition: StateFlow<org.maplibre.spatialk.geojson.Position?> = _selectedPosition.asStateFlow()

    private val _allActiveEmergencies = MutableStateFlow<List<com.jem.brigadadigital.domain.model.EmergencyEvent>>(emptyList())
    val allActiveEmergencies: StateFlow<List<com.jem.brigadadigital.domain.model.EmergencyEvent>> = _allActiveEmergencies.asStateFlow()

    private val _pastEmergencies = MutableStateFlow<List<com.jem.brigadadigital.domain.model.EmergencyEvent>>(emptyList())
    val pastEmergencies: StateFlow<List<com.jem.brigadadigital.domain.model.EmergencyEvent>> = _pastEmergencies.asStateFlow()

    private val _allActiveRespondersCount = MutableStateFlow(0)
    val allActiveRespondersCount: StateFlow<Int> = _allActiveRespondersCount.asStateFlow()

    private val _activeResponders = MutableStateFlow<List<com.jem.brigadadigital.domain.model.DetailedResponder>>(emptyList())
    val activeResponders: StateFlow<List<com.jem.brigadadigital.domain.model.DetailedResponder>> = _activeResponders.asStateFlow()

    private val _respondedIds = MutableStateFlow<Set<String>>(emptySet())
    val respondedIds: StateFlow<Set<String>> = _respondedIds.asStateFlow()


    data class AddressSuggestion(
        val display_name: String,
        val lat: Double,
        val lon: Double
    )

    fun initSession(user: com.jem.brigadadigital.domain.model.UserProfile) {
        currentUser = user
        fetchRespondedIds(user.uid)
        restartObservers()
    }

    private fun fetchRespondedIds(uid: String) {
        viewModelScope.launch {
            emergencyRepository.getRespondedEmergencyIds(uid).onSuccess { ids ->
                _respondedIds.value = ids
            }
        }
    }

    private fun restartObservers() {
        observeEmergenciesJob?.cancel()
        observeAllActiveEmergenciesJob?.cancel()
        observePastEmergenciesJob?.cancel()
        observeRespondersJob?.cancel()
        observeDetailedRespondersJob?.cancel()

        observeEmergencies()
        observeAllActiveEmergencies()
        observePastEmergencies()
        observeAllActiveResponders()
        observeAllActiveRespondersDetailed()
    }

    private fun observeEmergencies() {
        observeEmergenciesJob = viewModelScope.launch {
            combine(
                emergencyRepository.observeAllActiveEmergencies(),
                _respondedIds
            ) { result, responded ->
                Pair(result, responded)
            }.collect { (result, responded) ->
                result.onSuccess { list ->
                    val userCuartel = currentUser?.cuartelId ?: ""
                    val myEmergencies = list.filter { it.isGlobal || it.cuartelId == userCuartel }
                    
                    val unrespondedCandidate = myEmergencies.firstOrNull { !responded.contains(it.id) }
                    
                    if (unrespondedCandidate != null) {
                        // Verificación puntual (Double-check) para evitar falsos positivos tras reinicio
                        val trulyResponded = emergencyRepository.checkIfUserResponded(unrespondedCandidate.id, currentUser?.uid ?: "").getOrDefault(false)
                        if (trulyResponded) {
                            _respondedIds.value = _respondedIds.value + unrespondedCandidate.id
                            _emergencyState.value = EmergencyState.Idle
                        } else {
                            _emergencyState.value = EmergencyState.Active(unrespondedCandidate)
                        }
                    } else {
                        _emergencyState.value = EmergencyState.Idle
                    }
                }.onFailure { e ->
                    _errorMessage.value = "Error al escuchar emergencias: ${e.message}"
                    _emergencyState.value = EmergencyState.Idle
                }
            }
        }
    }

    private fun observeAllActiveEmergencies() {
        observeAllActiveEmergenciesJob = viewModelScope.launch {
            emergencyRepository.observeAllActiveEmergencies().collect { result ->
                result.onSuccess { emergencies ->
                    val userCuartel = currentUser?.cuartelId ?: ""
                    _allActiveEmergencies.value = emergencies.filter { it.isGlobal || it.cuartelId == userCuartel }
                }
            }
        }
    }

    private fun observePastEmergencies() {
        observePastEmergenciesJob = viewModelScope.launch {
            emergencyRepository.getPastEmergencies().collect { result ->
                result.onSuccess { emergencies ->
                    val userCuartel = currentUser?.cuartelId ?: ""
                    _pastEmergencies.value = emergencies.filter { it.isGlobal || it.cuartelId == userCuartel }
                }
            }
        }
    }

    private fun observeAllActiveResponders() {
        // Para el contador, solo contamos los que están en mis alertas (Locales + Globales)
        observeRespondersJob = viewModelScope.launch {
            emergencyRepository.observeAllActiveRespondersDetailed().collect { result ->
                result.onSuccess { detailedPairs ->
                    val userCuartel = currentUser?.cuartelId ?: ""
                    val emergencies = _allActiveEmergencies.value.associateBy { it.id }
                    
                    val relevantCount = detailedPairs.count { (emergencyId, _) ->
                        val emergency = emergencies[emergencyId]
                        emergency != null && (emergency.isGlobal || emergency.cuartelId == userCuartel)
                    }
                    _allActiveRespondersCount.value = relevantCount
                }
            }
        }
    }

    private fun observeAllActiveRespondersDetailed() {
        observeDetailedRespondersJob = viewModelScope.launch {
            emergencyRepository.observeAllActiveRespondersDetailed().collect { result ->
                result.onSuccess { detailedPairs ->
                    if (detailedPairs.isEmpty()) {
                        _activeResponders.value = emptyList()
                        return@onSuccess
                    }

                    val userCuartel = currentUser?.cuartelId ?: ""
                    val relevantEmergencies = _allActiveEmergencies.value.associateBy { it.id }

                    val filteredPairs = detailedPairs.filter { (emergencyId, _) ->
                        val emergency = relevantEmergencies[emergencyId]
                        emergency != null && (emergency.isGlobal || emergency.cuartelId == userCuartel)
                    }

                    if (filteredPairs.isEmpty()) {
                        _activeResponders.value = emptyList()
                        return@onSuccess
                    }

                    val uids = filteredPairs.map { it.second.uid }.distinct()
                    val profilesResult = userRepository.getUserProfiles(uids)
                    val profilesMap = profilesResult.getOrDefault(emptyList()).associateBy { it.uid }
                    
                    val finalDetailedList = filteredPairs.mapNotNull { (emergencyId, response) ->
                        val profile = profilesMap[response.uid]
                        val emergency = relevantEmergencies[emergencyId]
                        if (profile != null && emergency != null) {
                            com.jem.brigadadigital.domain.model.DetailedResponder(
                                uid = response.uid,
                                nombre = profile.nombre,
                                rango = profile.rango,
                                destacamento = profile.cuartelId,
                                especialidad = profile.especialidad,
                                emergencyId = emergencyId,
                                emergencyTitle = emergency.titulo
                            )
                        } else null
                    }
                    _activeResponders.value = finalDetailedList
                }
            }
        }
    }

    fun respondToEmergency(emergencyId: String, uid: String, isGoing: Boolean) {
        viewModelScope.launch {
            val result = emergencyRepository.respondToEmergency(emergencyId, uid, isGoing)
            if (result.isSuccess) {
                // Actualizar set local para descartar la alerta inmediatamente
                _respondedIds.value = _respondedIds.value + emergencyId
                if (isGoing) {
                    // TODO: In Phase 4, start foreground service for GPS tracking here
                } else {
                    _emergencyState.value = EmergencyState.Idle
                }
            } else {
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

    private val _selectedAddress = MutableStateFlow<String?>(null)
    val selectedAddress: StateFlow<String?> = _selectedAddress.asStateFlow()

    fun onLocationSelected(lat: Double, lon: Double, address: String?) {
        _selectedPosition.value = org.maplibre.spatialk.geojson.Position(lon, lat)
        if (address != null) {
            _selectedAddress.value = address
        } else {
            // Reverse Geocoding
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val urlString = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&addressdetails=1"
                    val connection = java.net.URL(urlString).openConnection()
                    connection.setRequestProperty("User-Agent", "BrigadaDigitalApp/1.0")
                    val responseText = connection.getInputStream().bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(responseText)
                    val displayName = json.optString("display_name", "Ubicación en Mapa")
                    _selectedAddress.value = displayName
                } catch (e: Exception) {
                    e.printStackTrace()
                    _selectedAddress.value = "Ubicación Seleccionada ($lat, $lon)"
                }
            }
        }
        _suggestions.value = emptyList()
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    // Actualizar createEmergency para incluir coordenadas y local/global
    fun createEmergency(
        titulo: String, 
        descripcion: String, 
        tipo: String, 
        direccion: String, 
        isGlobal: Boolean,
        lat: Double? = null, 
        lon: Double? = null
    ) {
        viewModelScope.launch {
            val userCuartel = currentUser?.cuartelId ?: ""
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
                isGlobal = isGlobal,
                cuartelId = userCuartel,
                timestamp = System.currentTimeMillis(),
                ubicacion = ubicacion
            )
            emergencyRepository.createEmergency(event)
            _selectedPosition.value = null // Reset position after creation
            _selectedAddress.value = null
        }
    }

    fun resetState() {
        currentUser = null
        observeEmergenciesJob?.cancel()
        observeAllActiveEmergenciesJob?.cancel()
        observePastEmergenciesJob?.cancel()
        observeRespondersJob?.cancel()
        observeDetailedRespondersJob?.cancel()
        
        _emergencyState.value = EmergencyState.Idle
        _allActiveEmergencies.value = emptyList()
        _pastEmergencies.value = emptyList()
        _allActiveRespondersCount.value = 0
        _activeResponders.value = emptyList()
        _errorMessage.value = null
        _suggestions.value = emptyList()
        _selectedPosition.value = null
        _selectedAddress.value = null
    }
}

class EmergencyViewModelFactory(
    private val repository: EmergencyRepository = EmergencyRepositoryImpl(),
    private val userRepository: com.jem.brigadadigital.domain.repository.UserRepository = com.jem.brigadadigital.data.repository.UserRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EmergencyViewModel(repository, userRepository) as T
    }
}
