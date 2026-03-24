package com.jem.brigadadigital.presentation.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModel
import com.jem.brigadadigital.presentation.profile.ProfileState
import com.jem.brigadadigital.presentation.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uid: String,
    viewModel: ProfileViewModel,
    emergencyViewModel: EmergencyViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToActiveAlerts: () -> Unit,
    onNavigateToResponders: () -> Unit,
    onNavigateToCreateEmergency: () -> Unit,
    onNavigateToAvailablePersonnel: () -> Unit,
    onNavigateToMoviles: () -> Unit,
    movilViewModel: com.jem.brigadadigital.presentation.movil.MovilViewModel
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activeEmergencies by emergencyViewModel.allActiveEmergencies.collectAsStateWithLifecycle()
    val pastEmergencies by emergencyViewModel.pastEmergencies.collectAsStateWithLifecycle()
    val responderCount by emergencyViewModel.allActiveRespondersCount.collectAsStateWithLifecycle()
    val availableCount by viewModel.availablePersonnelCount.collectAsStateWithLifecycle()
    val moviles by movilViewModel.moviles.collectAsStateWithLifecycle()

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Loaded) {
            movilViewModel.observeMoviles((profileState as ProfileState.Loaded).profile.cuartelId)
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val topAlerts = remember(activeEmergencies) { activeEmergencies.take(5) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Permission denied
        }
    }

    LaunchedEffect(uid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val hasNewAlerts by emergencyViewModel.hasUnrespondedAlerts.collectAsStateWithLifecycle()
            
            TopAppBar(
                title = { 
                    Text(
                        "Brigada Digital", 
                        fontWeight = FontWeight.Black, 
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    ) 
                },
                actions = {
                    IconButton(onClick = onNavigateToActiveAlerts) {
                        BadgedBox(
                            badge = {
                                if (hasNewAlerts) {
                                    Badge(
                                        containerColor = Color(0xFFE53935),
                                        contentColor = Color.White
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1C2E)) // OPAQUE
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1C2E), // Deep Navy
                            Color(0xFF0F101A)  // Near Black
                        )
                    )
                )
        ) {
            when (val state = profileState) {
                is ProfileState.Loading, ProfileState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF64B5F6))
                    }
                }
                is ProfileState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.checkUserProfile(uid) }) {
                            Text("Reintentar")
                        }
                    }
                }
                ProfileState.NotFound -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Perfil no encontrado", color = Color.White)
                    }
                }
                is ProfileState.Saved -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF64B5F6))
                    }
                }
                is ProfileState.Loaded -> {
                    val user = state.profile
                    var maplibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
                    
                    LaunchedEffect(maplibreMap, activeEmergencies) {
                        maplibreMap?.let { map ->
                            // Ensure style is loaded before adding markers
                            if (map.style != null) {
                                map.clear()
                                activeEmergencies.forEach { alert ->
                                    alert.ubicacion?.let { loc ->
                                        map.addMarker(
                                            org.maplibre.android.annotations.MarkerOptions()
                                                .position(org.maplibre.android.geometry.LatLng(loc.latitude, loc.longitude))
                                                .title(alert.titulo)
                                        )
                                    }
                                }
                                
                                if (activeEmergencies.size == 1) {
                                    activeEmergencies.first().ubicacion?.let { loc ->
                                        map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            org.maplibre.android.geometry.LatLng(loc.latitude, loc.longitude), 15.0
                                        ))
                                    }
                                } else if (activeEmergencies.isNotEmpty()) {
                                    map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                        org.maplibre.android.geometry.LatLng(-34.6037, -58.3816), 10.5
                                    ))
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))

                        // HEADER: Avatar + Greeting + SOS Alert (Megaphone)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.nombre.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hola, ${user.nombre}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Mantente seguro",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            val dispatchRoles = listOf("admin", "jefe", "oficial", "subjefe")
                            val canDispatch = remember(user) {
                                user.role.lowercase() in dispatchRoles || user.rango.lowercase() in dispatchRoles
                            }

                            if (canDispatch) {
                                Surface(
                                    modifier = Modifier.size(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFE53935),
                                    onClick = onNavigateToCreateEmergency
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Campaign,
                                            contentDescription = "Alert",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // MAP CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(32.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        org.maplibre.android.maps.MapView(ctx).apply {
                                            getMapAsync { map ->
                                                map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                                                    maplibreMap = map
                                                }
                                                map.uiSettings.isLogoEnabled = false
                                                map.uiSettings.isAttributionEnabled = false
                                            }
                                        }
                                    },
                                    update = { /* Updates handled by LaunchedEffect */ }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ALERTS CAROUSEL (Horizontal Pager)
                        if (topAlerts.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { topAlerts.size })
                            
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                pageSpacing = 16.dp
                            ) { page ->
                                val alert = topAlerts[page]
                                AlertCarouselItem(
                                    alert = alert,
                                    onClick = onNavigateToActiveAlerts
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Pager Indicator Dots
                            Row(
                                Modifier
                                    .height(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(topAlerts.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) 
                                        Color(0xFF64B5F6) 
                                    else 
                                        Color.White.copy(alpha = 0.2f)
                                        
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(8.dp)
                                            .background(color, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        } else {
                            // Empty State / Placeholder
                            Card(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF24273F))
                            ) {                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("No hay alertas activas", color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // ACTION GRID (2x2)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Personal\nDisponible",
                                value = availableCount.toString(),
                                icon = Icons.Default.LocationOn,
                                backgroundColor = Color(0xFF26C6DA),
                                onClick = onNavigateToAvailablePersonnel
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Personal\nActivo",
                                value = responderCount.toString(),
                                icon = Icons.Default.Security,
                                backgroundColor = Color(0xFF66BB6A),
                                onClick = onNavigateToResponders
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Alertas\nen Curso",
                                value = activeEmergencies.size.toString(),
                                icon = Icons.Default.NotificationsActive,
                                backgroundColor = Color(0xFFEF5350),
                                onClick = onNavigateToActiveAlerts
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Historial\nde Eventos",
                                value = pastEmergencies.size.toString(),
                                icon = Icons.Default.History,
                                backgroundColor = Color(0xFF880E4F),
                                onClick = onNavigateToHistory
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Guardias",
                                value = "",
                                icon = Icons.Default.CalendarToday,
                                backgroundColor = Color(0xFF7E57C2),
                                onClick = { /* Por implementar */ }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Móviles",
                                value = moviles.size.toString(),
                                icon = Icons.Default.DirectionsCar,
                                backgroundColor = Color(0xFF5C6BC0),
                                onClick = onNavigateToMoviles
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                is ProfileState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color.White)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AlertCarouselItem(
    alert: com.jem.brigadadigital.domain.model.EmergencyEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF24273F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE53935).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = alert.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = alert.tipo.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // Icon Background
            Surface(
                modifier = Modifier.size(44.dp).align(Alignment.TopStart),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.White.copy(alpha = 0.25f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            // Metric Value (Top Right)
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Title (Bottom Left)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}


