package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.repository.AuthRepository
import com.llr.rideapp.domain.repository.NotificationRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    var unreadCount by mutableStateOf(0)
        private set

    init {
        log.debug("[AdminDashboardScreen] --init")
        fetchUnreadCount()
    }

    private fun fetchUnreadCount() {
        log.debug("[AdminDashboardScreen] --fetchUnreadCount")
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount()
            result.onSuccess { unreadCount = it }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        log.debug("[AdminDashboardScreen] --logout")
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }
}

@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    onNavigateToRides: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCall: (String, String, Boolean, String) -> Unit,
    onLogout: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Dashboard Admin",
                unreadCount = viewModel.unreadCount,
                onNotificationClick = onNavigateToNotifications,
                onLogout = { viewModel.logout(onLogoutComplete = onLogout) }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToRides
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Gestion des Trajets", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Voir et modifier les statuts", color = TextSecondary)
                        }
                    }
                }

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToVehicles
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Flotte de Véhicules", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Gérer les véhicules et disponibilités", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
