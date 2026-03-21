package com.jem.brigadadigital.domain.model

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val rango: String = "",
    val especialidad: String = "",
    val cuartelId: String = "",
    val isAvailable: Boolean = false
)
