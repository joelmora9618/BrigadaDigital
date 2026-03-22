@file:OptIn(ExperimentalMaterial3Api::class)

package com.jem.brigadadigital.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.presentation.auth.AuthViewModel

@Composable
fun SetupProfileScreen(
    uid: String,
    viewModel: ProfileViewModel,
    onProfileSaved: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var cuartelId by remember { mutableStateOf("") }
    
    // Rango Dropdown State
    var rangoExpanded by remember { mutableStateOf(false) }
    var selectedRango by remember { mutableStateOf("Bombero") }
    val rangos = listOf("Aspirante", "Bombero", "Cabo", "Sargento", "Suboficial", "Oficial")

    // Especialidad Dropdown State
    var especialidadExpanded by remember { mutableStateOf(false) }
    var selectedEspecialidad by remember { mutableStateOf("General") }
    val especialidades = listOf("General", "Rescate", "Incendio", "Chofer", "Paramédico", "Materiales Peligrosos")

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Saved || profileState is ProfileState.Loaded) {
            onProfileSaved()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Completa tu Perfil",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rango Dropdown
            ExposedDropdownMenuBox(
                expanded = rangoExpanded,
                onExpandedChange = { rangoExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedRango,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rango") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rangoExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = rangoExpanded,
                    onDismissRequest = { rangoExpanded = false }
                ) {
                    rangos.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedRango = selectionOption
                                rangoExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Especialidad Dropdown
            ExposedDropdownMenuBox(
                expanded = especialidadExpanded,
                onExpandedChange = { especialidadExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedEspecialidad,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Especialidad") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = especialidadExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = especialidadExpanded,
                    onDismissRequest = { especialidadExpanded = false }
                ) {
                    especialidades.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedEspecialidad = selectionOption
                                especialidadExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = cuartelId,
                onValueChange = { cuartelId = it },
                label = { Text("Cuartel (Ej: Cuartel Central)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val profile = UserProfile(
                        uid = uid,
                        nombre = nombre,
                        apellido = apellido,
                        rango = selectedRango,
                        especialidad = selectedEspecialidad,
                        cuartelId = cuartelId,
                        disponible = true // Defaults to available when completing profile
                    )
                    viewModel.saveProfile(profile)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nombre.isNotBlank() && apellido.isNotBlank() && profileState !is ProfileState.Loading
            ) {
                Text("Finalizar Registro")
            }

            if (profileState is ProfileState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            if (profileState is ProfileState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (profileState as ProfileState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
