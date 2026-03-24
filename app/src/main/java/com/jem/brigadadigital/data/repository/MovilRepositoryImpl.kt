package com.jem.brigadadigital.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.jem.brigadadigital.domain.model.Movil
import com.jem.brigadadigital.domain.repository.MovilRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MovilRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MovilRepository {

    private val movilesCollection = firestore.collection("moviles")

    override fun observeMovilesByCuartel(cuartelId: String): Flow<Result<List<Movil>>> {
        return movilesCollection
            .whereEqualTo("cuartelId", cuartelId)
            .snapshots()
            .map { snapshot ->
                try {
                    val list = snapshot.toObjects(Movil::class.java)
                    Result.success(list)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }

    override suspend fun addMovil(movil: Movil): Result<Unit> {
        return try {
            val docRef = if (movil.id.isEmpty()) movilesCollection.document() else movilesCollection.document(movil.id)
            val finalMovil = movil.copy(id = docRef.id)
            docRef.set(finalMovil).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMovilStatus(movilId: String, nuevoEstado: String): Result<Unit> {
        return try {
            movilesCollection.document(movilId).update("estado", nuevoEstado).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
