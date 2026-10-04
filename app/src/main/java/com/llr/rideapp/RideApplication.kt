package com.llr.rideapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class RideApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initOsmdroid()
    }

    /**
     * Configuration d'osmdroid (fournisseur OpenStreetMap de AppMapSection).
     * - User-Agent obligatoire : conforme à la politique d'usage des tuiles OSM
     * - Cache de tuiles dans le répertoire privé de l'app (aucune permission stockage)
     */
    private fun initOsmdroid() {
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }
}