package com.example.nasa_spaceappchallenge2026_spacedrifters

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation.AppNavHost
import androidx.compose.foundation.layout.Box
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.emergencia.CapaEmergencia
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Única Activity de la app. Todas las pantallas son destinos de Navigation Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app siempre es oscura: iconos claros en las barras del sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SpaceDriftersTheme {
                Box {
                    AppNavHost()
                    // Encima de todo: si hay una emergencia (nivel 5) sin atender, ocupa la pantalla.
                    CapaEmergencia()
                }
            }
        }
    }
}
