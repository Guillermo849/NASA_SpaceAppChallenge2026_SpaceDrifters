package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Inicio del astronauta: de momento, la tarjeta "Pruebas de hoy". */
@Composable
fun InicioAstronautaScreen(
    onAtras: () -> Unit,
    onEmpezarStroop: () -> Unit,
    onEmpezarNBack: () -> Unit,
) {
    PantallaBase(
        titulo = stringResource(R.string.astronauta_inicio_titulo),
        onAtras = onAtras,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TarjetaPruebasDeHoy(onEmpezarStroop = onEmpezarStroop, onEmpezarNBack = onEmpezarNBack)
        }
    }
}

@Composable
private fun TarjetaPruebasDeHoy(onEmpezarStroop: () -> Unit, onEmpezarNBack: () -> Unit) {
    Card(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.pruebas_hoy_titulo), style = MaterialTheme.typography.titleLarge)
            FilaPrueba(
                nombre = stringResource(R.string.stroop_nombre),
                descripcion = stringResource(R.string.stroop_descripcion_corta),
                onEmpezar = onEmpezarStroop,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            FilaPrueba(
                nombre = stringResource(R.string.nback_nombre),
                descripcion = stringResource(R.string.nback_descripcion_corta),
                onEmpezar = onEmpezarNBack,
            )
        }
    }
}

@Composable
private fun FilaPrueba(nombre: String, descripcion: String, onEmpezar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = nombre, style = MaterialTheme.typography.titleMedium)
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onEmpezar, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.empezar))
        }
    }
}

@Preview
@Composable
private fun InicioAstronautaPreview() {
    SpaceDriftersTheme { InicioAstronautaScreen(onAtras = {}, onEmpezarStroop = {}, onEmpezarNBack = {}) }
}
