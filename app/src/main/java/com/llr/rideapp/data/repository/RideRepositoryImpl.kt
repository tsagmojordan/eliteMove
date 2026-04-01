package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.RideApiService
import com.llr.rideapp.data.remote.dto.CreateRideRequest
import com.llr.rideapp.domain.model.Ride
import com.llr.rideapp.domain.repository.RideRepository
import javax.inject.Inject

class RideRepositoryImpl @Inject constructor(
    private val rideApiService: RideApiService
) : RideRepository {

    override suspend fun createRide(
        userId: String, vehiculeId: String?,
        pickupLocation: String, dropoffLocation: String
    ): Result<Ride> = safeApiCall {
        val response = rideApiService.createRide(
            CreateRideRequest(userId, vehiculeId, pickupLocation, dropoffLocation)
        )
        response.body()?.toModel() ?: throw Exception("Données de trajet manquantes")
    }

    override suspend fun getRidesByUser(userId: String): Result<List<Ride>> = safeApiCall {
        val response = rideApiService.getRidesByUser(userId)
        response.body()?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun getAllRides(): Result<List<Ride>> = safeApiCall {
        val response = rideApiService.getAllRides()
        response.body()?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun updateRideStatus(id: String, status: String): Result<Ride> = safeApiCall {
        val response = rideApiService.updateRideStatus(id, status)
        response.body()?.toModel() ?: throw Exception("Echec de la mise à jour")
    }

    private fun com.llr.rideapp.data.remote.dto.RideDto.toModel() = Ride(
        id = id ?: "",
        userId = userId ?: "",
        vehiculeId = vehiculeId,
        pickupLocation = pickupLocation ?: "",
        dropoffLocation = dropoffLocation ?: "",
        status = status ?: "UNKNOWN",
        requestedAt = requestedAt,
        completedAt = completedAt,
        price = price
    )
}
