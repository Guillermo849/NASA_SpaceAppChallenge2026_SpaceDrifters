package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ControladorNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FaseNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FormaNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ResultadoEnsayoNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.AvisoPrototipo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.InterrumpirAlSalir
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MensajePrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ModoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ZonaRespuesta
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Prueba N-Back completa: instrucciones, prácticas 1-back y 2-back, prueba y fin. */
@Composable
fun NBackScreen(
    onVolver: () -> Unit,
    viewModel: NBackViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val etapa by viewModel.etapa.collectAsStateWithLifecycle()
    val fase by viewModel.fase.collectAsStateWithLifecycle()
    InterrumpirAlSalir(viewModel::interrumpir)
    // Pantalla completa desde las instrucciones: así el aviso de Android "Viewing full screen"
    // (solo sale la primera vez) aparece antes de empezar y no tapa la prueba cronometrada.
    ModoPrueba()

    val titulo = stringResource(R.string.nback_nombre)
    when (val actual = etapa) {
        is NBackViewModel.Etapa.Instrucciones -> InstruccionesNBack(
            conPractica = actual.conPractica,
            onEmpezar = viewModel::empezar,
            onAtras = onVolver,
        )
        is NBackViewModel.Etapa.EnCurso -> {
            BackHandler(onBack = viewModel::interrumpir)
            PruebaNBackEnCurso(actual.controlador, fase)
        }
        is NBackViewModel.Etapa.Intermedia -> if (actual.siguiente == NBackViewModel.TipoSesion.PRACTICA_2BACK) {
            MensajePrueba(
                tituloPantalla = titulo,
                titulo = stringResource(R.string.nback_intermedia_titulo),
                texto = stringResource(R.string.nback_intermedia_texto),
                boton = stringResource(R.string.nback_seguir_practica),
                onBoton = viewModel::empezar,
                onAtras = onVolver,
            )
        } else {
            MensajePrueba(
                tituloPantalla = titulo,
                titulo = stringResource(R.string.fin_practica_titulo),
                texto = stringResource(R.string.fin_practica_texto),
                boton = stringResource(R.string.empezar_prueba),
                onBoton = viewModel::empezar,
                onAtras = onVolver,
            )
        }
        is NBackViewModel.Etapa.Terminada -> MensajePrueba(
            tituloPantalla = titulo,
            titulo = stringResource(R.string.prueba_terminada_titulo),
            texto = stringResource(R.string.prueba_terminada_texto),
            boton = stringResource(R.string.volver_panel),
            onBoton = onVolver,
            onAtras = onVolver,
        )
        NBackViewModel.Etapa.Interrumpida -> MensajePrueba(
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
private fun InstruccionesNBack(conPractica: Boolean, onEmpezar: () -> Unit, onAtras: () -> Unit) {
    PantallaBase(titulo = stringResource(R.string.nback_nombre), onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.nback_instrucciones), style = MaterialTheme.typography.titleMedium)

            // Ejemplo visual: triángulo, círculo, triángulo → la tercera coincide con la primera.
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ejemplo_etiqueta),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        listOf(FormaNBack.TRIANGULO, FormaNBack.CIRCULO, FormaNBack.TRIANGULO)
                            .forEachIndexed { i, forma ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FormaNBackDibujo(
                                        forma = forma,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Text(
                                        text = (i + 1).toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                    }
                    Text(
                        text = stringResource(R.string.nback_ejemplo_respuesta),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (conPractica) {
                Text(
                    text = stringResource(R.string.nback_practica_aviso),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.nback_duracion),
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

/** La sesión en sí: pantalla completa, sin animaciones y con la pantalla siempre encendida. */
@Composable
private fun PruebaNBackEnCurso(controlador: ControladorNBack, fase: FaseNBack) {

    // Inicio del estímulo = frame en que se dibuja la forma (mismo reloj que los toques).
    // Ver el comentario equivalente en StroopScreen.
    LaunchedEffect(fase) {
        val actual = fase
        if (actual is FaseNBack.Estimulo) {
            val instante = withFrameNanos { it }
            controlador.estimuloMostrado(actual.indice, instante)
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
            indiceDe(fase)?.let { indice ->
                val progreso = if (controlador.esPractica()) R.string.prueba_progreso_practica else R.string.prueba_progreso
                Text(
                    text = stringResource(progreso, indice + 1, controlador.totalEstimulos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    if (controlador.nivel == 1) R.string.nback_recordatorio_1 else R.string.nback_recordatorio_2,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ContenidoFase(fase)
            }
            val forma = RoundedCornerShape(16.dp)
            ZonaRespuesta(
                onToque = controlador::pulsar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, forma)
                    .border(1.dp, MaterialTheme.colorScheme.outline, forma),
            ) {
                Text(
                    text = stringResource(R.string.nback_coincide),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        AvisoPrototipo(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ContenidoFase(fase: FaseNBack) {
    when (fase) {
        is FaseNBack.Estimulo -> FormaNBackDibujo(
            forma = fase.forma,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(160.dp),
        )
        is FaseNBack.Feedback -> {
            val estados = LocalEstadoColores.current
            val (texto, color) = when (fase.resultado.tipo) {
                ResultadoEnsayoNBack.Tipo.ACIERTO -> R.string.feedback_correcto to estados.normal
                ResultadoEnsayoNBack.Tipo.FALSA_ALARMA -> R.string.nback_feedback_falsa_alarma to estados.alerta
                else -> R.string.nback_feedback_omision to estados.alerta
            }
            Text(
                text = stringResource(texto),
                color = color,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        else -> Unit // Preparada, Vacio o Terminada: pantalla vacía.
    }
}

private fun indiceDe(fase: FaseNBack): Int? = when (fase) {
    is FaseNBack.Estimulo -> fase.indice
    is FaseNBack.Vacio -> fase.indice
    is FaseNBack.Feedback -> fase.indice
    else -> null
}

@Preview
@Composable
private fun InstruccionesNBackPreview() {
    SpaceDriftersTheme { InstruccionesNBack(conPractica = true, onEmpezar = {}, onAtras = {}) }
}
