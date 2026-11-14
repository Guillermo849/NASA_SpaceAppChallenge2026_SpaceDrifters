package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.PruebaParaResultado
import com.example.nasa_spaceappchallenge2026_spacedrifters.di.AppViewModelProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.HemogramasNasa
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombreOpcionResultado
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.nombrePruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.textoDe
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** La oficial médico introduce el resultado de una prueba; al guardar, el protocolo sigue. */
@Composable
fun ResultadoPruebaScreen(
    onAtras: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: ResultadoPruebaViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    PantallaBase(titulo = stringResource(R.string.resultado_titulo), onAtras = onAtras) { padding ->
        when (val e = estado) {
            EstadoResultado.Cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            EstadoResultado.NoExiste -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.resultado_no_existe))
            }
            is EstadoResultado.Listo -> FormularioResultado(
                datos = e.datos,
                modifier = Modifier.padding(padding),
                onOpcion = { opcion -> viewModel.guardarOpcion(e.datos.prueba.tipo, opcion); onGuardado() },
                onEcocardio = { vs, gc -> viewModel.guardarEcocardio(vs, gc); onGuardado() },
                onHemogramaNasa = { muestra -> viewModel.guardarHemogramaNasa(muestra); onGuardado() },
            )
        }
    }
}

@Composable
private fun FormularioResultado(
    datos: PruebaParaResultado,
    modifier: Modifier = Modifier,
    onOpcion: (String) -> Unit,
    onEcocardio: (Double, Double) -> Unit,
    onHemogramaNasa: (HemogramasNasa.Muestra) -> Unit = {},
) {
    val prueba = datos.prueba
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.resultado_para, stringResource(nombrePruebaClinica(prueba.tipo)), datos.nombre ?: "–"),
            style = MaterialTheme.typography.titleLarge,
        )
        if (prueba.realizadaEn != null) {
            Text(stringResource(R.string.resultado_ya_hecha), style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        if (datos.muestrasNasa.isNotEmpty()) {
            // Hemograma: primero los datos reales de NASA; debajo, a mano.
            HemogramaNasa(datos.muestrasNasa, onHemogramaNasa)
            Text(stringResource(R.string.hemo_o_a_mano), style = MaterialTheme.typography.titleSmall)
        }
        if (datos.opciones.isNotEmpty()) {
            OpcionesCerradas(datos.opciones, onOpcion)
        } else {
            CamposEcocardio(onEcocardio)
        }
    }
}

@Composable
private fun OpcionesCerradas(opciones: List<String>, onGuardar: (String) -> Unit) {
    var elegida by rememberSaveable { mutableStateOf<String?>(null) }
    Text(stringResource(R.string.resultado_elige), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        opciones.forEach { opcion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = opcion == elegida, role = Role.RadioButton) { elegida = opcion },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = opcion == elegida, onClick = null)
                Text(
                    textoDe(nombreOpcionResultado(opcion), opcion),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
    Button(
        onClick = { elegida?.let(onGuardar) },
        enabled = elegida != null,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Text(stringResource(R.string.resultado_guardar))
    }
}

@Composable
private fun CamposEcocardio(onGuardar: (Double, Double) -> Unit) {
    var vs by rememberSaveable { mutableStateOf("") }
    var gc by rememberSaveable { mutableStateOf("") }
    // Admite coma o punto decimal.
    fun numero(t: String) = t.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    val vsOk = numero(vs)
    val gcOk = numero(gc)
    CampoNumero(vs, { vs = it }, stringResource(R.string.resultado_vs), error = vs.isNotBlank() && vsOk == null)
    CampoNumero(gc, { gc = it }, stringResource(R.string.resultado_gc), error = gc.isNotBlank() && gcOk == null)
    Button(
        onClick = { if (vsOk != null && gcOk != null) onGuardar(vsOk, gcOk) },
        enabled = vsOk != null && gcOk != null,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Text(stringResource(R.string.resultado_guardar))
    }
}

@Composable
private fun CampoNumero(valor: String, onCambio: (String) -> Unit, etiqueta: String, error: Boolean) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error,
        supportingText = if (error) {
            { Text(stringResource(R.string.resultado_numero_invalido)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun FormularioResultadoPreview() {
    SpaceDriftersTheme {
        FormularioResultado(
            PruebaParaResultado(
                PruebaClinica(id = 1, astronautaId = 2, tipo = TipoPruebaClinica.ECO_DOPPLER_YUGULAR, pedidaEn = 0),
                "Leo Martín",
                InterpretePruebas.opciones(InterpretePruebas.ECO_DOPPLER_YUGULAR),
            ),
            onOpcion = {},
            onEcocardio = { _, _ -> },
        )
    }
}
