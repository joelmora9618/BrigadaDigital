package com.jem.brigadadigital.presentation.dispatch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.spatialk.geojson.Position
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    viewModel: EmergencyViewModel,
    onLocationConfirmed: (Double, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var maplibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Seleccionar Ubicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancelar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    maplibreMap?.cameraPosition?.target?.let { target ->
                        onLocationConfirmed(target.latitude, target.longitude)
                    }
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Confirmar Ubicación") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    org.maplibre.android.maps.MapView(ctx).apply {
                        getMapAsync { map ->
                            maplibreMap = map
                            map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                            map.moveCamera(
                                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                    org.maplibre.android.geometry.LatLng(-34.6037, -58.3816),
                                    13.0
                                )
                            )
                        }
                    }
                }
            )
            
            // Fixed Center Marker (Crosshair)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Simple Crosshair visual
                Divider(modifier = Modifier.width(24.dp).height(2.dp), color = Color.Red)
                Divider(modifier = Modifier.width(2.dp).height(24.dp), color = Color.Red)
                
                // Dot in center
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.Red
                ) {}
            }
            
            // Helper text
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                Text(
                    "Mueve el mapa para centrar el incidente",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
