package com.jem.brigadadigital.domain.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class EmergencyResponse(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    
    @get:PropertyName("isGoing") @set:PropertyName("isGoing")
    var isGoing: Boolean = false,
    
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long = 0L,
    
    @get:PropertyName("lastLocation") @set:PropertyName("lastLocation")
    var lastLocation: GeoPoint? = null,
    
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long? = null,
    
    @get:PropertyName("haLlegado") @set:PropertyName("haLlegado")
    var haLlegado: Boolean = false
)
