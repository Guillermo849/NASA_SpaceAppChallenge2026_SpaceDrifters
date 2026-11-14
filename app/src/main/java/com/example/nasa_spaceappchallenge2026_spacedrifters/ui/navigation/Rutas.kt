package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación tipadas (Navigation Compose).
// Para añadir una pantalla: declara aquí su ruta y regístrala en AppNavHost.
// Si necesita argumentos, usa una data class:
//     @Serializable data class RutaPerfil(val astronautaId: String)

@Serializable
data object RutaLogin

@Serializable
data object RutaSelectorRol

@Serializable
data object RutaInicioAstronauta

@Serializable
data object RutaTripulacion

@Serializable
data object RutaPruebaStroop

@Serializable
data object RutaPruebaNBack

@Serializable
data object RutaRegistroDiario

// Bienestar psicológico: check-in y herramientas
@Serializable
data object RutaBienestar

@Serializable
data object RutaRespiracion

@Serializable
data object RutaJuegoParejas

// Médico: perfil de un astronauta y detalle de una sesión
@Serializable
data class RutaPerfilAstronauta(val astronautaId: Long)

@Serializable
data class RutaDetalleSesion(val sesionId: Long)

// Oficial médico de a bordo: protocolos de la tripulación y resultado de una prueba
@Serializable
data object RutaOficialMedico

@Serializable
data class RutaResultadoPrueba(val pruebaId: Long)
