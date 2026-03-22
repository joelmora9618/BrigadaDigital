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
    
    // Cuartel Dropdown State
    var cuartelExpanded by remember { mutableStateOf(false) }
    var selectedCuartel by remember { mutableStateOf("Cuartel I - Montserrat") }
    val destacamentos = listOf(
        "Cuartel I - Montserrat",
        "Cuartel II - Pompeya",
        "Cuartel III - Barracas",
        "Cuartel IV - Recoleta",
        "Cuartel V - Belgrano",
        "Cuartel VI - Villa Crespo",
        "Cuartel VII - Flores",
        "Cuartel VIII - Nueva Pompeya",
        "Cuartel IX - Chacarita",
        "Cuartel X - Villa Lugano",
        "Cuartel XI - Villa Devoto",
        "Cuartel XII - Villa Urquiza",
        "Bomberos Voluntarios La Boca",
        "Bomberos Voluntarios San Telmo",
        "Bomberos Voluntarios Vuelta de Rocha",
        "B.V. Avellaneda",
        "B.V. Lanús",
        "B.V. Lomas de Zamora",
        "B.V. Quilmes",
        "B.V. San Martín",
        "B.V. Vicente López",
        "B.V. San Isidro",
        "B.V. Tigre",
        "B.V. Morón",
        "B.V. Tres de Febrero",
        "B.V. La Matanza",
        "B.V. Florencio Varela",
        "B.V. Berazategui",
        "B.V. Brown",
        "B.V. Moreno",
        "B.V. Merlo",
        "B.V. Esteban Echeverría",
        "B.V. Ezeiza",
        "B.V. Ituzaingó",
        "B.V. Hurlingham",
        "B.V. José C. Paz",
        "B.V. Malvinas Argentinas",
        "B.V. Pilar",
        "B.V. Escobar",
        "B.V. Ensenada",
        "B.V. Berisso"
    )
    
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
            
            // Cuartel (Destacamento) Dropdown
            ExposedDropdownMenuBox(
                expanded = cuartelExpanded,
                onExpandedChange = { cuartelExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCuartel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Destacamento / Cuartel") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cuartelExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = cuartelExpanded,
                    onDismissRequest = { cuartelExpanded = false }
                ) {
                    destacamentos.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedCuartel = selectionOption
                                cuartelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val profile = UserProfile(
                        uid = uid,
                        nombre = nombre,
                        apellido = apellido,
                        rango = selectedRango,
                        especialidad = selectedEspecialidad,
                        cuartelId = selectedCuartel,
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
