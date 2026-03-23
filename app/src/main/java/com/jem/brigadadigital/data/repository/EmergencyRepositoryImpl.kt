package com.jem.brigadadigital.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.jem.brigadadigital.domain.model.EmergencyEvent
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class EmergencyRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EmergencyRepository {

    override fun observeEmergency(emergencyId: String): Flow<Result<EmergencyEvent?>> {
        return firestore.collection("emergencies")
            .document(emergencyId)
            .snapshots()
            .map { documentSnapshot ->
                try {
                    if (!documentSnapshot.exists()) {
                        Result.success(null)
                    } else {
                        val event = documentSnapshot.toObject(EmergencyEvent::class.java)
                        Result.success(event)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error escuchando emergencia: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override fun observeActiveEmergency(): Flow<Result<EmergencyEvent?>> {
        return firestore.collection("emergencies")
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { querySnapshot ->
                try {
                    if (querySnapshot.isEmpty) {
                        Result.success(null)
                    } else {
                        val doc = querySnapshot.documents.first()
                        val event = doc.toObject(EmergencyEvent::class.java)
                        Result.success(event)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error escuchando emergencias: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override fun observeAllActiveEmergencies(): Flow<Result<List<EmergencyEvent>>> {
        return firestore.collection("emergencies")
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                try {
                    val events = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(EmergencyEvent::class.java)?.copy(id = doc.id)
                    }
                    Result.success(events)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error escuchando todas las emergencias: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override suspend fun respondToEmergency(emergencyId: String, uid: String, isGoing: Boolean): Result<Unit> {
        return try {
            val responseData = mapOf(
                "uid" to uid,
                "isGoing" to isGoing,
                "timestamp" to System.currentTimeMillis()
            )

            // Guardar la respuesta del bombero
            firestore.collection("emergencies")
                .document(emergencyId)
                .collection("responses")
                .document(uid)
                .set(responseData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun createTestEmergency(): Result<Unit> {
        return try {
            val dummyEvent = EmergencyEvent(
                id = "", // Generado automáticamente
                titulo = "🚨 SIMULACRO DE ALERTA",
                descripcion = "Esta es una alerta de prueba generada automáticamente.",
                direccion = "Ruta 2, Km 45",
                tipo = "Incendio",
                isActive = true,
                timestamp = System.currentTimeMillis(),
                ubicacion = null
            )
            firestore.collection("emergencies").add(dummyEvent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateTrackerLocation(emergencyId: String, uid: String, location: com.google.firebase.firestore.GeoPoint): Result<Unit> {
        return try {
            firestore.collection("emergencies")
                .document(emergencyId)
                .collection("responses")
                .document(uid)
                .update(
                    mapOf(
                        "lastLocation" to location,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
    
    // FASE 5
    override fun observeEmergencyResponses(emergencyId: String): Flow<Result<List<com.jem.brigadadigital.domain.model.EmergencyResponse>>> {
        return firestore.collection("emergencies")
            .document(emergencyId)
            .collection("responses")
            .whereEqualTo("isGoing", true)
            .snapshots()
            .map { querySnapshot ->
                try {
                    val responses = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.jem.brigadadigital.domain.model.EmergencyResponse::class.java)?.copy(uid = doc.id)
                    }
                    Result.success(responses)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error escuchando responses: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override fun getPastEmergencies(): Flow<Result<List<EmergencyEvent>>> {
        return firestore.collection("emergencies")
            .whereEqualTo("isActive", false)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { querySnapshot ->
                try {
                    val events = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(EmergencyEvent::class.java)?.copy(id = doc.id)
                    }
                    Result.success(events)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error obteniendo historial: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override fun observeAllActiveResponders(): Flow<Result<Int>> {
        return firestore.collectionGroup("responses")
            .whereEqualTo("isGoing", true)
            .snapshots()
            .map { querySnapshot ->
                Result.success(querySnapshot.size())
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error escuchando respondedores globales: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override fun observeAllActiveRespondersDetailed(): Flow<Result<List<Pair<String, com.jem.brigadadigital.domain.model.EmergencyResponse>>>> {
        return firestore.collectionGroup("responses")
            .whereEqualTo("isGoing", true)
            .snapshots()
            .map { querySnapshot ->
                try {
                    val detailedList = querySnapshot.documents.mapNotNull { doc ->
                        val response = doc.toObject(com.jem.brigadadigital.domain.model.EmergencyResponse::class.java)?.copy(uid = doc.id)
                        val emergencyId = doc.reference.parent.parent?.id
                        if (response != null && emergencyId != null) {
                            Pair(emergencyId, response)
                        } else null
                    }
                    Result.success(detailedList)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            .catch { e ->
                Log.e("EmergencyRepository", "Error escuchando respondedores detallados: ${e.message}")
                emit(Result.failure(e))
            }
    }

    override suspend fun createEmergency(event: EmergencyEvent): Result<Unit> {
        return try {
            firestore.collection("emergencies").add(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun closeEmergency(emergencyId: String, reportData: Map<String, Any>): Result<Unit> {
        return try {
            val updateData = reportData.toMutableMap()
            updateData["isActive"] = false
            updateData["closedAt"] = System.currentTimeMillis()
            
            firestore.collection("emergencies").document(emergencyId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun markAsArrived(emergencyId: String, uid: String): Result<Unit> {
        return try {
            firestore.collection("emergencies")
                .document(emergencyId)
                .collection("responses")
                .document(uid)
                .update("haLlegado", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getRespondedEmergencyIds(uid: String): Result<Set<String>> {
        return try {
            val querySnapshot = firestore.collectionGroup("responses")
                .whereEqualTo("uid", uid)
                .get()
                .await()
            
            val ids = querySnapshot.documents.mapNotNull { doc ->
                // The parent document of a response is the emergency document
                doc.reference.parent.parent?.id
            }.toSet()
            
            Result.success(ids)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun checkIfUserResponded(emergencyId: String, uid: String): Result<Boolean> {
        return try {
            val doc = firestore.collection("emergencies")
                .document(emergencyId)
                .collection("responses")
                .document(uid)
                .get()
                .await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
