package com.jem.brigadadigital.domain.repository

import com.jem.brigadadigital.domain.model.EmergencyEvent
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    fun observeActiveEmergency(): Flow<Result<EmergencyEvent?>>
    suspend fun respondToEmergency(emergencyId: String, uid: String, isGoing: Boolean): Result<Unit>
    suspend fun createTestEmergency(): Result<Unit> // Solo para propósitos de prueba
    suspend fun updateTrackerLocation(emergencyId: String, uid: String, location: com.google.firebase.firestore.GeoPoint): Result<Unit>
    
    // FASE 5 & 7
    fun observeEmergencyResponses(emergencyId: String): Flow<Result<List<com.jem.brigadadigital.domain.model.EmergencyResponse>>>
    fun getPastEmergencies(): Flow<Result<List<EmergencyEvent>>>
    suspend fun createEmergency(event: EmergencyEvent): Result<Unit>
    suspend fun closeEmergency(emergencyId: String, reportData: Map<String, Any>): Result<Unit>
    suspend fun markAsArrived(emergencyId: String, uid: String): Result<Unit>
}
