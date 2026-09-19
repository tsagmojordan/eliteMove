package com.llr.rideapp.domain.model

import com.llr.rideapp.utils.log

// ─── User & Auth ─────────────────────────────────────────────────────────────

data class User(
    val id: String,
    val firstname: String,
    val lastname: String,
    val username: String,
    val email: String,
    val enabled: Boolean,
    val roles: List<String>
)

enum class UserRole {
    CLIENT, ADMIN, SUPER_ADMIN, UNKNOWN;

    companion object {
        fun fromRoleNames(roles: List<String>): UserRole {
            log.debug("[Models] --fromRoleNames")
            return when {
                roles.any { it.contains("SUPER_ADMIN", ignoreCase = true) } -> SUPER_ADMIN
                roles.any { it.contains("ADMIN", ignoreCase = true) } -> ADMIN
                roles.any { it.contains("CLIENT", ignoreCase = true) || it.contains("USER", ignoreCase = true) } -> CLIENT
                else -> UNKNOWN
            }
        }
    }
}

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val roles: List<String>
)

// ─── Ride ─────────────────────────────────────────────────────────────────────

data class Ride(
    val id: String,
    val userId: String,
    val vehiculeId: String?,
    val pickupLocation: String,
    val dropoffLocation: String,
    val status: String,
    val requestedAt: String?,
    val completedAt: String?,
    val price: Double?
)

// ─── Vehicle ──────────────────────────────────────────────────────────────────
// Contrat C6 : le backend expose un `status` (VehiculeStatus), pas un booléen `available`.

data class Vehicle(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val vehiculeClass: String,
    val status: String,
    val price: Double?
)

/** Photo locale prête à être envoyée en multipart (part "photos"). */
class VehiclePhoto(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String
)

// ─── Role (SuperAdmin) ────────────────────────────────────────────────────────

data class Role(
    val id: String,
    val name: String,
    val description: String?
)

// ─── Call ─────────────────────────────────────────────────────────────────────
data class Call(
    val id: String,
    val callerId: String,
    val calleeId: String,
    val callType: String, // "AUDIO" | "VIDEO"
    val status: String,
    val startedAt: String?,
    val endedAt: String?
)

// ─── Notification ─────────────────────────────────────────────────────────────

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val read: Boolean,
    val createdAt: String
)
