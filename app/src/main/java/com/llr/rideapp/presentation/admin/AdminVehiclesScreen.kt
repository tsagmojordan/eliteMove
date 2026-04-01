package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.llr.rideapp.domain.model.Vehicle
import com.llr.rideapp.domain.repository.VehicleRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminVehiclesViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    var vehicles by mutableStateOf<List<Vehicle>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        log.debug("[AdminVehiclesScreen] --init")
        loadVehicles()
    }

    private fun loadVehicles() {
        log.debug("[AdminVehiclesScreen] --loadVehicles")
        isLoading = true
        error = null
        viewModelScope.launch {
            val result = vehicleRepository.getAvailableVehicles()
            isLoading = false
            result.fold(
                onSuccess = { vehicles = it },
                onFailure = { error = it.message ?: "Erreur chargement des véhicules" }
            )
        }
    }
}

@Composable
fun AdminVehiclesScreen(
    viewModel: AdminVehiclesViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAddVehicle: () -> Unit
) {
    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Flotte de Véhicules",
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
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        RideAppButton(
                            text = "Ajouter un Véhicule",
                            onClick = onNavigateToAddVehicle,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Filled.Add
                        )
                    }

                    if (viewModel.vehicles.isEmpty()) {
                        item {
                            Text(
                                "Aucun véhicule disponible.",
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    items(viewModel.vehicles) { vehicle ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${vehicle.brand} ${vehicle.model} (${vehicle.year})",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                StatusBadge(status = if (vehicle.available) "DISPONIBLE" else "INDISPONIBLE")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Immatriculation: ${vehicle.licensePlate}", color = TextSecondary)
                            Text("Classe: ${vehicle.vehiculeClass}", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
