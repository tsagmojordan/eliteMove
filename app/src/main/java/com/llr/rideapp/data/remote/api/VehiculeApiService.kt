package com.llr.rideapp.data.remote.api

import com.llr.rideapp.domain.model.VehiculeDto
import retrofit2.http.GET

interface VehiculeApiService {
    @GET("api/v1/vehicules")
    suspend fun getAllVehicules(): List<VehiculeDto>
}
