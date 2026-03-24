package com.jem.brigadadigital.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.presentation.auth.AuthViewModel
import com.jem.brigadadigital.presentation.dispatch.DispatchScreen
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModel
import com.jem.brigadadigital.presentation.history.HistoryScreen
import com.jem.brigadadigital.presentation.home.HomeScreen
import com.jem.brigadadigital.presentation.profile.ProfileScreen
import com.jem.brigadadigital.presentation.profile.ProfileViewModel

sealed class BottomNavItem(
    val route: String, 
    val title: String, 
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : BottomNavItem("home_tab", "Inicio", Icons.Filled.Home, Icons.Outlined.Home)
    data object AlertMap : BottomNavItem("map_tab", "Mapa", Icons.Filled.Map, Icons.Outlined.Map)
    data object Dispatch : BottomNavItem("dispatch_tab", "Alertas", Icons.Filled.Send, Icons.Outlined.Send)
    data object Profile : BottomNavItem("profile_tab", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainScreen(
    uid: String,
    currentUser: UserProfile,
    profileViewModel: ProfileViewModel,
    emergencyViewModel: EmergencyViewModel,
    authViewModel: AuthViewModel,
    parentNavController: NavHostController, // To navigate to Dashboard or external screens
    onNavigateToActiveAlerts: () -> Unit,
    onNavigateToResponders: () -> Unit
) {
    val bottomNavController = rememberNavController()
    
    // Roles with dispatch access
    val dispatchRoles = listOf("admin", "jefe", "oficial", "subjefe")
    val canDispatch = currentUser.role.lowercase() in dispatchRoles || 
                     currentUser.rango.lowercase() in dispatchRoles

    val tabs = mutableListOf(
        BottomNavItem.Home,
        BottomNavItem.AlertMap,
        BottomNavItem.Dispatch,
        BottomNavItem.Profile
    )

    val selectedTabRoute by emergencyViewModel.selectedTabRoute.collectAsStateWithLifecycle()
    val activeResponders by emergencyViewModel.activeResponders.collectAsStateWithLifecycle()
    
    // Sincronizar personal activo con ProfileViewModel para evitar solapamientos en las métricas
    LaunchedEffect(activeResponders) {
        val uids = activeResponders.map { it.uid }.toSet()
        profileViewModel.setActiveResponders(uids)
    }

    // Sincronizar navegación al recrear MainScreen (FASE 12)
    LaunchedEffect(Unit) {
        if (bottomNavController.currentDestination?.route != selectedTabRoute) {
            bottomNavController.navigate(selectedTabRoute) {
                popUpTo(bottomNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                        selected = isSelected,
                        onClick = {
                            emergencyViewModel.updateSelectedTab(item.route)
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            NavHost(navController = bottomNavController, startDestination = BottomNavItem.Home.route) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        uid = uid,
                        viewModel = profileViewModel,
                        emergencyViewModel = emergencyViewModel,
                        onNavigateToDashboard = { parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.Dashboard.createRoute()) },
                        onNavigateToHistory = { parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.History.route) },
                        onNavigateToActiveAlerts = onNavigateToActiveAlerts,
                        onNavigateToResponders = onNavigateToResponders,
                        onNavigateToCreateEmergency = { parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.CreateEmergency.route) },
                        onNavigateToAvailablePersonnel = { parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.AvailablePersonnel.route) },
                        onNavigateToMoviles = { parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.Moviles.route) }
                    )
                }
                composable(BottomNavItem.AlertMap.route) {
                    com.jem.brigadadigital.presentation.emergency.ActiveEmergenciesMapScreen(
                        viewModel = emergencyViewModel,
                        onNavigateToDashboard = { emergencyId ->
                            parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.Dashboard.createRoute(emergencyId))
                        }
                    )
                }
                composable(BottomNavItem.Dispatch.route) {
                    DispatchScreen(
                        userProfile = currentUser,
                        emergencyViewModel = emergencyViewModel,
                        onNavigateToDashboard = { emergencyId -> 
                            parentNavController.navigate(com.jem.brigadadigital.presentation.navigation.Screen.Dashboard.createRoute(emergencyId)) 
                        },
                        onOpenMapPicker = { parentNavController.navigate("map_picker") }
                    )
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        uid = uid,
                        userProfile = currentUser,
                        viewModel = profileViewModel,
                        authViewModel = authViewModel,
                        onLogout = {
                            profileViewModel.resetState()
                            emergencyViewModel.resetState()
                        }
                    )
                }
            }
        }
    }
}
