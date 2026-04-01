package com.llr.rideapp.presentation.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.llr.rideapp.presentation.auth.LoginScreen
import com.llr.rideapp.presentation.auth.RegisterScreen
import com.llr.rideapp.presentation.auth.SplashScreen
import com.llr.rideapp.presentation.admin.AdminDashboardScreen
import com.llr.rideapp.presentation.admin.AdminRidesScreen
import com.llr.rideapp.presentation.admin.AdminVehiclesScreen
import com.llr.rideapp.presentation.admin.AdminAddVehicleScreen
import com.llr.rideapp.presentation.call.CallScreen
import com.llr.rideapp.presentation.client.ClientDashboardScreen
import com.llr.rideapp.presentation.client.ClientNewRideScreen
import com.llr.rideapp.presentation.client.ClientRideHistoryScreen
import com.llr.rideapp.presentation.notification.NotificationsScreen
import com.llr.rideapp.presentation.superadmin.SuperAdminDashboardScreen
import com.llr.rideapp.presentation.superadmin.SuperAdminUsersScreen

@Composable
fun RideAppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        // ─── Auth ─────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToClientDashboard = { navController.navigate(Routes.CLIENT_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToAdminDashboard = { navController.navigate(Routes.ADMIN_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                onNavigateToSuperAdminDashboard = { navController.navigate(Routes.SUPER_ADMIN_DASHBOARD) { popUpTo(Routes.SPLASH) { inclusive = true } } }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = when (role) {
                        "SUPER_ADMIN" -> Routes.SUPER_ADMIN_DASHBOARD
                        "ADMIN" -> Routes.ADMIN_DASHBOARD
                        else -> Routes.CLIENT_DASHBOARD
                    }
                    navController.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ─── Client ───────────────────────────────────────────────────────────
        composable(Routes.CLIENT_DASHBOARD) {
            ClientDashboardScreen(
                onNavigateToNewRide = { navController.navigate(Routes.CLIENT_NEW_RIDE) },
                onNavigateToHistory = { navController.navigate(Routes.CLIENT_RIDE_HISTORY) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToCall = { callId, callType, isIncoming, remoteUserId ->
                    navController.navigate(Routes.callRoute(callId, callType, isIncoming, remoteUserId))
                },
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }

        composable(Routes.CLIENT_NEW_RIDE) {
            ClientNewRideScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CLIENT_RIDE_HISTORY) {
            ClientRideHistoryScreen(onBack = { navController.popBackStack() })
        }

        // ─── Admin ────────────────────────────────────────────────────────────
        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onNavigateToRides = { navController.navigate(Routes.ADMIN_RIDES) },
                onNavigateToVehicles = { navController.navigate(Routes.ADMIN_VEHICLES) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToCall = { callId, callType, isIncoming, remoteUserId ->
                    navController.navigate(Routes.callRoute(callId, callType, isIncoming, remoteUserId))
                },
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }

        composable(Routes.ADMIN_RIDES) {
            AdminRidesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ADMIN_VEHICLES) {
            AdminVehiclesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAddVehicle = { navController.navigate(Routes.ADMIN_ADD_VEHICLE) }
            )
        }

        composable(Routes.ADMIN_ADD_VEHICLE) {
            AdminAddVehicleScreen(onBack = { navController.popBackStack() })
        }

        // ─── Super Admin ──────────────────────────────────────────────────────
        composable(Routes.SUPER_ADMIN_DASHBOARD) {
            SuperAdminDashboardScreen(
                onNavigateToUsers = { navController.navigate(Routes.SUPER_ADMIN_USERS) },
                onNavigateToRides = { navController.navigate(Routes.ADMIN_RIDES) },
                onNavigateToVehicles = { navController.navigate(Routes.ADMIN_VEHICLES) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToCall = { callId, callType, isIncoming, remoteUserId ->
                    navController.navigate(Routes.callRoute(callId, callType, isIncoming, remoteUserId))
                },
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }

        composable(Routes.SUPER_ADMIN_USERS) {
            SuperAdminUsersScreen(onBack = { navController.popBackStack() })
        }

        // ─── Shared ───────────────────────────────────────────────────────────
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            Routes.CALL,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("isIncoming") { type = NavType.BoolType },
                navArgument("remoteUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val callType = backStackEntry.arguments?.getString("callType") ?: "AUDIO"
            val isIncoming = backStackEntry.arguments?.getBoolean("isIncoming") ?: false
            val remoteUserId = backStackEntry.arguments?.getString("remoteUserId") ?: ""

            CallScreen(
                callId = callId,
                callType = callType,
                isIncoming = isIncoming,
                remoteUserId = remoteUserId,
                onCallEnded = { navController.popBackStack() }
            )
        }
    }
}
