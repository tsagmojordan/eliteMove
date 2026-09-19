package com.llr.rideapp.utils

/**
 * Configuration réseau centralisée.
 * Adresse du serveur confirmée : http://147.79.118.51:7820
 */
object ApiConfig {
    const val BASE_URL = "http://147.79.118.51:7820/"

    /** Endpoint WebSocket STOMP (backend /ws-notifications, transport websocket natif SockJS). */
    const val WS_URL = "ws://147.79.118.51:7820/ws-notifications/websocket"
}