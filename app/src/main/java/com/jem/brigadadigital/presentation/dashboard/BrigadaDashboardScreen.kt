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

        var maplibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

        // Gestor de marcadores nativos
        LaunchedEffect(uiState.responders, maplibreMap, emergency.ubicacion) {
            val map = maplibreMap ?: return@LaunchedEffect
            val geoPoint = emergency.ubicacion
            
            // Limpiar marcadores anteriores (anotaciones)
            // Nota: Esto limpia TODAS las anotaciones. Si hay rutas, también se irían.
            map.clear()
            
            // 1. Añadir Marcador del Incidente
            if (geoPoint != null) {
                map.addMarker(
                    org.maplibre.android.annotations.MarkerOptions()
                        .position(org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude))
                        .title("INCIDENTE: ${emergency.titulo}")
                )
            }
            
            // 2. Añadir Marcadores de Bomberos
            uiState.responders.filter { it.response.lastLocation != null }.forEach { resp ->
                val loc = resp.response.lastLocation!!
                map.addMarker(
                    org.maplibre.android.annotations.MarkerOptions()
                        .position(org.maplibre.android.geometry.LatLng(loc.latitude, loc.longitude))
                        .title(resp.profile.nombre)
                        .snippet(if (resp.response.haLlegado) "EN EL LUGAR" else "EN CAMINO")
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Mapa de Flota (Top Half)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val geoPoint = emergency.ubicacion
                
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        org.maplibre.android.maps.MapView(ctx).apply {
                            getMapAsync { map ->
                                maplibreMap = map
                                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                                
                                if (geoPoint != null) {
                                    map.moveCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude),
                                            15.0
                                        )
                                    )
                                }
                            }
                        }
                    },
                    update = { view ->
                        // Re-centrar si cambia el geoPoint (desde el ViewModel)
                        maplibreMap?.let { map ->
                            if (geoPoint != null) {
                                val currentTarget = map.cameraPosition?.target
                                val sameLocation = currentTarget?.latitude == geoPoint.latitude && 
                                                 currentTarget?.longitude == geoPoint.longitude
                                if (!sameLocation) {
                                    map.moveCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            org.maplibre.android.geometry.LatLng(geoPoint.latitude, geoPoint.longitude),
                                            15.0
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
                
                // Overlay de cantidad de respondedores flotante
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
                
                // Botón Flotante para Finalizar Misión
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
