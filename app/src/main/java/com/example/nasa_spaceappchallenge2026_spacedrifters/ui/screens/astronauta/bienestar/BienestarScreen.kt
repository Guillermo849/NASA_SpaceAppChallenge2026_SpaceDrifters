package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.bienestar

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.ReglasBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.ReglasBienestar.Sugerencia
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Bienestar psicológico: check-in de hoy y herramientas (respiración, juego, conexión, apoyo). */
@Composable
fun BienestarScreen(
    onAtras: () -> Unit,
    onRespiracion: () -> Unit,
    onJuego: () -> Unit,
    viewModel: BienestarViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val semana by viewModel.semana.collectAsStateWithLifecycle()
    BienestarContenido(
        ui = ui,
        semana = semana,
        acciones = AccionesBienestar(
            onAtras = onAtras,
            onAnimo = viewModel::elegirAnimo,
            onEstres = viewModel::elegirEstres,
            onAislamiento = viewModel::elegirAislamiento,
            onGuardar = viewModel::guardar,
            onRespiracion = onRespiracion,
            onJuego = onJuego,
            onPedirApoyo = viewModel::pedirApoyo,
        ),
    )
}

private data class AccionesBienestar(
    val onAtras: () -> Unit = {},
    val onAnimo: (Int) -> Unit = {},
    val onEstres: (Int) -> Unit = {},
    val onAislamiento: (Int) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onRespiracion: () -> Unit = {},
    val onJuego: () -> Unit = {},
    val onPedirApoyo: () -> Unit = {},
)

@Composable
private fun BienestarContenido(ui: BienestarUi, semana: SemanaBienestarUi, acciones: AccionesBienestar) {
    var verConexion by rememberSaveable { mutableStateOf(false) }
    var confirmarApoyo by rememberSaveable { mutableStateOf(false) }
    PantallaBase(titulo = stringResource(R.string.bienestar_titulo), onAtras = acciones.onAtras) { padding ->
        if (ui.cargando) return@PantallaBase
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.bienestar_checkin_titulo), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.bienestar_checkin_ayuda),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Pregunta(R.string.bienestar_animo_pregunta, ui.animo, ::etiquetaAnimo, acciones.onAnimo)
            Pregunta(R.string.bienestar_estres_pregunta, ui.estres, ::etiquetaIntensidad, acciones.onEstres)
            Pregunta(R.string.bienestar_aislamiento_pregunta, ui.aislamiento, ::etiquetaIntensidad, acciones.onAislamiento)

            if (ui.error) {
                Text(stringResource(R.string.registro_error), color = MaterialTheme.colorScheme.error)
            }
            if (!ui.puedeGuardar && !ui.guardando) {
                Text(
                    stringResource(R.string.bienestar_faltan_respuestas),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = acciones.onGuardar,
                enabled = ui.puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(if (ui.guardadoHoy) R.string.bienestar_actualizar else R.string.bienestar_guardar))
            }

            if (ui.guardadoHoy) {
                TarjetaParaHoy(
                    // Si ya ha pedido apoyo, no se le vuelve a sugerir.
                    sugerencias = ui.sugerencias.filterNot { it == Sugerencia.HABLAR_APOYO && ui.apoyoPedido },
                    acciones = acciones,
                    onConexion = { verConexion = true },
                    onApoyo = { confirmarApoyo = true },
                )
            }

            // ---- Herramientas ----
            Text(stringResource(R.string.bienestar_herramientas), style = MaterialTheme.typography.titleLarge)
            Herramienta(
                titulo = R.string.bienestar_respiracion_titulo,
                texto = R.string.bienestar_respiracion_texto,
                boton = R.string.bienestar_empezar,
                onClick = acciones.onRespiracion,
            )
            Herramienta(
                titulo = R.string.juego_titulo,
                texto = R.string.bienestar_juego_texto,
                boton = R.string.bienestar_jugar,
                onClick = acciones.onJuego,
            )
            Herramienta(
                titulo = R.string.bienestar_conexion_titulo,
                texto = R.string.bienestar_conexion_texto,
                boton = if (verConexion) R.string.bienestar_ocultar else R.string.bienestar_ver_ideas,
                onClick = { verConexion = !verConexion },
            ) {
                if (verConexion) {
                    IDEAS_CONEXION.forEach { idea ->
                        Text("• ${stringResource(idea)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Herramienta(
                titulo = R.string.bienestar_apoyo_titulo,
                texto = if (ui.apoyoPedido) R.string.bienestar_apoyo_pedido else R.string.bienestar_apoyo_texto,
                boton = if (ui.apoyoPedido) null else R.string.bienestar_apoyo_boton,
                onClick = { confirmarApoyo = true },
            )
            Text(
                stringResource(R.string.bienestar_semana, semana.respiraciones, semana.partidas),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (confirmarApoyo) {
        AlertDialog(
            onDismissRequest = { confirmarApoyo = false },
            title = { Text(stringResource(R.string.bienestar_apoyo_titulo)) },
            text = { Text(stringResource(R.string.bienestar_apoyo_confirmar)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarApoyo = false
                    acciones.onPedirApoyo()
                }) { Text(stringResource(R.string.bienestar_apoyo_enviar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarApoyo = false }) { Text(stringResource(R.string.bienestar_cancelar)) }
            },
        )
    }
}

/** Pregunta con 5 respuestas, cada una con su texto (nunca solo un número o un color). */
@Composable
private fun Pregunta(@StringRes pregunta: Int, seleccion: Int?, etiqueta: (Int) -> Int, onElegir: (Int) -> Unit) {
    Tarjeta {
        Text(stringResource(pregunta), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (ReglasBienestar.MIN..ReglasBienestar.MAX).forEach { v ->
                FilterChip(
                    selected = v == seleccion,
                    onClick = { onElegir(v) },
                    label = { Text(stringResource(etiqueta(v))) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaParaHoy(
    sugerencias: List<Sugerencia>,
    acciones: AccionesBienestar,
    onConexion: () -> Unit,
    onApoyo: () -> Unit,
) {
    Tarjeta {
        Text(stringResource(R.string.bienestar_para_hoy), style = MaterialTheme.typography.titleMedium)
        if (sugerencias.isEmpty()) {
            Text(stringResource(R.string.bienestar_todo_bien), style = MaterialTheme.typography.bodyMedium)
        }
        sugerencias.forEach { s ->
            Text(stringResource(textoSugerencia(s)), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = when (s) {
                    Sugerencia.RESPIRACION -> acciones.onRespiracion
                    Sugerencia.CONEXION -> onConexion
                    Sugerencia.HABLAR_APOYO -> onApoyo
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(botonSugerencia(s))) }
        }
    }
}

@Composable
private fun Herramienta(
    @StringRes titulo: Int,
    @StringRes texto: Int,
    @StringRes boton: Int?,
    onClick: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    Tarjeta {
        Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(texto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        extra()
        if (boton != null) {
            OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(boton)) }
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { contenido() }
    }
}

@StringRes
fun etiquetaAnimo(v: Int): Int = when (v) {
    1 -> R.string.animo_1
    2 -> R.string.animo_2
    3 -> R.string.animo_3
    4 -> R.string.animo_4
    else -> R.string.animo_5
}

/** Estrés y aislamiento: 1 = nada … 5 = mucho. */
@StringRes
fun etiquetaIntensidad(v: Int): Int = when (v) {
    1 -> R.string.intensidad_1
    2 -> R.string.intensidad_2
    3 -> R.string.intensidad_3
    4 -> R.string.intensidad_4
    else -> R.string.intensidad_5
}

@StringRes
private fun textoSugerencia(s: Sugerencia): Int = when (s) {
    Sugerencia.HABLAR_APOYO -> R.string.sugerencia_apoyo
    Sugerencia.RESPIRACION -> R.string.sugerencia_respiracion
    Sugerencia.CONEXION -> R.string.sugerencia_conexion
}

@StringRes
private fun botonSugerencia(s: Sugerencia): Int = when (s) {
    Sugerencia.HABLAR_APOYO -> R.string.bienestar_apoyo_boton
    Sugerencia.RESPIRACION -> R.string.bienestar_empezar_respiracion
    Sugerencia.CONEXION -> R.string.bienestar_ver_ideas
}

private val IDEAS_CONEXION = listOf(
    R.string.conexion_idea_1,
    R.string.conexion_idea_2,
    R.string.conexion_idea_3,
    R.string.conexion_idea_4,
    R.string.conexion_idea_5,
)

@Preview(heightDp = 1800)
@Composable
private fun BienestarPreview() {
    SpaceDriftersTheme {
        BienestarContenido(
            ui = BienestarUi(
                cargando = false, animo = 2, estres = 4, aislamiento = 3, guardadoHoy = true,
                sugerencias = listOf(Sugerencia.HABLAR_APOYO, Sugerencia.RESPIRACION),
            ),
            semana = SemanaBienestarUi(3, 1),
            acciones = AccionesBienestar(),
        )
    }
}
