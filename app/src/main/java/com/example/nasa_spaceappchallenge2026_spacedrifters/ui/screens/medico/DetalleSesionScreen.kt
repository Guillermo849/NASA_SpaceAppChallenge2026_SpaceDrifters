package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.SesionConResumen
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FormaNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.fechaHora
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.formatoMetrica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombrePrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.numero
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback.nombre
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores

/** Detalle de una sesión para el médico: métricas arriba y todos los ensayos en una lista. */
@Composable
fun DetalleSesionScreen(
    onAtras: () -> Unit,
    viewModel: DetalleSesionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val sesion = ui.sesion
    PantallaBase(
        titulo = sesion?.let { stringResource(nombrePrueba(it.tipo)) } ?: stringResource(R.string.medico_detalle_titulo),
        onAtras = onAtras,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!ui.cargando && sesion == null) {
                item { Text(stringResource(R.string.estado_sin_datos), style = MaterialTheme.typography.bodyLarge) }
            }
            sesion?.let { item { TarjetaMetricas(it) } }
            if (ui.ensayos.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.medico_ensayos_titulo, ui.ensayos.size),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(ui.ensayos, key = { it.numero }) { FilaEnsayo(it, sesion?.tipo ?: TipoPrueba.STROOP) }
            }
        }
    }
}

@Composable
private fun TarjetaMetricas(s: SesionConResumen) {
    val estado = EstadoCognitivo.entries.firstOrNull { it.name == s.estado }
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(fechaHora(s.inicioEn), style = MaterialTheme.typography.titleMedium)
            if (!s.valida) {
                Text(stringResource(R.string.medico_no_valida_detalle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                MarcaEstado(estado ?: EstadoCognitivo.SIN_DATOS)
            }
            if (s.esLineaBase) Text(stringResource(R.string.medico_linea_base), style = MaterialTheme.typography.labelLarge)
            when (s.tipo) {
                TipoPrueba.STROOP -> {
                    Metrica(R.string.medico_metrica_stroop, s.efectoStroopMs?.let { formatoMetrica(TipoPrueba.STROOP, it) })
                    Metrica(R.string.metrica_tiempo_medio, s.tiempoMedioMs?.let { "${numero(it, 0)} ms" })
                    Metrica(R.string.metrica_porcentaje_aciertos, s.porcentajeAciertos?.let { "${numero(it, 1)} %" })
                    Metrica(R.string.metrica_errores_no_coincidentes, s.erroresNoCoincidentes?.toString())
                }
                TipoPrueba.NBACK -> {
                    Metrica(R.string.medico_metrica_nback, s.dPrima?.let { formatoMetrica(TipoPrueba.NBACK, it) })
                    Metrica(R.string.metrica_aciertos_coincidencias, s.aciertosCoincidencias?.let { "${numero(it, 1)} %" })
                    Metrica(R.string.metrica_falsas_alarmas, s.falsasAlarmas?.toString())
                    Metrica(R.string.metrica_tiempo_aciertos, s.tiempoAciertosMs?.let { "${numero(it, 0)} ms" })
                }
            }
        }
    }
}

@Composable
private fun Metrica(@StringRes etiqueta: Int, valor: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(etiqueta), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.6f))
        Text(valor ?: stringResource(R.string.estado_sin_datos), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.4f))
    }
}

/** "#7 · ROJO en tinta AZUL · No coincidente · Respuesta: AZUL · Correcto · 812 ms" */
@Composable
private fun FilaEnsayo(e: FilaEnsayoUi, tipo: TipoPrueba) {
    val estados = LocalEstadoColores.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row {
            Text("#${e.numero}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                val estimulo = when (tipo) {
                    TipoPrueba.STROOP -> stringResource(R.string.medico_estimulo_stroop, nombreColor(e.estimulo), nombreColor(e.tinta.orEmpty()))
                    TipoPrueba.NBACK -> nombreForma(e.estimulo)
                }
                val clase = when (tipo) {
                    TipoPrueba.STROOP -> stringResource(if (e.coincidente) R.string.medico_coincidente else R.string.medico_no_coincidente)
                    TipoPrueba.NBACK -> stringResource(if (e.coincidente) R.string.medico_era_coincidencia else R.string.medico_no_era_coincidencia)
                }
                val respuesta = when (tipo) {
                    TipoPrueba.STROOP -> e.respuesta?.let { stringResource(R.string.medico_respuesta, nombreColor(it)) } ?: stringResource(R.string.feedback_sin_respuesta)
                    TipoPrueba.NBACK -> stringResource(if (e.pulsado) R.string.medico_pulso else R.string.medico_no_pulso)
                }
                Text("$estimulo · $clase", style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(respuesta, e.tiempoMs?.let { "$it ms" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(if (e.correcto) R.string.feedback_correcto else R.string.feedback_incorrecto),
                style = MaterialTheme.typography.labelLarge,
                color = if (e.correcto) estados.normal else estados.alerta,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun nombreColor(codigo: String): String = stringResource(
    when (codigo) {
        "ROJO" -> R.string.color_rojo
        "AZUL" -> R.string.color_azul
        "VERDE" -> R.string.color_verde
        "AMARILLO" -> R.string.color_amarillo
        else -> R.string.estado_sin_datos
    },
)

@Composable
private fun nombreForma(codigo: String): String =
    FormaNBack.entries.firstOrNull { it.name == codigo }?.let { stringResource(nombre(it)) } ?: codigo
