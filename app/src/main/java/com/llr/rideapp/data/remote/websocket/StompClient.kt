package com.llr.rideapp.data.remote.websocket

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Message STOMP reçu du serveur (frame MESSAGE). */
data class StompMessage(
    val command: String,
    val headers: Map<String, String>,
    val body: String?
)

/**
 * Client STOMP 1.2 minimaliste construit sur le WebSocket d'OkHttp.
 * Se connecte au endpoint SockJS du backend (/ws-notifications/websocket),
 * passe le JWT dans le header Authorization au CONNECT, et réemet les frames
 * MESSAGE reçues via le SharedFlow [incoming].
 *
 * Reconnexion automatique avec backoff exponentiel tant que [connect] n'a pas
 * été suivi d'un [disconnect].
 */
@Singleton
class StompClient @Inject constructor() {

    companion object {
        private const val TAG = "StompClient"
        private const val NULL_CHAR = '\u0000'
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // connexion longue durée
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor()

    private val _incoming = MutableSharedFlow<StompMessage>(extraBufferCapacity = 128)
    val incoming: SharedFlow<StompMessage> = _incoming

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var url: String? = null
    @Volatile private var token: String? = null
    @Volatile private var wantConnected = false
    @Volatile private var reconnectAttempts = 0
    @Volatile private var frameBuffer = ""

    private val subscriptionId = AtomicInteger(0)
    private val pendingSubscriptions = LinkedHashSet<String>()
    private var connected = false

    fun connect(url: String, token: String?) {
        this.url = url
        this.token = token
        wantConnected = true
        reconnectAttempts = 0
        openSocket()
    }

    fun subscribe(destination: String) {
        synchronized(pendingSubscriptions) { pendingSubscriptions.add(destination) }
        webSocket?.let { ws -> if (connected) sendSubscribe(ws, destination) }
    }

    fun disconnect() {
        wantConnected = false
        connected = false
        synchronized(pendingSubscriptions) { pendingSubscriptions.clear() }
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        frameBuffer = ""
    }

    // ─── Internes ───────────────────────────────────────────────────────────────

    private fun openSocket() {
        val targetUrl = url ?: return
        val request = Request.Builder().url(targetUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket ouvert, envoi du CONNECT STOMP")
                val connectFrame = buildString {
                    append("CONNECT\n")
                    append("accept-version:1.2\n")
                    append("heart-beat:10000,10000\n")
                    token?.let {
                        append("Authorization:Bearer ").append(it).append('\n')
                    }
                    append('\n')
                }
                ws.send(connectFrame + NULL_CHAR)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleText(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket échec: ${t.message}")
                connected = false
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket fermé ($code)")
                connected = false
                scheduleReconnect()
            }
        })
    }

    /** Une frame CONNECTED du serveur confirme la session ; on (re)abonne alors. */
    private fun handleText(text: String) {
        frameBuffer += text
        // Les frames sont terminées par le caractère NULL STOMP.
        while (true) {
            val idx = frameBuffer.indexOf(NULL_CHAR)
            if (idx < 0) break
            val frame = frameBuffer.substring(0, idx)
            frameBuffer = frameBuffer.substring(idx + 1)
            processFrame(frame)
        }
    }

    private fun processFrame(frame: String) {
        // Heart-beat : ligne vide ou "\n" uniquement → ignorer
        if (frame.isBlank()) return

        val lines = frame.split("\n")
        val command = lines.firstOrNull()?.trim() ?: return
        val headers = mutableMapOf<String, String>()
        var i = 1
        while (i < lines.size && lines[i].isNotEmpty()) {
            val sep = lines[i].indexOf(':')
            if (sep > 0) {
                headers[unescape(lines[i].substring(0, sep))] = unescape(lines[i].substring(sep + 1))
            }
            i++
        }
        val body = if (i < lines.size) lines.subList(i + 1, lines.size).joinToString("\n") else ""

        when (command) {
            "CONNECTED" -> {
                connected = true
                reconnectAttempts = 0
                Log.d(TAG, "Session STOMP établie")
                val ws = webSocket ?: return
                synchronized(pendingSubscriptions) {
                    pendingSubscriptions.forEach { sendSubscribe(ws, it) }
                }
            }
            "MESSAGE" -> _incoming.tryEmit(StompMessage(command, headers, body))
            "ERROR" -> Log.e(TAG, "Erreur STOMP reçue: $body")
            // RECEIPT, etc. → ignorés
        }
    }

    private fun sendSubscribe(ws: WebSocket, destination: String) {
        val id = "sub-${subscriptionId.incrementAndGet()}"
        ws.send("SUBSCRIBE\nid:$id\ndestination:$destination\n\n$NULL_CHAR")
        Log.d(TAG, "Abonné à $destination")
    }

    private fun scheduleReconnect() {
        if (!wantConnected) return
        reconnectAttempts++
        val delay = minOf(MAX_RECONNECT_DELAY_MS, 1000L shl minOf(reconnectAttempts, 5))
        Log.d(TAG, "Reconnexion STOMP dans ${delay}ms (tentative $reconnectAttempts)")
        reconnectExecutor.schedule({
            if (wantConnected) openSocket()
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun unescape(value: String): String =
        value.replace("\\c", ":").replace("\\n", "\n").replace("\\\\", "\\")
}