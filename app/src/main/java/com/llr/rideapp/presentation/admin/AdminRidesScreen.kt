package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.Ride
import com.llr.rideapp.domain.repository.RideRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminRidesViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {

    var rides by mutableStateOf<List<Ride>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        log.debug("[AdminRidesScreen] --init")
        loadAllRides()
    }

    private fun loadAllRides() {
        log.debug("[AdminRidesScreen] --loadAllRides")
        isLoading = true
        error = null
        viewModelScope.launch {
            val result = rideRepository.getAllRides()
            isLoading = false
            result.fold(
                onSuccess = { rides = it },
                onFailure = { error = it.message ?: "Erreur chargement des trajets" }
            )
        }
    }

    fun updateStatus(rideId: String, newStatus: String) {
        log.debug("[AdminRidesScreen] --updateStatus")
        viewModelScope.launch {
            val result = rideRepository.updateRideStatus(rideId, newStatus)
            if (result.isSuccess) {
                loadAllRides() // Rafraîchir
            } else {
                error = result.exceptionOrNull()?.message ?: "Erreur de mise à jour"
            }
        }
    }
}

@Composable
fun AdminRidesScreen(
    viewModel: AdminRidesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Tous les Trajets",
                onBack = onBack
            )

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
            } else if (viewModel.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    ErrorText(message = viewModel.error!!)
                }
            } else if (viewModel.rides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun trajet dans le système.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.rides) { ride ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Client ID: ${ride.userId.take(8)}...",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                StatusBadge(status = ride.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("De: ${ride.pickupLocation}", color = TextSecondary)
                            Text("Vers: ${ride.dropoffLocation}", color = TextSecondary)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Action: Accepter
                            if (ride.status == "PENDING" || ride.status == "REQUESTED") {
                                RideAppButton(
                                    text = "ACCEPTER",
                                    onClick = { viewModel.updateStatus(ride.id, "ACCEPTED") },
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
