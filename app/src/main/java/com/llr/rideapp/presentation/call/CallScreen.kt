package com.llr.rideapp.presentation.call

import com.llr.rideapp.utils.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.isGranted
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.data.remote.websocket.CallEvent
import com.llr.rideapp.data.remote.websocket.CallRealtimeManager
import com.llr.rideapp.domain.model.UserRole
import com.llr.rideapp.domain.repository.CallRepository
import com.llr.rideapp.presentation.common.*
import com.llr.rideapp.webrtc.WebRtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    val webRtcManager: WebRtcManager,
    val callRealtimeManager: CallRealtimeManager,
    tokenManager: TokenManager
) : ViewModel() {

    var currentCallId by mutableStateOf<String?>(null)
    var isCallActive by mutableStateOf(false)
    var callStatus by mutableStateOf("Initialisation...")
    /** Positionnée quand le correspondant refuse/raccroche (fermeture auto de l'écran). */
    var remoteEnded by mutableStateOf(false)
        private set

    /**
     * Nom générique du correspondant : un client appelle/reçoit toujours le
     * support (« Support »), un admin appelle/reçoit toujours un client
     * (« Client »). L'UUID brut n'est jamais montré à l'utilisateur.
     */
    val remoteDisplayName: String =
        if (UserRole.fromRoleNames(tokenManager.getRoles()) == UserRole.CLIENT) "Support"
        else "Client"

    init {
        // Signalisation descendante : answer, candidats ICE et changements de statut
        // poussés par le backend sur /user/queue/calls (contrat C7).
        viewModelScope.launch {
            callRealtimeManager.callEvents.collect { event -> handleRealtimeEvent(event) }
        }
    }

    private fun handleRealtimeEvent(event: CallEvent) {
        when (event) {
            is CallEvent.SignalReceived -> {
                if (event.callId != currentCallId) return
                val payload = event.payload
                when {
                    payload.contains("\"type\":\"answer\"") || payload.contains("\"type\": \"answer\"") ->
                        webRtcManager.handleAnswer(payload)
                    payload.contains("\"type\":\"candidate\"") || payload.contains("\"type\": \"candidate\"") ->
                        webRtcManager.handleIceCandidate(payload)
                    // Les offers arrivent côté callee et sont mémorisées par le manager.
                }
            }
            is CallEvent.StatusChanged -> {
                if (event.callId != currentCallId) return
                when (event.status) {
                    "DECLINED", "MISSED" -> {
                        callStatus = if (event.status == "DECLINED") "Appel refusé" else "Appel manqué"
                        remoteEnded = true
                    }
                    "ENDED" -> {
                        callStatus = "Appel terminé"
                        remoteEnded = true
                    }
                    "ACCEPTED" -> callStatus = "Appel en cours"
                    "IN_PROGRESS" -> {
                        isCallActive = true
                        callStatus = "Appel en cours"
                    }
                }
            }
            is CallEvent.IncomingCall -> Unit // géré par les dashboards
        }
    }

    fun initOutgoingCall(remoteUserId: String, callType: String) {
        log.debug("[CallScreen] --initOutgoingCall")
        viewModelScope.launch {
            callStatus = "Appel en cours..."
            val result = callRepository.initiateCall(remoteUserId, callType)
            result.fold(
                onSuccess = { call ->
                    currentCallId = call.id
                    callStatus = "Génération de l'offre SDP..."
                    webRtcManager.createOffer(call.id, callType == "VIDEO") { offer ->
                        webRtcManager.sendSignalToBackend("""{"type":"offer","sdp":"$offer"}""")
                        callStatus = "Sonnerie..."
                    }
                },
                onFailure = {
                    callStatus = "Erreur: ${it.message}"
                }
            )
        }
    }

    fun answerIncomingCall(callId: String) {
        log.debug("[CallScreen] --answerIncomingCall")
        currentCallId = callId
        viewModelScope.launch {
            callStatus = "Connexion..."
            callRepository.acceptCall(callId)
            // Offer SDP reçue via WebSocket avant la réponse (mémorisée par le manager)
            val offer = callRealtimeManager.takeOffer(callId)
            if (offer.isNullOrBlank()) {
                callStatus = "Erreur: offer SDP introuvable"
                return@launch
            }
            webRtcManager.handleOffer(callId, offer) { answer ->
                webRtcManager.sendSignalToBackend("""{"type":"answer","sdp":"$answer"}""")
                isCallActive = true
                callStatus = "Appel en cours"
            }
        }
    }

    fun declineIncomingCall(callId: String, onDeclineComplete: () -> Unit) {
        log.debug("[CallScreen] --declineIncomingCall")
        viewModelScope.launch {
            callRepository.declineCall(callId)
            webRtcManager.endCall()
            isCallActive = false
            callStatus = "Appel refusé"
            onDeclineComplete()
        }
    }

    fun endCall(onEndComplete: () -> Unit) {
        log.debug("[CallScreen] --endCall")
        viewModelScope.launch {
            currentCallId?.let { callRepository.endCall(it) }
            webRtcManager.endCall()
            isCallActive = false
            callStatus = "Appel terminé"
            delay(1000)
            onEndComplete()
        }
    }

    override fun onCleared() {
        log.debug("[CallScreen] --onCleared")
        super.onCleared()
        webRtcManager.cleanup()
    }
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    callId: String,
    callType: String,
    isIncoming: Boolean,
    remoteUserId: String,
    viewModel: CallViewModel = hiltViewModel(),
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val permissions = remember(callType) {
        val list = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
        if (callType == "VIDEO") {
            list.add(android.Manifest.permission.CAMERA)
        }
        list
    }

    val permissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.webRtcManager.initialize(context)
            viewModel.callRealtimeManager.start()
            if (isIncoming) {
                viewModel.callStatus = "Appel entrant..."
            } else {
                viewModel.initOutgoingCall(remoteUserId, callType)
            }
        } else {
            viewModel.callStatus = "Permissions refusées"
        }
    }

    // Le correspondant a refusé/raccroché : fermeture automatique
    LaunchedEffect(viewModel.remoteEnded) {
        if (viewModel.remoteEnded) {
            viewModel.webRtcManager.endCall()
            delay(1000)
            onCallEnded()
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Avatar Placeholder — initiale du nom générique (S = Support, C = Client)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(SurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.remoteDisplayName.first().toString(),
                    fontSize = 64.sp,
                    color = AccentGold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = if (isIncoming) "Appel entrant" else "Appel sortant",
                color = AccentGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = viewModel.remoteDisplayName, color = TextPrimary, fontSize = 18.sp)
            Text(text = viewModel.callStatus, color = TextSecondary)

            Spacer(modifier = Modifier.weight(1f))

            // Interface boutons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isIncoming && !viewModel.isCallActive) {
                    // Refuser (PATCH /api/v1/calls/{id}/decline)
                    IconButton(
                        onClick = { viewModel.declineIncomingCall(callId, onDeclineComplete = onCallEnded) },
                        modifier = Modifier
                            .size(72.dp)
                            .background(ErrorRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "Refuser",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Répondre — l'offer SDP arrive via WebSocket (contrat C7)
                    IconButton(
                        onClick = { viewModel.answerIncomingCall(callId) },
                        modifier = Modifier
                            .size(72.dp)
                            .background(SuccessGreen, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Répondre",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.endCall(onEndComplete = onCallEnded) },
                        modifier = Modifier
                            .size(72.dp)
                            .background(ErrorRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "Raccrocher",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}