package com.jem.brigadadigital.presentation.dashboard

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.jem.brigadadigital.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrigadaDashboardScreen(
    viewModel: DashboardViewModel,
    currentUser: UserProfile,
    onCloseIncidentClicked: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emergency = uiState.activeEmergency
    var maplibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    // Filtro de respondedores válidos
    val validResponders = remember(uiState.responders) {
        uiState.responders.filter { 
            it.response.lastLocation != null && it.response.lastLocation!!.latitude != 0.0 && it.response.lastLocation!!.longitude != 0.0 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Comando") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading || emergency == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) CircularProgressIndicator() else Text("No hay emergencias activas.")
            }
            return@Scaffold
        }

        val geoPoint = emergency.ubicacion
        
        // CÁMARA DINÁMICA
        LaunchedEffect(geoPoint, maplibreMap) {
            val map = maplibreMap ?: return@LaunchedEffect
            if (geoPoint != null && geoPoint.latitude != 0.0) {
                map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude), 15.0), 1000)
            }
        }

        // GESTIÓN DE MARCADORES (Legacy + GeoJSON)
        LaunchedEffect(validResponders, geoPoint, maplibreMap) {
            val map = maplibreMap ?: return@LaunchedEffect
            
            // 1. Legacy Markers (Inmediatos)
            map.clear()
            if (geoPoint != null && geoPoint.latitude != 0.0) {
                map.addMarker(org.maplibre.android.annotations.MarkerOptions().position(org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude)).title("INCIDENTE"))
            }
            validResponders.forEach { 
                map.addMarker(org.maplibre.android.annotations.MarkerOptions().position(org.maplibre.android.geometry.LatLng(it.response.lastLocation!!.latitude, it.response.lastLocation!!.longitude)).title(it.profile.nombre))
            }

            // 3. Círculos removidos por petición del usuario (ya se ven los marcadores estándar)
        }

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        org.maplibre.android.maps.MapView(ctx).apply {
                            getMapAsync { map ->
                                maplibreMap = map
                                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                                map.uiSettings.isLogoEnabled = false
                                map.uiSettings.isAttributionEnabled = false
                                
                                // Default inicial en Buenos Aires O Incidente si existe
                                if (geoPoint != null && geoPoint.latitude != 0.0) {
                                    map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude), 15.0))
                                } else {
                                    map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(-34.6037, -58.3816), 11.0))
                                }
                            }
                        }
                    }
                )
                
                // Overlay informativo
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Text("Respondedores en Mapa: ${validResponders.size}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(uiState.responders) { responder ->
                    ListItem(
                        headlineContent = { Text("${responder.profile.nombre} ${responder.profile.apellido}") },
                        supportingContent = { Text(if (responder.response.haLlegado) "EN EL LUGAR" else "EN CAMINO") },
                        trailingContent = { Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = if (responder.response.haLlegado) Color.Green else Color.Gray) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
