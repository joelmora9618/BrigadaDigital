package com.jem.brigadadigital.domain.model

import com.google.firebase.firestore.GeoPoint

data class EmergencyResponse(
    val uid: String = "",
    val isGoing: Boolean = false,
    val timestamp: Long = 0L,
    val lastLocation: GeoPoint? = null,
    val updatedAt: Long? = null,
    val haLlegado: Boolean = false
)
