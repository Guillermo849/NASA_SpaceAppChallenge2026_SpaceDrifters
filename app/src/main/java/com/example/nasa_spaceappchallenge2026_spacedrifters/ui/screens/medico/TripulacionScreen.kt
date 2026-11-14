package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.etiquetaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreCargo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Tripulación vista por el médico de vuelo desde la Tierra. */
@Composable
fun TripulacionScreen(
    onAtras: () -> Unit,
    onAstronauta: (Long) -> Unit,
    viewModel: TripulacionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tripulacion by viewModel.tripulacion.collectAsStateWithLifecycle()
    TripulacionContenido(tripulacion, onAtras, onAstronauta)
}

@Composable
private fun TripulacionContenido(
    tripulacion: List<FilaTripulanteUi>?,
    onAtras: () -> Unit,
    onAstronauta: (Long) -> Unit,
) {
    PantallaBase(
        titulo = stringResource(R.string.medico_tripulacion_titulo),
        onAtras = onAtras,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.medico_tripulacion_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tripulacion != null && tripulacion.isEmpty()) {
                item { Text(stringResource(R.string.medico_sin_tripulacion), style = MaterialTheme.typography.bodyLarge) }
            }
            items(tripulacion.orEmpty(), key = { it.id }) { fila ->
                TarjetaTripulante(fila, onClick = { onAstronauta(fila.id) })
            }
        }
    }
}

@Composable
private fun TarjetaTripulante(fila: FilaTripulanteUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = fila.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = fila.cargo?.let { stringResource(nombreCargo(it)) } ?: stringResource(R.string.estado_sin_datos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EstadoPrueba(R.string.stroop_nombre, fila.ultimoStroop)
                EstadoPrueba(R.string.nback_nombre, fila.ultimoNBack)
                val estados = LocalEstadoColores.current
                Text(
                    text = if (fila.alertasActivas == 0) {
                        stringResource(R.string.medico_sin_alertas)
                    } else {
                        pluralStringResource(R.plurals.medico_alertas_activas, fila.alertasActivas, fila.alertasActivas)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (fila.alertasActivas == 0) MaterialTheme.colorScheme.onSurfaceVariant else estados.alerta,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_siguiente),
                contentDescription = stringResource(R.string.cd_entrar),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Stroop · Normal" con su color; sin sesiones, "Sin datos". */
@Composable
private fun EstadoPrueba(nombre: Int, estado: EstadoCognitivo?) {
    val e = estado ?: EstadoCognitivo.SIN_DATOS
    MarcaEstado(estado = e, texto = "${stringResource(nombre)} · ${stringResource(etiquetaEstado(e))}")
}

@Preview
@Composable
private fun TripulacionPreview() {
    SpaceDriftersTheme {
        TripulacionContenido(
            tripulacion = listOf(
                FilaTripulanteUi(1, "Maya Okafor", CargoTripulante.COMANDANTE, 2, EstadoCognitivo.NORMAL, EstadoCognitivo.ALERTA),
                FilaTripulanteUi(3, "Ane Etxeberria", CargoTripulante.OFICIAL_MEDICO, 0, EstadoCognitivo.NORMAL, EstadoCognitivo.NORMAL),
            ),
            onAtras = {},
            onAstronauta = {},
        )
    }
}
