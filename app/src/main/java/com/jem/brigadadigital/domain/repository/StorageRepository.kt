package com.jem.brigadadigital.domain.repository

import android.net.Uri

interface StorageRepository {
    /**
     * Sube una imagen a Firebase Storage y retorna la URL de descarga.
     * @param path Ruta en el storage (ej: "emergencies/123/photos/image.jpg")
     * @param uri URI local del archivo
     */
    suspend fun uploadPhoto(path: String, uri: Uri): Result<String>
}
