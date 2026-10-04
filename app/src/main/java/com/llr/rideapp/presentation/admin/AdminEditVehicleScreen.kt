package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.VehiculeClass
import com.llr.rideapp.domain.repository.VehicleRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminEditVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val vehicleId: String = savedStateHandle.get<String>("vehicleId") ?: ""

    var brand by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var licensePlate by mutableStateOf("")
    // Classes alignées sur l'enum backend (contrat C6) : ECO, CONFORT, PREMIUM, VAN
    var vehiculeClass by mutableStateOf(VehiculeClass.ECO)
    var price by mutableStateOf("")

    var isLoading by mutableStateOf(true)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _editSuccessEvent = mutableStateOf(false)
    val editSuccessEvent: State<Boolean> = _editSuccessEvent

    init {
        log.debug("[AdminEditVehicleScreen] --init vehicleId=$vehicleId")
        loadVehicle()
    }

    /** Pré-remplit le formulaire depuis GET /api/v1/vehicules/{id}. */
    fun loadVehicle() {
        if (vehicleId.isBlank()) {
            error = "Identifiant du véhicule manquant"
            isLoading = false
            return
        }
        viewModelScope.launch {
            vehicleRepository.getVehicleById(vehicleId).fold(
                onSuccess = { vehicle ->
                    brand = vehicle.brand
                    model = vehicle.model
                    year = vehicle.year.toString()
                    licensePlate = vehicle.licensePlate
                    vehiculeClass = runCatching { VehiculeClass.valueOf(vehicle.vehiculeClass) }
                        .getOrDefault(VehiculeClass.ECO)
                    price = vehicle.price?.toInt()?.toString() ?: ""
                    isLoading = false
                },
                onFailure = { e ->
                    error = e.message ?: "Erreur chargement du véhicule"
                    isLoading = false
                }
            )
        }
    }

    /** PUT /api/v1/vehicules/{id} — champs uniquement, les photos ne sont pas modifiables ici. */
    fun saveVehicle() {
        log.debug("[AdminEditVehicleScreen] --saveVehicle")
        val y = year.toIntOrNull()
        val p = price.toIntOrNull()
        if (brand.isBlank() || model.isBlank() || y == null || licensePlate.isBlank() || p == null) {
            error = "Veuillez remplir tous les champs correctement"
            return
        }

        isSaving = true
        error = null
        viewModelScope.launch {
            val result = vehicleRepository.updateVehicle(
                id = vehicleId,
                brand = brand,
                model = model,
                year = y,
                licensePlate = licensePlate,
                vehiculeClass = vehiculeClass.name,
                price = p
            )
            isSaving = false
            result.fold(
                onSuccess = { _editSuccessEvent.value = true },
                onFailure = { error = it.message ?: "Erreur de mise à jour du véhicule" }
            )
        }
    }
}

@Composable
fun AdminEditVehicleScreen(
    viewModel: AdminEditVehicleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    if (viewModel.editSuccessEvent.value) {
        LaunchedEffect(Unit) { onBack() }
    }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            RideAppTopBar(
                title = "Modifier le Véhicule",
                onBack = onBack
            )

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGold)
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = "Modification de la flotte",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )

                viewModel.error?.let {
                    ErrorText(it)
                }

                RideAppTextField(
                    value = viewModel.brand,
                    onValueChange = { viewModel.brand = it },
                    label = "Marque (ex: Toyota)",
                    leadingIcon = Icons.Filled.DirectionsCar
                )
                RideAppTextField(
                    value = viewModel.model,
                    onValueChange = { viewModel.model = it },
                    label = "Modèle (ex: Corolla)",
                    leadingIcon = Icons.Filled.DirectionsCar
                )
                RideAppTextField(
                    value = viewModel.year,
                    onValueChange = { viewModel.year = it.filter { char -> char.isDigit() } },
                    label = "Année (ex: 2022)",
                    leadingIcon = Icons.Filled.Numbers,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                RideAppTextField(
                    value = viewModel.licensePlate,
                    onValueChange = { viewModel.licensePlate = it.uppercase() },
                    label = "Plaque d'immatriculation",
                    leadingIcon = Icons.Filled.Info
                )
                RideAppTextField(
                    value = viewModel.price,
                    onValueChange = { viewModel.price = it.filter { char -> char.isDigit() } },
                    label = "Prix (FCFA, ex: 500)",
                    leadingIcon = Icons.Filled.AttachMoney,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Sélecteur de classe — valeurs de l'enum backend VehiculeClass
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Classe", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VehiculeClass.values()) { cls ->
                            RideAppFilterChip(
                                selected = viewModel.vehiculeClass == cls,
                                icon = cls.categoryIcon(),
                                label = cls.name,
                                onClick = { viewModel.vehiculeClass = cls }
                            )
                        }
                    }
                }

                // Le PUT ne met à jour que les champs — les photos se gèrent à la création
                Text(
                    "Les photos du véhicule ne sont pas modifiables ici.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                RideAppButton(
                    text = "Enregistrer les modifications",
                    onClick = { viewModel.saveVehicle() },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Save,
                    isLoading = viewModel.isSaving
                )
            }
        }
    }
}