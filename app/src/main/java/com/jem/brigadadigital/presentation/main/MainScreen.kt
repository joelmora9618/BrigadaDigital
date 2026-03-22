package com.jem.brigadadigital.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jem.brigadadigital.domain.model.UserProfile
import com.jem.brigadadigital.presentation.auth.AuthViewModel
import com.jem.brigadadigital.presentation.dispatch.DispatchScreen
import com.jem.brigadadigital.presentation.emergency.EmergencyViewModel
import com.jem.brigadadigital.presentation.history.HistoryScreen
import com.jem.brigadadigital.presentation.home.HomeScreen
import com.jem.brigadadigital.presentation.profile.ProfileScreen
import com.jem.brigadadigital.presentation.profile.ProfileViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : BottomNavItem("home_tab", "Inicio", Icons.Default.Home)
    data object History : BottomNavItem("history_tab", "Historial", Icons.Default.List)
    data object Dispatch : BottomNavItem("dispatch_tab", "Despacho", Icons.Default.Send)
    data object Profile : BottomNavItem("profile_tab", "Perfil", Icons.Default.Person)
}

@Composable
fun MainScreen(
    uid: String,
    currentUser: UserProfile,
    profileViewModel: ProfileViewModel,
    emergencyViewModel: EmergencyViewModel,
    authViewModel: AuthViewModel,
    parentNavController: NavHostController // To navigate to Dashboard or external screens
) {
    val bottomNavController = rememberNavController()
    
    // Roles with dispatch access
    val dispatchRoles = listOf("admin", "jefe", "oficial", "subjefe")
    val canDispatch = currentUser.role.lowercase() in dispatchRoles || 
                     currentUser.rango.lowercase() in dispatchRoles

    val tabs = mutableListOf(
        BottomNavItem.Home,
        BottomNavItem.History
    )
    if (canDispatch) {
        tabs.add(BottomNavItem.Dispatch)
    }
    tabs.add(BottomNavItem.Profile)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                tabs.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = bottomNavController, startDestination = BottomNavItem.Home.route) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        uid = uid,
                        viewModel = profileViewModel,
                        emergencyViewModel = emergencyViewModel,
                        onNavigateToDashboard = { parentNavController.navigate("dashboard") },
                        onNavigateToHistory = { bottomNavController.navigate(BottomNavItem.History.route) } // Navigate within bottom nav!
                    )
                }
                composable(BottomNavItem.History.route) {
                    HistoryScreen(
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }
                composable(BottomNavItem.Dispatch.route) {
                    DispatchScreen(
                        userProfile = currentUser,
                        emergencyViewModel = emergencyViewModel,
                        onNavigateToDashboard = { parentNavController.navigate("dashboard") },
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
                            // Auth ViewModel already handles clear, we just want to jump to login on parent nav
                            // Handled by AuthState listener in Root NavGraph typically.
                        }
                    )
                }
            }
        }
    }
}
