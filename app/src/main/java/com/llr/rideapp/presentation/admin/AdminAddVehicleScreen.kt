package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.repository.VehicleRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAddVehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    var brand by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var licensePlate by mutableStateOf("")
    var vehiculeClass by mutableStateOf("ECONOMY")

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _addSuccessEvent = mutableStateOf(false)
    val addSuccessEvent: State<Boolean> = _addSuccessEvent

    fun createVehicle() {
        log.debug("[AdminAddVehicleScreen] --createVehicle")
        val y = year.toIntOrNull()
        if (brand.isBlank() || model.isBlank() || y == null || licensePlate.isBlank()) {
            error = "Veuillez remplir tous les champs correctement"
            return
        }

        isLoading = true
        error = null
        viewModelScope.launch {
            val result = vehicleRepository.createVehicle(brand, model, y, licensePlate, vehiculeClass)
            isLoading = false
            result.fold(
                onSuccess = { _addSuccessEvent.value = true },
                onFailure = { error = it.message ?: "Erreur d'ajout du véhicule" }
            )
        }
    }
}

@Composable
fun AdminAddVehicleScreen(
    viewModel: AdminAddVehicleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    if (viewModel.addSuccessEvent.value) {
        LaunchedEffect(Unit) { onBack() }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Nouveau Véhicule",
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
                    text = "Ajout à la flotte",
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
                    value = viewModel.brand,
                    onValueChange = { viewModel.brand = it },
                    label = "Marque (ex: Toyota)",
                    leadingIcon = Icons.Filled.DirectionsCar
                )
                Spacer(modifier = Modifier.height(16.dp))
                RideAppTextField(
                    value = viewModel.model,
                    onValueChange = { viewModel.model = it },
                    label = "Modèle (ex: Corolla)",
                    leadingIcon = Icons.Filled.DirectionsCar
                )
                Spacer(modifier = Modifier.height(16.dp))
                RideAppTextField(
                    value = viewModel.year,
                    onValueChange = { viewModel.year = it.filter { char -> char.isDigit() } },
                    label = "Année (ex: 2022)",
                    leadingIcon = Icons.Filled.Numbers,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                RideAppTextField(
                    value = viewModel.licensePlate,
                    onValueChange = { viewModel.licensePlate = it.uppercase() },
                    label = "Plaque d'immatriculation",
                    leadingIcon = Icons.Filled.Info
                )
                Spacer(modifier = Modifier.height(32.dp))

                RideAppButton(
                    text = "Enregistrer",
                    onClick = { viewModel.createVehicle() },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = viewModel.isLoading
                )
            }
        }
    }
}
