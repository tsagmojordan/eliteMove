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
    val webRtcManager: WebRtcManager
) : ViewModel() {

    var currentCallId by mutableStateOf<String?>(null)
    var isCallActive by mutableStateOf(false)
    var callStatus by mutableStateOf("Initialisation...")

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

    fun answerIncomingCall(callId: String, offerSdp: String) {
        log.debug("[CallScreen] --answerIncomingCall")
        currentCallId = callId
        viewModelScope.launch {
            callStatus = "Connexion..."
            callRepository.acceptCall(callId)
            webRtcManager.handleOffer(callId, offerSdp) { answer ->
                webRtcManager.sendSignalToBackend("""{"type":"answer","sdp":"$answer"}""")
                isCallActive = true
                callStatus = "Appel en cours"
            }
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
            if (isIncoming) {
                viewModel.callStatus = "Appel entrant..."
                // Logique de récupération de l'offer omise pour l'exemple
            } else {
                viewModel.initOutgoingCall(remoteUserId, callType)
            }
        } else {
            viewModel.callStatus = "Permissions refusées"
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(SurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "R", fontSize = 64.sp, color = AccentGold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = if (isIncoming) "Appel entrant" else "Appel sortant",
                color = AccentGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = remoteUserId, color = TextPrimary, fontSize = 18.sp)
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
                    IconButton(
                        onClick = { viewModel.answerIncomingCall(callId, "") },
                        modifier = Modifier
                            .size(72.dp)
                            .background(SuccessGreen, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Filled.Call, contentDescription = "Répondre", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                IconButton(
                    onClick = { viewModel.endCall(onEndComplete = onCallEnded) },
                    modifier = Modifier
                        .size(72.dp)
                        .background(ErrorRed, CircleShape)
                ) {
                    Icon(imageVector = Icons.Filled.CallEnd, contentDescription = "Raccrocher", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}
