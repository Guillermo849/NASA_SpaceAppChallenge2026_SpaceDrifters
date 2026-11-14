package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FormaNBack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Dibuja una forma del N-Back rellena de [color], centrada en el espacio disponible. */
@Composable
fun FormaNBackDibujo(forma: FormaNBack, color: Color, modifier: Modifier = Modifier) {
    val descripcion = stringResource(nombre(forma))
    Canvas(modifier = modifier.semantics { contentDescription = descripcion }) {
        dibujarForma(forma, color)
    }
}

@StringRes
fun nombre(forma: FormaNBack): Int = when (forma) {
    FormaNBack.TRIANGULO -> R.string.forma_triangulo
    FormaNBack.CIRCULO -> R.string.forma_circulo
    FormaNBack.CUADRADO -> R.string.forma_cuadrado
    FormaNBack.ESTRELLA -> R.string.forma_estrella
    FormaNBack.ROMBO -> R.string.forma_rombo
    FormaNBack.HEXAGONO -> R.string.forma_hexagono
    FormaNBack.CRUZ -> R.string.forma_cruz
    FormaNBack.MEDIA_LUNA -> R.string.forma_media_luna
}

private fun DrawScope.dibujarForma(forma: FormaNBack, color: Color) {
    val c = center
    val r = min(size.width, size.height) / 2f * 0.9f
    when (forma) {
        FormaNBack.TRIANGULO -> drawPath(poligono(c, List(3) { r }, -90f), color)
        FormaNBack.CIRCULO -> drawCircle(color, radius = r * 0.85f, center = c)
        FormaNBack.CUADRADO -> {
            val lado = r * 1.5f
            drawRect(color, topLeft = Offset(c.x - lado / 2, c.y - lado / 2), size = Size(lado, lado))
        }
        FormaNBack.ESTRELLA -> drawPath(poligono(c, List(10) { i -> if (i % 2 == 0) r else r * 0.42f }, -90f), color)
        FormaNBack.ROMBO -> drawPath(
            Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r * 0.65f, c.y)
                lineTo(c.x, c.y + r)
                lineTo(c.x - r * 0.65f, c.y)
                close()
            },
            color,
        )
        FormaNBack.HEXAGONO -> drawPath(poligono(c, List(6) { r * 0.9f }, 0f), color)
        FormaNBack.CRUZ -> {
            val largo = r * 1.7f
            val ancho = r * 0.55f
            drawRect(color, topLeft = Offset(c.x - ancho / 2, c.y - largo / 2), size = Size(ancho, largo))
            drawRect(color, topLeft = Offset(c.x - largo / 2, c.y - ancho / 2), size = Size(largo, ancho))
        }
        FormaNBack.MEDIA_LUNA -> {
            val luna = Path().apply { addOval(Rect(c, r * 0.85f)) }
            val corte = Path().apply { addOval(Rect(Offset(c.x + r * 0.45f, c.y - r * 0.2f), r * 0.75f)) }
            drawPath(Path().apply { op(luna, corte, PathOperation.Difference) }, color)
        }
    }
}

/** Polígono con un vértice por radio, repartidos a ángulos iguales desde [anguloInicial] (grados). */
private fun poligono(centro: Offset, radios: List<Float>, anguloInicial: Float): Path = Path().apply {
    radios.forEachIndexed { i, radio ->
        val angulo = (anguloInicial + 360f * i / radios.size) * PI.toFloat() / 180f
        val x = centro.x + radio * cos(angulo)
        val y = centro.y + radio * sin(angulo)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
