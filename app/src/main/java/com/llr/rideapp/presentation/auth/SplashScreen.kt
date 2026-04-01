package com.llr.rideapp.presentation.auth

import com.llr.rideapp.utils.log

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.R
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.domain.model.UserRole
import com.llr.rideapp.presentation.common.AccentGold
import com.llr.rideapp.presentation.common.GradientBackground
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _navigationEvent = mutableStateOf<SplashDestination?>(null)
    val navigationEvent: State<SplashDestination?> = _navigationEvent

    init {
        log.debug("[SplashScreen] --init")
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        log.debug("[SplashScreen] --checkAuthStatus")
        viewModelScope.launch {
            delay(2000) // Simulation temps de chargement
            if (tokenManager.isLoggedIn()) {
                val role = UserRole.fromRoleNames(tokenManager.getRoles())
                when (role) {
                    UserRole.SUPER_ADMIN -> _navigationEvent.value = SplashDestination.SUPER_ADMIN
                    UserRole.ADMIN -> _navigationEvent.value = SplashDestination.ADMIN
                    else -> _navigationEvent.value = SplashDestination.CLIENT
                }
            } else {
                _navigationEvent.value = SplashDestination.LOGIN
            }
        }
    }
}

enum class SplashDestination {
    LOGIN, CLIENT, ADMIN, SUPER_ADMIN
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToClientDashboard: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToSuperAdminDashboard: () -> Unit
) {
    val navEvent by viewModel.navigationEvent

    LaunchedEffect(navEvent) {
        when (navEvent) {
            SplashDestination.LOGIN -> onNavigateToLogin()
            SplashDestination.CLIENT -> onNavigateToClientDashboard()
            SplashDestination.ADMIN -> onNavigateToAdminDashboard()
            SplashDestination.SUPER_ADMIN -> onNavigateToSuperAdminDashboard()
            null -> {}
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder for logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🚗", fontSize = 80.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "RideApp",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
            Text(
                text = "Premium Transport Services",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}
