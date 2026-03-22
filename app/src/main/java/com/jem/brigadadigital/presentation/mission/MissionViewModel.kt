package com.jem.brigadadigital.presentation.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl
import com.jem.brigadadigital.domain.model.EmergencyEvent
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MissionViewModel(
    private val repository: EmergencyRepository
) : ViewModel() {

    private val _missionState = MutableStateFlow<MissionState>(MissionState.Loading)
    val missionState: StateFlow<MissionState> = _missionState.asStateFlow()

    private val _routePoints = MutableStateFlow<List<org.maplibre.spatialk.geojson.Position>>(emptyList())
    val routePointsResource: StateFlow<List<org.maplibre.spatialk.geojson.Position>> = _routePoints.asStateFlow()

    private val _isArrived = MutableStateFlow(false)
    val isArrived: StateFlow<Boolean> = _isArrived.asStateFlow()

    fun initMission(emergencyId: String, uid: String) {
        observeEmergency()
        observeMyResponse(emergencyId, uid)
    }

    private fun observeMyResponse(emergencyId: String, uid: String) {
        viewModelScope.launch {
            repository.observeEmergencyResponses(emergencyId).collect { result ->
                result.onSuccess { responses ->
                    val myResponse = responses.find { it.uid == uid }
                    _isArrived.value = myResponse?.haLlegado ?: false
                }
            }
        }
    }

    private fun observeEmergency() {
        viewModelScope.launch {
            repository.observeActiveEmergency().collect { result ->
                result.onSuccess { emergency ->
                    if (emergency != null) {
                        _missionState.value = MissionState.Active(emergency)
                    } else {
                        _missionState.value = MissionState.Finished
                    }
                }.onFailure {
                    _missionState.value = MissionState.Finished
                }
            }
        }
    }

    fun finishMission(emergencyId: String, uid: String) {
        viewModelScope.launch {
            // Se asume que dejar de ir apaga el estado activo para el bombero.
            repository.respondToEmergency(emergencyId, uid, false)
        }
    }

    fun fetchRoute(startLat: Double, startLon: Double, endLat: Double, endLon: Double) {
        viewModelScope.launch {
            try {
                val urlString = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson"
                val response = withContext(Dispatchers.IO) {
                    URL(urlString).readText()
                }
                
                val json = JSONObject(response)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    val points = mutableListOf<org.maplibre.spatialk.geojson.Position>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        points.add(org.maplibre.spatialk.geojson.Position(coord.getDouble(0), coord.getDouble(1)))
                    }
                    _routePoints.value = points
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class MissionState {
    data object Loading : MissionState()
    data class Active(val emergency: EmergencyEvent) : MissionState()
    data object Finished : MissionState()
}

class MissionViewModelFactory(
    private val repository: EmergencyRepository = EmergencyRepositoryImpl()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MissionViewModel(repository) as T
    }
}
