package com.llr.rideapp.data.repository

import com.google.gson.Gson
import com.llr.rideapp.data.remote.api.VehicleApiService
import com.llr.rideapp.data.remote.dto.CreateVehicleRequest
import com.llr.rideapp.domain.model.Vehicle
import com.llr.rideapp.domain.model.VehiclePhoto
import com.llr.rideapp.domain.repository.VehicleRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class VehicleRepositoryImpl @Inject constructor(
    private val vehicleApiService: VehicleApiService
) : VehicleRepository {

    private val gson = Gson()

    override suspend fun getAllVehicles(): Result<List<Vehicle>> = safeApiCall {
        val response = vehicleApiService.getAllVehicles()
        response.body()?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun getAvailableVehicles(): Result<List<Vehicle>> = safeApiCall {
        val response = vehicleApiService.getAvailableVehicles()
        response.body()?.map { it.toModel() } ?: emptyList()
    }

    override suspend fun getVehicleById(id: String): Result<Vehicle> = safeApiCall {
        val response = vehicleApiService.getVehicleById(id)
        response.body()?.toModel()
            ?: throw Exception("Erreur lors du chargement du véhicule (HTTP ${response.code()})")
    }

    override suspend fun createVehicle(
        brand: String, model: String, year: Int,
        licensePlate: String, vehiculeClass: String, price: Int,
        photos: List<VehiclePhoto>?
    ): Result<Vehicle> = safeApiCall {
        // Contrat C6 : multipart avec part "request" (JSON) + part "photos" (fichiers)
        val requestJson = gson.toJson(
            CreateVehicleRequest(brand, model, year, licensePlate, vehiculeClass, price)
        )
        val requestPart = requestJson.toRequestBody("application/json".toMediaTypeOrNull())

        val photoParts = photos?.map { photo ->
            MultipartBody.Part.createFormData(
                "photos",
                photo.name,
                photo.bytes.toRequestBody(photo.mimeType.toMediaTypeOrNull())
            )
        }

        val response = vehicleApiService.createVehicle(requestPart, photoParts)
        response.body()?.toModel()
            ?: throw Exception("Erreur lors de la création du véhicule (HTTP ${response.code()})")
    }

    override suspend fun updateVehicle(
        id: String, brand: String, model: String, year: Int,
        licensePlate: String, vehiculeClass: String, price: Int
    ): Result<Vehicle> = safeApiCall {
        val response = vehicleApiService.updateVehicle(
            id, CreateVehicleRequest(brand, model, year, licensePlate, vehiculeClass, price)
        )
        response.body()?.toModel()
            ?: throw Exception("Erreur lors de la mise à jour du véhicule")
    }

    override suspend fun updateVehicleStatus(id: String, status: String): Result<Unit> = safeApiCall {
        val response = vehicleApiService.updateVehicleStatus(id, status)
        if (!response.isSuccessful) throw Exception("Erreur changement de statut (HTTP ${response.code()})")
        Unit
    }

    override suspend fun deleteVehicle(id: String): Result<Unit> = safeApiCall {
        val response = vehicleApiService.deleteVehicle(id)
        if (!response.isSuccessful) throw Exception("Erreur suppression du véhicule (HTTP ${response.code()})")
        Unit
    }

    private fun com.llr.rideapp.data.remote.dto.VehicleDto.toModel() = Vehicle(
        id = id ?: "",
        brand = brand ?: "",
        model = model ?: "",
        year = year ?: 0,
        licensePlate = licensePlate ?: "",
        vehiculeClass = vehiculeClass ?: "",
        status = status ?: "UNKNOWN",
        price = price
    )
}