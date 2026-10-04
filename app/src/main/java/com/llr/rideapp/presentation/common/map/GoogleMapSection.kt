package com.llr.rideapp.presentation.common.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.llr.rideapp.R
import com.llr.rideapp.domain.model.VehiculeDto

/** Conversion interne vers le type LatLng de Google Maps. */
internal fun MapPoint.toGoogleLatLng(): LatLng = LatLng(latitude, longitude)

/** Icône voiture stylisée (design system) convertie en BitmapDescriptor. */
private fun carBitmapDescriptor(context: Context): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_car)
        ?: return BitmapDescriptorFactory.defaultMarker()
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

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
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPoint.toGoogleLatLng(), MapConfig.MAP_ZOOM_DEFAULT.toFloat())
    }

    // Style "Night" premium (JSON dans res/raw/map_style_dark.json)
    val mapStyleOptions = if (MapConfig.DARK_MODE) {
        remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) }
    } else null

    val carIcon = remember { carBitmapDescriptor(context) }

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
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapStyleOptions = mapStyleOptions
        )
    ) {
        vehicules.forEach { v ->
            val lat = v.latitude ?: DEFAULT_MAP_POINT.latitude
            val lng = v.longitude ?: DEFAULT_MAP_POINT.longitude
            Marker(
                state = MarkerState(position = LatLng(lat, lng)),
                title = "${v.brand} ${v.model}",
                snippet = v.licensePlate,
                icon = carIcon
            )
        }
    }
}