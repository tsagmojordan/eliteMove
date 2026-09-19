package com.llr.rideapp.presentation.admin

import com.llr.rideapp.utils.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llr.rideapp.domain.model.VehiculeClass
import com.llr.rideapp.domain.model.VehiclePhoto
import com.llr.rideapp.domain.repository.VehicleRepository
import com.llr.rideapp.presentation.common.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AdminAddVehicleViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    var brand by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableStateOf("")
    var licensePlate by mutableStateOf("")
    // Classes alignées sur l'enum backend (contrat C6) : ECO, CONFORT, PREMIUM, VAN
    var vehiculeClass by mutableStateOf(VehiculeClass.ECO)
    var price by mutableStateOf("")

    var photoUris by mutableStateOf<List<Uri>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _addSuccessEvent = mutableStateOf(false)
    val addSuccessEvent: State<Boolean> = _addSuccessEvent

    fun onPhotosPicked(uris: List<Uri>) {
        log.debug("[AdminAddVehicleScreen] --onPhotosPicked (${uris.size})")
        photoUris = uris.take(MAX_PHOTOS)
    }

    fun clearPhotos() {
        photoUris = emptyList()
    }

    fun createVehicle() {
        log.debug("[AdminAddVehicleScreen] --createVehicle")
        val y = year.toIntOrNull()
        val p = price.toIntOrNull()
        if (brand.isBlank() || model.isBlank() || y == null || licensePlate.isBlank() || p == null) {
            error = "Veuillez remplir tous les champs correctement"
            return
        }

        isLoading = true
        error = null
        viewModelScope.launch {
            // Lecture des URI → octets (hors thread principal)
            val photos = withContext(Dispatchers.IO) {
                photoUris.mapNotNull { uri -> readPhoto(uri) }
            }
            val result = vehicleRepository.createVehicle(
                brand = brand,
                model = model,
                year = y,
                licensePlate = licensePlate,
                vehiculeClass = vehiculeClass.name,
                price = p,
                photos = photos
            )
            isLoading = false
            result.fold(
                onSuccess = { _addSuccessEvent.value = true },
                onFailure = { error = it.message ?: "Erreur d'ajout du véhicule" }
            )
        }
    }

    private fun readPhoto(uri: Uri): VehiclePhoto? = try {
        val mimeType = appContext.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        val name = uri.lastPathSegment ?: "photo_${System.currentTimeMillis()}.jpg"
        if (bytes != null) VehiclePhoto(name, bytes, mimeType) else null
    } catch (e: Exception) {
        log.debug("[AdminAddVehicleScreen] --readPhoto échec: ${e.message}")
        null
    }

    companion object {
        const val MAX_PHOTOS = 3
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

    // Sélecteur de photos (max 3) — part "photos" du multipart (contrat C6)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(AdminAddVehicleViewModel.MAX_PHOTOS)
    ) { uris -> viewModel.onPhotosPicked(uris) }

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
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    text = "Ajout à la flotte",
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
                            FilterChip(
                                selected = viewModel.vehiculeClass == cls,
                                onClick = { viewModel.vehiculeClass = cls },
                                label = { Text(cls.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Sélecteur de photos
                Column(modifier = Modifier.fillMaxWidth()) {
                    RideAppButton(
                        text = if (viewModel.photoUris.isEmpty()) "Ajouter des photos (max 3)"
                               else "${viewModel.photoUris.size} photo(s) sélectionnée(s)",
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.AddAPhoto,
                        enabled = viewModel.photoUris.size < AdminAddVehicleViewModel.MAX_PHOTOS
                    )
                    if (viewModel.photoUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.TextButton(onClick = { viewModel.clearPhotos() }) {
                            Text("Retirer les photos", color = TextSecondary)
                        }
                    }
                }

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