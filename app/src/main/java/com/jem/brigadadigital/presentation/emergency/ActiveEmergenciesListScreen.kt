package com.jem.brigadadigital.presentation.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveEmergenciesListScreen(
    viewModel: EmergencyViewModel,
    onItemClicked: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val activeList by viewModel.allActiveEmergencies.collectAsStateWithLifecycle()
    val respondedIds by viewModel.respondedIds.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertas en Curso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E), // Darker for deep mode
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
            if (activeList.isEmpty()) {
                Text(
                    "No hay alertas en curso",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeList.size) { index ->
                        val emergency = activeList[index]
                        val isNew = emergency.id !in respondedIds
                        EmergencyOverviewItem(
                            emergency = emergency, 
                            isNew = isNew,
                            onClick = { onItemClicked(emergency.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyOverviewItem(
    emergency: com.jem.brigadadigital.domain.model.EmergencyEvent,
    isNew: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = emergency.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNew) {
                        Badge(containerColor = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 8.dp)) {
                            Text("NUEVA", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                    Badge(containerColor = if (isNew) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error) {
                        Text("ACTIVA", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${emergency.tipo} • ${emergency.direccion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            val date = java.util.Date(emergency.timestamp)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            Text(
                text = "Iniciada: ${format.format(date)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}
