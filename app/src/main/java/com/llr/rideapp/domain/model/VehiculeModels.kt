package com.llr.rideapp.domain.model



enum class VehiculeClass {
    STANDARD, PREMIUM, VAN, LUXURY, ECO
}

enum class VehiculeStatus {
    AVAILABLE, BUSY, OFFLINE
}

data class VehiculeDto(
    val id: String,
    val brand: String,
    val model: String,
    val imagePath: String?,
    val year: Int,
    val licensePlate: String,
    val vehiculeClass: VehiculeClass,
    val status: VehiculeStatus,
    val longitude: Double?,
    val latitude: Double?,
    val price: Double? = null
)
