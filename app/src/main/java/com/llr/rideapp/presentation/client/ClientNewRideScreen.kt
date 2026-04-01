package com.llr.rideapp.presentation.client

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import com.llr.rideapp.domain.repository.RideRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientNewRideViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    var pickupLocation by mutableStateOf("")
    var dropoffLocation by mutableStateOf("")

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _rideSuccessEvent = mutableStateOf(false)
    val rideSuccessEvent: State<Boolean> = _rideSuccessEvent

    fun requestRide() {
        log.debug("[ClientNewRideScreen] --requestRide")
        if (pickupLocation.isBlank() || dropoffLocation.isBlank()) {
            error = "Veuillez entrer les deux adresses"
            return
        }

        val userId = tokenManager.getUserId() ?: return

        isLoading = true
        error = null
        viewModelScope.launch {
            val result = rideRepository.createRide(
                userId = userId,
                vehiculeId = null, // Backend handle assignment
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation
            )
            isLoading = false
            result.fold(
                onSuccess = {
                    _rideSuccessEvent.value = true
                },
                onFailure = {
                    error = it.message ?: "Erreur création trajet"
                }
            )
        }
    }
}

@Composable
fun ClientNewRideScreen(
    viewModel: ClientNewRideViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    if (viewModel.rideSuccessEvent.value) {
        LaunchedEffect(Unit) {
            onBack()
        }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Nouveau Trajet",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Où allez-vous ?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
                Spacer(modifier = Modifier.height(32.dp))

                viewModel.error?.let {
                    ErrorText(it)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                RideAppTextField(
                    value = viewModel.pickupLocation,
                    onValueChange = { viewModel.pickupLocation = it },
                    label = "Adresse de départ",
                    leadingIcon = Icons.Filled.LocationOn
                )
                Spacer(modifier = Modifier.height(16.dp))
                RideAppTextField(
                    value = viewModel.dropoffLocation,
                    onValueChange = { viewModel.dropoffLocation = it },
                    label = "Adresse d'arrivée",
                    leadingIcon = Icons.Filled.LocationOn
                )
                Spacer(modifier = Modifier.height(32.dp))

                RideAppButton(
                    text = "Commander",
                    onClick = { viewModel.requestRide() },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = viewModel.isLoading
                )
            }
        }
    }
}
