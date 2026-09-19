package com.llr.rideapp.domain.model

// Enums alignés sur le backend (contrat C6) :
//   VehiculeClass : ECO, CONFORT, PREMIUM, VAN
//   VehiculeStatus : AVAILABLE, IN_RIDE, MAINTENANCE, OUT_OF_SERVICE

enum class VehiculeClass {
    ECO, CONFORT, PREMIUM, VAN
}

enum class VehiculeStatus {
    AVAILABLE, IN_RIDE, MAINTENANCE, OUT_OF_SERVICE
}

data class VehiculeDto(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val vehiculeClass: VehiculeClass? = null,
    val status: VehiculeStatus? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val price: Double? = null,
    // Chemins des photos côté serveur (VehiculeDto backend)
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,
    val photo1MimeType: String? = null,
    // Champ des endpoints /with-thumbnails : JPEG 200x200 encodé en Base64
    val thumbnail: String? = null
)