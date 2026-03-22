package com.jem.brigadadigital.presentation.emergency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jem.brigadadigital.domain.model.EmergencyEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveEmergenciesMapScreen(
    viewModel: EmergencyViewModel,
    onNavigateToDashboard: (String) -> Unit
) {
    val activeEmergencies by viewModel.allActiveEmergencies.collectAsStateWithLifecycle()
    var maplibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    val context = LocalContext.current

    // Gestor de marcadores nativos
    LaunchedEffect(activeEmergencies, maplibreMap) {
        val map = maplibreMap ?: return@LaunchedEffect
        map.clear()
        
        activeEmergencies.forEach { emergency ->
            val geoPoint = emergency.ubicacion
            if (geoPoint != null) {
                map.addMarker(
                    org.maplibre.android.annotations.MarkerOptions()
                        .position(org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude))
                        .title(emergency.titulo)
                        .snippet(emergency.tipo)
                )
            }
        }

        // Listener para clics en marcadores
        map.setOnMarkerClickListener { marker ->
            // Buscar la emergencia correspondiente por título y posición (Hack ya que Marker no guarda ID directamente en API básica)
            val clickedEmergency = activeEmergencies.find { 
                it.titulo == marker.title && 
                it.ubicacion?.latitude == marker.position.latitude && 
                it.ubicacion?.longitude == marker.position.longitude 
            }
            clickedEmergency?.let { onNavigateToDashboard(it.id) }
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Alertas Activas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    org.maplibre.android.maps.MapView(ctx).apply {
                        getMapAsync { map ->
                            maplibreMap = map
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                            
                            // Si hay alertas, centrar en la primera
                            if (activeEmergencies.isNotEmpty()) {
                                activeEmergencies.firstOrNull { it.ubicacion != null }?.let { emergency ->
                                    map.animateCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            org.maplibre.android.geometry.LatLng(
                                                emergency.ubicacion!!.latitude,
                                                emergency.ubicacion!!.longitude
                                            ),
                                            12.0
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            )

            if (activeEmergencies.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(paddingValues)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Vigilancia Activa: No hay alertas en curso",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(paddingValues)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Alertas Activas: ${activeEmergencies.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}
