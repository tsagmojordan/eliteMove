package com.llr.rideapp.presentation.superadmin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
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
import com.llr.rideapp.data.remote.websocket.CallEvent
import com.llr.rideapp.data.remote.websocket.CallRealtimeManager
import com.llr.rideapp.domain.repository.AuthRepository
import com.llr.rideapp.domain.repository.NotificationRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuperAdminDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    val callRealtimeManager: CallRealtimeManager
) : ViewModel() {

    var unreadCount by mutableStateOf(0)
        private set

    init {
        log.debug("[SuperAdminDashboardScreen] --init")
        fetchUnreadCount()
        viewModelScope.launch { callRealtimeManager.start() }
        viewModelScope.launch {
            callRealtimeManager.notificationEvents.collect { fetchUnreadCount() }
        }
    }

    private fun fetchUnreadCount() {
        log.debug("[SuperAdminDashboardScreen] --fetchUnreadCount")
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount()
            result.onSuccess { unreadCount = it }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        log.debug("[SuperAdminDashboardScreen] --logout")
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }
}

@Composable
fun SuperAdminDashboardScreen(
    viewModel: SuperAdminDashboardViewModel = hiltViewModel(),
    onNavigateToUsers: () -> Unit,
    onNavigateToRoles: () -> Unit,
    onNavigateToRides: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCall: (String, String, Boolean, String) -> Unit,
    onLogout: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Super Admin Dashboard",
                unreadCount = viewModel.unreadCount,
                onNotificationClick = onNavigateToNotifications,
                onLogout = { viewModel.logout(onLogoutComplete = onLogout) }
            )

            // Appels entrants poussés par le backend sur /user/queue/calls (contrat C7)
            LaunchedEffect(Unit) {
                viewModel.callRealtimeManager.callEvents.collect { event ->
                    if (event is CallEvent.IncomingCall) {
                        onNavigateToCall(event.callId, event.callType, true, event.callerId)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToUsers
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Gestion des Utilisateurs", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Activer/Désactiver, rôles, suppression", color = TextSecondary)
                        }
                    }
                }

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToRoles
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Gestion des Rôles", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Créer, lister et supprimer des rôles", color = TextSecondary)
                        }
                    }
                }

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
                            Text("Tous les Trajets", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Gérer l'ensemble des trajets", color = TextSecondary)
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
                            Text("Gérer la flotte disponible", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
