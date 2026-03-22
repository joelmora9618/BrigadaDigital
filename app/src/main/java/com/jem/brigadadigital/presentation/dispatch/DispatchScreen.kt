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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    userProfile: UserProfile,
    emergencyViewModel: EmergencyViewModel,
    onNavigateToDashboard: () -> Unit,
    onOpenMapPicker: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf("Incendio") }
    val tipos = listOf("Incendio", "Accidente", "Rescate", "Servicio Especial")
    val context = LocalContext.current

    val suggestions by emergencyViewModel.suggestions.collectAsStateWithLifecycle()
    val selectedPos by emergencyViewModel.selectedPosition.collectAsStateWithLifecycle()

    LaunchedEffect(selectedPos) {
        if (selectedPos != null) {
            direccion = "UBICACIÓN SELECCIONADA EN MAPA"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Panel de Despacho", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nueva Alerta General",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
            
            // Selector de Tipo
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

            // Campo de Dirección con Autocompletado
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

            Spacer(modifier = Modifier.weight(1f))

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
                        lat = lat,
                        lon = lon
                    )
                    
                    Toast.makeText(context, "ALERTA GENERAL ENVIADA A TODA LA BRIGADA", Toast.LENGTH_LONG).show()
                    
                    // Reset form
                    titulo = ""
                    direccion = ""
                    descripcion = ""
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Siren")
                Spacer(modifier = Modifier.width(8.dp))
                Text("LANZAR ALERTA GENERAL", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // NUEVO: Botón para acceder al Monitor de Mando
            OutlinedButton(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MONITOR DE MANDO (REAL-TIME)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
