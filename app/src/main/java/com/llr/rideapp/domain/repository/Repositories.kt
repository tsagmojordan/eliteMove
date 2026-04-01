package com.llr.rideapp.domain.repository

import com.llr.rideapp.domain.model.*

interface AuthRepository {
    suspend fun login(usernameOrEmail: String, password: String): Result<AuthToken>
    suspend fun register(
        firstname: String, lastname: String, username: String,
        email: String, password: String
    ): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun getUserById(id: String): Result<User>
}

interface RideRepository {
    suspend fun createRide(
        userId: String, vehiculeId: String?,
        pickupLocation: String, dropoffLocation: String
    ): Result<Ride>
    suspend fun getRidesByUser(userId: String): Result<List<Ride>>
    suspend fun getAllRides(): Result<List<Ride>>
    suspend fun updateRideStatus(id: String, status: String): Result<Ride>
}

interface VehicleRepository {
    suspend fun getAvailableVehicles(): Result<List<Vehicle>>
    suspend fun createVehicle(
        brand: String, model: String, year: Int,
        licensePlate: String, vehiculeClass: String
    ): Result<Vehicle>
}

interface CallRepository {
    suspend fun initiateCall(calleeId: String, callType: String): Result<Call>
    suspend fun acceptCall(callId: String): Result<Call>
    suspend fun declineCall(callId: String): Result<Call>
    suspend fun endCall(callId: String): Result<Call>
    suspend fun sendSignaling(callId: String, signal: String): Result<Unit>
    suspend fun getCallHistory(page: Int, size: Int): Result<List<Call>>
}

interface NotificationRepository {
    suspend fun getAllNotifications(): Result<List<AppNotification>>
    suspend fun getUnreadCount(): Result<Int>
    suspend fun markAsRead(notificationId: String): Result<AppNotification>
    suspend fun markAllAsRead(): Result<Unit>
}

interface UserRepository {
    suspend fun getAllUsers(search: String?): Result<List<User>>
    suspend fun updateUserStatus(id: String, enabled: Boolean): Result<User>
    suspend fun assignRoles(id: String, roleIds: List<String>): Result<User>
}
