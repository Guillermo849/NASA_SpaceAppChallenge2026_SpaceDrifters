package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.colorEstado

/** Escala vertical de la gráfica del médico: incluye todos los puntos y el rango habitual. */
data class EscalaGrafica(val minimo: Double, val maximo: Double)

fun escalaMedico(puntos: List<PuntoMedico>, media: Double?, desviacion: Double?): EscalaGrafica {
    val valores = puntos.map { it.valor }.toMutableList()
    if (media != null && desviacion != null) {
        valores += media - desviacion
        valores += media + desviacion
    }
    val bajo = valores.minOrNull() ?: return EscalaGrafica(0.0, 1.0)
    val alto = valores.maxOrNull() ?: return EscalaGrafica(0.0, 1.0)
    val rango = (alto - bajo).takeIf { it > 1e-9 } ?: 1.0
    return EscalaGrafica(bajo - rango * 0.1, alto + rango * 0.1)
}

/**
 * Gráfica de 30 días para el médico, con los valores reales (efecto Stroop en ms, d′): un punto por
 * sesión (círculo del color de su estado; las de línea base, cuadrado hueco), la franja del rango
 * habitual (media ± 1 desviación) y la media en discontinua. [descripcion] es el resumen accesible.
 */
@Composable
fun GraficaMedico(
    puntos: List<PuntoMedico>,
    media: Double?,
    desviacion: Double?,
    escala: EscalaGrafica,
    desde: Long,
    hasta: Long,
    descripcion: String,
    modifier: Modifier = Modifier,
) {
    val colorBanda = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val colorMedia = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val colorLinea = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val colorLineaBase = MaterialTheme.colorScheme.onSurface
    val colorRejilla = MaterialTheme.colorScheme.outline
    val colores = puntos.map { colorEstado(it.estado) }

    Canvas(modifier.semantics { contentDescription = descripcion }) {
        val margen = 10.dp.toPx()
        val ancho = size.width - 2 * margen
        val alto = size.height - 2 * margen
        fun y(v: Double): Float =
            margen + alto * (1f - ((v - escala.minimo) / (escala.maximo - escala.minimo)).toFloat().coerceIn(0f, 1f))
        fun x(t: Long): Float =
            if (hasta <= desde) margen + ancho / 2 else margen + ancho * ((t - desde).toFloat() / (hasta - desde)).coerceIn(0f, 1f)

        // Bordes de la zona de la gráfica.
        drawLine(colorRejilla, Offset(0f, margen), Offset(size.width, margen), strokeWidth = 1.dp.toPx())
        drawLine(colorRejilla, Offset(0f, size.height - margen), Offset(size.width, size.height - margen), strokeWidth = 1.dp.toPx())

        if (media != null && desviacion != null) {
            val arriba = y(media + desviacion)
            drawRect(colorBanda, topLeft = Offset(0f, arriba), size = Size(size.width, y(media - desviacion) - arriba))
            drawLine(
                colorMedia, Offset(0f, y(media)), Offset(size.width, y(media)),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
        }

        // Línea que une las sesiones de misión (la línea base no forma parte de la evolución).
        val mision = puntos.filter { !it.esLineaBase }
        if (mision.size > 1) {
            val linea = Path()
            mision.forEachIndexed { i, p ->
                if (i == 0) linea.moveTo(x(p.fechaEn), y(p.valor)) else linea.lineTo(x(p.fechaEn), y(p.valor))
            }
            drawPath(linea, colorLinea, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        puntos.forEachIndexed { i, p ->
            val c = Offset(x(p.fechaEn), y(p.valor))
            if (p.esLineaBase) {
                val lado = 9.dp.toPx()
                drawRect(
                    colorLineaBase,
                    topLeft = Offset(c.x - lado / 2, c.y - lado / 2),
                    size = Size(lado, lado),
                    style = Stroke(width = 2.dp.toPx()),
                )
            } else {
                drawCircle(colores[i], radius = 4.5.dp.toPx(), center = c)
            }
        }
    }
}
