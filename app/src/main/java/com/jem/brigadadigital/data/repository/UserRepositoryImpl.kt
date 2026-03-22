package com.jem.brigadadigital.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            firestore.collection("users").document(profile.uid)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun updateAvailability(uid: String, disponible: Boolean): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update("disponible", disponible)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override fun observeUserProfile(uid: String): Flow<Result<UserProfile?>> {
        return firestore.collection("users").document(uid).snapshots()
            .map { snapshot ->
                try {
                    if (snapshot.exists()) {
                        val profile = snapshot.toObject(UserProfile::class.java)
                        Result.success(profile)
                    } else {
                        Result.success(null)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
}
