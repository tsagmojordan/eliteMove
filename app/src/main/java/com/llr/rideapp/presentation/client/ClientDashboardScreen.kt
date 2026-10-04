package com.llr.rideapp.presentation.client

import com.llr.rideapp.utils.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.data.remote.api.VehiculeApiService
import com.llr.rideapp.data.remote.websocket.CallEvent
import com.llr.rideapp.data.remote.websocket.CallRealtimeManager
import com.llr.rideapp.domain.model.Ride
import com.llr.rideapp.domain.model.VehiculeClass
import com.llr.rideapp.domain.model.VehiculeDto
import com.llr.rideapp.domain.model.VehiculeStatus
import com.llr.rideapp.domain.repository.AuthRepository
import com.llr.rideapp.domain.repository.CallRepository
import com.llr.rideapp.domain.repository.NotificationRepository
import com.llr.rideapp.domain.repository.RideRepository
import com.llr.rideapp.utils.ApiConfig
import com.llr.rideapp.presentation.common.*
import com.llr.rideapp.presentation.common.map.AppMapSection
import com.llr.rideapp.presentation.common.map.MapPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI States ───────────────────────────────────────────────────────────────

sealed class VehiculeUiState {
    object Loading : VehiculeUiState()
    object Success : VehiculeUiState()
    data class Error(val message: String) : VehiculeUiState()
}

sealed class RideOrderUiState {
    object Idle : RideOrderUiState()
    object Loading : RideOrderUiState()
    data class Success(val ride: Ride) : RideOrderUiState()
    data class Error(val message: String) : RideOrderUiState()
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class ClientDashboardViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val vehiculeApiService: VehiculeApiService,
    private val rideRepository: RideRepository,
    private val callRepository: CallRepository,
    val callRealtimeManager: CallRealtimeManager
) : ViewModel() {

    var unreadCount by mutableStateOf(0)
        private set

    /** Résolution de l'admin de support (contrat C10) avant d'initier l'appel. */
    var isResolvingSupport by mutableStateOf(false)
        private set
    var supportError by mutableStateOf<String?>(null)
        private set

    private val _vehicules = MutableStateFlow<List<VehiculeDto>>(emptyList())
    val vehicules: StateFlow<List<VehiculeDto>> = _vehicules

    private val _selectedClass = MutableStateFlow<VehiculeClass?>(null)
    val selectedClass: StateFlow<VehiculeClass?> = _selectedClass

    val filteredVehicules = combine(_vehicules, _selectedClass) { list, cls ->
        list.filter {
            it.status == VehiculeStatus.AVAILABLE && (cls == null || it.vehiculeClass == cls)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<VehiculeUiState>(VehiculeUiState.Loading)
    val uiState: StateFlow<VehiculeUiState> = _uiState

    private val _rideOrderUiState = MutableStateFlow<RideOrderUiState>(RideOrderUiState.Idle)
    val rideOrderUiState: StateFlow<RideOrderUiState> = _rideOrderUiState

    init {
        log.debug("[ClientDashboardScreen] --init")
        fetchUnreadCount()
        loadVehicules()
        observeRealtime()
    }

    private fun fetchUnreadCount() {
        log.debug("[ClientDashboardScreen] --fetchUnreadCount")
        viewModelScope.launch {
            val result = notificationRepository.getUnreadCount()
            result.onSuccess { unreadCount = it }
        }
    }

    /** Connexion WebSocket + rafraîchissement du badge à chaque notification temps réel. */
    private fun observeRealtime() {
        viewModelScope.launch { callRealtimeManager.start() }
        viewModelScope.launch {
            callRealtimeManager.notificationEvents.collect { fetchUnreadCount() }
        }
    }

    fun loadVehicules() {
        log.debug("[ClientDashboardScreen] --loadVehicules")
        viewModelScope.launch {
            _uiState.value = VehiculeUiState.Loading
            try {
                // Endpoints /with-thumbnails : liste + miniature Base64 en une requête
                _vehicules.value = vehiculeApiService.getAllWithThumbnails()
                _uiState.value = VehiculeUiState.Success
            } catch (e: Exception) {
                _uiState.value = VehiculeUiState.Error(e.message ?: "Erreur réseau")
            }
        }
    }

    /** Token d'accès pour charger les photos via Coil (header Authorization). */
    fun accessToken(): String? = tokenManager.getAccessToken()

    fun selectClass(cls: VehiculeClass?) { _selectedClass.value = cls }

    fun logout(onLogoutComplete: () -> Unit) {
        log.debug("[ClientDashboardScreen] --logout")
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }

    fun orderRide(vehiculeId: String, pickupLocation: String, dropoffLocation: String) {
        log.debug("[ClientDashboardScreen] --orderRide")
        viewModelScope.launch {
            _rideOrderUiState.value = RideOrderUiState.Loading
            val userId = tokenManager.getUserId() ?: run {
                _rideOrderUiState.value = RideOrderUiState.Error("Utilisateur non connecté")
                return@launch
            }
            val result = rideRepository.createRide(userId, vehiculeId, pickupLocation, dropoffLocation)
            result.fold(
                onSuccess = { ride -> _rideOrderUiState.value = RideOrderUiState.Success(ride) },
                onFailure = { e -> _rideOrderUiState.value = RideOrderUiState.Error(e.message ?: "Erreur réseau") }
            )
        }
    }

    fun resetOrderState() {
        log.debug("[ClientDashboardScreen] --resetOrderState")
        _rideOrderUiState.value = RideOrderUiState.Idle
    }

    /**
     * Bouton « Support » : demande au backend l'ID d'un admin disponible
     * (contrat C10 — random côté serveur, admins en appel exclus), puis
     * navigue vers l'écran d'appel sortant avec cet ID réel.
     */
    fun startSupportCall(onAdminResolved: (String) -> Unit) {
        log.debug("[ClientDashboardScreen] --startSupportCall")
        if (isResolvingSupport) return
        isResolvingSupport = true
        supportError = null
        viewModelScope.launch {
            callRepository.getSupportAdminId().fold(
                onSuccess = { adminId ->
                    isResolvingSupport = false
                    onAdminResolved(adminId)
                },
                onFailure = { e ->
                    isResolvingSupport = false
                    supportError = e.message ?: "Support indisponible"
                }
            )
        }
    }

    fun clearSupportError() {
        log.debug("[ClientDashboardScreen] --clearSupportError")
        supportError = null
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDashboardScreen(
    viewModel: ClientDashboardViewModel = hiltViewModel(),
    onNavigateToNewRide: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCall: (String, String, Boolean, String) -> Unit,
    onLogout: () -> Unit
) {
    val unreadCount = viewModel.unreadCount
    val vehicules by viewModel.filteredVehicules.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val rideOrderUiState by viewModel.rideOrderUiState.collectAsState()

    var userLocation by remember { mutableStateOf<MapPoint?>(null) }
    var selectedVehicule by remember { mutableStateOf<VehiculeDto?>(null) }
    var orderPopupVehicule by remember { mutableStateOf<VehiculeDto?>(null) }

    // Appels entrants poussés par le backend sur /user/queue/calls (contrat C7)
    LaunchedEffect(Unit) {
        viewModel.callRealtimeManager.callEvents.collect { event ->
            if (event is CallEvent.IncomingCall) {
                onNavigateToCall(event.callId, event.callType, true, event.callerId)
            }
        }
    }

    // Détail du véhicule sélectionné (photos pleine résolution via les endpoints /photoN)
    selectedVehicule?.let { vehicule ->
        VehiculeDetailDialog(
            vehicule = vehicule,
            accessToken = viewModel.accessToken(),
            onDismiss = { selectedVehicule = null },
            onCommand = {
                selectedVehicule = null
                viewModel.resetOrderState()
                orderPopupVehicule = vehicule
            }
        )
    }

    GradientBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                RideAppTopBar(
                    showLogo = true,
                    unreadCount = unreadCount,
                    onNotificationClick = onNavigateToNotifications,
                    onLogout = { viewModel.logout(onLogoutComplete = onLogout) }
                )
            },
            bottomBar = {
                DashboardBottomBar(
                    unreadCount = unreadCount,
                    isSupportLoading = viewModel.isResolvingSupport,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToCall = {
                        // Contrat C10 : l'ID de l'admin est résolu par le backend
                        // juste avant l'appel — plus d'UUID codé en dur.
                        viewModel.startSupportCall { adminId ->
                            onNavigateToCall("out", "AUDIO", false, adminId)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AppMapSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f),
                    vehicules = vehicules,
                    userLocation = userLocation,
                    onUserLocationUpdated = { userLocation = it },
                    selectedVehicule = selectedVehicule
                )
                VehiculeListSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    vehicules = vehicules,
                    selectedClass = selectedClass,
                    onClassSelected = { viewModel.selectClass(it) },
                    onVehiculeTap = { selectedVehicule = it },
                    onCommandVehicule = {
                        viewModel.resetOrderState()
                        orderPopupVehicule = it
                    }
                )
            }
        }
    }

    // ─── Popup de commande ───────────────────────────────────────────────────
    if (orderPopupVehicule != null) {
        var pickup by remember { mutableStateOf("") }
        var dropoff by remember { mutableStateOf("") }
        val vehicule = orderPopupVehicule!!

        AlertDialog(
            onDismissRequest = {
                if (rideOrderUiState !is RideOrderUiState.Loading) {
                    orderPopupVehicule = null
                    viewModel.resetOrderState()
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column {
                    Text(
                        text = "Commander un véhicule",
                        color = AccentGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${vehicule.brand} ${vehicule.model} • ${vehicule.year}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (vehicule.price != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Prix : ${vehicule.price} FCFA",
                            color = AccentGold,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = pickup,
                        onValueChange = { pickup = it },
                        label = { Text("Point de départ") },
                        leadingIcon = {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentGold)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            focusedLabelColor = AccentGold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dropoff,
                        onValueChange = { dropoff = it },
                        label = { Text("Destination") },
                        leadingIcon = {
                            Icon(Icons.Filled.Flag, contentDescription = null, tint = AccentGold)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            focusedLabelColor = AccentGold
                        )
                    )

                    when (val state = rideOrderUiState) {
                        is RideOrderUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentGold)
                            }
                        }
                        is RideOrderUiState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 12.dp),
                                fontSize = 13.sp
                            )
                        }
                        is RideOrderUiState.Success -> {
                            val priceLabel = state.ride.price?.let { "${it} FCFA" } ?: "N/A"
                            Text(
                                text = "✅ Commande confirmée !\nPrix de la course : $priceLabel\nStatut : ${state.ride.status}",
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(top = 12.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            LaunchedEffect(Unit) {
                                delay(2500)
                                orderPopupVehicule = null
                                viewModel.resetOrderState()
                            }
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (rideOrderUiState !is RideOrderUiState.Success) {
                    Button(
                        onClick = { viewModel.orderRide(vehicule.id, pickup, dropoff) },
                        enabled = pickup.isNotBlank() && dropoff.isNotBlank() && rideOrderUiState !is RideOrderUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = PrimaryDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirmer", fontWeight = FontWeight.Bold)
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

    // ─── Erreur de résolution du support (contrat C10 : 503 / aucun admin) ──
    viewModel.supportError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSupportError() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Support indisponible",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = { Text(message, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSupportError() }) {
                    Text("OK", color = AccentGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────
// Icônes filaires (outline) modernes, fond bleu marine, pilule dorée arrondie
// englobant l'icône de l'onglet actif ("Accueil").

@Composable
fun DashboardBottomBar(
    unreadCount: Int,
    isSupportLoading: Boolean = false,
    onNavigateToHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCall: () -> Unit
) {
    NavigationBar(
        containerColor = PrimaryDark,
        contentColor = AccentGold
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Accueil") },
            label = { Text("Accueil") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryDark,
                selectedTextColor = AccentGold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = AccentGold
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.History, contentDescription = "Trajets") },
            label = { Text("Trajets") },
            selected = false,
            onClick = onNavigateToHistory,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = {
                if (unreadCount > 0) {
                    BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifs")
                    }
                } else {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifs")
                }
            },
            label = { Text("Notifs") },
            selected = false,
            onClick = onNavigateToNotifications,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = {
                if (isSupportLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AccentGold
                    )
                } else {
                    Icon(Icons.Outlined.SupportAgent, contentDescription = "Support")
                }
            },
            label = { Text("Support") },
            selected = false,
            onClick = onNavigateToCall,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
    }
}

// ─── Map Section ─────────────────────────────────────────────────────────────
// Rendu déplacé vers presentation/common/map/ (AppMapSection) avec double
// fournisseur OpenStreetMap/Google Maps — voir MapConfig.USE_OPENSTREETMAP.

// ─── Vehicle List Section ────────────────────────────────────────────────────

@Composable
fun VehiculeListSection(
    modifier: Modifier = Modifier,
    vehicules: List<VehiculeDto>,
    selectedClass: VehiculeClass?,
    onClassSelected: (VehiculeClass?) -> Unit,
    onVehiculeTap: (VehiculeDto) -> Unit,
    onCommandVehicule: (VehiculeDto) -> Unit
) {
    Column(modifier = modifier.background(PrimaryDark)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                RideAppFilterChip(
                    selected = selectedClass == null,
                    icon = Icons.Outlined.Search,
                    label = "Toutes",
                    onClick = { onClassSelected(null) }
                )
            }
            items(VehiculeClass.values()) { cls ->
                RideAppFilterChip(
                    selected = selectedClass == cls,
                    icon = cls.categoryIcon(),
                    label = cls.name,
                    onClick = { onClassSelected(cls) }
                )
            }
        }

        if (vehicules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun véhicule disponible", color = TextSecondary)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(vehicules) { vehicule ->
                    VehiculeCard(
                        vehicule = vehicule,
                        onTap = { onVehiculeTap(vehicule) },
                        onCommandClick = { onCommandVehicule(vehicule) }
                    )
                }
            }
        }
    }
}

// ─── Vehicle Card ────────────────────────────────────────────────────────────

/** Format premium du prix : "15,000 FCFA" (séparateur de milliers). */
private fun formatFcfa(price: Double?): String? = price?.let {
    String.format(java.util.Locale.US, "%,.0f", it) + " FCFA"
}

/** Badge de disponibilité moderne : pastille verte "En ligne". */
@Composable
private fun AvailabilityBadge(available: Boolean) {
    Surface(
        color = if (available) Color(0xFF22C55E) else Color(0xFF6B7280),
        shape = RoundedCornerShape(50),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
            Text(
                text = if (available) "En ligne" else "Indisponible",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun VehiculeCard(
    vehicule: VehiculeDto,
    onTap: () -> Unit,
    onCommandClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(300.dp)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Image / placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                // Miniature Base64 renvoyée par les endpoints /with-thumbnails.
                // Coil 2.x ne charge PAS les data URIs (« data:image/jpeg;base64,... »
                // n'est supporté qu'à partir de Coil 3.1) : on décode le Base64
                // en ByteArray, format que Coil 2.x sait décoder nativement.
                val thumbnailBytes = vehicule.thumbnail?.let { b64 ->
                    runCatching {
                        val payload = b64.substringAfter("base64,", b64)
                        android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
                    }.getOrNull()
                }
                if (thumbnailBytes != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailBytes)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image véhicule",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(48.dp)
                    )
                }
                // Badge de disponibilité basé sur le statut réel du véhicule
                AvailabilityBadge(
                    available = vehicule.status == VehiculeStatus.AVAILABLE
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nom du véhicule — gras, bien visible
            Text(
                text = "${vehicule.brand} ${vehicule.model}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = TextPrimary
            )
            // Détails — police plus petite et grise
            Text(
                text = "Modèle ${vehicule.year} • ${vehicule.licensePlate}",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            formatFcfa(vehicule.price)?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bouton pleine largeur doré, texte contrasté
            Button(
                onClick = onCommandClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = PrimaryDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Commander", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Vehicle Detail Dialog ───────────────────────────────────────────────────

/**
 * Détail d'un véhicule : photos pleine résolution via GET /api/v1/vehicules/{id}/photoN
 * (Coil, avec le header Authorization) + informations + action commander.
 */
@Composable
fun VehiculeDetailDialog(
    vehicule: VehiculeDto,
    accessToken: String?,
    onDismiss: () -> Unit,
    onCommand: () -> Unit
) {
    val context = LocalContext.current

    fun photoRequest(endpoint: String) = ImageRequest.Builder(context)
        .data("${ApiConfig.BASE_URL}api/v1/vehicules/${vehicule.id}/$endpoint")
        .apply {
            accessToken?.let { addHeader("Authorization", "Bearer $it") }
        }
        .crossfade(true)
        .build()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text = "${vehicule.brand} ${vehicule.model} • ${vehicule.year}",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${vehicule.licensePlate} • ${vehicule.vehiculeClass?.name ?: ""}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("photo1", "photo2", "photo3").forEach { endpoint ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = photoRequest(endpoint),
                                contentDescription = "Photo véhicule",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (vehicule.price != null) {
                    Text(
                        text = "${vehicule.price} FCFA",
                        color = AccentGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                vehicule.status?.let {
                    Text(
                        text = "Statut : ${it.name}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCommand,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = PrimaryDark
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Commander", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = TextSecondary)
            }
        }
    )
}
