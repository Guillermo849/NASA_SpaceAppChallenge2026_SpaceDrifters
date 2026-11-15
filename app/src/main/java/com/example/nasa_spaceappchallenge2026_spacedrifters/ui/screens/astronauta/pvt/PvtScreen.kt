package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.pvt

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt.ControladorPvt
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt.FasePvt
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.AvisoPrototipo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.InterrumpirAlSalir
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MensajePrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ModoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaEspera
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ZonaRespuesta
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.fraseResultado
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import kotlinx.coroutines.delay

/** Prueba PVT completa: instrucciones, práctica, prueba y fin. */
@Composable
fun PvtScreen(
    onVolver: () -> Unit,
    viewModel: PvtViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val etapa by viewModel.etapa.collectAsStateWithLifecycle()
    val fase by viewModel.fase.collectAsStateWithLifecycle()
    InterrumpirAlSalir(viewModel::interrumpir)
    // Pantalla completa para que el aviso "Viewing full screen" salga antes de empezar.
    ModoPrueba()

    val titulo = stringResource(R.string.pvt_nombre)
    when (val actual = etapa) {
        PvtViewModel.Etapa.Cargando -> PantallaEspera(titulo, onAtras = onVolver)
        PvtViewModel.Etapa.Guardando -> PantallaEspera(titulo, onAtras = null)
        is PvtViewModel.Etapa.Instrucciones -> InstruccionesPvt(
            conPractica = actual.conPractica,
            onEmpezar = viewModel::empezar,
            onAtras = onVolver,
        )
        is PvtViewModel.Etapa.EnCurso -> {
            BackHandler(onBack = viewModel::interrumpir)
            PruebaPvtEnCurso(actual.controlador, fase)
        }
        PvtViewModel.Etapa.FinPractica -> MensajePrueba(
            tituloPantalla = titulo,
            titulo = stringResource(R.string.fin_practica_titulo),
            texto = stringResource(R.string.pvt_fin_practica_texto),
            boton = stringResource(R.string.empezar_prueba),
            onBoton = viewModel::empezar,
            onAtras = onVolver,
        )
        is PvtViewModel.Etapa.Terminada -> MensajePrueba(
            tituloPantalla = titulo,
            titulo = stringResource(R.string.prueba_terminada_titulo),
            texto = stringResource(fraseResultado(TipoPrueba.PVT, actual.estado)),
            boton = stringResource(R.string.volver_panel),
            onBoton = onVolver,
            onAtras = onVolver,
        )
        PvtViewModel.Etapa.Interrumpida -> MensajePrueba(
            tituloPantalla = titulo,
            titulo = stringResource(R.string.prueba_interrumpida_titulo),
            texto = stringResource(R.string.prueba_interrumpida_texto),
            boton = stringResource(R.string.volver_panel),
            onBoton = onVolver,
            onAtras = onVolver,
        )
    }
}

@Composable
private fun InstruccionesPvt(conPractica: Boolean, onEmpezar: () -> Unit, onAtras: () -> Unit) {
    PantallaBase(titulo = stringResource(R.string.pvt_nombre), onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pvt_instrucciones),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.pvt_como_funciona),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (conPractica) {
                Text(
                    text = stringResource(R.string.pvt_practica_aviso),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.pvt_duracion),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onEmpezar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.empezar))
            }
        }
    }
}

/** La prueba en sí: pantalla oscura con el estímulo en el centro y el botón de reacción abajo. */
@Composable
private fun PruebaPvtEnCurso(controlador: ControladorPvt, fase: FasePvt) {
    // Cuando aparece el estímulo, avisamos al controlador del instante real del frame.
    LaunchedEffect(fase) {
        val actual = fase
        if (actual is FasePvt.Estimulo) {
            val instante = withFrameNanos { it }
            controlador.estimuloMostrado(actual.numero, instante)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            // Barra de progreso temporal (anima el ISI para dar feedback visual)
            BarraProgresoIsi(fase)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ContenidoFasePvt(fase)
            }

            // Botón de respuesta grande: ocupa toda la parte baja de la pantalla.
            ZonaRespuesta(
                onToque = { instanteNanos -> controlador.responder(instanteNanos) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .background(
                        color = when (fase) {
                            is FasePvt.Estimulo -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                Text(
                    text = when (fase) {
                        is FasePvt.Estimulo -> stringResource(R.string.pvt_toca_ahora)
                        else -> stringResource(R.string.pvt_espera)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = when (fase) {
                        is FasePvt.Estimulo -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
        AvisoPrototipo(Modifier.navigationBarsPadding())
    }
}

/** Muestra el contador de ms (el estímulo visual del PVT) o el símbolo de espera. */
@Composable
private fun ContenidoFasePvt(fase: FasePvt) {
    val estados = LocalEstadoColores.current
    when (fase) {
        is FasePvt.Isi -> Text(
            text = "+",
            fontSize = 56.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        is FasePvt.Estimulo -> {
            // Contador en ms que corre desde que el estímulo apareció.
            var ms by remember { mutableLongStateOf(0L) }
            LaunchedEffect(fase.inicioNanos) {
                val inicio = System.nanoTime()
                while (true) {
                    ms = (System.nanoTime() - inicio) / 1_000_000L
                    delay(16) // ~60 fps
                }
            }
            Text(
                text = ms.toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        is FasePvt.Feedback -> {
            val (texto, color) = when {
                fase.resultado.isAnticipacion() ->
                    stringResource(R.string.pvt_feedback_anticipacion) to estados.alerta
                fase.resultado.esLapso() ->
                    stringResource(R.string.pvt_feedback_lapso) to estados.vigilar
                else -> {
                    val tr = fase.resultado.tiempoMs ?: 0L
                    stringResource(R.string.pvt_feedback_valido, tr) to estados.normal
                }
            }
            Text(
                text = texto,
                color = color,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
        }
        else -> Unit // Preparada o Terminada: vacío.
    }
}

/**
 * Barra fina en la parte superior que se llena durante el ISI, dando al astronauta
 * una pista visual del tiempo que falta para que salga el siguiente estímulo.
 * Solo es indicativa (no revela el instante exacto para no permitir anticipar).
 */
@Composable
private fun BarraProgresoIsi(fase: FasePvt) {
    var progreso by remember { mutableStateOf(0f) }

    LaunchedEffect(fase) {
        when (fase) {
            is FasePvt.Isi -> {
                // Anima de 0 a ~0.9 durante el ISI (nunca llega a 1 para no revelar el momento).
                progreso = 0f
                val duracionEstimadaMs = 6_000L // valor medio del ISI (2–10 s)
                val inicio = System.currentTimeMillis()
                while (true) {
                    val transcurrido = (System.currentTimeMillis() - inicio).toFloat()
                    progreso = (transcurrido / duracionEstimadaMs).coerceIn(0f, 0.9f)
                    delay(50)
                }
            }
            is FasePvt.Estimulo -> progreso = 1f
            else -> progreso = 0f
        }
    }

    val animado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progresoIsi",
    )

    LinearProgressIndicator(
        progress = { animado },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Preview
@Composable
private fun InstruccionesPvtPreview() {
    SpaceDriftersTheme { InstruccionesPvt(conPractica = true, onEmpezar = {}, onAtras = {}) }
}
