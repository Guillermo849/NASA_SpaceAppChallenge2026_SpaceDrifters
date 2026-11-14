package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.constantes

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ConstantesRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ProtocoloTocado
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ResultadoConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.ReglasConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.ReglasConstantes.Estado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoDe
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Tomar constantes (FC, tensión, SpO2, FR) de un astronauta, con síntomas y contexto. */
@Composable
fun ConstantesScreen(
    onAtras: () -> Unit,
    viewModel: ConstantesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    ConstantesContenido(
        ui = ui,
        retrasoMin = viewModel.retrasoMin,
        acciones = AccionesConstantes(
            onAtras = onAtras,
            onEscribir = viewModel::escribir,
            onSintoma = viewModel::alternarSintoma,
            onSinSintomas = viewModel::sinSintomas,
            onEjercicio = viewModel::alternarEjercicio,
            onEstres = viewModel::alternarEstres,
            onGuardar = viewModel::guardar,
        ),
    )
}

private data class AccionesConstantes(
    val onAtras: () -> Unit = {},
    val onEscribir: (String, String) -> Unit = { _, _ -> },
    val onSintoma: (String) -> Unit = {},
    val onSinSintomas: () -> Unit = {},
    val onEjercicio: () -> Unit = {},
    val onEstres: () -> Unit = {},
    val onGuardar: () -> Unit = {},
)

@Composable
private fun ConstantesContenido(ui: ConstantesUi, retrasoMin: Long, acciones: AccionesConstantes) {
    PantallaBase(titulo = stringResource(R.string.constantes_titulo), onAtras = acciones.onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ui.nombre?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
            val resultado = ui.resultado
            if (resultado == null) Formulario(ui, acciones) else Resultado(ui.guardadas, resultado, retrasoMin, acciones.onAtras)
        }
    }
}

// ---- Formulario ----

@Composable
private fun Formulario(ui: ConstantesUi, acciones: AccionesConstantes) {
    Text(
        stringResource(R.string.constantes_ayuda),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Campo(ReglasConstantes.FC, ui, acciones.onEscribir, Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Campo(ReglasConstantes.PAS, ui, acciones.onEscribir, Modifier.weight(1f))
        Campo(ReglasConstantes.PAD, ui, acciones.onEscribir, Modifier.weight(1f))
    }
    Campo(ReglasConstantes.SPO2, ui, acciones.onEscribir, Modifier.fillMaxWidth())
    Campo(ReglasConstantes.FR, ui, acciones.onEscribir, Modifier.fillMaxWidth())

    Text(stringResource(R.string.constantes_sintomas_pregunta), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = ui.sintomas.isEmpty(),
            onClick = acciones.onSinSintomas,
            label = { Text(stringResource(R.string.sintomas_ninguno)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        ConstantesRepository.SINTOMAS.forEach { s ->
            FilterChip(
                selected = s in ui.sintomas,
                onClick = { acciones.onSintoma(s) },
                label = { Text(textoDe(nombreSintomaConstantes(s), s)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
    Text(stringResource(R.string.constantes_contexto_pregunta), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = ui.ejercicioUltimaHora,
            onClick = acciones.onEjercicio,
            label = { Text(stringResource(R.string.constantes_contexto_ejercicio)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
            selected = ui.estresUltimaHora,
            onClick = acciones.onEstres,
            label = { Text(stringResource(R.string.constantes_contexto_estres)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }

    ui.error?.let {
        Text(
            stringResource(textoError(it)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Button(
        onClick = acciones.onGuardar,
        enabled = !ui.guardando,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(stringResource(R.string.constantes_guardar))
    }
}

@Composable
private fun Campo(metrica: String, ui: ConstantesUi, onEscribir: (String, String) -> Unit, modifier: Modifier) {
    val mal = metrica in ui.camposMal
    val posible = ReglasConstantes.posible(metrica)
    OutlinedTextField(
        value = ui.campos[metrica].orEmpty(),
        onValueChange = { onEscribir(metrica, it) },
        label = { Text(stringResource(nombreConstante(metrica))) },
        suffix = { Text(stringResource(unidadConstante(metrica))) },
        isError = mal,
        supportingText = if (mal) {
            { Text(stringResource(R.string.constantes_rango_posible, posible[0], posible[1])) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

// ---- Resultado ----

@Composable
private fun Resultado(guardadas: Map<String, Int>, r: ResultadoConstantes, retrasoMin: Long, onVolver: () -> Unit) {
    val colores = LocalEstadoColores.current
    Text(stringResource(R.string.constantes_guardadas), style = MaterialTheme.typography.titleLarge)
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CAMPOS_CONSTANTES.filter { it in guardadas }.forEach { metrica ->
                val estado = r.estados[metrica]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stringResource(nombreConstante(metrica))}: ${guardadas.getValue(metrica)} ${stringResource(unidadConstante(metrica))}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(textoEstado(estado)),
                        style = MaterialTheme.typography.labelLarge,
                        color = when (estado) {
                            Estado.ALTO, Estado.BAJO -> colores.alerta
                            Estado.DENTRO -> colores.normal
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }

    if (r.protocolos.isNotEmpty()) {
        Text(stringResource(R.string.constantes_protocolos), style = MaterialTheme.typography.titleMedium)
        r.protocolos.forEach { LineaProtocolo(it) }
        Text(stringResource(R.string.constantes_mira_panel), style = MaterialTheme.typography.bodyMedium)
    } else if (r.estados.values.any { it != Estado.DENTRO }) {
        Text(stringResource(R.string.constantes_fuera_sin_protocolo), style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        stringResource(R.string.constantes_llega_medico, retrasoMin),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onVolver, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(stringResource(R.string.bienestar_volver))
    }
}

@Composable
private fun LineaProtocolo(p: ProtocoloTocado) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(
                if (p.nuevo) R.string.constantes_protocolo_nuevo else R.string.constantes_protocolo_sigue,
                textoDe(nombreProtocolo(p.protocoloId), p.protocoloId),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        MarcaNivel(p.nivel)
    }
}

// ---- Textos ----

@StringRes
fun nombreConstante(metrica: String): Int = when (metrica) {
    ReglasConstantes.FC -> R.string.constante_fc
    ReglasConstantes.PAS -> R.string.constante_pas
    ReglasConstantes.PAD -> R.string.constante_pad
    ReglasConstantes.SPO2 -> R.string.constante_spo2
    else -> R.string.constante_fr
}

@StringRes
fun unidadConstante(metrica: String): Int = when (metrica) {
    ReglasConstantes.FC -> R.string.unidad_lpm
    ReglasConstantes.PAS, ReglasConstantes.PAD -> R.string.unidad_mmhg
    ReglasConstantes.SPO2 -> R.string.unidad_porcentaje
    else -> R.string.unidad_rpm
}

@StringRes
private fun textoEstado(e: Estado?): Int = when (e) {
    Estado.ALTO -> R.string.constantes_estado_alto
    Estado.BAJO -> R.string.constantes_estado_bajo
    Estado.DENTRO -> R.string.constantes_estado_dentro
    null -> R.string.constantes_estado_sin_referencia
}

@StringRes
private fun textoError(e: ErrorConstantes): Int = when (e) {
    ErrorConstantes.NINGUNA -> R.string.constantes_error_ninguna
    ErrorConstantes.FUERA_DE_LO_POSIBLE -> R.string.constantes_error_imposible
    ErrorConstantes.TENSION_INCOHERENTE -> R.string.constantes_error_tension
    ErrorConstantes.AL_GUARDAR -> R.string.registro_error
}

/** Síntomas que se preguntan al tomar las constantes, en lenguaje sencillo. */
@StringRes
fun nombreSintomaConstantes(codigo: String): Int? = when (codigo) {
    "MAREO" -> R.string.sintoma_c_mareo
    "FATIGA" -> R.string.sintoma_c_fatiga
    "PRESINCOPE" -> R.string.sintoma_c_presincope
    "CEFALEA" -> R.string.sintoma_c_cefalea
    "DISNEA" -> R.string.sintoma_c_disnea
    "EDEMA_FACIAL_CERVICAL" -> R.string.sintoma_c_edema
    else -> null
}

@Preview(heightDp = 1200)
@Composable
private fun ConstantesPreview() {
    SpaceDriftersTheme {
        ConstantesContenido(
            ui = ConstantesUi(
                nombre = "Ane Etxeberria",
                campos = mapOf("FC" to "118", "PAS" to "88", "PAD" to "60", "SPO2" to "", "FR" to "720"),
                camposMal = setOf("FR"),
                sintomas = setOf("MAREO"),
                error = ErrorConstantes.FUERA_DE_LO_POSIBLE,
            ),
            retrasoMin = 18,
            acciones = AccionesConstantes(),
        )
    }
}
