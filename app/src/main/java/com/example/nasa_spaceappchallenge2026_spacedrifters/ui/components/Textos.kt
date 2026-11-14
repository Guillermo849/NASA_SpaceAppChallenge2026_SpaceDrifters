package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Textos y marcas que comparten las pantallas del astronauta y del médico.

@StringRes
fun nombreCargo(cargo: CargoTripulante): Int = when (cargo) {
    CargoTripulante.COMANDANTE -> R.string.cargo_comandante
    CargoTripulante.INGENIERIA_VUELO -> R.string.cargo_ingenieria_vuelo
    CargoTripulante.OFICIAL_MEDICO -> R.string.cargo_oficial_medico
    CargoTripulante.ESPECIALISTA_MISION -> R.string.cargo_especialista_mision
}

@StringRes
fun nombrePrueba(tipo: TipoPrueba): Int = when (tipo) {
    TipoPrueba.STROOP -> R.string.stroop_nombre
    TipoPrueba.NBACK -> R.string.nback_nombre
}

/** Métrica principal con su unidad: "85 ms" (efecto Stroop) o "d′ 2,24". */
fun formatoMetrica(tipo: TipoPrueba, valor: Double): String = when (tipo) {
    TipoPrueba.STROOP -> "${numero(valor, 0)} ms"
    TipoPrueba.NBACK -> "d′ ${numero(valor, 2)}"
}

/** Número con los decimales indicados y el separador del idioma del móvil. */
fun numero(valor: Double, decimales: Int): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = decimales
        maximumFractionDigits = decimales
    }.format(valor)

/** "14 nov 2026, 8:20" en el formato del idioma del móvil. */
fun fechaHora(instante: Long, zona: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(instante).atZone(zona)
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

/** Prioridad de una alerta con su texto y su color (nunca solo el color). */
@Composable
fun MarcaPrioridad(prioridad: Regla.Prioridad, modifier: Modifier = Modifier) {
    val estados = LocalEstadoColores.current
    val (texto, color) = when (prioridad) {
        Regla.Prioridad.AHORA -> R.string.prioridad_ahora to estados.alerta
        Regla.Prioridad.HOY -> R.string.prioridad_hoy to estados.vigilar
        Regla.Prioridad.PUEDE_ESPERAR -> R.string.prioridad_puede_esperar to estados.sinDatos
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(text = stringResource(texto), style = MaterialTheme.typography.labelLarge, color = color)
    }
}
