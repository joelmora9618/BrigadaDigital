package com.jem.brigadadigital.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jem.brigadadigital.presentation.auth.AuthState
import com.jem.brigadadigital.presentation.auth.AuthViewModel
import com.jem.brigadadigital.presentation.auth.AuthViewModelFactory
import com.jem.brigadadigital.presentation.auth.login.LoginScreen
import com.jem.brigadadigital.presentation.auth.register.RegisterScreen
import com.jem.brigadadigital.presentation.emergency.EmergencyScreen
import com.jem.brigadadigital.presentation.emergency.EmergencyState
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModel
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModelFactory
import com.jem.brigadadigital.presentation.home.HomeScreen
import com.jem.brigadadigital.presentation.profile.ProfileState
import com.jem.brigadadigital.presentation.profile.ProfileViewModel
import com.jem.brigadadigital.presentation.profile.ProfileViewModelFactory
import com.jem.brigadadigital.presentation.profile.SetupProfileScreen

// FASE 5
import com.jem.brigadadigital.presentation.dashboard.BrigadaDashboardScreen
import com.jem.brigadadigital.presentation.dashboard.DashboardViewModel
import com.jem.brigadadigital.presentation.closure.ClosureReportScreen
import com.jem.brigadadigital.presentation.history.HistoryScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object RootHome : Screen("root_home")
    
    // FASE 5
    data object Dashboard : Screen("dashboard/{emergencyId}") {
        fun createRoute(emergencyId: String = "") = if (emergencyId.isEmpty()) "dashboard/none" else "dashboard/$emergencyId"
    }
    data object MapPicker : Screen("map_picker")
    data object ClosureReport : Screen("closure_report/{emergencyId}") {
        fun createRoute(emergencyId: String) = "closure_report/$emergencyId"
    }
    data object History : Screen("history")
}

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
    val emergencyViewModel: EmergencyViewModel = viewModel(factory = EmergencyViewModelFactory())

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.RootHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.RootHome.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.RootHome.route) {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            
            if (authState is AuthState.Success) {
                val uid = (authState as AuthState.Success).userUid
                val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
                val emergencyState by emergencyViewModel.emergencyState.collectAsStateWithLifecycle()
                val emergencyError by emergencyViewModel.errorMessage.collectAsStateWithLifecycle()

                var isMissionActive by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                var completedEmergencyId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

                val context = LocalContext.current

                // Mostrar error visual en caso de fallar permisos al crear simulacro
                LaunchedEffect(emergencyError) {
                    if (emergencyError != null) {
                        Toast.makeText(context, emergencyError, Toast.LENGTH_LONG).show()
                        emergencyViewModel.clearError()
                    }
                }

                LaunchedEffect(uid) {
                    if (profileState is ProfileState.Idle) {
                        profileViewModel.checkUserProfile(uid)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Underneath layer: Setup Profile or Home Screen
                    when (profileState) {
                        is ProfileState.Loading, is ProfileState.Idle -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is ProfileState.NotFound -> {
                            SetupProfileScreen(
                                uid = uid,
                                viewModel = profileViewModel,
                                onProfileSaved = {
                                    profileViewModel.checkUserProfile(uid)
                                }
                            )
                        }
                        is ProfileState.Loaded -> {
                            val currentUser = (profileState as ProfileState.Loaded).profile
                            com.jem.brigadadigital.presentation.main.MainScreen(
                                uid = uid,
                                currentUser = currentUser,
                                profileViewModel = profileViewModel,
                                emergencyViewModel = emergencyViewModel,
                                authViewModel = authViewModel,
                                parentNavController = navController
                            )
                        }
                        is ProfileState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val msg = (profileState as ProfileState.Error).message
                                Text("Ocurrió un error: $msg")
                            }
                        }
                        else -> {}
                    }

                    // Topmost layer: Emergency Overlay or Mission Map
                    if (emergencyState is EmergencyState.Active && (emergencyState as EmergencyState.Active).emergency.id != completedEmergencyId) {
                        val activeEmergency = (emergencyState as EmergencyState.Active).emergency
                        if (isMissionActive) {
                            val missionViewModel: com.jem.brigadadigital.presentation.mission.MissionViewModel = viewModel(factory = com.jem.brigadadigital.presentation.mission.MissionViewModelFactory())
                            LaunchedEffect(activeEmergency.id, uid) {
                                missionViewModel.initMission(activeEmergency.id, uid)
                            }
                            com.jem.brigadadigital.presentation.mission.ActiveMissionScreen(
                                uid = uid,
                                viewModel = missionViewModel,
                                onMissionEnded = {
                                    isMissionActive = false
                                    completedEmergencyId = activeEmergency.id
                                }
                            )
                        } else {
                            EmergencyScreen(
                                emergency = activeEmergency,
                                uid = uid,
                                viewModel = emergencyViewModel,
                                onConfirmed = {
                                    isMissionActive = true
                                }
                            )
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            isMissionActive = false
                        }
                    }
                }
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RootHome.route) { inclusive = true }
                    }
                }
            }
        }
        
        // FASE 5 RUTAS
        composable(Screen.Dashboard.route) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            val dashboardViewModel: DashboardViewModel = viewModel()
            
            // Si pasamos el ID por SavedStateHandle (que viewModel() lo hace automáticamente si usamos el factory por defecto o Hilt)
            // No necesitamos pasarlo manualmente si el ViewModel lo lee del SavedStateHandle.
            
            val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
            val currentUser = if (profileState is ProfileState.Loaded) (profileState as ProfileState.Loaded).profile else com.jem.brigadadigital.domain.model.UserProfile()
            
            BrigadaDashboardScreen(
                viewModel = dashboardViewModel,
                currentUser = currentUser,
                onCloseIncidentClicked = { id ->
                    navController.navigate(Screen.ClosureReport.createRoute(id))
                }
            )
        }

        composable(Screen.MapPicker.route) {
            com.jem.brigadadigital.presentation.dispatch.MapPickerScreen(
                viewModel = emergencyViewModel,
                onLocationConfirmed = { lat, lon ->
                    emergencyViewModel.onLocationSelected(lat, lon, null)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ClosureReport.route) { backStackEntry ->
            val emergencyId = backStackEntry.arguments?.getString("emergencyId") ?: ""
            val context = LocalContext.current
            ClosureReportScreen(
                emergencyId = emergencyId,
                onSaved = {
                    navController.navigate(Screen.RootHome.route) {
                        popUpTo(Screen.RootHome.route) { inclusive = true }
                    }
                    Toast.makeText(context, "Incidente cerrado correctamente.", Toast.LENGTH_SHORT).show()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

    }
}
