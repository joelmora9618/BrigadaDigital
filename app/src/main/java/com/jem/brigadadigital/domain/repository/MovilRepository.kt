package com.jem.brigadadigital.domain.repository

import com.jem.brigadadigital.domain.model.Movil
import kotlinx.coroutines.flow.Flow

interface MovilRepository {
    fun observeMovilesByCuartel(cuartelId: String): Flow<Result<List<Movil>>>
    suspend fun addMovil(movil: Movil): Result<Unit>
    suspend fun updateMovilStatus(movilId: String, nuevoEstado: String): Result<Unit>
}
