package com.jem.brigadadigital.domain.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class EmergencyEvent(
    @DocumentId
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val ubicacion: GeoPoint? = null,
    val direccion: String = "",
    val tipo: String = "", // e.g., Incendio, Accidente, Rescate
    val evidencias: List<String> = emptyList(), // URLs de fotos de la intervención
    val timestamp: Long = System.currentTimeMillis(),
    
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true
)
