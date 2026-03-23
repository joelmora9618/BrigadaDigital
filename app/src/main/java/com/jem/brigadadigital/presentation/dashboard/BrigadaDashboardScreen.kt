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
            // CABECERA DEL INCIDENTE (Nueva)
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = emergency.tipo.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = emergency.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = emergency.direccion.ifEmpty { "Ubicación no especificada" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (emergency.descripcion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = emergency.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(0.9f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        org.maplibre.android.maps.MapView(ctx).apply {
                            getMapAsync { map ->
                                maplibreMap = map
                                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                                map.uiSettings.isLogoEnabled = false
                                map.uiSettings.isAttributionEnabled = false
                                
                                if (geoPoint != null && geoPoint.latitude != 0.0) {
                                    map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude), 15.0))
                                } else {
                                    map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(org.maplibre.android.geometry.LatLng(-34.6037, -58.3816), 11.0))
                                }
                            }
                        }
                    }
                )
                
                Card(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        "Respondedores en Mapa: ${validResponders.size}", 
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), 
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // LISTADO DE PERSONAL
            Text(
                "PERSONAL ASIGNADO",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            
            if (uiState.responders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay personal en camino todavía", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1.1f).fillMaxWidth()) {
                    items(uiState.responders) { responder ->
                        val hasLocation = responder.response.lastLocation != null && responder.response.lastLocation?.latitude != 0.0
                        
                        ListItem(
                            headlineContent = { 
                                Text(
                                    "${responder.profile.nombre} ${responder.profile.apellido}",
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            supportingContent = { 
                                Column {
                                    Text(
                                        if (responder.response.haLlegado) "ARRIVED / EN EL LUGAR" else "EN ROUTE / EN CAMINO",
                                        color = if (responder.response.haLlegado) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!hasLocation && !responder.response.haLlegado) {
                                        Text("Sin ubicación GPS activa", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            },
                            trailingContent = { 
                                Column(horizontalAlignment = Alignment.End) {
                                    if (responder.eta != null) {
                                        Text(
                                            responder.eta, 
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle, 
                                        contentDescription = null, 
                                        tint = if (responder.response.haLlegado) Color(0xFF2E7D32) else Color.Gray.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = responder.profile.nombre.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
