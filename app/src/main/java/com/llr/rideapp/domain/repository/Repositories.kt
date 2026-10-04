package com.llr.rideapp.domain.repository

import com.llr.rideapp.domain.model.*

interface AuthRepository {
    suspend fun login(usernameOrEmail: String, password: String): Result<AuthToken>
    suspend fun register(
        firstname: String, lastname: String, username: String,
        email: String, phone: String, password: String
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
    suspend fun getAllVehicles(): Result<List<Vehicle>>
    suspend fun getAvailableVehicles(): Result<List<Vehicle>>
    suspend fun getVehicleById(id: String): Result<Vehicle>
    suspend fun createVehicle(
        brand: String, model: String, year: Int,
        licensePlate: String, vehiculeClass: String, price: Int,
        photos: List<VehiclePhoto>?
    ): Result<Vehicle>
    suspend fun updateVehicle(
        id: String, brand: String, model: String, year: Int,
        licensePlate: String, vehiculeClass: String, price: Int
    ): Result<Vehicle>
    suspend fun updateVehicleStatus(id: String, status: String): Result<Unit>
    suspend fun deleteVehicle(id: String): Result<Unit>
}

interface CallRepository {
    suspend fun initiateCall(calleeId: String, callType: String): Result<Call>
    suspend fun acceptCall(callId: String): Result<Call>
    suspend fun declineCall(callId: String): Result<Call>
    suspend fun endCall(callId: String): Result<Call>
    suspend fun sendSignaling(callId: String, signal: String): Result<Unit>
    suspend fun getCallHistory(page: Int, size: Int): Result<List<Call>>
    suspend fun getSupportAdminId(): Result<String>
}

interface NotificationRepository {
    suspend fun getAllNotifications(): Result<List<AppNotification>>
    suspend fun getUnreadCount(): Result<Int>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
}

interface UserRepository {
    suspend fun getAllUsers(search: String?): Result<List<User>>
    suspend fun updateUserStatus(id: String, enabled: Boolean): Result<User>
    suspend fun assignRoles(id: String, roleIds: List<String>): Result<User>
    suspend fun deleteUser(id: String): Result<Unit>
    suspend fun getRoles(search: String?): Result<List<Role>>
    suspend fun createRole(name: String, description: String): Result<Role>
    suspend fun deleteRole(roleId: String): Result<Unit>
}