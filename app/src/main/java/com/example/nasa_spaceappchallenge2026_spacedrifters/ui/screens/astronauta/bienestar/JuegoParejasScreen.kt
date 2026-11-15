package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.bienestar

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.EtiquetasUi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoCometa
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoLuna
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoLunaCrater
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoMarte
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoSaturno
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoSol
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoTierra
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.CuerpoTierraTierra
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import kotlin.math.cos
import kotlin.math.sin

/** Símbolos de las cartas (en el orden de JuegoParejas: símbolo 0 = TIERRA...). */
enum class CuerpoCeleste { TIERRA, LUNA, MARTE, SATURNO, SOL, COMETA }

@StringRes
fun nombreCuerpo(c: CuerpoCeleste): Int = when (c) {
    CuerpoCeleste.TIERRA -> R.string.cuerpo_tierra
    CuerpoCeleste.LUNA -> R.string.cuerpo_luna
    CuerpoCeleste.MARTE -> R.string.cuerpo_marte
    CuerpoCeleste.SATURNO -> R.string.cuerpo_saturno
    CuerpoCeleste.SOL -> R.string.cuerpo_sol
    CuerpoCeleste.COMETA -> R.string.cuerpo_cometa
}

/** "Parejas orbitales": 12 cartas, 6 parejas de cuerpos celestes. */
@Composable
fun JuegoParejasScreen(
    onAtras: () -> Unit,
    viewModel: JuegoParejasViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    JuegoParejasContenido(ui, onAtras, viewModel::voltear, viewModel::otraPartida)
}

@Composable
private fun JuegoParejasContenido(ui: JuegoUi, onAtras: () -> Unit, onCarta: (Int) -> Unit, onOtra: () -> Unit) {
    PantallaBase(titulo = stringResource(R.string.juego_titulo), onAtras = onAtras) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.juego_instrucciones),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.juego_marcador, ui.parejasEncontradas, ui.parejas, ui.intentos),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            // 3 columnas × 4 filas.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag(EtiquetasUi.TABLERO_PAREJAS),
            ) {
                ui.cartas.chunked(COLUMNAS).forEachIndexed { fila, cartas ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        cartas.forEachIndexed { col, carta ->
                            Carta(
                                numero = fila * COLUMNAS + col + 1,
                                carta = carta,
                                onClick = { onCarta(fila * COLUMNAS + col) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            if (ui.terminado) {
                Text(
                    stringResource(R.string.juego_terminado, ui.intentos),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onOtra, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.juego_otra))
                }
                OutlinedButton(onClick = onAtras, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.bienestar_volver))
                }
            }
        }
    }
}

private const val COLUMNAS = 3

@Composable
private fun Carta(numero: Int, carta: CartaUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val nombre = stringResource(nombreCuerpo(carta.cuerpo))
    val descripcion = when {
        carta.emparejada -> stringResource(R.string.cd_carta_emparejada, numero, nombre)
        carta.bocaArriba -> stringResource(R.string.cd_carta_boca_arriba, numero, nombre)
        else -> stringResource(R.string.cd_carta_boca_abajo, numero)
    }
    Card(
        onClick = onClick,
        enabled = !carta.bocaArriba,
        border = BorderStroke(
            if (carta.emparejada) 3.dp else 1.dp,
            if (carta.emparejada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        colors = CardDefaults.cardColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .aspectRatio(0.8f)
            .semantics(mergeDescendants = true) { contentDescription = descripcion },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (carta.bocaArriba) {
                Canvas(Modifier.weight(1f).aspectRatio(1f)) { dibujarCuerpo(carta.cuerpo) }
                Text(nombre, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("✦", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Dibujos sencillos de cada cuerpo, en un lienzo cuadrado. */
private fun DrawScope.dibujarCuerpo(c: CuerpoCeleste) {
    val r = size.minDimension / 2
    val centro = Offset(size.width / 2, size.height / 2)
    when (c) {
        CuerpoCeleste.TIERRA -> {
            drawCircle(CuerpoTierra, r * 0.8f, centro)
            drawOval(CuerpoTierraTierra, Offset(centro.x - r * 0.45f, centro.y - r * 0.45f), Size(r * 0.55f, r * 0.4f))
            drawOval(CuerpoTierraTierra, Offset(centro.x + r * 0.05f, centro.y + r * 0.1f), Size(r * 0.45f, r * 0.35f))
        }
        CuerpoCeleste.LUNA -> {
            drawCircle(CuerpoLuna, r * 0.75f, centro)
            drawCircle(CuerpoLunaCrater, r * 0.15f, Offset(centro.x - r * 0.25f, centro.y - r * 0.2f))
            drawCircle(CuerpoLunaCrater, r * 0.1f, Offset(centro.x + r * 0.3f, centro.y + r * 0.1f))
            drawCircle(CuerpoLunaCrater, r * 0.08f, Offset(centro.x - r * 0.05f, centro.y + r * 0.35f))
        }
        CuerpoCeleste.MARTE -> {
            drawCircle(CuerpoMarte, r * 0.7f, centro)
            drawCircle(CuerpoLuna, r * 0.15f, Offset(centro.x, centro.y - r * 0.55f))
        }
        CuerpoCeleste.SATURNO -> {
            drawCircle(CuerpoSaturno, r * 0.5f, centro)
            rotate(-20f, centro) {
                drawOval(
                    CuerpoSaturno,
                    Offset(centro.x - r * 0.95f, centro.y - r * 0.22f),
                    Size(r * 1.9f, r * 0.44f),
                    style = Stroke(r * 0.08f),
                )
            }
        }
        CuerpoCeleste.SOL -> {
            drawCircle(CuerpoSol, r * 0.5f, centro)
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                drawLine(
                    CuerpoSol,
                    Offset(centro.x + (r * 0.62f * cos(a)).toFloat(), centro.y + (r * 0.62f * sin(a)).toFloat()),
                    Offset(centro.x + (r * 0.9f * cos(a)).toFloat(), centro.y + (r * 0.9f * sin(a)).toFloat()),
                    strokeWidth = r * 0.1f,
                )
            }
        }
        CuerpoCeleste.COMETA -> {
            val cabeza = Offset(centro.x + r * 0.4f, centro.y - r * 0.4f)
            val cola = Path().apply {
                moveTo(cabeza.x - r * 0.15f, cabeza.y - r * 0.15f)
                lineTo(centro.x - r * 0.9f, centro.y + r * 0.9f)
                lineTo(cabeza.x + r * 0.15f, cabeza.y + r * 0.15f)
                close()
            }
            drawPath(cola, CuerpoCometa.copy(alpha = 0.5f))
            drawCircle(CuerpoCometa, r * 0.25f, cabeza)
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun JuegoParejasPreview() {
    SpaceDriftersTheme {
        JuegoParejasContenido(
            ui = JuegoUi(
                cartas = CuerpoCeleste.entries.flatMap { listOf(it, it) }.mapIndexed { i, c ->
                    CartaUi(c, bocaArriba = i < 8, emparejada = i < 4)
                },
                intentos = 5,
                parejasEncontradas = 2,
            ),
            onAtras = {}, onCarta = {}, onOtra = {},
        )
    }
}
