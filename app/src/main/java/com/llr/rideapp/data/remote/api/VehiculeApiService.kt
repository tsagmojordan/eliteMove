package com.llr.rideapp.data.remote.api

import com.llr.rideapp.domain.model.VehiculeDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Endpoints véhicules consommés directement (DTOs bruts, sans wrapper ApiResponse).
 * Les réponses "with-thumbnails" incluent le champ `thumbnail` (Base64 JPEG).
 */
interface VehiculeApiService {

    @GET("api/v1/vehicules")
    suspend fun getAllVehicules(): List<VehiculeDto>

    @GET("api/v1/vehicules/with-thumbnails")
    suspend fun getAllWithThumbnails(): List<VehiculeDto>

    @GET("api/v1/vehicules/available/with-thumbnails")
    suspend fun getAvailableWithThumbnails(): List<VehiculeDto>

    @GET("api/v1/vehicules/{id}")
    suspend fun getVehiculeById(@Path("id") id: String): VehiculeDto
}