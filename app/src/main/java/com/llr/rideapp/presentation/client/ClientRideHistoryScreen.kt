package com.llr.rideapp.presentation.client

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.domain.model.Ride
import com.llr.rideapp.domain.repository.RideRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientRideHistoryViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    var rides by mutableStateOf<List<Ride>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        log.debug("[ClientRideHistoryScreen] --init")
        loadRides()
    }

    private fun loadRides() {
        log.debug("[ClientRideHistoryScreen] --loadRides")
        val userId = tokenManager.getUserId() ?: return

        isLoading = true
        error = null
        viewModelScope.launch {
            val result = rideRepository.getRidesByUser(userId)
            isLoading = false
            result.fold(
                onSuccess = { rides = it },
                onFailure = { error = it.message ?: "Erreur chargement de l'historique" }
            )
        }
    }
}

@Composable
fun ClientRideHistoryScreen(
    viewModel: ClientRideHistoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Mes Trajets",
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
                    Text("Aucun trajet trouvé.", color = TextSecondary, fontSize = 18.sp)
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
                                    text = "Date: ${ride.requestedAt ?: "".substringBefore("T")}",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                StatusBadge(status = ride.status)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("De : ${ride.pickupLocation}", color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Vers : ${ride.dropoffLocation}", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
