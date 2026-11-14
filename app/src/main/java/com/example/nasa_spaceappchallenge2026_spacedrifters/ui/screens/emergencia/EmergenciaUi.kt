package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.emergencia

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.Emergencia
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.AvisoPrototipo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ModoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Texto de un signo de alarma de assets/protocolos.json (o de un dato que llevó a emergencia). */
@StringRes
fun nombreSignoAlarma(codigo: String): Int? = when (codigo) {
    "DOLOR_TORACICO" -> R.string.signo_dolor_toracico
    "SINCOPE" -> R.string.signo_sincope
    "DISNEA_GRAVE" -> R.string.signo_disnea_grave
    "ALTERACION_LENGUAJE" -> R.string.signo_alteracion_lenguaje
    "DISNEA" -> R.string.signo_disnea
    "DETERIORO_COGNITIVO_BRUSCO" -> R.string.signo_deterioro_cognitivo_brusco
    "MAREO_INTENSO" -> R.string.signo_mareo_intenso
    else -> null
}

/** Pasos del protocolo de emergencia, en orden. */
private val PASOS_EMERGENCIA = listOf(
    R.string.emergencia_paso_1,
    R.string.emergencia_paso_2,
    R.string.emergencia_paso_3,
    R.string.emergencia_paso_4,
    R.string.emergencia_paso_5,
)

/**
 * Se pone encima de toda la app (MainActivity): si algún astronauta tiene una emergencia sin
 * atender, la pantalla de emergencia ocupa todo, en cualquier pantalla y rol.
 */
@Composable
fun CapaEmergencia(viewModel: EmergenciaViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val emergencias by viewModel.emergencias.collectAsStateWithLifecycle()
    val emergencia = emergencias.firstOrNull() ?: return
    // No se puede salir con "atrás": solo marcándola como atendida.
    BackHandler {}
    ModoPrueba()
    PantallaEmergencia(
        emergencia = emergencia,
        otras = emergencias.size - 1,
        onAtendida = { viewModel.atender(emergencia.astronautaId) },
    )
}

@Composable
fun PantallaEmergencia(
    emergencia: Emergencia,
    otras: Int,
    onAtendida: () -> Unit,
    zona: ZoneId = ZoneId.systemDefault(),
) {
    val rojo = MaterialTheme.colorScheme.error
    var confirmar by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(rojo.copy(alpha = 0.14f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Nivel: número + nombre + color, nunca el color solo.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).background(rojo, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("5", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onError)
                    }
                    Column {
                        Text(
                            stringResource(R.string.emergencia_titulo),
                            style = MaterialTheme.typography.headlineMedium,
                            color = rojo,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.emergencia_nivel),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        emergencia.nombre ?: stringResource(R.string.emergencia_astronauta_desconocido),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val motivos = emergencia.motivos.map { m -> nombreSignoAlarma(m)?.let { stringResource(it) } ?: m }
                    Text(
                        stringResource(R.string.emergencia_motivo, motivos.joinToString(", ")),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    emergencia.desde?.let {
                        Text(
                            stringResource(R.string.emergencia_desde, hora(it, zona)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Pasos del protocolo de emergencia.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, rojo.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.emergencia_pasos_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    PASOS_EMERGENCIA.forEachIndexed { i, paso ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = rojo,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(stringResource(paso), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                // A la Tierra se avisa, pero no se espera su respuesta.
                Text(
                    emergencia.llegaTierraEn?.let { stringResource(R.string.emergencia_tierra, hora(it, zona)) }
                        ?: stringResource(R.string.emergencia_tierra_sin_hora),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (otras > 0) {
                    Text(
                        pluralStringResource(R.plurals.emergencia_otras, otras, otras),
                        style = MaterialTheme.typography.titleSmall,
                        color = rojo,
                    )
                }
            }

            Button(
                onClick = { confirmar = true },
                colors = ButtonDefaults.buttonColors(containerColor = rojo, contentColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.emergencia_atendida), style = MaterialTheme.typography.titleMedium)
            }
            AvisoPrototipo()
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text(stringResource(R.string.emergencia_atendida_confirmar_titulo)) },
            text = { Text(stringResource(R.string.emergencia_atendida_confirmar_texto)) },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onAtendida() }) {
                    Text(stringResource(R.string.emergencia_atendida_si))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text(stringResource(R.string.cancelar)) }
            },
        )
    }
}

private fun hora(instante: Long, zona: ZoneId): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(instante).atZone(zona))

/**
 * Botón grande "Tengo un síntoma de alarma" (panel del astronauta). Abre una lista para marcar
 * qué síntoma tiene; al confirmar, nivel 5 al instante.
 */
@Composable
fun BotonAlarma(signos: List<String>, onActivar: (Set<String>) -> Unit, modifier: Modifier = Modifier) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val rojo = MaterialTheme.colorScheme.error
    Button(
        onClick = { abierto = true },
        colors = ButtonDefaults.buttonColors(containerColor = rojo, contentColor = MaterialTheme.colorScheme.onError),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(stringResource(R.string.alarma_boton), style = MaterialTheme.typography.titleMedium)
    }
    if (abierto) {
        DialogoAlarma(signos, onCancelar = { abierto = false }, onActivar = { abierto = false; onActivar(it) })
    }
}

@Composable
private fun DialogoAlarma(signos: List<String>, onCancelar: () -> Unit, onActivar: (Set<String>) -> Unit) {
    var marcados by rememberSaveable { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.alarma_titulo)) },
        text = {
            Column {
                Text(stringResource(R.string.alarma_explicacion), style = MaterialTheme.typography.bodyMedium)
                signos.forEach { s ->
                    val marcado = s in marcados
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(marcado, role = Role.Checkbox) { marcados = if (it) marcados + s else marcados - s },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = marcado, onCheckedChange = null)
                        Text(
                            nombreSignoAlarma(s)?.let { stringResource(it) } ?: s,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onActivar(marcados) }, enabled = marcados.isNotEmpty()) {
                Text(stringResource(R.string.alarma_activar), color = if (marcados.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}

@Preview(heightDp = 900)
@Composable
private fun PantallaEmergenciaPreview() {
    SpaceDriftersTheme {
        PantallaEmergencia(
            Emergencia(2, "Leo Martín", listOf("DOLOR_TORACICO", "SINCOPE"), 0L, 18 * 60_000L),
            otras = 0,
            onAtendida = {},
            zona = ZoneId.of("UTC"),
        )
    }
}
