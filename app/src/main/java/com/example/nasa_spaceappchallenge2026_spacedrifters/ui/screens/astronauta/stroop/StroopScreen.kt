package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.stroop

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.Color
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
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ColorStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ControladorStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.FaseStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.AvisoPrototipo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.InterrumpirAlSalir
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.MensajePrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ModoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.ZonaRespuesta
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.StroopAmarillo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.StroopAzul
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.StroopRojo
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.StroopVerde

/** Prueba Stroop completa: instrucciones, práctica, prueba y fin. */
@Composable
fun StroopScreen(
    onVolver: () -> Unit,
    viewModel: StroopViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val etapa by viewModel.etapa.collectAsStateWithLifecycle()
    val fase by viewModel.fase.collectAsStateWithLifecycle()
    InterrumpirAlSalir(viewModel::interrumpir)
    // Pantalla completa desde las instrucciones: así el aviso de Android "Viewing full screen"
    // (solo sale la primera vez) aparece antes de empezar y no tapa la prueba cronometrada.
    ModoPrueba()

    val titulo = stringResource(R.string.stroop_nombre)
    when (val actual = etapa) {
        is StroopViewModel.Etapa.Instrucciones -> InstruccionesStroop(
            conPractica = actual.conPractica,
            onEmpezar = viewModel::empezar,
            onAtras = onVolver,
        )
        is StroopViewModel.Etapa.EnCurso -> {
            BackHandler(onBack = viewModel::interrumpir)
            PruebaStroopEnCurso(actual.controlador, fase)
        }
        StroopViewModel.Etapa.FinPractica -> MensajePrueba(
            tituloPantalla = titulo,
            titulo = stringResource(R.string.fin_practica_titulo),
            texto = stringResource(R.string.fin_practica_texto),
            boton = stringResource(R.string.empezar_prueba),
            onBoton = viewModel::empezar,
            onAtras = onVolver,
        )
        is StroopViewModel.Etapa.Terminada -> MensajePrueba(
            tituloPantalla = titulo,
            titulo = stringResource(R.string.prueba_terminada_titulo),
            texto = stringResource(R.string.prueba_terminada_texto),
            boton = stringResource(R.string.volver_panel),
            onBoton = onVolver,
            onAtras = onVolver,
        )
        StroopViewModel.Etapa.Interrumpida -> MensajePrueba(
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
private fun InstruccionesStroop(conPractica: Boolean, onEmpezar: () -> Unit, onAtras: () -> Unit) {
    PantallaBase(titulo = stringResource(R.string.stroop_nombre), onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(R.string.stroop_instrucciones), style = MaterialTheme.typography.titleMedium)

            // Ejemplo visual: ROJO escrito en tinta azul. El texto de debajo explica la respuesta,
            // así que no depende de distinguir el color.
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.ejemplo_etiqueta),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.color_rojo),
                        color = tinta(ColorStroop.AZUL),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.stroop_ejemplo_respuesta, stringResource(R.string.color_azul)),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (conPractica) {
                Text(
                    text = stringResource(R.string.stroop_practica_aviso),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.stroop_duracion),
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

/** La prueba en sí: pantalla completa, sin animaciones y con la pantalla siempre encendida. */
@Composable
private fun PruebaStroopEnCurso(controlador: ControladorStroop, fase: FaseStroop) {

    // Inicio del estímulo = cuando se dibuja de verdad, no cuando cambia el estado.
    // El efecto arranca cuando ya está aplicada la composición con el estímulo; el frame que
    // esperamos aquí llega cuando ese contenido se presenta en pantalla. frameTimeNanos usa el
    // reloj monótono (System.nanoTime), el mismo que el instante de los eventos táctiles.
    LaunchedEffect(fase) {
        val actual = fase
        if (actual is FaseStroop.Estimulo) {
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
                    text = stringResource(progreso, indice + 1, controlador.totalEnsayos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ContenidoFase(fase)
            }
            BotonesRespuesta(onToque = controlador::responder)
        }
        AvisoPrototipo(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ContenidoFase(fase: FaseStroop) {
    when (fase) {
        is FaseStroop.Fijacion -> Text(
            text = "+",
            fontSize = 56.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        is FaseStroop.Estimulo -> Text(
            text = stringResource(nombre(fase.estimulo.palabra)),
            color = tinta(fase.estimulo.tinta),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        is FaseStroop.Feedback -> {
            val estados = LocalEstadoColores.current
            val (texto, color) = when {
                fase.resultado.esOmision() -> R.string.feedback_sin_respuesta to estados.sinDatos
                fase.resultado.esCorrecto() -> R.string.feedback_correcto to estados.normal
                else -> R.string.feedback_incorrecto to estados.alerta
            }
            Text(
                text = stringResource(texto),
                color = color,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        else -> Unit // Preparada o Terminada: pantalla vacía.
    }
}

/** Siempre en la misma posición, con el nombre en color neutro. */
private val ORDEN_BOTONES = listOf(
    listOf(ColorStroop.ROJO, ColorStroop.AZUL),
    listOf(ColorStroop.VERDE, ColorStroop.AMARILLO),
)

@Composable
private fun BotonesRespuesta(onToque: (ColorStroop, Long) -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ORDEN_BOTONES.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                fila.forEach { color ->
                    ZonaRespuesta(
                        onToque = { instante -> onToque(color, instante) },
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, forma)
                            .border(1.dp, MaterialTheme.colorScheme.outline, forma),
                    ) {
                        Text(
                            text = stringResource(nombre(color)),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun indiceDe(fase: FaseStroop): Int? = when (fase) {
    is FaseStroop.Fijacion -> fase.indice
    is FaseStroop.Estimulo -> fase.indice
    is FaseStroop.Feedback -> fase.indice
    else -> null
}

@StringRes
private fun nombre(color: ColorStroop): Int = when (color) {
    ColorStroop.ROJO -> R.string.color_rojo
    ColorStroop.AZUL -> R.string.color_azul
    ColorStroop.VERDE -> R.string.color_verde
    ColorStroop.AMARILLO -> R.string.color_amarillo
}

private fun tinta(color: ColorStroop): Color = when (color) {
    ColorStroop.ROJO -> StroopRojo
    ColorStroop.AZUL -> StroopAzul
    ColorStroop.VERDE -> StroopVerde
    ColorStroop.AMARILLO -> StroopAmarillo
}

@Preview
@Composable
private fun InstruccionesStroopPreview() {
    SpaceDriftersTheme { InstruccionesStroop(conPractica = true, onEmpezar = {}, onAtras = {}) }
}
