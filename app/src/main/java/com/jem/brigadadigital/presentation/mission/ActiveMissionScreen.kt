package com.jem.brigadadigital.presentation.mission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.spatialk.geojson.Position
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jem.brigadadigital.service.EmergencyTrackingService

@Composable
fun ActiveMissionScreen(
    uid: String,
    viewModel: MissionViewModel,
    onMissionEnded: () -> Unit
) {
    val context = LocalContext.current
    val missionState by viewModel.missionState.collectAsStateWithLifecycle()

    var locationPermissionsGranted by remember { mutableStateOf(false) }
    var backgroundPermissionRequested by remember { mutableStateOf(false) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val emergency = (missionState as? MissionState.Active)?.emergency
            startTrackingService(
                context, 
                uid, 
                emergency?.id,
                emergency?.ubicacion?.latitude,
                emergency?.ubicacion?.longitude
            )
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineGranted || coarseGranted) {
            locationPermissionsGranted = true
            // Después de fine location, en Android 10+ pedimos el background location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !backgroundPermissionRequested) {
                backgroundPermissionRequested = true
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                val emergency = (missionState as? MissionState.Active)?.emergency
                startTrackingService(
                    context, 
                    uid, 
                    emergency?.id,
                    emergency?.ubicacion?.latitude,
                    emergency?.ubicacion?.longitude
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold { paddingValues ->
        when (val state = missionState) {
            is MissionState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MissionState.Active -> {
                val emergency = state.emergency
                val geoPoint = emergency.ubicacion
                
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    // INFO CARD
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Misión Activa: ${emergency.titulo}", style = MaterialTheme.typography.titleLarge)
                            Text(text = emergency.direccion, style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = {
                                    val uri = Uri.parse("google.navigation:q=${geoPoint?.latitude},${geoPoint?.longitude}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        val geoUri = Uri.parse("geo:${geoPoint?.latitude},${geoPoint?.longitude}?q=${geoPoint?.latitude},${geoPoint?.longitude}")
                                        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        context.startActivity(geoIntent)
                                    }
                                }) {
                                    Text("Navegar")
                                }
                            }
                        }
                    }

                    // BOTON PARA PEDIR BACKGROUND SI FALLÓ EL AUTOMÁTICO
                    if (!backgroundPermissionRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("El cuartel necesita saber tu posición mientras tienes el teléfono bloqueado.")
                                Button(onClick = {
                                    backgroundPermissionRequested = true
                                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }) {
                                    Text("Permitir rastreo en segundo plano")
                                }
                            }
                        }
                    }

                    // MAPA Y CHINCHETAS
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (locationPermissionsGranted) {
                            val routePoints by viewModel.routePointsResource.collectAsStateWithLifecycle()
                            
                            // Effect to fetch route once we have destination
                            LaunchedEffect(emergency.ubicacion) {
                                val dest = emergency.ubicacion
                                if (dest != null) {
                                    // Para propósitos de la demo, usamos una ubicación inicial fija o pedimos la última
                                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                    try {
                                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                            if (loc != null) {
                                                viewModel.fetchRoute(loc.latitude, loc.longitude, dest.latitude, dest.longitude)
                                            }
                                        }
                                    } catch (e: SecurityException) {}
                                }
                            }

                            val initialCameraPosition = remember(geoPoint) {
                                if (geoPoint != null) {
                                    org.maplibre.compose.camera.CameraPosition(
                                        target = org.maplibre.spatialk.geojson.Position(geoPoint.longitude, geoPoint.latitude),
                                        zoom = 15.0
                                    )
                                } else {
                                    org.maplibre.compose.camera.CameraPosition(
                                        target = org.maplibre.spatialk.geojson.Position(0.0, 0.0),
                                        zoom = 1.0
                                    )
                                }
                            }
                            val cameraState = org.maplibre.compose.camera.rememberCameraState(initialCameraPosition)

                            Box(modifier = Modifier.fillMaxSize()) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        org.maplibre.android.maps.MapView(ctx).apply {
                                            getMapAsync { map ->
                                                map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                                                val dest = emergency.ubicacion
                                                if (dest != null) {
                                                    map.moveCamera(
                                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                                            org.maplibre.android.geometry.LatLng(dest.latitude, dest.longitude),
                                                            15.0
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                                
                                // DIBUJO MANUAL DE RUTA (HACK COMPATIBILIDAD V0.12.1)
                                if (routePoints.isNotEmpty()) {
                                    val zoom = cameraState.position.zoom
                                    val center = cameraState.position.target
                                    
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val centerLatRad = Math.toRadians(center.latitude)
                                        val metersPerPixel = 156543.03392 * Math.cos(centerLatRad) / Math.pow(2.0, zoom)
                                        val earthRadius = 6378137.0
                                        
                                        val path = Path()
                                        routePoints.forEachIndexed { index, pos ->
                                            val dLat = Math.toRadians(pos.latitude - center.latitude)
                                            val dLon = Math.toRadians(pos.longitude - center.longitude)
                                            val dx = earthRadius * dLon * Math.cos(centerLatRad)
                                            val dy = earthRadius * dLat
                                            
                                            val x = size.width / 2 + (dx / metersPerPixel).toFloat()
                                            val y = size.height / 2 - (dy / metersPerPixel).toFloat()
                                            
                                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }
                                        
                                        drawPath(
                                            path = path,
                                            color = androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
                                            style = Stroke(width = 12f)
                                        )
                                    }
                                }
                                
                                // Overlay de Marcador (Usando Compose estándar anclado al centro para compatibilidad)
                                if (geoPoint != null) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.Warning,
                                        contentDescription = "Ubicación del Incidente",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(48.dp)
                                            .padding(bottom = 24.dp) // Offset para que la punta apunte al centro exacto
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Esperando permisos de GPS...")
                            }
                        }
                    }
                    
                    // LLEGUÉ AL LUGAR
                    val isArrived by viewModel.isArrived.collectAsStateWithLifecycle()
                    
                    if (isArrived) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = "¡ESTÁ EN EL LUGAR DEL INCIDENTE!",
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isArrived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        ),
                        onClick = {
                            stopTrackingService(context)
                            viewModel.finishMission(emergency.id, uid)
                            onMissionEnded()
                        }
                    ) {
                        Text(
                            if (isArrived) "CONFIRMAR FINALIZACIÓN" else "LLEGUÉ AL LUGAR", 
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            is MissionState.Finished -> {
                LaunchedEffect(Unit) {
                    stopTrackingService(context)
                    onMissionEnded()
                }
            }
        }
    }
}

private fun startTrackingService(context: Context, uid: String, emergencyId: String?, targetLat: Double? = null, targetLon: Double? = null) {
    if (emergencyId == null) return
    val intent = Intent(context, EmergencyTrackingService::class.java).apply {
        action = EmergencyTrackingService.ACTION_START
        putExtra(EmergencyTrackingService.EXTRA_UID, uid)
        putExtra(EmergencyTrackingService.EXTRA_EMERGENCY_ID, emergencyId)
        if (targetLat != null && targetLon != null) {
            putExtra(EmergencyTrackingService.EXTRA_TARGET_LAT, targetLat)
            putExtra(EmergencyTrackingService.EXTRA_TARGET_LON, targetLon)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopTrackingService(context: Context) {
    val intent = Intent(context, EmergencyTrackingService::class.java).apply {
        action = EmergencyTrackingService.ACTION_STOP
    }
    context.startService(intent)
}
