package com.llr.rideapp.data.remote.api

import com.llr.rideapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<AuthResponse>>
}

interface UserApiService {

    @POST("api/v1/users")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<UserDto>>

    @GET("api/v1/users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<ApiResponse<UserDto>>

    @GET("api/v1/users")
    suspend fun getAllUsers(@Query("search") search: String? = null): Response<ApiResponse<List<UserDto>>>

    @PATCH("api/v1/users/{id}/status")
    suspend fun updateUserStatus(
        @Path("id") id: String,
        @Query("enabled") enabled: Boolean
    ): Response<ApiResponse<UserDto>>

    @POST("api/v1/users/{id}/roles")
    suspend fun assignRoles(
        @Path("id") id: String,
        @Body request: AssignRoleRequest
    ): Response<ApiResponse<UserDto>>
}

interface RideApiService {

    @POST("api/v1/rides")
    suspend fun createRide(@Body request: CreateRideRequest): Response<RideDto>

    @GET("api/v1/rides/user/{userId}")
    suspend fun getRidesByUser(@Path("userId") userId: String): Response<List<RideDto>>

    @GET("api/v1/rides")
    suspend fun getAllRides(): Response<List<RideDto>>

    @PATCH("api/v1/rides/{id}/status")
    suspend fun updateRideStatus(
        @Path("id") id: String,
        @Query("status") status: String
    ): Response<RideDto>
}

interface VehicleApiService {

    @GET("api/v1/vehicules/available")
    suspend fun getAvailableVehicles(): Response<ApiResponse<List<VehicleDto>>>

    @POST("api/v1/vehicules")
    suspend fun createVehicle(@Body request: CreateVehicleRequest): Response<ApiResponse<VehicleDto>>
}

interface CallApiService {

    @POST("api/v1/calls")
    suspend fun initiateCall(@Body request: InitiateCallRequest): Response<ApiResponse<CallDto>>

    @PATCH("api/v1/calls/{callId}/accept")
    suspend fun acceptCall(@Path("callId") callId: String): Response<Unit>

    @PATCH("api/v1/calls/{callId}/decline")
    suspend fun declineCall(@Path("callId") callId: String): Response<Unit>

    @PATCH("api/v1/calls/{callId}/end")
    suspend fun endCall(
        @Path("callId") callId: String,
        @Body request: EndCallRequest = EndCallRequest()
    ): Response<Unit>

    @POST("api/v1/calls/signaling")
    suspend fun sendSignaling(@Body request: SignalingRequest): Response<Unit>

    @GET("api/v1/calls/history")
    suspend fun getCallHistory(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<List<CallDto>>>
}

interface NotificationApiService {

    @GET("api/v1/notifications/in-app")
    suspend fun getAllNotifications(): Response<ApiResponse<List<NotificationDto>>>

    @GET("api/v1/notifications/in-app/unread/count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountDto>>

    @PATCH("api/v1/notifications/in-app/{notificationId}/read")
    suspend fun markAsRead(@Path("notificationId") notificationId: String): Response<ApiResponse<NotificationDto>>

    @PATCH("api/v1/notifications/in-app/read-all")
    suspend fun markAllAsRead(): Response<ApiResponse<Unit>>
}
