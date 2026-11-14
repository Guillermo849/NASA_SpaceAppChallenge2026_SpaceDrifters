package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.PasoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ProtocoloEnCurso
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.colorNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreAccion
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreNodo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombrePruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoDe
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Oficial médico de a bordo: protocolos abiertos de la tripulación, sus pasos y las pruebas pendientes. */
@Composable
fun OficialMedicoScreen(
    onAtras: () -> Unit,
    onPrueba: (Long) -> Unit,
    viewModel: OficialMedicoViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val protocolos by viewModel.protocolos.collectAsStateWithLifecycle()
    OficialMedicoContenido(protocolos, onAtras, onPrueba)
}

@Composable
private fun OficialMedicoContenido(
    protocolos: List<ProtocoloEnCurso>?,
    onAtras: () -> Unit,
    onPrueba: (Long) -> Unit,
) {
    PantallaBase(titulo = stringResource(R.string.oficial_titulo), onAtras = onAtras) { padding ->
        when {
            protocolos == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            protocolos.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.oficial_vacio), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(protocolos, key = { it.protocolo.id }) { TarjetaProtocolo(it, onPrueba) }
            }
        }
    }
}

@Composable
private fun TarjetaProtocolo(p: ProtocoloEnCurso, onPrueba: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Quién, qué protocolo y nivel (número + nombre + color).
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(p.nombre ?: stringResource(R.string.estado_sin_datos), style = MaterialTheme.typography.titleLarge)
                Text(
                    textoDe(nombreProtocolo(p.protocolo.protocoloId), p.protocolo.protocoloId),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MarcaNivel(p.protocolo.nivel)
                p.protocolo.nivelPropuesto?.let {
                    Text(
                        stringResource(R.string.oficial_propone_bajar, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.oficial_pendientes), style = MaterialTheme.typography.titleMedium)
            if (p.pendientes.isEmpty()) {
                Text(
                    stringResource(R.string.oficial_sin_pendientes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            p.pendientes.forEach { prueba -> FilaPruebaPendiente(prueba, onPrueba) }

            if (p.acciones.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(R.string.oficial_que_hacer), style = MaterialTheme.typography.titleMedium)
                p.acciones.forEach { a ->
                    Text("• " + textoDe(nombreAccion(a), a), style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.oficial_pasos), style = MaterialTheme.typography.titleMedium)
            PasosProtocolo(p.pasos)
        }
    }
}

@Composable
private fun FilaPruebaPendiente(prueba: PruebaClinica, onPrueba: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(nombrePruebaClinica(prueba.tipo)), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.oficial_pedida, hora(prueba.pedidaEn)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = { onPrueba(prueba.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.oficial_introducir))
        }
    }
}

/** Los pasos recorridos, como una línea de tiempo; el actual, marcado. */
@Composable
fun PasosProtocolo(pasos: List<PasoProtocolo>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pasos.forEach { paso ->
            val color = if (paso.actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(12.dp)
                        .then(
                            if (paso.actual) Modifier.background(color, CircleShape)
                            else Modifier.border(2.dp, color, CircleShape),
                        ),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        textoDe(nombreNodo(paso.nodo), paso.nodo),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (paso.actual) FontWeight.Bold else FontWeight.Normal,
                        color = if (paso.actual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(if (paso.actual) R.string.oficial_paso_actual else R.string.oficial_paso_hecho),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                        )
                        paso.nivel?.let {
                            Text(
                                stringResource(R.string.nivel_con_numero, it, stringResource(nombreNivel(it))),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorNivel(it),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hora(instante: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(instante).atZone(ZoneId.systemDefault()))

@Preview(heightDp = 1100)
@Composable
private fun OficialMedicoPreview() {
    SpaceDriftersTheme {
        OficialMedicoContenido(
            protocolos = listOf(
                ProtocoloEnCurso(
                    protocolo = ProtocoloActivo(
                        id = 1, astronautaId = 2, protocoloId = "trombosis-yugular", nodoActual = "p1-flujo-alterado",
                        nivel = 3, iniciadoEn = 0, actualizadoEn = 0, estado = EstadoProtocolo.ESPERANDO_PRUEBA,
                        recorrido = "p1-cefalea,p1-doppler,p1-resultado-doppler,p1-flujo-alterado",
                    ),
                    nombre = "Leo Martín",
                    pasos = listOf(
                        PasoProtocolo("p1-cefalea", null, 2, false),
                        PasoProtocolo("p1-doppler", null, 3, false),
                        PasoProtocolo("p1-flujo-alterado", null, 3, true),
                    ),
                    acciones = listOf("COMPARAR_ECO_DOPPLER_ANTERIOR", "PEDIR_ANALITICA"),
                    pendientes = listOf(
                        PruebaClinica(id = 1, astronautaId = 2, protocoloActivoId = 1, tipo = TipoPruebaClinica.DIMERO_D, pedidaEn = 0),
                        PruebaClinica(id = 2, astronautaId = 2, protocoloActivoId = 1, tipo = TipoPruebaClinica.HEMOGRAMA, pedidaEn = 0),
                    ),
                ),
            ),
            onAtras = {},
            onPrueba = {},
        )
    }
}
