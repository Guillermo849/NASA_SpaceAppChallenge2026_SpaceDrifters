package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.EtiquetasUi
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ProtocoloEnCurso
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.colorNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreAccion
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombrePruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoDe
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Qué ocurre, en lenguaje sencillo, para el astronauta. */
@StringRes
private fun explicacionProtocolo(id: String): Int? = when (id) {
    "trombosis-yugular" -> R.string.explicacion_trombosis_yugular
    "hemodinamico" -> R.string.explicacion_hemodinamico
    "cognitivo-sueno" -> R.string.explicacion_cognitivo_sueno
    "cardiorrespiratorio" -> R.string.explicacion_cardiorrespiratorio
    else -> null
}

/**
 * Paso 5 del plan: si el astronauta tiene un protocolo abierto, una tarjeta arriba del panel con
 * el nivel (número + nombre + color), qué ocurre y lo que tiene que hacer.
 */
@Composable
fun TarjetaProtocoloAstronauta(p: ProtocoloEnCurso, modifier: Modifier = Modifier) {
    val color = colorNivel(p.protocolo.nivel)
    OutlinedCard(modifier = modifier.fillMaxWidth().testTag(EtiquetasUi.PROTOCOLO_ASTRONAUTA), border = BorderStroke(2.dp, color)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                textoDe(nombreProtocolo(p.protocolo.protocoloId), p.protocolo.protocoloId),
                style = MaterialTheme.typography.titleMedium,
            )
            MarcaNivel(p.protocolo.nivel)
            explicacionProtocolo(p.protocolo.protocoloId)?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium)
            }
            if (p.acciones.isNotEmpty()) {
                Text(stringResource(R.string.astronauta_protocolo_que_hacer), style = MaterialTheme.typography.titleSmall)
                p.acciones.forEach { a ->
                    Text("• " + textoDe(nombreAccion(a), a), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (p.pendientes.isNotEmpty()) {
                val pruebas = p.pendientes.map { stringResource(nombrePruebaClinica(it.tipo)) }
                Text(
                    stringResource(R.string.astronauta_protocolo_pruebas, pruebas.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


@Preview
@Composable
private fun TarjetaProtocoloAstronautaPreview() {
    SpaceDriftersTheme {
        TarjetaProtocoloAstronauta(
            ProtocoloEnCurso(
                protocolo = ProtocoloActivo(
                    id = 1, astronautaId = 1, protocoloId = "hemodinamico", nodoActual = "p2a-repetir", nivel = 3,
                    iniciadoEn = 0, actualizadoEn = 0, estado = EstadoProtocolo.ESPERANDO_PRUEBA,
                ),
                nombre = "Maya Okafor",
                pasos = emptyList(),
                acciones = listOf("REVISAR_BALANCE_HIDRICO_24H", "REPETIR_MEDICION_REPOSO_5_MIN"),
                pendientes = listOf(PruebaClinica(astronautaId = 1, tipo = TipoPruebaClinica.REPETICION_CONSTANTES, pedidaEn = 0)),
            ),
        )
    }
}
