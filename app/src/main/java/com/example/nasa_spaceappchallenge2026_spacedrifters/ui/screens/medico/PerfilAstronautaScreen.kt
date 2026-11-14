package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.NotaMedica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.DatoNoche
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.DatoSesion
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ExplicacionAlerta
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.UmbralRegla
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Tendencia
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaPrioridad
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.etiquetaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.fechaHora
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.formatoMetrica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreCargo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombrePrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.numero
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.registro.nombreSintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Lo que el médico puede hacer desde el perfil. */
class AccionesPerfil(
    val confirmarAlerta: (Long) -> Unit = {},
    val pedirOtraPrueba: (Long) -> Unit = {},
    val programarPrueba: (TipoPrueba) -> Unit = {},
    val recalcularLineaBase: () -> Unit = {},
    val anadirNota: (String) -> Unit = {},
    val mensajeVisto: () -> Unit = {},
)

/** Perfil de un astronauta para el médico. De momento, una pestaña: "Pruebas cognitivas". */
@Composable
fun PerfilAstronautaScreen(
    onAtras: () -> Unit,
    onSesion: (Long) -> Unit,
    viewModel: PerfilAstronautaViewModel = viewModel(factory = AppViewModelProvider.Factory),
    fisiologicoVm: FisiologicoMedicoViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val perfil by viewModel.perfil.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val fisiologico by fisiologicoVm.fisiologico.collectAsStateWithLifecycle()
    val notas by fisiologicoVm.notas.collectAsStateWithLifecycle()
    val mensajeFisio by fisiologicoVm.mensaje.collectAsStateWithLifecycle()
    PerfilContenido(
        perfil = perfil,
        mensaje = mensaje,
        onAtras = onAtras,
        onSesion = onSesion,
        pestanaFisiologica = {
            PestanaFisiologica(
                datos = fisiologico,
                notas = notas,
                mensaje = mensajeFisio,
                acciones = AccionesFisiologico(
                    confirmarNivel = fisiologicoVm::confirmarNivel,
                    bajarNivel = fisiologicoVm::bajarNivel,
                    pedirPrueba = fisiologicoVm::pedirPrueba,
                    anadirIndicacion = fisiologicoVm::anadirIndicacion,
                    mensajeVisto = fisiologicoVm::mensajeVisto,
                ),
            )
        },
        acciones = AccionesPerfil(
            confirmarAlerta = viewModel::confirmarAlerta,
            pedirOtraPrueba = viewModel::pedirOtraPrueba,
            programarPrueba = viewModel::programarPrueba,
            recalcularLineaBase = viewModel::recalcularLineaBase,
            anadirNota = viewModel::anadirNota,
            mensajeVisto = viewModel::mensajeVisto,
        ),
    )
}

@Composable
private fun PerfilContenido(
    perfil: PerfilMedicoUi,
    mensaje: Int?,
    onAtras: () -> Unit,
    onSesion: (Long) -> Unit,
    acciones: AccionesPerfil,
    ahora: Long = remember { System.currentTimeMillis() },
    pestanaFisiologica: @Composable () -> Unit = {},
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    PantallaBase(titulo = perfil.nombre ?: stringResource(R.string.medico_perfil_titulo), onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = listOfNotNull(
                    perfil.cargo?.let { stringResource(nombreCargo(it)) },
                    perfil.diaMision?.let { stringResource(R.string.panel_dia_mision, it) },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // Pestañas del perfil: el resto del equipo puede añadir las suyas.
            PrimaryTabRow(selectedTabIndex = pestana, containerColor = MaterialTheme.colorScheme.background) {
                Tab(
                    selected = pestana == 0,
                    onClick = { pestana = 0 },
                    text = { Text(stringResource(R.string.medico_pestana_cognitivas)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                Tab(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    text = { Text(stringResource(R.string.medico_pestana_fisiologico)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            if (pestana == 0) PestanaPruebasCognitivas(perfil, mensaje, onSesion, acciones, ahora)
            else pestanaFisiologica()
        }
    }
}

@Composable
private fun PestanaPruebasCognitivas(
    perfil: PerfilMedicoUi,
    mensaje: Int?,
    onSesion: (Long) -> Unit,
    acciones: AccionesPerfil,
    ahora: Long,
) {
    if (mensaje != null) {
        LaunchedEffect(mensaje) {
            delay(3_000)
            acciones.mensajeVisto()
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (mensaje != null) {
            item { Text(stringResource(mensaje), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        }
        perfil.enCamino?.let { item { AvisoEnCamino(it) } }
        items(perfil.pruebas, key = { "prueba-${it.tipo}" }) { TarjetaPrueba(it, ahora) }
        item { SeccionAlertas(perfil.alertas, acciones) }
        item { SeccionAcciones(perfil.pruebasPendientes, acciones) }
        if (perfil.notas.isNotEmpty()) item { SeccionNotas(perfil.notas) }
        item { Text(stringResource(R.string.medico_sesiones_titulo), style = MaterialTheme.typography.titleLarge) }
        if (!perfil.cargando && perfil.sesiones.isEmpty()) {
            item { Text(stringResource(R.string.medico_sin_sesiones), style = MaterialTheme.typography.bodyMedium) }
        }
        items(perfil.sesiones, key = { "sesion-${it.sesionId}" }) { FilaSesion(it, onClick = { onSesion(it.sesionId) }) }
    }
}

// ---- Datos en camino ----

@Composable
private fun AvisoEnCamino(enCamino: EnCaminoUi) {
    val color = LocalEstadoColores.current.vigilar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color, MaterialTheme.shapes.medium)
            .padding(12.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.medico_en_camino, enCamino.sesiones, enCamino.sesiones, enCamino.minutosHastaLaPrimera),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}

// ---- Tarjeta de resumen + gráfica de 30 días ----

@Composable
private fun TarjetaPrueba(p: PruebaMedicoUi, ahora: Long) {
    val zona = remember { ZoneId.systemDefault() }
    val desde = remember(ahora) {
        java.time.Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate().minusDays(29).atStartOfDay(zona).toInstant().toEpochMilli()
    }
    val nombre = stringResource(nombrePrueba(p.tipo))
    val sinDatos = stringResource(R.string.estado_sin_datos)
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nombre, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                MarcaEstado(p.ultimoEstado ?: EstadoCognitivo.SIN_DATOS)
            }
            Dato(R.string.medico_ultima_sesion, p.ultimaFecha?.let { fechaHora(it) } ?: sinDatos)
            Dato(etiquetaMetrica(p.tipo), p.ultimoValor?.let { formatoMetrica(p.tipo, it) } ?: sinDatos)
            Dato(R.string.medico_tendencia, stringResource(textoTendencia(p.tendencia)))
            Dato(
                R.string.medico_linea_base,
                if (p.media != null && p.desviacion != null) rangoTexto(p.tipo, p.media, p.desviacion) else stringResource(R.string.estado_sin_linea_base),
            )

            val escala = escalaMedico(p.puntos, p.media, p.desviacion)
            GraficaMedico(
                puntos = p.puntos,
                media = p.media,
                desviacion = p.desviacion,
                escala = escala,
                desde = desde,
                hasta = ahora,
                descripcion = pluralStringResource(R.plurals.medico_cd_grafica, p.puntos.size, nombre, p.puntos.size),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(fechaCorta(desde), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.grafica_hoy), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = stringResource(R.string.medico_escala, formatoMetrica(p.tipo, escala.minimo), formatoMetrica(p.tipo, escala.maximo)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Leyenda(conRango = p.media != null)
        }
    }
}

@Composable
private fun Leyenda(conRango: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
            Text(stringResource(R.string.medico_leyenda_mision), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).border(2.dp, MaterialTheme.colorScheme.onSurface))
            Text(stringResource(R.string.medico_leyenda_linea_base), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (conRango) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 18.dp, height = 10.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)))
                Text(stringResource(R.string.medico_leyenda_rango), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Dato(@StringRes etiqueta: Int, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(etiqueta), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
    }
}

@StringRes
private fun etiquetaMetrica(tipo: TipoPrueba) = when (tipo) {
    TipoPrueba.STROOP -> R.string.medico_metrica_stroop
    TipoPrueba.NBACK -> R.string.medico_metrica_nback
}

@StringRes
private fun textoTendencia(t: Tendencia.Sentido) = when (t) {
    Tendencia.Sentido.MEJORANDO -> R.string.tendencia_mejorando
    Tendencia.Sentido.ESTABLE -> R.string.tendencia_estable
    Tendencia.Sentido.EMPEORANDO -> R.string.tendencia_empeorando
    Tendencia.Sentido.SIN_DATOS -> R.string.estado_sin_datos
}

/** "85 ± 12 ms" o "d′ 2,88 ± 0,29". */
private fun rangoTexto(tipo: TipoPrueba, media: Double, desviacion: Double): String = when (tipo) {
    TipoPrueba.STROOP -> "${numero(media, 0)} ± ${numero(desviacion, 0)} ms"
    TipoPrueba.NBACK -> "d′ ${numero(media, 2)} ± ${numero(desviacion, 2)}"
}

private fun fechaCorta(instante: Long): String =
    java.time.Instant.ofEpochMilli(instante).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

// ---- Alertas activas ----

@Composable
private fun SeccionAlertas(alertas: List<AlertaMedicoUi>, acciones: AccionesPerfil) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.medico_alertas_titulo), style = MaterialTheme.typography.titleLarge)
        if (alertas.isEmpty()) {
            Text(stringResource(R.string.medico_sin_alertas_activas), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        alertas.forEach { TarjetaAlerta(it, acciones) }
    }
}

@Composable
private fun TarjetaAlerta(a: AlertaMedicoUi, acciones: AccionesPerfil) {
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            a.prioridad?.let { MarcaPrioridad(it) }
            Text(stringResource(tituloReglaMedico(a.reglaId)), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.medico_generada, fechaHora(a.generadaEn)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            a.explicacion?.let { PorQueHaSaltado(a.reglaId, it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = { acciones.pedirOtraPrueba(a.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medico_pedir_otra_prueba))
                }
                Button(onClick = { acciones.confirmarAlerta(a.id) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medico_confirmar))
                }
            }
        }
    }
}

/** "Por qué ha saltado": regla, umbral, datos y fuente. */
@Composable
private fun PorQueHaSaltado(reglaId: String, e: ExplicacionAlerta) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.medico_por_que), style = MaterialTheme.typography.labelLarge)
        LineaPorQue(R.string.medico_por_que_regla, reglaId)
        LineaPorQue(R.string.medico_por_que_umbral, textoUmbral(e.umbral))
        e.sesiones.forEach { LineaPorQue(R.string.medico_por_que_dato, textoSesion(it)) }
        if (e.noches.isNotEmpty()) {
            LineaPorQue(
                R.string.medico_por_que_dato,
                e.noches.map { textoNoche(it) }.joinToString("; ") +
                    (e.suenoHabitual?.let { " " + stringResource(R.string.medico_sueno_habitual, numero(it, 1)) } ?: ""),
            )
        }
        if (e.sintomas.isNotEmpty()) LineaPorQue(R.string.medico_por_que_dato, nombresSintomas(e.sintomas))
        LineaPorQue(R.string.medico_por_que_fuente, e.fuentes.map { nombreFuente(it) }.joinToString(", "))
    }
}

@Composable
private fun LineaPorQue(@StringRes etiqueta: Int, valor: String) {
    Text("${stringResource(etiqueta)}: $valor", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun textoUmbral(u: UmbralRegla): String {
    val estados = u.estados.mapNotNull { n -> EstadoCognitivo.entries.firstOrNull { it.name == n } }
        .map { stringResource(etiquetaEstado(it)) }
        .joinToString(stringResource(R.string.medico_o))
    return buildList {
        if (u.sesionesSeguidas > 0) add(pluralStringResource(R.plurals.medico_umbral_seguidas, u.sesionesSeguidas, u.sesionesSeguidas, estados))
        else add(stringResource(R.string.medico_umbral_estado, estados))
        if (u.nochesBajoBase > 0) add(pluralStringResource(R.plurals.medico_umbral_noches, u.nochesBajoBase, u.nochesBajoBase))
        if (u.sintomas.isNotEmpty()) add(stringResource(R.string.medico_umbral_sintomas, nombresSintomas(u.sintomas)))
    }.joinToString(" + ")
}

@Composable
private fun textoSesion(d: DatoSesion): String {
    val tipo = TipoPrueba.entries.firstOrNull { it.name == d.prueba }
    val estado = EstadoCognitivo.entries.firstOrNull { it.name == d.estado }
    return listOfNotNull(
        fechaIso(d.fecha),
        tipo?.let { stringResource(nombrePrueba(it)) },
        if (tipo != null && d.valor != null) formatoMetrica(tipo, d.valor) else null,
        estado?.let { stringResource(etiquetaEstado(it)) },
    ).joinToString(" · ")
}

@Composable
private fun textoNoche(n: DatoNoche): String =
    "${fechaIso(n.fecha)}: ${n.horas?.let { stringResource(R.string.registro_horas, numero(it, 1)) } ?: stringResource(R.string.estado_sin_datos)}"

@Composable
private fun nombresSintomas(codigos: List<String>): String =
    codigos.mapNotNull { c -> Sintoma.entries.firstOrNull { it.name == c } }.map { stringResource(nombreSintoma(it)) }.joinToString(", ")

@Composable
private fun nombreFuente(codigo: String): String = when (codigo) {
    TipoPrueba.STROOP.name -> stringResource(R.string.stroop_nombre)
    TipoPrueba.NBACK.name -> stringResource(R.string.nback_nombre)
    AlertasRepository.FUENTE_REGISTRO -> stringResource(R.string.medico_fuente_registro)
    else -> codigo
}

private fun fechaIso(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrDefault(iso)

@StringRes
private fun tituloReglaMedico(reglaId: String): Int = when (reglaId) {
    "stroop-tendencia" -> R.string.medico_regla_stroop_tendencia
    "nback-tendencia" -> R.string.medico_regla_nback_tendencia
    "fatiga-cognitiva-combinada" -> R.string.medico_regla_fatiga
    "concentracion-sintomas" -> R.string.medico_regla_concentracion
    else -> R.string.regla_generica
}

// ---- Acciones del médico ----

@Composable
private fun SeccionAcciones(pendientes: List<TipoPrueba>, acciones: AccionesPerfil) {
    var elegirPrueba by rememberSaveable { mutableStateOf(false) }
    var escribirNota by rememberSaveable { mutableStateOf(false) }
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.medico_acciones_titulo), style = MaterialTheme.typography.titleLarge)
            pendientes.forEach {
                Text(
                    stringResource(R.string.medico_prueba_pendiente, stringResource(nombrePrueba(it))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalEstadoColores.current.vigilar,
                )
            }
            OutlinedButton(onClick = { elegirPrueba = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.medico_programar_prueba))
            }
            OutlinedButton(onClick = acciones.recalcularLineaBase, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.medico_recalcular_linea_base))
            }
            OutlinedButton(onClick = { escribirNota = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.medico_anadir_nota))
            }
        }
    }

    if (elegirPrueba) {
        AlertDialog(
            onDismissRequest = { elegirPrueba = false },
            title = { Text(stringResource(R.string.medico_programar_prueba)) },
            text = { Text(stringResource(R.string.medico_programar_texto)) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoPrueba.entries.forEach { tipo ->
                        TextButton(onClick = { acciones.programarPrueba(tipo); elegirPrueba = false }) {
                            Text(stringResource(nombrePrueba(tipo)))
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { elegirPrueba = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
    if (escribirNota) {
        var texto by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { escribirNota = false },
            title = { Text(stringResource(R.string.medico_anadir_nota)) },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text(stringResource(R.string.medico_nota_etiqueta)) },
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(enabled = texto.isNotBlank(), onClick = { acciones.anadirNota(texto); escribirNota = false }) {
                    Text(stringResource(R.string.guardar))
                }
            },
            dismissButton = { TextButton(onClick = { escribirNota = false }) { Text(stringResource(R.string.cancelar)) } },
        )
    }
}

@Composable
private fun SeccionNotas(notas: List<NotaMedica>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.medico_notas_titulo), style = MaterialTheme.typography.titleLarge)
        notas.forEach {
            Column {
                Text(fechaHora(it.creadaEn), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(it.texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---- Tabla de sesiones ----

@Composable
private fun FilaSesion(f: FilaSesionUi, onClick: () -> Unit) {
    val sinDatos = stringResource(R.string.estado_sin_datos)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = listOfNotNull(
                    fechaHora(f.fechaEn),
                    stringResource(nombrePrueba(f.tipo)),
                    if (f.esLineaBase) stringResource(R.string.medico_linea_base) else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            when {
                !f.valida -> Text(stringResource(R.string.medico_no_valida), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> MarcaEstado(f.estado ?: EstadoCognitivo.SIN_DATOS)
            }
        }
        Text(
            text = listOf(
                f.valor?.let { formatoMetrica(f.tipo, it) } ?: sinDatos,
                stringResource(R.string.medico_aciertos, f.aciertos?.let { numero(it, 0) } ?: "–"),
                stringResource(if (f.valida) R.string.medico_valida_si else R.string.medico_valida_no),
                stringResource(R.string.medico_sueno_kss, f.horasSueno?.let { numero(it, 1) } ?: "–", f.somnolencia?.toString() ?: "–"),
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 6.dp))
    }
}

// ---- Vista previa con datos inventados ----

@Preview(heightDp = 1800)
@Composable
private fun PerfilPreview() {
    val ahora = LocalDate.of(2026, 11, 14).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val dia = 86_400_000L
    val puntos = List(10) { i ->
        PuntoMedico(i.toLong(), ahora - (9 - i) * dia, 2.9 - (if (i > 6) (i - 6) * 0.25 else 0.0), if (i > 7) EstadoCognitivo.ALERTA else EstadoCognitivo.NORMAL, i < 2)
    }
    SpaceDriftersTheme {
        PerfilContenido(
            perfil = PerfilMedicoUi(
                cargando = false,
                nombre = "Maya Okafor",
                cargo = CargoTripulante.COMANDANTE,
                diaMision = 14,
                enCamino = EnCaminoUi(1, 12),
                pruebas = listOf(
                    PruebaMedicoUi(TipoPrueba.NBACK, ahora, 2.24, EstadoCognitivo.ALERTA, Tendencia.Sentido.EMPEORANDO, 2.88, 0.29, puntos),
                ),
                alertas = listOf(AlertaMedicoUi(1, "nback-tendencia", Regla.Prioridad.HOY, ahora, null)),
                sesiones = listOf(FilaSesionUi(1, TipoPrueba.NBACK, ahora, 2.24, 75.0, EstadoCognitivo.ALERTA, true, false, 5.0, 8)),
            ),
            mensaje = null,
            onAtras = {},
            onSesion = {},
            acciones = AccionesPerfil(),
            ahora = ahora,
        )
    }
}
