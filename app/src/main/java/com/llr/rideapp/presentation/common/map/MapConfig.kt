package com.llr.rideapp.presentation.common.map

/**
 * Coordonnée géographique indépendante du fournisseur de carte
 * (Google Maps ou OpenStreetMap) pour pouvoir basculer sans toucher aux écrans.
 */
data class MapPoint(val latitude: Double, val longitude: Double)

/** Position par défaut : Douala */
val DEFAULT_MAP_POINT = MapPoint(4.0511, 9.7679)

/**
 * Fournisseur de carte actif de l'application.
 *
 * - true  = OpenStreetMap (osmdroid) — aucune clé API requise
 * - false = Google Maps (maps-compose) — nécessite MAPS_API_KEY dans app/build.gradle.kts
 *
 * Basculer ce flag suffit pour migrer : les deux implémentations sont conservées
 * (GoogleMapSection / OsmMapSection) et l'écran appelant reste identique.
 */
object MapConfig {
    const val USE_OPENSTREETMAP = true

    /** Zoom initial de la carte. */
    const val MAP_ZOOM_DEFAULT = 12.0

    /** Zoom lors de la sélection d'un véhicule ou du recentrage sur l'utilisateur. */
    const val MAP_ZOOM_SELECTED = 15.0
}