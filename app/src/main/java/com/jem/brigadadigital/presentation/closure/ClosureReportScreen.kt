package com.jem.brigadadigital.presentation.closure

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.jem.brigadadigital.domain.repository.EmergencyRepository
import com.jem.brigadadigital.data.repository.EmergencyRepositoryImpl
import com.jem.brigadadigital.domain.repository.StorageRepository
import com.jem.brigadadigital.data.repository.StorageRepositoryImpl
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosureReportScreen(
    emergencyId: String,
    emergencyRepository: EmergencyRepository = EmergencyRepositoryImpl(),
    storageRepository: StorageRepository = StorageRepositoryImpl(),
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }
    
    // Form State
    var selectedVehicles by remember { mutableStateOf(setOf<String>()) }
    var personnelCount by remember { mutableStateOf(1) }
    var briefDescription by remember { mutableStateOf("") }
    
    // Photo State
    val capturedPhotos = remember { mutableStateListOf<Uri>() }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val availableVehicles = listOf(
        "Móvil 1 (Ataque Rápido)",
        "Móvil 2 (Cisterna)",
        "Móvil 3 (Escalera)",
        "Unidad de Rescate",
        "Ambulancia"
    )

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { capturedPhotos.add(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reporte de Cierre (Evidencias)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Móviles Intervinientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column {
                availableVehicles.forEach { vehicle ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedVehicles.contains(vehicle),
                            onCheckedChange = { checked ->
                                val newSet = selectedVehicles.toMutableSet()
                                if (checked) newSet.add(vehicle) else newSet.remove(vehicle)
                                selectedVehicles = newSet
                            }
                        )
                        Text(text = vehicle)
                    }
                }
            }
            
            HorizontalDivider()
            
            Text(
                text = "Evidencia Fotográfica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    OutlinedCard(
                        onClick = {
                            val uri = createPhotoUri(context)
                            tempPhotoUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Capturar")
                        }
                    }
                }

                items(capturedPhotos) { uri ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { capturedPhotos.remove(uri) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            HorizontalDivider()

            Text(
                text = "Breve Descripción del Servicio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = briefDescription,
                onValueChange = { briefDescription = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("Ej: Extinción de incendio forestal. Sin heridos.") },
                maxLines = 5
            )
            
            Button(
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val photoUrls = mutableListOf<String>()
                            capturedPhotos.forEachIndexed { index, uri ->
                                val fileName = "photo_${index}_${System.currentTimeMillis()}.jpg"
                                val path = "emergencies/$emergencyId/photos/$fileName"
                                val uploadResult = storageRepository.uploadPhoto(path, uri)
                                uploadResult.onSuccess { url -> photoUrls.add(url) }
                            }

                            val reportData = mapOf(
                                "vehicles" to selectedVehicles.toList(),
                                "personnelCount" to (selectedVehicles.size * 2), // Demo: Estimado por móviles
                                "description" to briefDescription,
                                "evidencias" to photoUrls
                            )
                            
                            val result = emergencyRepository.closeEmergency(emergencyId, reportData)
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                Toast.makeText(context, "Error al cerrar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("FINALIZAR INCIDENTE Y SUBIR REPORTE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun createPhotoUri(context: Context): Uri {
    val directory = File(context.filesDir, "Pictures")
    if (!directory.exists()) directory.mkdirs()
    val file = File(directory, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
