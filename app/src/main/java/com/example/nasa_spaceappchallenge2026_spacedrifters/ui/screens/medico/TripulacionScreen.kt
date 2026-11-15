package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MarcaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.colorNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreNivel
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.colorEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.etiquetaEstado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreCargo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Panel del médico ("centro de control"): situación de la misión, resumen de la tripulación por
 * estado y una tarjeta por tripulante (lo más urgente primero) con sus pruebas cognitivas, protocolos,
 * avisos y constantes de los últimos días. Solo lo que ya ha llegado a la Tierra.
 */
@Composable
fun TripulacionScreen(
    onAtras: () -> Unit,
    onAstronauta: (Long) -> Unit,
    /** Protocolos a bordo: pruebas pendientes, resultados y pasos de cada protocolo. */
    onProtocolos: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    viewModel: TripulacionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val panel by viewModel.panel.collectAsStateWithLifecycle()
    TripulacionContenido(panel, onAtras, onAstronauta, onProtocolos, onCerrarSesion)
}

@Composable
private fun TripulacionContenido(
    panel: PanelMedicoUi?,
    onAtras: () -> Unit,
    onAstronauta: (Long) -> Unit,
    onProtocolos: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
) {
    PantallaBase(
        titulo = stringResource(R.string.medico_tripulacion_titulo),
        onAtras = onAtras,
        onCerrarSesion = onCerrarSesion,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (panel != null) {
                CabeceraMision(panel)
                ResumenTripulacion(panel)
            }
            TarjetaProtocolosABordo(panel?.conProtocolo ?: 0, onProtocolos)
            Text(stringResource(R.string.medico_panel_tripulacion), style = MaterialTheme.typography.titleLarge)
            if (panel != null && panel.tripulacion.isEmpty()) {
                Text(stringResource(R.string.medico_sin_tripulacion), style = MaterialTheme.typography.bodyLarge)
            }
            panel?.tripulacion.orEmpty().forEach { fila ->
                TarjetaTripulante(fila, onClick = { onAstronauta(fila.id) })
            }
        }
    }
}

// ---- Cabecera y resumen ----

@Composable
private fun CabeceraMision(panel: PanelMedicoUi) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.medico_panel_titulo), style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(
                    panel.diaMision?.let { stringResource(R.string.medico_panel_dia, it) },
                    stringResource(R.string.medico_panel_retraso, panel.retrasoMin),
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(
                    R.string.medico_panel_actualizado,
                    Instant.ofEpochMilli(panel.actualizadoEn).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Cuántos tripulantes hay en cada estado (número grande + texto) y una barra con el reparto. */
@Composable
private fun ResumenTripulacion(panel: PanelMedicoUi) {
    val estados = listOf(EstadoCognitivo.ALERTA, EstadoCognitivo.VIGILAR, EstadoCognitivo.NORMAL)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            estados.forEach { e ->
                FichaNumero(
                    numero = panel.cuantos(e),
                    texto = stringResource(etiquetaResumen(e)),
                    color = colorEstado(e),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        BarraReparto(panel)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FichaNumero(
                numero = panel.avisos,
                texto = stringResource(R.string.medico_panel_avisos),
                color = if (panel.avisos > 0) LocalEstadoColores.current.alerta else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            FichaNumero(
                numero = panel.conProtocolo,
                texto = stringResource(R.string.medico_panel_con_protocolo),
                color = if (panel.conProtocolo > 0) LocalEstadoColores.current.vigilar else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@StringRes
private fun etiquetaResumen(e: EstadoCognitivo): Int = when (e) {
    EstadoCognitivo.ALERTA -> R.string.medico_panel_en_alerta
    EstadoCognitivo.VIGILAR -> R.string.medico_panel_a_vigilar
    else -> R.string.medico_panel_normal
}

@Composable
private fun FichaNumero(numero: Int, texto: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(numero.toString(), style = MaterialTheme.typography.headlineMedium, color = color)
        Text(texto, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Reparto de la tripulación por estado, de un vistazo (los números ya están en las fichas). */
@Composable
private fun BarraReparto(panel: PanelMedicoUi) {
    val total = panel.tripulacion.size
    if (total == 0) return
    val tramos = listOf(EstadoCognitivo.ALERTA, EstadoCognitivo.VIGILAR, EstadoCognitivo.NORMAL, EstadoCognitivo.SIN_DATOS)
        .map { it to panel.cuantos(it) }
        .filter { it.second > 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tramos.forEach { (estado, n) ->
            Box(
                Modifier
                    .weight(n.toFloat())
                    .fillMaxSize()
                    .background(colorEstado(estado)),
            )
        }
    }
}

/** Acceso a los protocolos fisiológicos de toda la tripulación (pruebas pendientes y resultados). */
@Composable
private fun TarjetaProtocolosABordo(conProtocolo: Int, onAbrir: () -> Unit) {
    Card(
        onClick = onAbrir,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                Text(text = stringResource(R.string.oficial_titulo), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (conProtocolo > 0) {
                        pluralStringResource(R.plurals.medico_panel_protocolos_abiertos, conProtocolo, conProtocolo)
                    } else {
                        stringResource(R.string.medico_protocolos_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_siguiente),
                contentDescription = stringResource(R.string.cd_entrar),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---- Tarjeta de cada tripulante ----

@Composable
private fun TarjetaTripulante(fila: FilaTripulanteUi, onClick: () -> Unit) {
    val general = fila.estadoGeneral
    Card(
        onClick = onClick,
        border = BorderStroke(if (general == EstadoCognitivo.ALERTA) 2.dp else 1.dp, colorEstado(general).copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Quién es y cómo está en general.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(fila.nombre, colorEstado(general))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(fila.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        fila.cargo?.let { stringResource(nombreCargo(it)) } ?: stringResource(R.string.estado_sin_datos),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MarcaEstado(general)
                }
                Icon(
                    painter = painterResource(R.drawable.ic_siguiente),
                    contentDescription = stringResource(R.string.cd_entrar),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Cognitivo · Fisiológico · Avisos.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Indicador(R.string.medico_panel_cognitivo, Modifier.weight(1.2f)) {
                    EstadoCorto(R.string.stroop_nombre, fila.ultimoStroop)
                    EstadoCorto(R.string.nback_nombre, fila.ultimoNBack)
                }
                Indicador(R.string.medico_panel_fisiologico, Modifier.weight(1f)) {
                    if (fila.nivelFisiologico != null) {
                        // Número grande y su nombre debajo (en la columna estrecha no cabe en una línea).
                        Text(
                            fila.nivelFisiologico.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = colorNivel(fila.nivelFisiologico),
                        )
                        Text(
                            stringResource(nombreNivel(fila.nivelFisiologico)),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorNivel(fila.nivelFisiologico),
                            maxLines = 2,
                        )
                    } else {
                        Text(
                            stringResource(R.string.medico_panel_sin_protocolos),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Indicador(R.string.medico_panel_avisos_corto, Modifier.weight(0.8f)) {
                    Text(
                        fila.alertasActivas.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (fila.alertasActivas > 0) LocalEstadoColores.current.alerta else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (fila.alertasActivas == 0) stringResource(R.string.medico_sin_alertas)
                        else pluralStringResource(R.plurals.medico_alertas_activas, fila.alertasActivas, fila.alertasActivas),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            // Constantes de los últimos días.
            if (fila.constantes.isEmpty()) {
                Text(
                    stringResource(R.string.medico_panel_sin_constantes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    fila.constantes.forEach { MiniConstante(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Avatar(nombre: String, color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(3.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nombre.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

@Composable
private fun Indicador(@StringRes titulo: Int, modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(titulo),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        contenido()
    }
}

/** "Stroop · Vigilar" en pequeño; sin sesiones, "Sin datos". */
@Composable
private fun EstadoCorto(@StringRes nombre: Int, estado: EstadoCognitivo?) {
    val e = estado ?: EstadoCognitivo.SIN_DATOS
    MarcaEstado(estado = e, texto = "${stringResource(nombre)} · ${stringResource(etiquetaEstado(e))}")
}

/** Una constante: su último valor, si está en su rango (con texto) y su tendencia de los últimos días. */
@Composable
private fun MiniConstante(c: ConstanteMiniUi, modifier: Modifier) {
    val colores = LocalEstadoColores.current
    val color = if (c.fuera) colores.alerta else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(nombreCorto(c.metrica)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            c.ultimo?.let { "${Math.round(it)}" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
        Text(
            stringResource(unidadCorta(c.metrica)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Tendencia(c.puntos.map { it.second }, color)
        Text(
            stringResource(
                when {
                    !c.conReferencia -> R.string.medico_panel_sin_referencia
                    !c.fuera -> R.string.medico_panel_en_rango
                    c.alto -> R.string.medico_panel_alta
                    else -> R.string.medico_panel_baja
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                !c.conReferencia -> MaterialTheme.colorScheme.onSurfaceVariant
                c.fuera -> colores.alerta
                else -> colores.normal
            },
        )
    }
}

/** Línea de tendencia (decorativa: el valor y su estado ya van en texto). */
@Composable
private fun Tendencia(valores: List<Double>, color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clearAndSetSemantics { },
    ) {
        if (valores.size < 2) return@Canvas
        val min = valores.min()
        val max = valores.max()
        val rango = (max - min).takeIf { it > 0 } ?: 1.0
        fun punto(i: Int) = Offset(
            x = size.width * i / (valores.size - 1),
            y = (size.height - 3.dp.toPx()) * (1 - ((valores[i] - min) / rango).toFloat()) + 1.5.dp.toPx(),
        )
        val camino = Path().apply {
            moveTo(punto(0).x, punto(0).y)
            for (i in 1 until valores.size) lineTo(punto(i).x, punto(i).y)
        }
        drawPath(camino, color.copy(alpha = 0.8f), style = Stroke(width = 1.5.dp.toPx()))
        drawCircle(color, radius = 2.5.dp.toPx(), center = punto(valores.size - 1))
    }
}

@StringRes
private fun nombreCorto(m: MetricaConstante): Int = when (m) {
    MetricaConstante.FC -> R.string.medico_corto_fc
    MetricaConstante.PAS -> R.string.medico_corto_pas
    MetricaConstante.SPO2 -> R.string.medico_corto_spo2
    MetricaConstante.FR -> R.string.medico_corto_fr
}

@StringRes
private fun unidadCorta(m: MetricaConstante): Int = when (m) {
    MetricaConstante.FC -> R.string.unidad_lpm
    MetricaConstante.PAS -> R.string.unidad_mmhg
    MetricaConstante.SPO2 -> R.string.unidad_porcentaje
    MetricaConstante.FR -> R.string.medico_corto_unidad_fr
}

@Preview(heightDp = 1600)
@Composable
private fun TripulacionPreview() {
    val ahora = System.currentTimeMillis()
    val hora = 3_600_000L
    fun serie(vararg v: Double) = v.mapIndexed { i, x -> ahora - (v.size - i) * 6 * hora to x }
    SpaceDriftersTheme {
        TripulacionContenido(
            panel = PanelMedicoUi(
                tripulacion = ordenarPorUrgencia(
                    listOf(
                        FilaTripulanteUi(
                            1, "Maya Okafor", CargoTripulante.COMANDANTE, 2, EstadoCognitivo.NORMAL, EstadoCognitivo.ALERTA,
                            nivelFisiologico = 3,
                            constantes = listOf(
                                ConstanteMiniUi(MetricaConstante.FC, serie(62.0, 64.0, 70.0, 88.0, 96.0), 96.0, fuera = true, alto = true),
                                ConstanteMiniUi(MetricaConstante.PAS, serie(118.0, 116.0, 110.0, 102.0, 94.0), 94.0, fuera = true, alto = false),
                                ConstanteMiniUi(MetricaConstante.SPO2, serie(98.0, 98.0, 97.0, 98.0, 98.0), 98.0, fuera = false, alto = false),
                                ConstanteMiniUi(MetricaConstante.FR, serie(14.0, 15.0, 14.0, 14.0, 15.0), 15.0, fuera = false, alto = false),
                            ),
                        ),
                        FilaTripulanteUi(3, "Ane Etxeberria", CargoTripulante.OFICIAL_MEDICO, 0, EstadoCognitivo.NORMAL, EstadoCognitivo.NORMAL),
                    ),
                ),
                diaMision = 14,
                retrasoMin = 18,
                actualizadoEn = ahora,
            ),
            onAtras = {},
            onAstronauta = {},
        )
    }
}
