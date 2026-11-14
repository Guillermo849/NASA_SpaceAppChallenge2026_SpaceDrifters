package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val EsquemaControlMision = darkColorScheme(
    primary = Acento,
    onPrimary = Fondo,
    secondary = Acento,
    onSecondary = Fondo,
    tertiary = Aviso,
    onTertiary = Fondo,
    background = Fondo,
    onBackground = Texto,
    surface = Fondo,
    onSurface = Texto,
    // Distinto de los surfaceContainer: si coincidiera con Panel, las tarjetas tomarían
    // onSurfaceVariant (texto gris) como color de contenido.
    surfaceVariant = Borde,
    onSurfaceVariant = TextoSecundario,
    surfaceContainerLowest = Fondo,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = Panel,
    surfaceContainerHighest = Panel,
    outline = Borde,
    outlineVariant = Borde,
    error = Alerta,
    onError = Fondo,
)

/**
 * Colores de estado (Normal, Vigilar, Alerta, Sin datos).
 * El color nunca va solo: siempre acompañado de su texto.
 */
@Immutable
data class EstadoColores(
    val normal: Color = Normal,
    val vigilar: Color = Aviso,
    val alerta: Color = Alerta,
    val sinDatos: Color = TextoSecundario,
)

val LocalEstadoColores = staticCompositionLocalOf { EstadoColores() }

/** Tema de la app: siempre oscuro, estilo control de misión. */
@Composable
fun SpaceDriftersTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEstadoColores provides EstadoColores()) {
        MaterialTheme(
            colorScheme = EsquemaControlMision,
            typography = SpaceDriftersTypography,
            content = content,
        )
    }
}
