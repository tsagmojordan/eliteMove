package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // Flotte complète (tous statuts) — pas seulement les disponibles
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

    fun loadVehicles() {
        log.debug("[AdminVehiclesScreen] --loadVehicles")
        isLoading = true
        error = null
        viewModelScope.launch {
            val result = vehicleRepository.getAllVehicles()
            isLoading = false
            result.fold(
                onSuccess = { vehicles = it },
                onFailure = { error = it.message ?: "Erreur chargement des véhicules" }
            )
        }
    }

    /** Bascule AVAILABLE ↔ MAINTENANCE (PATCH /api/v1/vehicules/{id}/status). */
    fun toggleStatus(vehicle: Vehicle) {
        log.debug("[AdminVehiclesScreen] --toggleStatus")
        viewModelScope.launch {
            val newStatus = if (vehicle.status == "AVAILABLE") "MAINTENANCE" else "AVAILABLE"
            val result = vehicleRepository.updateVehicleStatus(vehicle.id, newStatus)
            result.fold(
                onSuccess = { loadVehicles() },
                onFailure = { error = it.message ?: "Erreur changement de statut" }
            )
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        log.debug("[AdminVehiclesScreen] --deleteVehicle")
        viewModelScope.launch {
            val result = vehicleRepository.deleteVehicle(vehicle.id)
            result.fold(
                onSuccess = { loadVehicles() },
                onFailure = { error = it.message ?: "Erreur suppression du véhicule" }
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
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

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
                                "Aucun véhicule dans la flotte.",
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    items(viewModel.vehicles) { vehicle ->
                        val isAvailable = vehicle.status == "AVAILABLE"
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
                                StatusBadge(status = if (isAvailable) "DISPONIBLE" else vehicle.status)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Immatriculation: ${vehicle.licensePlate}", color = TextSecondary)
                            Text("Classe: ${vehicle.vehiculeClass}", color = TextSecondary)
                            vehicle.price?.let {
                                Text("Prix: $it FCFA", color = AccentGold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RideAppButton(
                                    text = if (isAvailable) "MAINTENANCE" else "DISPONIBLE",
                                    onClick = { viewModel.toggleStatus(vehicle) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    icon = Icons.Filled.Build
                                )
                                IconButton(
                                    onClick = { vehicleToDelete = vehicle },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Supprimer",
                                        tint = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation de suppression
    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Supprimer le véhicule ?", color = AccentGold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${vehicle.brand} ${vehicle.model} (${vehicle.licensePlate}) sera définitivement supprimé.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVehicle(vehicle)
                        vehicleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}