package com.llr.rideapp.presentation.common.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.llr.rideapp.domain.model.VehiculeDto
import com.llr.rideapp.presentation.common.AccentGold
import kotlinx.coroutines.tasks.await

/**
 * Pont entre les composants de l'app et la caméra de la carte active,
 * quel que soit le fournisseur (Google Maps ou OpenStreetMap).
 */
class MapCameraController {
    internal var moveCamera: ((MapPoint, Double) -> Unit)? = null

    /** Centre la caméra sur [point] au niveau de zoom [zoom]. */
    fun centerOn(point: MapPoint, zoom: Double) {
        moveCamera?.invoke(point, zoom)
    }
}

@Composable
fun rememberMapCameraController(): MapCameraController = remember { MapCameraController() }

/**
 * Section carte du dashboard client, indépendante du fournisseur.
 *
 * Gère ce qui est commun aux deux fournisseurs : la demande de permission de
 * localisation, la récupération de la dernière position connue (FusedLocation),
 * le recentrage sur le véhicule sélectionné et le bouton "centrer sur moi".
 * Le rendu est délégué à [GoogleMapSection] ou [OsmMapSection] selon
 * [MapConfig.USE_OPENSTREETMAP].
 */
@Composable
fun AppMapSection(
    modifier: Modifier = Modifier,
    vehicules: List<VehiculeDto>,
    userLocation: MapPoint?,
    onUserLocationUpdated: (MapPoint) -> Unit,
    selectedVehicule: VehiculeDto?
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    val cameraController = rememberMapCameraController()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasLocationPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    onUserLocationUpdated(MapPoint(location.latitude, location.longitude))
                }
            } catch (e: SecurityException) {
                // Permission refusée
            } catch (e: Exception) {
                // Ignorer l'erreur Play Services
            }
        }
    }

    // Recentre la caméra sur le véhicule sélectionné
    LaunchedEffect(selectedVehicule) {
        selectedVehicule?.let { v ->
            val lat = v.latitude ?: DEFAULT_MAP_POINT.latitude
            val lng = v.longitude ?: DEFAULT_MAP_POINT.longitude
            cameraController.centerOn(MapPoint(lat, lng), MapConfig.MAP_ZOOM_SELECTED)
        }
    }

    Box(modifier = modifier) {
        val initialPoint = userLocation ?: DEFAULT_MAP_POINT
        if (MapConfig.USE_OPENSTREETMAP) {
            OsmMapSection(
                modifier = Modifier.fillMaxSize(),
                vehicules = vehicules,
                initialPoint = initialPoint,
                hasLocationPermission = hasLocationPermission,
                cameraController = cameraController
            )
        } else {
            GoogleMapSection(
                modifier = Modifier.fillMaxSize(),
                vehicules = vehicules,
                initialPoint = initialPoint,
                hasLocationPermission = hasLocationPermission,
                cameraController = cameraController
            )
        }

        FloatingActionButton(
            onClick = {
                userLocation?.let { cameraController.centerOn(it, MapConfig.MAP_ZOOM_SELECTED) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color.White,
            contentColor = AccentGold
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Centrer")
        }
    }
}