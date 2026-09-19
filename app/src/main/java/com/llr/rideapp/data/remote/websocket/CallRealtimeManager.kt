package com.llr.rideapp.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.llr.rideapp.data.local.TokenManager
import com.llr.rideapp.utils.ApiConfig
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Événements d'appel poussés par le backend sur /user/queue/calls (contrat C7).
 */
sealed class CallEvent {
    data class IncomingCall(
        val callId: String,
        val callerId: String,
        val calleeId: String,
        val callType: String
    ) : CallEvent()

    /** Signalisation WebRTC reçue (offer, answer ou candidat ICE), payload JSON en string. */
    data class SignalReceived(
        val callId: String,
        val payload: String
    ) : CallEvent()

    data class StatusChanged(
        val callId: String,
        val status: String
    ) : CallEvent()
}

/**
 * Gestionnaire temps réel des appels et notifications.
 * Se connecte au WebSocket STOMP du backend dès qu'un écran principal est affiché
 * (start()), s'abonne à /user/queue/calls et /user/queue/notifications, et expose :
 *  - [callEvents]        : INCOMING_CALL / SIGNAL / CALL_STATUS
 *  - [notificationEvents]: signal "il y a du nouveau" côté notifications (refresh du badge)
 *  - [takeOffer]         : récupère (et consomme) la dernière offer SDP reçue pour un appel.
 */
@Singleton
class CallRealtimeManager @Inject constructor(
    private val stompClient: StompClient,
    private val tokenManager: TokenManager
) {

    companion object {
        private const val TAG = "CallRealtimeManager"
        private const val DEST_CALLS = "/user/queue/calls"
        private const val DEST_NOTIFICATIONS = "/user/queue/notifications"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val offers = ConcurrentHashMap<String, String>()

    @Volatile private var started = false

    private val _callEvents = MutableSharedFlow<CallEvent>(extraBufferCapacity = 64)
    val callEvents: SharedFlow<CallEvent> = _callEvents

    private val _notificationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val notificationEvents: SharedFlow<Unit> = _notificationEvents

    /** À appeler depuis les ViewModels des écrans principaux (dashboards). Idempotent. */
    fun start() {
        if (started) return
        val token = tokenManager.getAccessToken()
        if (token == null) {
            Log.d(TAG, "start() ignoré : aucun token")
            return
        }
        started = true
        stompClient.connect(ApiConfig.WS_URL, token)
        stompClient.subscribe(DEST_CALLS)
        stompClient.subscribe(DEST_NOTIFICATIONS)
        scope.launch {
            stompClient.incoming.collect { message -> handleMessage(message) }
        }
    }

    fun stop() {
        started = false
        offers.clear()
        stompClient.disconnect()
    }

    /** Consomme la dernière offer SDP reçue pour cet appel (null si aucune). */
    fun takeOffer(callId: String): String? = offers.remove(callId)

    private fun handleMessage(message: StompMessage) {
        if (message.command != "MESSAGE") return
        val destination = message.headers["destination"] ?: ""

        if (destination.contains("notifications")) {
            _notificationEvents.tryEmit(Unit)
            return
        }

        val body = message.body?.takeIf { it.isNotBlank() } ?: return
        val obj = try {
            gson.fromJson(body, JsonObject::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Message WS illisible: $body")
            return
        }

        when (obj.get("type")?.takeIf { it.isJsonPrimitive }?.asString) {
            "INCOMING_CALL" -> {
                val call = obj.getAsJsonObject("call")
                val callId = call.stringField("id")
                if (callId != null) {
                    _callEvents.tryEmit(
                        CallEvent.IncomingCall(
                            callId = callId,
                            callerId = call.stringField("callerId").orEmpty(),
                            calleeId = call.stringField("calleeId").orEmpty(),
                            callType = call.stringField("callType") ?: "AUDIO"
                        )
                    )
                }
            }
            "SIGNAL" -> {
                val callId = obj.stringField("callId") ?: return
                // Le backend relaie la payload telle que reçue : string (JSON échappé)
                // ou objet — on normalise en string JSON.
                val payload = when (val signal = obj.get("signal")) {
                    is JsonPrimitive -> signal.asString
                    null -> return
                    else -> signal.toString()
                }
                // Mémorise la dernière offer reçue pour cet appel (utile au callee).
                if (payload.contains("\"type\":\"offer\"") || payload.contains("\"type\": \"offer\"")) {
                    offers[callId] = payload
                }
                _callEvents.tryEmit(CallEvent.SignalReceived(callId, payload))
            }
            "CALL_STATUS" -> {
                val callId = obj.stringField("callId") ?: return
                val status = obj.stringField("status") ?: return
                _callEvents.tryEmit(CallEvent.StatusChanged(callId, status))
            }
        }
    }

    private fun JsonObject.stringField(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString
}