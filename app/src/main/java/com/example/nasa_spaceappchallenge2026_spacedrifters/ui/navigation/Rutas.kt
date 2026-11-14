package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación tipadas (Navigation Compose).
// Para añadir una pantalla: declara aquí su ruta y regístrala en AppNavHost.
// Si necesita argumentos, usa una data class:
//     @Serializable data class RutaPerfil(val astronautaId: String)

@Serializable
data object RutaSelectorRol

@Serializable
data object RutaInicioAstronauta

@Serializable
data object RutaTripulacion
