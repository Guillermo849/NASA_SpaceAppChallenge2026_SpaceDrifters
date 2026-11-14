package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Mientras está en pantalla: oculta las barras del sistema (pantalla completa) y mantiene la
 * pantalla encendida. Al salir lo deja todo como estaba. Para las pruebas cognitivas.
 */
@Composable
fun ModoPrueba() {
    val vista = LocalView.current
    val ventana = LocalActivity.current?.window
    DisposableEffect(vista, ventana) {
        vista.keepScreenOn = true
        val barras = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        barras?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        barras?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            vista.keepScreenOn = false
            barras?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
