package com.llr.rideapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.llr.rideapp.presentation.common.PrimaryDark
import com.llr.rideapp.presentation.common.RideAppTheme
import com.llr.rideapp.presentation.navigation.RideAppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RideAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryDark
                ) {
                    RideAppNavGraph()
                }
            }
        }
    }
}
