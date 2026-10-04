package com.llr.rideapp.data.remote.api

import com.llr.rideapp.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    // Contrat C2 : inscription publique (backend B1)
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<UserResponse>>
}

interface UserApiService {

    @GET("api/v1/users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<ApiResponse<UserDto>>

    // Contrat C5 : réponse paginée ApiResponse<PaginatedResponse<UserResponse>>
    @GET("api/v1/users")
    suspend fun getAllUsers(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<ApiResponse<PaginatedResponse<UserDto>>>

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

    @DELETE("api/v1/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<ResponseBody>
}

interface RoleApiService {

    @GET("api/v1/roles")
    suspend fun getRoles(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<ApiResponse<PaginatedResponse<RoleResponse>>>

    @POST("api/v1/roles")
    suspend fun createRole(@Body request: RoleRequest): Response<ApiResponse<RoleResponse>>

    @DELETE("api/v1/roles/{id}")
    suspend fun deleteRole(@Path("id") id: String): Response<ResponseBody>
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

    // Contrat C6 : listes BRUTES (pas de wrapper ApiResponse), champ `status` (pas `available`)
    @GET("api/v1/vehicules/available")
    suspend fun getAvailableVehicles(): Response<List<VehicleDto>>

    @GET("api/v1/vehicules")
    suspend fun getAllVehicles(): Response<List<VehicleDto>>

    @GET("api/v1/vehicules/{id}")
    suspend fun getVehicleById(@Path("id") id: String): Response<VehicleDto>

    // Contrat C6 : création en multipart — part "request" (JSON) + part "photos" (fichiers)
    @Multipart
    @POST("api/v1/vehicules")
    suspend fun createVehicle(
        @Part("request") request: RequestBody,
        @Part photos: List<MultipartBody.Part>? = null
    ): Response<VehicleDto>

    @PUT("api/v1/vehicules/{id}")
    suspend fun updateVehicle(
        @Path("id") id: String,
        @Body request: CreateVehicleRequest
    ): Response<VehicleDto>

    @PATCH("api/v1/vehicules/{id}/status")
    suspend fun updateVehicleStatus(
        @Path("id") id: String,
        @Query("status") status: String
    ): Response<ResponseBody>

    @DELETE("api/v1/vehicules/{id}")
    suspend fun deleteVehicle(@Path("id") id: String): Response<ResponseBody>
}

interface CallApiService {

    // Contrat C3 : réponse BRUTE InitiateCallResponse { callId, message }
    @POST("api/v1/calls")
    suspend fun initiateCall(@Body request: InitiateCallRequest): Response<InitiateCallResponse>

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
    ): Response<List<CallDto>>
}

interface NotificationApiService {

    // Contrat C4 : ApiResponse<PaginatedResponse<InAppNotificationResponse>>
    @GET("api/v1/notifications/in-app")
    suspend fun getAllNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<ApiResponse<PaginatedResponse<NotificationDto>>>

    // Contrat C4 : data est un Long (nombre brut)
    @GET("api/v1/notifications/in-app/unread/count")
    suspend fun getUnreadCount(): Response<ApiResponse<Long>>

    // Contrat C4 : ApiResponse<Void> → succès = code 2xx uniquement
    @PATCH("api/v1/notifications/in-app/{notificationId}/read")
    suspend fun markAsRead(@Path("notificationId") notificationId: String): Response<ResponseBody>

    @PATCH("api/v1/notifications/in-app/read-all")
    suspend fun markAllAsRead(): Response<ResponseBody>
}