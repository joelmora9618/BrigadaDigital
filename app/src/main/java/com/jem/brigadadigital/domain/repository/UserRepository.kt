package com.jem.brigadadigital.domain.repository

import com.jem.brigadadigital.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    suspend fun getUserProfile(uid: String): Result<UserProfile?>
    suspend fun updateAvailability(uid: String, disponible: Boolean): Result<Unit>
    fun observeUserProfile(uid: String): Flow<Result<UserProfile?>>
    suspend fun getUserProfiles(uids: List<String>): Result<List<UserProfile>>
    fun observeAvailablePersonnel(cuartelId: String): Flow<Result<Int>>
}
