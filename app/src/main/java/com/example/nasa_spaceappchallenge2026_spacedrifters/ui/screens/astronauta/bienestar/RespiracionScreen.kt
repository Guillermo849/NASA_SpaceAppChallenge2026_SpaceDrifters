package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.bienestar

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.RespiracionGuiada
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.RespiracionGuiada.Fase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.RespiracionCirculo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

private enum class PasoRespiracion { ELEGIR, EN_CURSO, TERMINADA }

/** Respiración en caja: 4 s inspirar, 4 mantener, 4 espirar, 4 mantener. */
@Composable
fun RespiracionScreen(
    onAtras: () -> Unit,
    viewModel: RespiracionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var paso by rememberSaveable { mutableStateOf(PasoRespiracion.ELEGIR) }
    var minutos by rememberSaveable { mutableIntStateOf(3) }
    var transcurrido by remember { mutableLongStateOf(0L) }
    val guia = remember(minutos) { RespiracionGuiada(RespiracionGuiada.ciclosEnMinutos(minutos)) }

    fun terminar() {
        val estado = guia.estado(transcurrido)
        val completos = if (estado.isTerminado) guia.ciclos else estado.ciclo - 1
        viewModel.registrar(completos, transcurrido)
    }

    // Si sale a mitad, cuenta lo que ha hecho (si ha completado al menos un ciclo).
    val alSalir by rememberUpdatedState { if (paso == PasoRespiracion.EN_CURSO) terminar() }
    DisposableEffect(Unit) { onDispose { alSalir() } }

    if (paso == PasoRespiracion.EN_CURSO) {
        // Pantalla encendida mientras dura la sesión.
        val vista = LocalView.current
        DisposableEffect(vista) {
            vista.keepScreenOn = true
            onDispose { vista.keepScreenOn = false }
        }
        LaunchedEffect(guia) {
            val inicio = withFrameMillis { it }
            while (true) {
                transcurrido = withFrameMillis { it } - inicio
                if (guia.estado(transcurrido).isTerminado) {
                    terminar()
                    paso = PasoRespiracion.TERMINADA
                    break
                }
            }
        }
    }

    PantallaBase(titulo = stringResource(R.string.bienestar_respiracion_titulo), onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (paso) {
                PasoRespiracion.ELEGIR -> Elegir(minutos, onMinutos = { minutos = it }) {
                    viewModel.nueva()
                    transcurrido = 0
                    paso = PasoRespiracion.EN_CURSO
                }
                PasoRespiracion.EN_CURSO -> EnCurso(guia, transcurrido) {
                    terminar()
                    paso = PasoRespiracion.TERMINADA
                }
                PasoRespiracion.TERMINADA -> Terminada(
                    onOtra = { paso = PasoRespiracion.ELEGIR },
                    onVolver = onAtras,
                )
            }
        }
    }
}

@Composable
private fun Elegir(minutos: Int, onMinutos: (Int) -> Unit, onEmpezar: () -> Unit) {
    Text(stringResource(R.string.respiracion_explicacion), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.respiracion_duracion), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1, 3, 5).forEach { m ->
            FilterChip(
                selected = m == minutos,
                onClick = { onMinutos(m) },
                label = { Text(stringResource(R.string.respiracion_minutos, m)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
    Button(onClick = onEmpezar, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(stringResource(R.string.bienestar_empezar))
    }
}

@Composable
private fun EnCurso(guia: RespiracionGuiada, transcurrido: Long, onParar: () -> Unit) {
    val estado = guia.estado(transcurrido)
    val acento = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radioMax = size.minDimension / 2
            val centro = Offset(size.width / 2, size.height / 2)
            // Guía: el tamaño máximo, para ver hasta dónde llenar.
            drawCircle(acento, radius = radioMax - 2.dp.toPx(), center = centro, style = Stroke(2.dp.toPx()))
            drawCircle(RespiracionCirculo, radius = radioMax * estado.tamano, center = centro)
            drawCircle(acento, radius = radioMax * estado.tamano, center = centro, style = Stroke(4.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(textoFase(estado.fase)),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(estado.segundosRestantesFase.toString(), style = MaterialTheme.typography.displaySmall)
        }
    }
    Text(
        stringResource(R.string.respiracion_ciclo, estado.ciclo, guia.ciclos),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onParar, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.respiracion_terminar))
    }
}

@Composable
private fun Terminada(onOtra: () -> Unit, onVolver: () -> Unit) {
    Text(stringResource(R.string.respiracion_hecho), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(stringResource(R.string.respiracion_hecho_texto), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Button(onClick = onVolver, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(stringResource(R.string.bienestar_volver))
    }
    OutlinedButton(onClick = onOtra, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.respiracion_otra))
    }
}

@StringRes
private fun textoFase(fase: Fase): Int = when (fase) {
    Fase.INSPIRA -> R.string.respiracion_inspira
    Fase.MANTEN_LLENO, Fase.MANTEN_VACIO -> R.string.respiracion_manten
    Fase.ESPIRA -> R.string.respiracion_espira
}

@Preview(heightDp = 800)
@Composable
private fun RespiracionPreview() {
    SpaceDriftersTheme {
        PantallaBase(titulo = "Respiración guiada") { padding ->
            Column(Modifier.padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                EnCurso(RespiracionGuiada(3), 2_000L) {}
            }
        }
    }
}
