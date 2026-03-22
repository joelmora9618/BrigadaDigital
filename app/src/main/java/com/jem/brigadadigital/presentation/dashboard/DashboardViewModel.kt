package com.jem.brigadadigital.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.domain.model.EmergencyEvent
import com.jem.brigadadigital.domain.model.EmergencyResponse
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import com.jem.brigadadigital.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ResponderData(
    val profile: UserProfile,
    val response: EmergencyResponse,
    val eta: String? = null
)

data class DashboardState(
    val activeEmergency: EmergencyEvent? = null,
    val responders: List<ResponderData> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val etas: Map<String, String> = emptyMap() // uid -> eta string
)

class DashboardViewModel(
    private val emergencyRepo: EmergencyRepository = com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl(),
    private val userRepo: UserRepository = com.jem.brigadadigital.data.repository.UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            emergencyRepo.observeActiveEmergency().collectLatest { result ->
                result.onSuccess { emergency ->
                    if (emergency != null) {
                        _uiState.value = _uiState.value.copy(activeEmergency = emergency, isLoading = false)
                        observeResponses(emergency.id)
                    } else {
                        _uiState.value = _uiState.value.copy(activeEmergency = null, isLoading = false, responders = emptyList())
                    }
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.localizedMessage, isLoading = false)
                }
            }
        }
    }

    private var observeResponsesJob: kotlinx.coroutines.Job? = null

    private fun observeResponses(emergencyId: String) {
        observeResponsesJob?.cancel()
        observeResponsesJob = viewModelScope.launch {
            emergencyRepo.observeEmergencyResponses(emergencyId).collectLatest { result ->
                result.onSuccess { responses ->
                    // Por cada respuesta, obtener el UserProfile para saber nombre y rango
                    val combinedList = mutableListOf<ResponderData>()
                    for (response in responses) {
                        val profileResult = userRepo.getUserProfile(response.uid)
                        val profile = profileResult.getOrNull() ?: UserProfile(uid = response.uid, nombre = "Desconocido")
                        combinedList.add(ResponderData(profile, response))
                    }
                    _uiState.value = _uiState.value.copy(responders = combinedList)
                    fetchEtas(emergencyId, combinedList)
                }.onFailure {
                    // ignorar
                }
            }
        }
    }

    private fun fetchEtas(emergencyId: String, responders: List<ResponderData>) {
        val destination = _uiState.value.activeEmergency?.ubicacion ?: return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val updatedEtas = mutableMapOf<String, String>()
            
            responders.forEach { responder ->
                val origin = responder.response.lastLocation
                if (origin != null && !responder.response.haLlegado) {
                    try {
                        val urlString = "https://router.project-osrm.org/table/v1/driving/${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}?sources=0&destinations=1"
                        val responseText = java.net.URL(urlString).readText()
                        val json = org.json.JSONObject(responseText)
                        val durations = json.getJSONArray("durations")
                        val durationSeconds = durations.getJSONArray(0).getDouble(0)
                        
                        val minutes = (durationSeconds / 60).toInt()
                        updatedEtas[responder.profile.uid] = "$minutes min"
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (responder.response.haLlegado) {
                    updatedEtas[responder.profile.uid] = "LLEGÓ"
                }
            }
            
            _uiState.value = _uiState.value.copy(
                etas = _uiState.value.etas + updatedEtas,
                responders = _uiState.value.responders.map { r ->
                    r.copy(eta = updatedEtas[r.profile.uid])
                }
            )
        }
    }
}
