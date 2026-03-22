package com.jem.brigadadigital.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.jem.brigadadigital.domain.repository.StorageRepository
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class StorageRepositoryImpl(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : StorageRepository {

    override suspend fun uploadPhoto(path: String, uri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
