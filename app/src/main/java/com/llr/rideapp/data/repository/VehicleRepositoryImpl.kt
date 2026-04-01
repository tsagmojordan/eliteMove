package com.llr.rideapp.data.repository

import com.llr.rideapp.data.remote.api.VehicleApiService
import com.llr.rideapp.data.remote.dto.CreateVehicleRequest
import com.llr.rideapp.domain.model.Vehicle
import com.llr.rideapp.domain.repository.VehicleRepository
import javax.inject.Inject

class VehicleRepositoryImpl @Inject constructor(
    private val vehicleApiService: VehicleApiService
) : VehicleRepository {

    override suspend fun getAvailableVehicles(): Result<List<Vehicle>> = safeApiCall {
        val response = vehicleApiService.getAvailableVehicles()
        response.body()?.data?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun createVehicle(
        brand: String, model: String, year: Int,
        licensePlate: String, vehiculeClass: String
    ): Result<Vehicle> = safeApiCall {
        val response = vehicleApiService.createVehicle(
            CreateVehicleRequest(brand, model, year, licensePlate, vehiculeClass)
        )
        response.body()?.data?.toModel() ?: throw Exception("Données de véhicule manquantes")
    }

    private fun com.llr.rideapp.data.remote.dto.VehicleDto.toModel() = Vehicle(
        id = id ?: "",
        brand = brand ?: "",
        model = model ?: "",
        year = year ?: 0,
        licensePlate = licensePlate ?: "",
        vehiculeClass = vehiculeClass ?: "",
        available = available ?: false
    )
}
