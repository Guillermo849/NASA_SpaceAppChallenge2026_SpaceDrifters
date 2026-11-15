package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R

/**
 * Estructura común de todas las pantallas: barra superior con título (y botón de volver si
 * [onAtras] no es null), el contenido, y el aviso de prototipo abajo.
 * Con [titulo] null no hay barra superior (para pantallas con su propia cabecera, como el
 * selector de rol); el aviso de prototipo sigue siempre.
 * [onCerrarSesion] muestra un icono de logout en el extremo derecho de la barra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBase(
    titulo: String?,
    onAtras: (() -> Unit)? = null,
    onCerrarSesion: (() -> Unit)? = null,
    /** En pantallas anchas (tablet) el contenido se centra con este ancho como máximo. */
    anchoMaximo: Dp = ANCHO_CONTENIDO,
    contenido: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            if (titulo != null) TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    if (onAtras != null) {
                        // IconButton ya garantiza una zona táctil de 48 dp.
                        IconButton(onClick = onAtras) {
                            Icon(
                                painter = painterResource(R.drawable.ic_atras),
                                contentDescription = stringResource(R.string.cd_atras),
                            )
                        }
                    }
                },
                actions = {
                    if (onCerrarSesion != null) {
                        IconButton(onClick = onCerrarSesion) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cerrar_sesion),
                                contentDescription = stringResource(R.string.cd_cerrar_sesion),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = { AvisoPrototipo(Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        // En tablet no estiramos la interfaz del móvil a todo el ancho: columna centrada.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.fillMaxHeight().widthIn(max = anchoMaximo)) {
                contenido(padding)
            }
        }
    }
}

/** Ancho máximo del contenido de una pantalla normal (formularios, perfiles...) en tablet. */
val ANCHO_CONTENIDO = 840.dp

/** Ancho máximo de los paneles que aprovechan más espacio (centro de control médico). */
val ANCHO_PANEL = 1200.dp

/** Contenido provisional para pantallas que aún no están hechas. */
@Composable
fun EnConstruccion(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pantalla_en_construccion),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
