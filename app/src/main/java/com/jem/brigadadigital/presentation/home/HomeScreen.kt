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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    onNavigateToResponders: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activeEmergencies by emergencyViewModel.allActiveEmergencies.collectAsStateWithLifecycle()
    val pastEmergencies by emergencyViewModel.pastEmergencies.collectAsStateWithLifecycle()
    val responderCount by emergencyViewModel.allActiveRespondersCount.collectAsStateWithLifecycle()
    val availableCount by viewModel.availablePersonnelCount.collectAsStateWithLifecycle()
    
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text("Brigada Digital", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineLarge)
                            if (profileState is ProfileState.Loaded) {
                                val user = (profileState as ProfileState.Loaded).profile
                                Text(
                                    text = user.cuartelId.ifEmpty { "Sin Destacamento" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    actions = {
                        IconButton(onClick = { /* Future: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        when (val state = profileState) {
            is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Alertas",
                                value = activeEmergencies.size.toString(),
                                subtitle = "En Curso",
                                icon = Icons.Default.Warning,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToActiveAlerts
                            )
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Personal",
                                value = responderCount.toString(),
                                subtitle = "Activo",
                                icon = Icons.Default.Groups,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onClick = onNavigateToResponders
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Disponibles",
                                value = availableCount.toString(),
                                subtitle = "En Cuartel",
                                icon = Icons.Default.CellTower,
                                accentColor = MaterialTheme.colorScheme.tertiary
                            )
                            MetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Historial",
                                value = pastEmergencies.size.toString(),
                                subtitle = "Eventos",
                                icon = Icons.Default.List,
                                accentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                onClick = onNavigateToHistory
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (topAlerts.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Alertas en Curso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToActiveAlerts) {
                                Text("Ver todas")
                            }
                        }
                        
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
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CellTower, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Vigilancia de Emergencias en Tiempo Real Activa",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                }
            }
            is ProfileState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                        Button(onClick = { viewModel.checkUserProfile(uid) }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.aspectRatio(1.1f), // Slightly less than square for better horizontal space
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Icon Background Accent (Top Right)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = accentColor.copy(alpha = 0.9f)
                )
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
            .height(130.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
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
                    maxLines = 1
                )
                Text(
                    text = alert.direccion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text(alert.tipo.uppercase(), style = MaterialTheme.typography.labelSmall)
                    }
                    if (alert.isGlobal) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("GLOBAL", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}


