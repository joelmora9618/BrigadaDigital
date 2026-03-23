package com.jem.brigadadigital.presentation.emergency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveEmergenciesMapScreen(
    viewModel: EmergencyViewModel,
    onNavigateToDashboard: (String) -> Unit
) {
    val activeEmergencies by viewModel.allActiveEmergencies.collectAsStateWithLifecycle()
    var maplibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    
    // Filtro de coordenadas válidas
    val validAlerts = remember(activeEmergencies) {
        activeEmergencies.filter { 
            it.ubicacion != null && it.ubicacion!!.latitude != 0.0 && it.ubicacion!!.longitude != 0.0 
        }
    }

    // Centrar automáticamente
    LaunchedEffect(validAlerts, maplibreMap) {
        val map = maplibreMap ?: return@LaunchedEffect
        if (validAlerts.isNotEmpty()) {
            val builder = org.maplibre.android.geometry.LatLngBounds.Builder()
            validAlerts.forEach { builder.include(org.maplibre.android.geometry.LatLng(it.ubicacion!!.latitude, it.ubicacion!!.longitude)) }
            try {
                if (validAlerts.size > 1) {
                    map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(builder.build(), 150), 1000)
                } else {
                    val first = validAlerts.first()
                    map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(first.ubicacion!!.latitude, first.ubicacion!!.longitude), 14.0), 800)
                }
            } catch (e: Exception) {
                map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(-34.6037, -58.3816), 11.0), 800)
            }
        }
    }

    // Marcadores Redundantes
    LaunchedEffect(validAlerts, maplibreMap) {
        val map = maplibreMap ?: return@LaunchedEffect
        
        map.clear()
        validAlerts.forEach { emergency ->
            map.addMarker(org.maplibre.android.annotations.MarkerOptions()
                .position(org.maplibre.android.geometry.LatLng(emergency.ubicacion!!.latitude, emergency.ubicacion!!.longitude))
                .title(emergency.titulo))
        }
            // 2. Círculos removidos por petición del usuario (ya se ven los marcadores estándar)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de la Brigada", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E), titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    maplibreMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(-34.6037, -58.3816), 11.0))
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    org.maplibre.android.maps.MapView(ctx).apply {
                        getMapAsync { map ->
                            maplibreMap = map
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                            map.uiSettings.isLogoEnabled = false
                            map.uiSettings.isAttributionEnabled = false
                            
                            map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(-34.6037, -58.3816), 11.0))
                            
                            map.setOnMarkerClickListener { marker ->
                                val emergency = validAlerts.find { it.titulo == marker.title }
                                if (emergency != null) onNavigateToDashboard(emergency.id)
                                true
                            }
                        }
                    }
                }
            )

            // Info Card Mejorada
            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Total Activas: ${activeEmergencies.size}", style = MaterialTheme.typography.labelMedium)
                        Text(text = "En Mapa: ${validAlerts.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (validAlerts.size < activeEmergencies.size) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Hay alertas sin ubicación asignada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
