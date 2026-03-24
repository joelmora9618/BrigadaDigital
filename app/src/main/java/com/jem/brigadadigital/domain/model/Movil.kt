package com.jem.brigadadigital.domain.model

data class Movil(
    val id: String = "",
    val nombre: String = "", // E.g., "Móvil 14", "Unidad 2"
    val tipo: String = "",   // E.g., "Autobomba", "Rescate", "Logística"
    val patente: String = "",
    val estado: String = "Disponible", // Disponible, En Servicio, Fuera de Servicio
    val cuartelId: String = ""
)
