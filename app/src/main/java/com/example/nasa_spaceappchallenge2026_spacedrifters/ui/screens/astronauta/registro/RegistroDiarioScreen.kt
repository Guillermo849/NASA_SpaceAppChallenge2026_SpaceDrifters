package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.registro

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoEjercicio
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.registro.ReglasRegistro
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme
import java.text.NumberFormat

/** Registro diario: horas de sueño, somnolencia (KSS) y síntomas de hoy. */
@Composable
fun RegistroDiarioScreen(
    onAtras: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: RegistroDiarioViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.guardado) { if (ui.guardado) onGuardado() }
    RegistroDiarioContenido(
        ui = ui,
        onAtras = onAtras,
        onSumarSueno = viewModel::sumarSueno,
        onSomnolencia = viewModel::elegirSomnolencia,
        onSintoma = viewModel::alternarSintoma,
        onSinSintomas = viewModel::sinSintomas,
        onGuardar = viewModel::guardar,
        habitos = AccionesHabitos(
            onSumarAgua = viewModel::sumarAgua,
            onIngesta = viewModel::elegirIngesta,
            onSumarEjercicio = viewModel::sumarEjercicio,
            onEjercicio = viewModel::alternarEjercicio,
            onSinEjercicio = viewModel::sinEjercicio,
        ),
    )
}

/** Lo que se puede hacer en los apartados de hidratación, nutrición y actividad física. */
private data class AccionesHabitos(
    val onSumarAgua: (Int) -> Unit = {},
    val onIngesta: (Int) -> Unit = {},
    val onSumarEjercicio: (Int) -> Unit = {},
    val onEjercicio: (TipoEjercicio) -> Unit = {},
    val onSinEjercicio: () -> Unit = {},
)

@Composable
private fun RegistroDiarioContenido(
    ui: RegistroUi,
    onAtras: () -> Unit,
    onSumarSueno: (Int) -> Unit,
    onSomnolencia: (Int) -> Unit,
    onSintoma: (Sintoma) -> Unit,
    onSinSintomas: () -> Unit,
    onGuardar: () -> Unit,
    habitos: AccionesHabitos = AccionesHabitos(),
) {
    PantallaBase(titulo = stringResource(R.string.registro_titulo), onAtras = onAtras) { padding ->
        if (ui.cargando) return@PantallaBase
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Seccion(titulo = R.string.registro_sueno_pregunta) { Sueno(ui.horasSueno, onSumarSueno) }
            Seccion(titulo = R.string.registro_somnolencia_pregunta) { Somnolencia(ui.somnolencia, onSomnolencia) }
            Seccion(titulo = R.string.registro_sintomas_pregunta) { Sintomas(ui.sintomas, onSintoma, onSinSintomas) }

            // Hábitos de ayer: opcionales; lo que no se anota se guarda como "Sin datos".
            Text(text = stringResource(R.string.registro_habitos_titulo), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.registro_habitos_ayuda),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Seccion(titulo = R.string.registro_agua_pregunta) {
                Agua(ui.aguaLitros, ui.objetivos.aguaLitros, habitos.onSumarAgua)
            }
            Seccion(titulo = R.string.registro_ingesta_pregunta) { Ingesta(ui.ingesta, habitos.onIngesta) }
            Seccion(titulo = R.string.registro_ejercicio_pregunta) {
                Ejercicio(ui.ejercicioMinutos, ui.ejercicios, ui.objetivos.ejercicioMinutos, habitos)
            }

            if (ui.error) {
                Text(
                    text = stringResource(R.string.registro_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (ui.somnolencia == null) {
                Text(
                    text = stringResource(R.string.registro_falta_somnolencia),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onGuardar,
                enabled = ui.puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(if (ui.esEdicion) R.string.registro_guardar_cambios else R.string.registro_guardar))
            }
        }
    }
}

@Composable
private fun Seccion(@StringRes titulo: Int, contenido: @Composable () -> Unit) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(titulo), style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

// ---- Horas de sueño ----

@Composable
private fun Sueno(horas: Double, onSumar: (Int) -> Unit) {
    val formato = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonPaso("−", stringResource(R.string.cd_menos_sueno), enabled = horas > ReglasRegistro.SUENO_MIN) { onSumar(-1) }
        Text(
            text = stringResource(R.string.registro_horas, formato.format(horas)),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
        BotonPaso("+", stringResource(R.string.cd_mas_sueno), enabled = horas < ReglasRegistro.SUENO_MAX) { onSumar(1) }
    }
}

@Composable
private fun BotonPaso(simbolo: String, descripcion: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(56.dp)
            .semantics { contentDescription = descripcion },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text = simbolo, style = MaterialTheme.typography.headlineMedium)
    }
}

// ---- Somnolencia (escala de Karolinska) ----

@Composable
private fun Somnolencia(seleccion: Int?, onElegir: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Dos filas (1–5 y 6–9) para que cada botón tenga al menos 48 dp de ancho.
        listOf(1..5, 6..9).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                fila.forEach { k ->
                    val elegido = k == seleccion
                    val descripcion = "$k. ${descripcionKss(k)}"
                    val modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = descripcion }
                    if (elegido) {
                        Button(onClick = { onElegir(k) }, modifier = modifier, contentPadding = PaddingValues(0.dp)) {
                            Text(k.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        OutlinedButton(onClick = { onElegir(k) }, modifier = modifier, contentPadding = PaddingValues(0.dp)) {
                            Text(k.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                // La segunda fila tiene 4: un hueco para que los botones midan lo mismo que arriba.
                if (fila.count() < 5) Spacer(Modifier.weight(1f))
            }
        }
        Text(
            text = seleccion?.let { "$it · ${descripcionKss(it)}" } ?: stringResource(R.string.registro_kss_extremos),
            style = MaterialTheme.typography.bodyMedium,
            color = if (seleccion != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun descripcionKss(k: Int): String = stringResource(
    when (k) {
        1 -> R.string.kss_1
        2 -> R.string.kss_2
        3 -> R.string.kss_3
        4 -> R.string.kss_4
        5 -> R.string.kss_5
        6 -> R.string.kss_6
        7 -> R.string.kss_7
        8 -> R.string.kss_8
        else -> R.string.kss_9
    },
)

// ---- Síntomas ----

@Composable
private fun Sintomas(seleccion: Set<Sintoma>, onSintoma: (Sintoma) -> Unit, onNinguno: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = seleccion.isEmpty(),
            onClick = onNinguno,
            label = { Text(stringResource(R.string.sintomas_ninguno)) },
        )
        Sintoma.entries.forEach { s ->
            FilterChip(
                selected = s in seleccion,
                onClick = { onSintoma(s) },
                label = { Text(stringResource(nombreSintoma(s))) },
            )
        }
    }
}

// ---- Hidratación ----

@Composable
private fun Agua(litros: Double?, objetivo: Double, onSumar: (Int) -> Unit) {
    Contador(
        valor = litros?.let { stringResource(R.string.registro_litros, numeroCorto(it, 2)) },
        menos = stringResource(R.string.cd_menos_agua),
        mas = stringResource(R.string.cd_mas_agua),
        puedeMenos = litros == null || litros > ReglasRegistro.AGUA_MIN,
        puedeMas = litros == null || litros < ReglasRegistro.AGUA_MAX,
        onSumar = onSumar,
    )
    TextoObjetivo(
        objetivo = stringResource(R.string.registro_objetivo_agua, numeroCorto(objetivo, 2)),
        cumple = ReglasRegistro.cumpleObjetivo(litros, objetivo),
    )
}

// ---- Nutrición ----

@Composable
private fun Ingesta(seleccion: Int?, onElegir: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ReglasRegistro.INGESTA_OPCIONES.forEach { porcentaje ->
            FilterChip(
                selected = porcentaje == seleccion,
                onClick = { onElegir(porcentaje) },
                label = { Text(stringResource(nombreIngesta(porcentaje))) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
    Text(
        text = if (seleccion == null) {
            stringResource(R.string.registro_sin_anotar)
        } else {
            stringResource(R.string.registro_ingesta_valor, seleccion)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@StringRes
fun nombreIngesta(porcentaje: Int): Int = when (porcentaje) {
    0 -> R.string.ingesta_nada
    25 -> R.string.ingesta_poco
    50 -> R.string.ingesta_mitad
    75 -> R.string.ingesta_casi_todo
    else -> R.string.ingesta_todo
}

// ---- Actividad física ----

@Composable
private fun Ejercicio(minutos: Int?, tipos: Set<TipoEjercicio>, objetivo: Int, acciones: AccionesHabitos) {
    Contador(
        valor = minutos?.let { stringResource(R.string.registro_minutos, it) },
        menos = stringResource(R.string.cd_menos_ejercicio),
        mas = stringResource(R.string.cd_mas_ejercicio),
        puedeMenos = minutos == null || minutos > ReglasRegistro.EJERCICIO_MIN,
        puedeMas = minutos == null || minutos < ReglasRegistro.EJERCICIO_MAX,
        onSumar = acciones.onSumarEjercicio,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = minutos == 0,
            onClick = acciones.onSinEjercicio,
            label = { Text(stringResource(R.string.ejercicio_ninguno)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        TipoEjercicio.entries.forEach { t ->
            FilterChip(
                selected = t in tipos,
                onClick = { acciones.onEjercicio(t) },
                label = { Text(stringResource(nombreEjercicio(t))) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
    TextoObjetivo(
        objetivo = stringResource(R.string.registro_objetivo_ejercicio, objetivo),
        cumple = ReglasRegistro.cumpleObjetivo(minutos?.toDouble(), objetivo.toDouble()),
    )
}

@StringRes
fun nombreEjercicio(tipo: TipoEjercicio): Int = when (tipo) {
    TipoEjercicio.CINTA -> R.string.ejercicio_cinta
    TipoEjercicio.BICICLETA -> R.string.ejercicio_bicicleta
    TipoEjercicio.FUERZA -> R.string.ejercicio_fuerza
}

// ---- Piezas comunes ----

/** − valor + ; sin valor muestra "Sin anotar" y el primer toque pone el objetivo. */
@Composable
private fun Contador(
    valor: String?,
    menos: String,
    mas: String,
    puedeMenos: Boolean,
    puedeMas: Boolean,
    onSumar: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonPaso("−", menos, enabled = puedeMenos) { onSumar(-1) }
        Text(
            text = valor ?: stringResource(R.string.registro_sin_anotar),
            style = if (valor != null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = if (valor != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
        BotonPaso("+", mas, enabled = puedeMas) { onSumar(1) }
    }
}

/** Objetivo diario y, si hay dato, si se ha llegado (con texto, no solo color). */
@Composable
private fun TextoObjetivo(objetivo: String, cumple: Boolean?) {
    val estado = when (cumple) {
        null -> null
        true -> stringResource(R.string.registro_objetivo_cumplido)
        false -> stringResource(R.string.registro_objetivo_bajo)
    }
    Text(
        text = listOfNotNull(objetivo, estado).joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun numeroCorto(valor: Double, decimales: Int): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 0
        maximumFractionDigits = decimales
    }.format(valor)

@StringRes
fun nombreSintoma(sintoma: Sintoma): Int = when (sintoma) {
    Sintoma.DOLOR_CABEZA -> R.string.sintoma_dolor_cabeza
    Sintoma.DIFICULTAD_CONCENTRACION -> R.string.sintoma_dificultad_concentracion
    Sintoma.FATIGA -> R.string.sintoma_fatiga
    Sintoma.NAUSEAS -> R.string.sintoma_nauseas
}

@Preview(heightDp = 1800)
@Composable
private fun RegistroDiarioPreview() {
    SpaceDriftersTheme {
        RegistroDiarioContenido(
            ui = RegistroUi(
                cargando = false, horasSueno = 6.5, somnolencia = 7, sintomas = setOf(Sintoma.FATIGA),
                aguaLitros = 1.5, ingesta = 75, ejercicioMinutos = null,
            ),
            onAtras = {}, onSumarSueno = {}, onSomnolencia = {}, onSintoma = {}, onSinSintomas = {}, onGuardar = {},
        )
    }
}
