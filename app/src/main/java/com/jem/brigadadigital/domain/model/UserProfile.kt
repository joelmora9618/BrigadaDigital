package com.jem.brigadadigital.domain.model

data class UserProfile(
    val uid: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val rango: String = "",
    val especialidad: String = "",
    val cuartelId: String = "",
    val disponible: Boolean = false, // Renombrado para evitar bugs de mapeo de Firebase con el prefijo "is"
    val role: String = "bombero" // "bombero", "admin", "jefe"
)
