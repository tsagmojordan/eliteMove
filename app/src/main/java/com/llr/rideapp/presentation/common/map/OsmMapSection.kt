package com.llr.rideapp.presentation.common.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.llr.rideapp.R
import com.llr.rideapp.domain.model.VehiculeDto
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * Style de tuiles sombres "Night" (CartoDB Dark) — rendu premium luxueux
 * équivalent au mode night de Google Maps, sans clé API.
 */
private fun darkTileSource(): OnlineTileSourceBase = object : OnlineTileSourceBase(
    "CartoDark", 0, 20, 256, ".png",
    arrayOf("https://basemaps.cartocdn.com/dark_all/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
}

/**
 * Rendu de la carte avec OpenStreetMap (osmdroid) — aucune clé API requise.
 *
 * Le User-Agent et le cache de tuiles sont configurés dans RideApplication
 * (conformité à la politique d'usage des tuiles OSM).
 */
@Composable
internal fun OsmMapSection(
    modifier: Modifier = Modifier,
    vehicules: List<VehiculeDto>,
    initialPoint: MapPoint,
    hasLocationPermission: Boolean,
    cameraController: MapCameraController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Icône voiture stylisée (design system) pour les chauffeurs à proximité
    val carIcon = remember { ContextCompat.getDrawable(context, R.drawable.ic_map_car) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(if (MapConfig.DARK_MODE) darkTileSource() else TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(MapConfig.MAP_ZOOM_DEFAULT)
            controller.setCenter(GeoPoint(initialPoint.latitude, initialPoint.longitude))
        }
    }

    // Point bleu "ma position" (via LocationManager, sans Play Services)
    val myLocationOverlay = remember(mapView) { MyLocationNewOverlay(mapView) }
    var locationOverlayEnabled by remember { mutableStateOf(false) }

    // Branche la caméra OSM sur le contrôleur commun (FAB + sélection véhicule)
    DisposableEffect(cameraController, mapView) {
        cameraController.moveCamera = { point, zoom ->
            mapView.controller.animateTo(GeoPoint(point.latitude, point.longitude))
            mapView.controller.setZoom(zoom)
        }
        onDispose { cameraController.moveCamera = null }
    }

    // Cycle de vie osmdroid : pause/reprise des chargements de tuiles
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocationOverlay.disableMyLocation()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        // Ajoute l'overlay "ma position" sous les marqueurs, une seule fois
        factory = {
            mapView.overlays.add(myLocationOverlay)
            mapView
        },
        update = { view ->
            view.overlays.removeAll { it is Marker }

            // Marqueurs véhicules — reconstruits à chaque changement de liste
            vehicules.forEach { v ->
                val lat = v.latitude ?: DEFAULT_MAP_POINT.latitude
                val lng = v.longitude ?: DEFAULT_MAP_POINT.longitude
                Marker(view).apply {
                    position = GeoPoint(lat, lng)
                    title = "${v.brand} ${v.model}"
                    snippet = v.licensePlate
                    icon = carIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }.also(view.overlays::add)
            }

            // Point bleu activé uniquement avec la permission accordée
            if (hasLocationPermission && !locationOverlayEnabled) {
                myLocationOverlay.enableMyLocation()
                locationOverlayEnabled = true
            } else if (!hasLocationPermission && locationOverlayEnabled) {
                myLocationOverlay.disableMyLocation()
                locationOverlayEnabled = false
            }

            view.invalidate()
        }
    )
}