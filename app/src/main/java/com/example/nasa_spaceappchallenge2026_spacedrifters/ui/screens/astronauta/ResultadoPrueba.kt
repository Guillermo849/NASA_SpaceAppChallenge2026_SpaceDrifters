package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import android.util.Log
import androidx.annotation.StringRes
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Frase para el astronauta al terminar una prueba: su resultado frente a su línea base, sin
 * números en bruto. [estado] null = no se pudo guardar la sesión.
 */
@StringRes
fun fraseResultado(tipo: TipoPrueba, estado: EstadoCognitivo?): Int = when (estado) {
    null -> R.string.resultado_error_guardado
    EstadoCognitivo.NORMAL -> R.string.resultado_normal
    EstadoCognitivo.SIN_LINEA_BASE -> R.string.resultado_sin_linea_base
    EstadoCognitivo.SIN_DATOS -> R.string.resultado_sin_datos
    EstadoCognitivo.VIGILAR -> when (tipo) {
        TipoPrueba.STROOP -> R.string.stroop_resultado_vigilar
        TipoPrueba.NBACK -> R.string.nback_resultado_vigilar
        TipoPrueba.PVT -> R.string.pvt_resultado_vigilar
    }
    EstadoCognitivo.ALERTA -> when (tipo) {
        TipoPrueba.STROOP -> R.string.stroop_resultado_alerta
        TipoPrueba.NBACK -> R.string.nback_resultado_alerta
        TipoPrueba.PVT -> R.string.pvt_resultado_alerta
    }
}

/**
 * Evalúa las reglas de alerta del astronauta tras guardar algo. Un fallo aquí se registra pero no
 * estropea el guardado (la sesión o el registro ya están guardados).
 */
suspend fun AlertasRepository.evaluarSinFallar(astronautaId: Long) {
    runCatching { evaluar(astronautaId) }
        .onFailure { Log.e("Alertas", "No se han podido evaluar las reglas", it) }
}

/**
 * Lanza un guardado en [ambito] (el de la app, para que termine aunque se cierre la pantalla).
 * Si falla, lo registra y devuelve null en vez de tumbar la app.
 */
fun <T> guardarEnSegundoPlano(ambito: CoroutineScope, guardar: suspend () -> T?): Deferred<T?> =
    ambito.async {
        runCatching { guardar() }
            .onFailure { Log.e("Pruebas", "No se ha podido guardar la sesión", it) }
            .getOrNull()
    }
