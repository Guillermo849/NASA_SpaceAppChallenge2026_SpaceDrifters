package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.AnaliticasNasa
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreParametroAnalitica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.numero
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoDe
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.unidadAnalitica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.LocalEstadoColores

/** "Antes del vuelo · 3 días" / "Tras el regreso · 82 días". */
@Composable
fun momentoMuestra(m: AnaliticasNasa.Muestra): String = pluralStringResource(
    if (m.isAntesDelVuelo) R.plurals.hemo_antes else R.plurals.hemo_despues,
    m.dias,
    m.dias,
)

/**
 * Analíticas reales de NASA (OSDR, misión Inspiration4) del tripulante que corresponde a este
 * astronauta: hemograma (LSDS-7) o bioquímica (LSDS-8). Se elige una muestra, se ve su tabla con lo
 * que sale de rango y se guarda como resultado.
 */
@Composable
fun AnaliticaNasa(
    tipo: TipoPruebaClinica,
    muestras: List<AnaliticasNasa.Muestra>,
    onUsar: (AnaliticasNasa.Muestra) -> Unit,
) {
    // Por defecto, la primera tras el regreso (la más cercana al vuelo).
    val porDefecto = muestras.indexOfFirst { !it.isAntesDelVuelo }.takeIf { it >= 0 } ?: 0
    var elegida by rememberSaveable { mutableIntStateOf(porDefecto) }
    val muestra = muestras.getOrNull(elegida) ?: return
    val alerta = LocalEstadoColores.current.alerta
    val normal = LocalEstadoColores.current.normal
    val bioquimica = tipo == TipoPruebaClinica.BIOQUIMICA

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(if (bioquimica) R.string.bq_titulo else R.string.hemo_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(if (bioquimica) R.string.bq_explicacion else R.string.hemo_explicacion, muestra.tripulante),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            muestras.forEachIndexed { i, m ->
                FilterChip(
                    selected = i == elegida,
                    onClick = { elegida = i },
                    label = { Text(m.momento) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Text(momentoMuestra(muestra), style = MaterialTheme.typography.titleSmall)

        // Resumen: nunca solo con color.
        val fuera = muestra.fueraDeRango()
        Text(
            if (fuera.isEmpty()) stringResource(R.string.hemo_resumen_normal)
            else pluralStringResource(R.plurals.hemo_resumen_alterado, fuera.size, fuera.size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (fuera.isEmpty()) normal else alerta,
        )

        HorizontalDivider()
        muestra.parametros.forEach { p ->
            val unidad = stringResource(unidadAnalitica(p.codigo, p.unidad))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    textoDe(nombreParametroAnalitica(p.codigo), p.codigo),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "${numero(p.valor, decimales(p.valor))} $unidad".trim() + when {
                            p.isBajo -> " · " + stringResource(R.string.hemo_bajo)
                            p.isAlto -> " · " + stringResource(R.string.hemo_alto)
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (p.isFueraDeRango) alerta else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        textoRango(p),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (muestra.sinDato.isNotEmpty()) {
            Text(
                stringResource(R.string.hemo_sin_dato, muestra.sinDato.map { textoDe(nombreParametroAnalitica(it), it) }.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = { onUsar(muestra) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(if (bioquimica) R.string.bq_usar else R.string.hemo_usar))
        }
    }
}

/** "Rango 3,6–5,1", o "≥ 60" / "≤ 1,2" si el rango es abierto por un lado. */
@Composable
private fun textoRango(p: AnaliticasNasa.Parametro): String = when {
    p.hasMinimo() && p.hasMaximo() ->
        stringResource(R.string.hemo_rango, numero(p.minimo, decimales(p.minimo)), numero(p.maximo, decimales(p.maximo)))
    p.hasMinimo() -> stringResource(R.string.bq_rango_min, numero(p.minimo, decimales(p.minimo)))
    p.hasMaximo() -> stringResource(R.string.bq_rango_max, numero(p.maximo, decimales(p.maximo)))
    else -> ""
}

/** Decimales para mostrar un valor del archivo tal cual (11.5 → 1, 2667 → 0). */
private fun decimales(v: Double): Int = if (v == Math.floor(v)) 0 else if (v * 10 == Math.floor(v * 10)) 1 else 2
