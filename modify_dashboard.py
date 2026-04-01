import re

file_path = "/home/jordan-tsagmo/Desktop/PROJET/2025-2026/FREELANCE/LLR CONSULTING/CODE/app/src/main/java/com/llr/rideapp/presentation/client/ClientDashboardScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# 1. Add imports
imports = """import com.llr.rideapp.domain.repository.RideRepository
import com.llr.rideapp.domain.model.Ride
import android.widget.Toast
"""
content = re.sub(r'(import com\.llr\.rideapp\.domain\.repository\.AuthRepository)', r'\1\n' + imports, content)

# 2. Add RideOrderUiState
state_classes = """sealed class VehiculeUiState {
    object Loading : VehiculeUiState()
    object Success : VehiculeUiState()
    data class Error(val message: String) : VehiculeUiState()
}

sealed class RideOrderUiState {
    object Idle : RideOrderUiState()
    object Loading : RideOrderUiState()
    data class Success(val ride: Ride) : RideOrderUiState()
    data class Error(val message: String) : RideOrderUiState()
}"""
content = re.sub(r'sealed class VehiculeUiState \{.*?\n\}', state_classes, content, flags=re.DOTALL)

# 3. Add RideRepository to ViewModel
content = re.sub(
    r'private val vehiculeApiService: VehiculeApiService',
    r'private val vehiculeApiService: VehiculeApiService,\n    private val rideRepository: RideRepository',
    content
)

# 4. Add logic to ViewModel
view_model_logic = """
    private val _rideOrderUiState = MutableStateFlow<RideOrderUiState>(RideOrderUiState.Idle)
    val rideOrderUiState: StateFlow<RideOrderUiState> = _rideOrderUiState

    fun orderRide(vehiculeId: String, pickupLocation: String, dropoffLocation: String) {
        viewModelScope.launch {
            _rideOrderUiState.value = RideOrderUiState.Loading
            try {
                val userId = tokenManager.getUserId() ?: ""
                val result = rideRepository.createRide(userId, vehiculeId, pickupLocation, dropoffLocation)
                result.onSuccess { ride ->
                    _rideOrderUiState.value = RideOrderUiState.Success(ride)
                }.onFailure { e ->
                    _rideOrderUiState.value = RideOrderUiState.Error(e.message ?: "Erreur réseau")
                }
            } catch (e: Exception) {
                _rideOrderUiState.value = RideOrderUiState.Error(e.message ?: "Erreur réseau")
            }
        }
    }

    fun resetOrderState() {
        _rideOrderUiState.value = RideOrderUiState.Idle
    }

    init {"""
content = re.sub(r'\n    init \{', view_model_logic, content)

# 5. Add UI state variables
ui_states = """    val vehicules by viewModel.filteredVehicules.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()

    val rideOrderUiState by viewModel.rideOrderUiState.collectAsState()
    var orderPopupVehicule by remember { mutableStateOf<VehiculeDto?>(null) }"""
content = re.sub(r'    val vehicules by viewModel\.filteredVehicules\.collectAsState\(\)\n    val selectedClass by viewModel\.selectedClass\.collectAsState\(\)', ui_states, content)

# 6. Change onCommandVehicule action
content = re.sub(
    r'onCommandVehicule = \{ onNavigateToNewRide\(\) \}',
    r'onCommandVehicule = { orderPopupVehicule = it }',
    content
)

# 7. Add the dialog at the end of the Screen composable
dialog_code = """
            }
        }

        if (orderPopupVehicule != null) {
            var pickup by remember { mutableStateOf("") }
            var dropoff by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { 
                    if (rideOrderUiState !is RideOrderUiState.Loading) {
                        orderPopupVehicule = null
                        viewModel.resetOrderState()
                    }
                },
                title = {
                    Text("Commander le véhicule", color = AccentGold, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text("Véhicule: ${orderPopupVehicule?.brand} ${orderPopupVehicule?.model}")
                        if (orderPopupVehicule?.price != null) {
                            Text("Prix du véhicule: ${orderPopupVehicule?.price}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pickup,
                            onValueChange = { pickup = it },
                            label = { Text("Point de départ") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dropoff,
                            onValueChange = { dropoff = it },
                            label = { Text("Destination") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        when (val state = rideOrderUiState) {
                            is RideOrderUiState.Loading -> {
                                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                            }
                            is RideOrderUiState.Error -> {
                                Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                            }
                            is RideOrderUiState.Success -> {
                                val ridePrice = state.ride.price ?: orderPopupVehicule?.price ?: "Non défini"
                                Text("Commande réussie ! Prix de la course : $ridePrice", color = Color(0xFF4CAF50), modifier = Modifier.padding(top = 16.dp))
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2000)
                                    orderPopupVehicule = null
                                    viewModel.resetOrderState()
                                    // on peut rediriger vers l'historique si on veut, ou rester ici
                                }
                            }
                            else -> {}
                        }
                    }
                },
                confirmButton = {
                    if (rideOrderUiState !is RideOrderUiState.Success) {
                        Button(
                            onClick = { viewModel.orderRide(orderPopupVehicule!!.id, pickup, dropoff) },
                            enabled = pickup.isNotBlank() && dropoff.isNotBlank() && rideOrderUiState !is RideOrderUiState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                        ) {
                            Text("Confirmer", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    if (rideOrderUiState !is RideOrderUiState.Success && rideOrderUiState !is RideOrderUiState.Loading) {
                        TextButton(onClick = { 
                            orderPopupVehicule = null
                            viewModel.resetOrderState()
                        }) {
                            Text("Annuler", color = TextSecondary)
                        }
                    }
                }
            )
        }
    }
}"""
content = re.sub(r'            }\n        }\n    }\n}', dialog_code, content)

with open(file_path, "w") as f:
    f.write(content)

print("Modification complete.")
