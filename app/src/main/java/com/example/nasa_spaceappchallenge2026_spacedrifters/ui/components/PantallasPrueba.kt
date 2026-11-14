package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components

import android.os.SystemClock
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private const val NANOS_POR_MS = 1_000_000L

/**
 * Zona táctil de respuesta de una prueba cognitiva. La respuesta se registra en el propio evento
 * táctil (al apoyar el dedo), con el instante del evento en nanosegundos de CLOCK_MONOTONIC, no al
 * recomponer. Sin ripple ni animaciones. Con lectores de pantalla (y en tests) también responde
 * a la acción de clic, con el instante actual.
 */
@Composable
fun ZonaRespuesta(
    onToque: (instanteNanos: Long) -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable BoxScope.() -> Unit,
) {
    val alTocar by rememberUpdatedState(onToque)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val toque = awaitFirstDown()
                    alTocar(toque.uptimeMillis * NANOS_POR_MS)
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick {
                    alTocar(SystemClock.uptimeMillis() * NANOS_POR_MS)
                    true
                }
            },
        contentAlignment = Alignment.Center,
        content = contenido,
    )
}

/**
 * Salir de la app (o bloquear la pantalla) a mitad de prueba la interrumpe.
 * Girar el móvil no, porque la Activity se recrea pero el ViewModel sigue.
 */
@Composable
fun InterrumpirAlSalir(onInterrumpir: () -> Unit) {
    val actividad = LocalActivity.current
    val interrumpir by rememberUpdatedState(onInterrumpir)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (actividad?.isChangingConfigurations != true) interrumpir()
    }
}

/** Pantalla entre etapas de una prueba (fin de práctica, prueba terminada, interrumpida...). */
@Composable
fun MensajePrueba(
    tituloPantalla: String,
    titulo: String,
    texto: String,
    boton: String,
    onBoton: () -> Unit,
    onAtras: () -> Unit,
) {
    PantallaBase(titulo = tituloPantalla, onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = titulo, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onBoton,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(boton)
            }
        }
    }
}
