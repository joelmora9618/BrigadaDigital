package com.jem.brigadadigital.presentation.dispatch

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModel

import androidx.compose.material.icons.filled.LocationOn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    userProfile: UserProfile,
    emergencyViewModel: EmergencyViewModel,
    onNavigateToDashboard: (String) -> Unit,
    onOpenMapPicker: () -> Unit
) {
    val dispatchRoles = listOf("admin", "jefe", "oficial", "subjefe")
    val canCreateAlert = remember(userProfile) {
        userProfile.role.lowercase() in dispatchRoles || userProfile.rango.lowercase() in dispatchRoles
    }

    val availableTabs = remember(canCreateAlert) {
        mutableListOf<String>().apply {
            if (canCreateAlert) add("Nueva Alerta")
            add("En Curso")
            add("Finalizadas")
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Panel de Despacho", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    actions = {
                        IconButton(onClick = { onNavigateToDashboard("") }) {
                            Icon(Icons.Default.List, contentDescription = "Monitor de Mando")
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                ) {
                    availableTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTab < availableTabs.size) {
                when (availableTabs[selectedTab]) {
                    "Nueva Alerta" -> NewAlertForm(userProfile, emergencyViewModel, paddingValues, onOpenMapPicker)
                    "En Curso" -> ActiveEmergenciesList(emergencyViewModel, paddingValues, onNavigateToDashboard)
                    "Finalizadas" -> FinishedEmergenciesList(emergencyViewModel, paddingValues)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAlertForm(
    userProfile: UserProfile,
    emergencyViewModel: EmergencyViewModel,
    paddingValues: PaddingValues,
    onOpenMapPicker: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf("Incendio") }
    var isGlobal by remember { mutableStateOf(false) }
    val tipos = listOf("Incendio", "Accidente", "Rescate", "Servicio Especial")
    val context = LocalContext.current

    val suggestions by emergencyViewModel.suggestions.collectAsStateWithLifecycle()
    val selectedPos by emergencyViewModel.selectedPosition.collectAsStateWithLifecycle()
    val selectedAddress by emergencyViewModel.selectedAddress.collectAsStateWithLifecycle()

    LaunchedEffect(selectedAddress) {
        if (selectedAddress != null) {
            direccion = selectedAddress!!
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Default.Send,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isGlobal) "Nueva Alerta GLOBAL" else "Nueva Alerta LOCAL (${userProfile.cuartelId})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isGlobal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Despachando como: ${userProfile.rango} ${userProfile.nombre}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título Corto (Ej: Siniestro Vial Ruta 2)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                readOnly = true,
                value = tipoSeleccionado,
                onValueChange = { },
                label = { Text("Tipo de Siniestro") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                tipos.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            tipoSeleccionado = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        var isSuggestionsExpanded by remember { mutableStateOf(false) }
        
        ExposedDropdownMenuBox(
            expanded = isSuggestionsExpanded && suggestions.isNotEmpty(),
            onExpandedChange = { isSuggestionsExpanded = it }
        ) {
            OutlinedTextField(
                value = direccion,
                onValueChange = { 
                    direccion = it
                    emergencyViewModel.searchAddress(it)
                    isSuggestionsExpanded = true
                },
                label = { Text("Dirección Exacta") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    IconButton(onClick = onOpenMapPicker) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Mapa")
                    }
                }
            )
            
            ExposedDropdownMenu(
                expanded = isSuggestionsExpanded && suggestions.isNotEmpty(),
                onDismissRequest = { isSuggestionsExpanded = false }
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion.display_name) },
                        onClick = {
                            direccion = suggestion.display_name
                            emergencyViewModel.onLocationSelected(suggestion.lat, suggestion.lon, suggestion.display_name)
                            isSuggestionsExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Información Adicional (Opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alerta Global",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isGlobal) "Se enviará a TODA la brigada" else "Solo visible para ${userProfile.cuartelId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isGlobal,
                onCheckedChange = { isGlobal = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.error,
                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }

        Button(
            onClick = {
                if (titulo.isBlank() || direccion.isBlank()) {
                    Toast.makeText(context, "El título y la dirección son obligatorios", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                val lat = selectedPos?.latitude
                val lon = selectedPos?.longitude
                
                emergencyViewModel.createEmergency(
                    titulo = titulo.uppercase(),
                    descripcion = descripcion,
                    tipo = tipoSeleccionado,
                    direccion = direccion,
                    isGlobal = isGlobal,
                    lat = lat,
                    lon = lon
                )
                
                val msg = if (isGlobal) "ALERTA GLOBAL ENVIADA A TODA LA BRIGADA" else "ALERTA LOCAL DESPACHADA (${userProfile.cuartelId})"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                
                titulo = ""
                direccion = ""
                descripcion = ""
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(if (isGlobal) Icons.Filled.Warning else Icons.Filled.Send, contentDescription = "Siren")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGlobal) "LANZAR ALERTA GLOBAL" else "LANZAR ALERTA LOCAL",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ActiveEmergenciesList(
    emergencyViewModel: EmergencyViewModel,
    paddingValues: PaddingValues,
    onItemClicked: (String) -> Unit
) {
    val activeList by emergencyViewModel.allActiveEmergencies.collectAsStateWithLifecycle()
    
    if (activeList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay alertas en curso", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeList.size) { index ->
                val emergency = activeList[index]
                EmergencyItem(emergency, onClick = { onItemClicked(emergency.id) })
            }
        }
    }
}

@Composable
fun FinishedEmergenciesList(
    emergencyViewModel: EmergencyViewModel,
    paddingValues: PaddingValues
) {
    val finishedList by emergencyViewModel.pastEmergencies.collectAsStateWithLifecycle()
    
    if (finishedList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay alertas finalizadas", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(finishedList.size) { index ->
                val emergency = finishedList[index]
                EmergencyItem(emergency)
            }
        }
    }
}

@Composable
fun EmergencyItem(
    emergency: com.jem.brigadadigital.domain.model.EmergencyEvent,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                Badge(
                    containerColor = if (emergency.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                ) {
                    Text(if (emergency.isActive) "EN CURSO" else "FINALIZADA")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (emergency.isGlobal) {
                    AssistChip(
                        onClick = {},
                        label = { Text("GLOBAL", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.error
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("LOCAL", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (emergency.isGlobal) "Todas las zonas" else emergency.cuartelId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${emergency.tipo} - ${emergency.direccion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            val date = java.util.Date(emergency.timestamp)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            Text(
                text = "Iniciada: ${format.format(date)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
