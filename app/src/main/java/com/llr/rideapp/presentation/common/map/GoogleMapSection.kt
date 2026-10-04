package com.llr.rideapp.presentation.common.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.llr.rideapp.domain.model.VehiculeDto

/** Conversion interne vers le type LatLng de Google Maps. */
internal fun MapPoint.toGoogleLatLng(): LatLng = LatLng(latitude, longitude)

/**
 * Rendu de la carte avec Google Maps (maps-compose).
 *
 * Conservé pour permettre la migration retour depuis OpenStreetMap :
 * passer [MapConfig.USE_OPENSTREETMAP] à false (et configurer MAPS_API_KEY
 * dans app/build.gradle.kts) suffit.
 */
@Composable
internal fun GoogleMapSection(
    modifier: Modifier = Modifier,
    vehicules: List<VehiculeDto>,
    initialPoint: MapPoint,
    hasLocationPermission: Boolean,
    cameraController: MapCameraController
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPoint.toGoogleLatLng(), MapConfig.MAP_ZOOM_DEFAULT.toFloat())
    }

    // Branche la caméra Google sur le contrôleur commun
    DisposableEffect(cameraController, cameraPositionState) {
        cameraController.moveCamera = { point, zoom ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(point.toGoogleLatLng(), zoom.toFloat())
        }
        onDispose { cameraController.moveCamera = null }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
    ) {
        vehicules.forEach { v ->
            val lat = v.latitude ?: DEFAULT_MAP_POINT.latitude
            val lng = v.longitude ?: DEFAULT_MAP_POINT.longitude
            Marker(
                state = MarkerState(position = LatLng(lat, lng)),
                title = "${v.brand} ${v.model}",
                snippet = v.licensePlate
            )
        }
    }
}