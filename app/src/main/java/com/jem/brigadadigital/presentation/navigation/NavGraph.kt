package com.jem.brigadadigital.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.jem.brigadadigital.presentation.home.HomeScreen
import com.jem.brigadadigital.presentation.profile.ProfileState
import com.jem.brigadadigital.presentation.profile.ProfileViewModel
import com.jem.brigadadigital.presentation.profile.ProfileViewModelFactory
import com.jem.brigadadigital.presentation.profile.SetupProfileScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object RootHome : Screen("root_home") // Handles routing to Home or SetupProfile
}

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())

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
            // Obtain current UID from AuthViewModel state
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            
            // Check if UID is available
            if (authState is AuthState.Success) {
                val uid = (authState as AuthState.Success).userUid
                val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()

                LaunchedEffect(uid) {
                    if (profileState is ProfileState.Idle) {
                        profileViewModel.checkUserProfile(uid)
                    }
                }

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
                                profileViewModel.checkUserProfile(uid) // Reload profile
                            }
                        )
                    }
                    is ProfileState.Loaded -> {
                        HomeScreen(uid = uid, viewModel = profileViewModel)
                    }
                    is ProfileState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val msg = (profileState as ProfileState.Error).message
                            Text("Ocurrió un error: $msg")
                        }
                    }
                    else -> {}
                }
            } else {
                // If the user loses authentication state, redirect to Login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.RootHome.route) { inclusive = true }
                    }
                }
            }
        }
    }
}
