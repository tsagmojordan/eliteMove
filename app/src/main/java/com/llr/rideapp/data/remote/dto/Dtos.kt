package com.llr.rideapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Generic API Response Wrapper ────────────────────────────────────────────
// Correspond exactement à la structure renvoyée par le backend :
// { "success": true, "message": "...", "data": {...}, "status": 200, "timestamp": "..." }

data class ApiResponse<T>(
    @SerializedName("success")   val success: Boolean?,
    @SerializedName("message")   val message: String?,
    @SerializedName("data")      val data: T?,
    @SerializedName("status")    val status: String?,
    @SerializedName("timestamp") val timestamp: String?
)

// Réponse paginée du backend : { content: [...], pageNumber, pageSize, totalElements, totalPages, last }
data class PaginatedResponse<T>(
    @SerializedName("content")       val content: List<T>?,
    @SerializedName("pageNumber")    val pageNumber: Int?,
    @SerializedName("pageSize")      val pageSize: Int?,
    @SerializedName("totalElements") val totalElements: Long?,
    @SerializedName("totalPages")    val totalPages: Int?,
    @SerializedName("last")          val last: Boolean?
)

// ─── Auth DTOs ────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("usernameOrEmail") val usernameOrEmail: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname") val lastname: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    // Contrat C11 : optionnel à l'API, mais requis par le formulaire mobile.
    // Format : « + » optionnel puis 8 à 15 chiffres (validation backend @Pattern).
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

// AuthResponse : champ "data" dans le wrapper ApiResponse de POST /api/v1/auth/login
data class AuthResponse(
    @SerializedName("accessToken")  val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType")    val tokenType: String,
    @SerializedName("expiresIn")    val expiresIn: Long,
    @SerializedName("user")         val user: UserResponse
)

data class UserResponse(
    @SerializedName("id")        val id: String,
    @SerializedName("firstname") val firstname: String,
    @SerializedName("lastname")  val lastname: String,
    @SerializedName("username")  val username: String,
    @SerializedName("email")     val email: String,
    @SerializedName("enabled")   val enabled: Boolean,
    @SerializedName("roles")     val roles: List<RoleResponse>,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class RoleResponse(
    @SerializedName("id")          val id: String?,
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("permissions") val permissions: List<PermissionResponse>?,
    @SerializedName("createdAt")   val createdAt: String?,
    @SerializedName("updatedAt")   val updatedAt: String?
)

data class PermissionResponse(
    @SerializedName("id")          val id: String?,
    @SerializedName("name")        val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("module")      val module: String?,
    @SerializedName("createdAt")   val createdAt: String?,
    @SerializedName("updatedAt")   val updatedAt: String?
)

data class RoleRequest(
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String?
)

// ─── User DTOs ────────────────────────────────────────────────────────────────

data class UserDto(
    @SerializedName("id") val id: String?,
    @SerializedName("firstname") val firstname: String?,
    @SerializedName("lastname") val lastname: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("enabled") val enabled: Boolean?,
    @SerializedName("roles") val roles: List<RoleDto>?
)

data class RoleDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
)

data class AssignRoleRequest(
    @SerializedName("roleIds") val roleIds: List<String>
)

// ─── Ride DTOs ────────────────────────────────────────────────────────────────

data class CreateRideRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("vehiculeId") val vehiculeId: String?,
    @SerializedName("pickupLocation") val pickupLocation: String,
    @SerializedName("dropoffLocation") val dropoffLocation: String
)

data class RideDto(
    @SerializedName("id") val id: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("vehiculeId") val vehiculeId: String?,
    @SerializedName("pickupLocation") val pickupLocation: String?,
    @SerializedName("dropoffLocation") val dropoffLocation: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("requestedAt") val requestedAt: String?,
    @SerializedName("completedAt") val completedAt: String?,
    // Ajouté côté backend (contrat C8) — null tant que la course n'est pas acceptée
    @SerializedName("price") val price: Double?
)

// ─── Vehicle DTOs ──────────────────────────────────────────────────────────────
// Le backend renvoie les listes de véhicules en DTO BRUT (sans wrapper ApiResponse)
// et utilise le champ `status` (enum VehiculeStatus), pas un booléen `available`.

data class VehicleDto(
    @SerializedName("id") val id: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("licensePlate") val licensePlate: String?,
    @SerializedName("vehiculeClass") val vehiculeClass: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("price") val price: Double?
)

data class CreateVehicleRequest(
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("year") val year: Int,
    @SerializedName("licensePlate") val licensePlate: String,
    @SerializedName("vehiculeClass") val vehiculeClass: String,
    @SerializedName("price") val price: Int
)

// ─── Call DTOs ─────────────────────────────────────────────────────────────────
// POST /api/v1/calls renvoie InitiateCallResponse BRUT (contrat C3), pas un wrapper.

data class InitiateCallRequest(
    @SerializedName("calleeId") val calleeId: String,
    @SerializedName("callType") val callType: String // "AUDIO" | "VIDEO"
)

data class InitiateCallResponse(
    @SerializedName("callId") val callId: String?,
    @SerializedName("message") val message: String?
)

// GET /api/v1/calls/history renvoie une liste BRUTE de CallResponse.
data class CallDto(
    @SerializedName("id") val id: String?,
    @SerializedName("callerId") val callerId: String?,
    @SerializedName("calleeId") val calleeId: String?,
    @SerializedName("callType") val callType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("answeredAt") val answeredAt: String?,
    @SerializedName("endedAt") val endedAt: String?,
    @SerializedName("durationSeconds") val durationSeconds: Long?,
    @SerializedName("endReason") val endReason: String?,
    @SerializedName("isActive") val isActive: Boolean?,
    @SerializedName("isTerminated") val isTerminated: Boolean?
)

data class SignalingRequest(
    @SerializedName("callId") val callId: String,
    @SerializedName("signal") val signal: String
)

data class EndCallRequest(
    @SerializedName("reason") val reason: String = "NORMAL"
)

// ─── Notification DTOs ─────────────────────────────────────────────────────────
// Contrat C4 : champ `subject` (pas `title`) ; unread count = ApiResponse<Long> ;
// markAsRead / read-all renvoient ApiResponse<Void>.

data class NotificationDto(
    @SerializedName("id") val id: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("read") val read: Boolean?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("templateCode") val templateCode: String?,
    @SerializedName("createdAt") val createdAt: String?
)