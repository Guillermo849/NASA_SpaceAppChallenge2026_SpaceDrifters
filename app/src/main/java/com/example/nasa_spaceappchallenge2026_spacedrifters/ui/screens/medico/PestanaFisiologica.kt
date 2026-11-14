package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.NotaMedica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoMedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.fechaHora
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreNodo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreOpcionResultado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreParametroHemograma
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombrePruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.numero
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoDe
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoMotivo

/** Lo que el médico puede hacer desde la pestaña. */
data class AccionesFisiologico(
    val confirmarNivel: (Long) -> Unit = {},
    val bajarNivel: (Long, Int) -> Unit = { _, _ -> },
    val pedirPrueba: (Long, TipoPruebaClinica) -> Unit = { _, _ -> },
    val anadirIndicacion: (String) -> Unit = {},
    val mensajeVisto: () -> Unit = {},
)

/**
 * Pestaña "Fisiológico" del perfil (paso 6): nivel actual, protocolos con su línea de tiempo y las
 * acciones del médico, constantes con su franja, comparación de pruebas e indicaciones.
 */
@Composable
fun PestanaFisiologica(
    datos: FisiologicoMedicoUi,
    notas: List<NotaMedica>,
    @StringRes mensaje: Int?,
    acciones: AccionesFisiologico,
) {
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            kotlinx.coroutines.delay(4_000)
            acciones.mensajeVisto()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        mensaje?.let { Text(stringResource(it), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }

        // Nivel actual (lo recibido en la Tierra).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.fisio_nivel_actual), style = MaterialTheme.typography.titleMedium)
            if (datos.nivelActual != null) MarcaNivel(datos.nivelActual)
            else Text(stringResource(R.string.fisio_sin_protocolos_abiertos), style = MaterialTheme.typography.bodyMedium)
        }

        Text(stringResource(R.string.fisio_protocolos), style = MaterialTheme.typography.titleLarge)
        if (datos.protocolos.isEmpty() && !datos.cargando) {
            Text(stringResource(R.string.fisio_sin_protocolos), style = MaterialTheme.typography.bodyMedium)
        }
        datos.protocolos.forEach { TarjetaProtocoloMedico(it, acciones) }

        Text(stringResource(R.string.fisio_constantes), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.fisio_constantes_explicacion, DIAS_LINEA_BASE_CONSTANTES),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        datos.series.forEach { TarjetaConstante(it) }

        Text(stringResource(R.string.fisio_comparacion), style = MaterialTheme.typography.titleLarge)
        if (datos.comparaciones.isEmpty()) {
            Text(stringResource(R.string.fisio_sin_comparacion), style = MaterialTheme.typography.bodyMedium)
        }
        datos.comparaciones.forEach { TarjetaComparacion(it) }

        SeccionIndicaciones(notas, acciones.anadirIndicacion)
    }
}

// ---- Protocolos ----

@Composable
private fun TarjetaProtocoloMedico(p: ProtocoloMedicoUi, acciones: AccionesFisiologico) {
    var bajar by rememberSaveable { mutableStateOf(false) }
    var pedir by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                textoDe(nombreProtocolo(p.protocolo.protocoloId), p.protocolo.protocoloId),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MarcaNivel(p.nivel)
                if (p.cerrado) Text(stringResource(R.string.fisio_cerrado), style = MaterialTheme.typography.labelLarge)
            }
            p.nivelPropuesto?.let {
                Text(stringResource(R.string.fisio_propone_bajar, it), style = MaterialTheme.typography.bodyMedium)
            }
            if (p.enCamino > 0) {
                Text(
                    pluralStringResource(R.plurals.fisio_en_camino, p.enCamino, p.enCamino),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (p.pendientes.isNotEmpty()) {
                val nombres = p.pendientes.map { stringResource(nombrePruebaClinica(it.tipo)) }
                Text(stringResource(R.string.fisio_pendientes, nombres.joinToString(", ")), style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()
            Text(stringResource(R.string.fisio_linea_tiempo), style = MaterialTheme.typography.titleSmall)
            p.tiempo.forEach { ItemTiempo(it) }

            if (!p.cerrado) {
                HorizontalDivider()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { acciones.confirmarNivel(p.protocolo.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.fisio_confirmar_nivel))
                    }
                    if (p.nivel > 1) {
                        OutlinedButton(onClick = { bajar = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.fisio_bajar_nivel))
                        }
                    }
                    if (p.protocolo.protocoloId != FisiologicoRepository.PROTOCOLO_EMERGENCIA) {
                        OutlinedButton(onClick = { pedir = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.fisio_pedir_prueba))
                        }
                    }
                }
            }
        }
    }
    if (bajar) {
        val opciones = (1 until p.nivel).toList().reversed()
        DialogoOpciones(
            titulo = stringResource(R.string.fisio_bajar_nivel),
            opciones = opciones,
            inicial = p.nivelPropuesto ?: opciones.first(),
            texto = { stringResource(R.string.nivel_con_numero, it, stringResource(nombreNivel(it))) },
            onCancelar = { bajar = false },
            onAceptar = { bajar = false; acciones.bajarNivel(p.protocolo.id, it) },
        )
    }
    if (pedir) {
        DialogoOpciones(
            titulo = stringResource(R.string.fisio_pedir_prueba),
            opciones = TipoPruebaClinica.entries.toList(),
            inicial = null,
            texto = { stringResource(nombrePruebaClinica(it)) },
            onCancelar = { pedir = false },
            onAceptar = { pedir = false; acciones.pedirPrueba(p.protocolo.id, it) },
        )
    }
}

@Composable
private fun ItemTiempo(item: ItemTiempoUi) {
    val hora = fechaHora(item.instante)
    val texto = when (item) {
        is ItemTiempoUi.CambioNivel -> {
            val cambio = if (item.nivelAntes == item.nivelDespues) {
                stringResource(R.string.fisio_t_nivel_igual, item.nivelDespues)
            } else {
                stringResource(R.string.fisio_t_nivel, item.nivelAntes, item.nivelDespues)
            }
            val quien = when (item.confirmadoPor) {
                null -> stringResource(R.string.fisio_t_automatico)
                FisiologicoMedicoRepository.CONFIRMA_MEDICO -> stringResource(R.string.fisio_t_medico)
                else -> stringResource(R.string.fisio_t_tripulacion)
            }
            listOf(cambio, textoDe(nombreNodo(item.nodo), item.nodo), stringResource(R.string.fisio_t_motivo, textoMotivo(item.motivo)), quien)
                .joinToString(" · ")
        }
        is ItemTiempoUi.PruebaPedida -> stringResource(R.string.fisio_t_pedida, stringResource(nombrePruebaClinica(item.tipo)))
        is ItemTiempoUi.PruebaHecha -> stringResource(
            R.string.fisio_t_resultado,
            stringResource(nombrePruebaClinica(item.prueba.tipo)),
            textoResultado(item.prueba),
        )
    }
    Column {
        Text(hora, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Resultado de una prueba en texto: la opción ("Flujo retrógrado") o VS y GC del ecocardio. */
@Composable
fun textoResultado(prueba: PruebaClinica): String {
    val valores = prueba.resultado?.let { FisiologicoRepository.deJson(it).comoMapa() } ?: return "–"
    if (prueba.tipo == TipoPruebaClinica.ECOCARDIO) {
        val vs = (valores[InterpretePruebas.VS_ML] as? Number)?.toDouble()
        val gc = (valores[InterpretePruebas.GC_LMIN] as? Number)?.toDouble()
        return stringResource(R.string.fisio_ecocardio_valores, vs?.let { numero(it, 0) } ?: "–", gc?.let { numero(it, 1) } ?: "–")
    }
    val opcion = InterpretePruebas.opcionDe(prueba.tipo.name, valores) ?: return "–"
    val texto = textoDe(nombreOpcionResultado(opcion), opcion)
    // Hemograma real de NASA: qué muestra es y qué parámetros salieron de rango.
    val muestra = valores[InterpretePruebas.MUESTRA_NASA] as? String ?: return texto
    val momento = muestra.substringAfter("_whole-blood_").substringBefore("_cbc")
    val fuera = (valores[InterpretePruebas.FUERA_DE_RANGO] as? String).orEmpty().split(',').filter { it.isNotBlank() }
        .map { textoDe(nombreParametroHemograma(it), it) }
    return listOfNotNull(
        texto,
        stringResource(R.string.hemo_resultado_muestra, muestra.substringBefore('_'), momento),
        fuera.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.hemo_resultado_fuera, it.joinToString(", ")) },
    ).joinToString(" · ")
}

// ---- Constantes ----

@StringRes
private fun nombreMetrica(m: MetricaConstante) = when (m) {
    MetricaConstante.FC -> R.string.fisio_fc
    MetricaConstante.PAS -> R.string.fisio_pas
    MetricaConstante.SPO2 -> R.string.fisio_spo2
    MetricaConstante.FR -> R.string.fisio_fr
}

@Composable
private fun TarjetaConstante(s: SerieConstanteUi) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(nombreMetrica(s.metrica)), style = MaterialTheme.typography.titleMedium)
            val ultimo = s.ultimo
            if (ultimo == null) {
                Text(stringResource(R.string.estado_sin_datos), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val decimales = if (s.metrica == MetricaConstante.SPO2) 1 else 0
            Text(
                stringResource(
                    if (s.fuera(ultimo)) R.string.fisio_ultimo_fuera else R.string.fisio_ultimo_dentro,
                    numero(ultimo, decimales),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (s.fuera(ultimo)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            val base = s.base
            val franja = s.franja
            if (base != null && franja != null) {
                Text(
                    stringResource(R.string.fisio_base_franja, numero(base, decimales), numero(franja[0], decimales), numero(franja[1], decimales)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val puntos = s.puntos.map { (t, v) ->
                PuntoMedico(0, t, v, if (s.fuera(v)) EstadoCognitivo.ALERTA else EstadoCognitivo.NORMAL, esLineaBase = false)
            }
            val media = base
            val semi = franja?.let { (it[1] - it[0]) / 2 }
            GraficaMedico(
                puntos = puntos,
                media = media,
                desviacion = semi,
                escala = escalaMedico(puntos, media, semi),
                desde = s.puntos.first().first,
                hasta = s.puntos.last().first,
                descripcion = stringResource(R.string.fisio_grafica_cd, stringResource(nombreMetrica(s.metrica))),
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
        }
    }
}

// ---- Comparación ----

@Composable
private fun TarjetaComparacion(c: ComparacionUi) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(nombrePruebaClinica(c.tipo)), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.fisio_ultima, fechaHora(c.ultima.realizadaEn ?: 0L), textoResultado(c.ultima)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                c.anterior?.let { stringResource(R.string.fisio_anterior, fechaHora(it.realizadaEn ?: 0L), textoResultado(it)) }
                    ?: stringResource(R.string.fisio_sin_anterior),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Indicaciones ----

@Composable
private fun SeccionIndicaciones(notas: List<NotaMedica>, onAnadir: (String) -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.fisio_indicaciones), style = MaterialTheme.typography.titleLarge)
    if (notas.isEmpty()) Text(stringResource(R.string.fisio_sin_indicaciones), style = MaterialTheme.typography.bodyMedium)
    notas.forEach { n ->
        Column {
            Text(fechaHora(n.creadaEn), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(n.texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
    OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.fisio_anadir_indicacion))
    }
    if (abierto) {
        var texto by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(stringResource(R.string.fisio_anadir_indicacion)) },
            text = {
                OutlinedTextField(value = texto, onValueChange = { texto = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            },
            confirmButton = {
                TextButton(onClick = { abierto = false; onAnadir(texto) }, enabled = texto.isNotBlank()) {
                    Text(stringResource(R.string.fisio_guardar))
                }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

/** Diálogo con una lista de opciones (una sola elegida). */
@Composable
private fun <T> DialogoOpciones(
    titulo: String,
    opciones: List<T>,
    inicial: T?,
    texto: @Composable (T) -> String,
    onCancelar: () -> Unit,
    onAceptar: (T) -> Unit,
) {
    var elegida by rememberSaveable { mutableStateOf(inicial?.let { opciones.indexOf(it) } ?: -1) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(Modifier.selectableGroup()) {
                opciones.forEachIndexed { i, o ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = i == elegida, role = Role.RadioButton) { elegida = i },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = i == elegida, onClick = null)
                        Text(texto(o), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { opciones.getOrNull(elegida)?.let(onAceptar) }, enabled = elegida >= 0) {
                Text(stringResource(R.string.fisio_aceptar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) } },
    )
}
