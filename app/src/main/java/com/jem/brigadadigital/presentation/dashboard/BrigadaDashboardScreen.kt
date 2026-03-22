package com.jem.brigadadigital.presentation.dashboard

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jem.brigadadigital.domain.model.UserProfile
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrigadaDashboardScreen(
    viewModel: DashboardViewModel,
    currentUser: UserProfile,
    onCloseIncidentClicked: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emergency = uiState.activeEmergency

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Comando") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (emergency == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No hay emergencias activas en este momento.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Mapa de Flota (Top Half)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val geoPoint = emergency.ubicacion
                val cameraPosition = remember(geoPoint) {
                    if (geoPoint != null) {
                        org.maplibre.compose.camera.CameraPosition(
                            target = org.maplibre.spatialk.geojson.Position(geoPoint.longitude, geoPoint.latitude),
                            zoom = 13.0
                        )
                    } else {
                        org.maplibre.compose.camera.CameraPosition(
                            target = org.maplibre.spatialk.geojson.Position(0.0, 0.0),
                            zoom = 1.0
                        )
                    }
                }
                
                val cameraState = org.maplibre.compose.camera.rememberCameraState(cameraPosition)

                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        org.maplibre.android.maps.MapView(ctx).apply {
                            getMapAsync { map ->
                                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                                if (geoPoint != null) {
                                    map.moveCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude),
                                            13.0
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
                // Marcador Central del Incidente UI Overlay
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Incidente",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .padding(bottom = 24.dp)
                )

                // Renderizar los bomberos sobre el mapa (Proyección manual a px)
                if (geoPoint != null) {
                    val centerLatRad = Math.toRadians(geoPoint.latitude)
                    // Escala aproximada a Zoom 13.0
                    val metersPerPixel = 156543.03392 * Math.cos(centerLatRad) / Math.pow(2.0, 13.0)

                    uiState.responders.filter { it.response.lastLocation != null }.forEach { resp ->
                        val loc = resp.response.lastLocation!!
                        val dLat = Math.toRadians(loc.latitude - geoPoint.latitude)
                        val dLon = Math.toRadians(loc.longitude - geoPoint.longitude)
                        val earthRadius = 6378137.0
                        val dx = earthRadius * dLon * Math.cos(centerLatRad)
                        val dy = earthRadius * dLat

                        val offsetX = (dx / metersPerPixel).dp
                        val offsetY = (-dy / metersPerPixel).dp // Y hacia abajo en UI

                        val isArrived = resp.response.haLlegado
                        val markerColor = if (isArrived) Color(0xFF4CAF50) else Color.Blue // Verde si llegó, Azul si no
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = offsetX, y = offsetY)
                                .size(24.dp)
                                .background(markerColor, shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = resp.profile.nombre.take(1),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Overlay de cantidad de respondedores flotante (Glassmorphism M3 style)
                ElevatedCard(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.95f))
                ) {
                    Text(
                        text = "En Camino: ${uiState.responders.size}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Botón Flotante para Finalizar Misión (Extended FAB)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    if (currentUser.role == "admin" || currentUser.role == "jefe") {
                        ExtendedFloatingActionButton(
                            onClick = { onCloseIncidentClicked(emergency.id) },
                            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                            text = { Text("FINALIZAR MISIÓN") },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        // Demo temporal
                        ExtendedFloatingActionButton(
                            onClick = { onCloseIncidentClicked(emergency.id) },
                            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                            text = { Text("FINALIZAR (Demo)") },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            // Lista de Personal (Bottom Half)
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Personal de Respuesta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(uiState.responders) { responder ->
                        ListItem(
                            headlineContent = { Text("${responder.profile.nombre} ${responder.profile.apellido}", fontWeight = FontWeight.Bold) },
                            supportingContent = { 
                                val statusText = if (responder.response.haLlegado) "EN EL LUGAR" else "EN CAMINO"
                                Text("Estado: $statusText | ETA: ${responder.eta ?: "Calculando..."}") 
                            },
                            trailingContent = {
                                val iconColor = if (responder.response.haLlegado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Estado",
                                    tint = iconColor
                                )
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
