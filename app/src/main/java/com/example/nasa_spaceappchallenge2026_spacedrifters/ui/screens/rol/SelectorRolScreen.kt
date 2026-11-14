package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.rol

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Pantalla inicial: elegir rol. Sin login. */
@Composable
fun SelectorRolScreen(
    onAstronauta: () -> Unit,
    onMedico: () -> Unit,
) {
    PantallaBase(titulo = stringResource(R.string.selector_titulo)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.selector_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TarjetaRol(
                icono = R.drawable.ic_astronauta,
                descripcionIcono = stringResource(R.string.cd_astronauta),
                titulo = stringResource(R.string.rol_astronauta),
                descripcion = stringResource(R.string.rol_astronauta_desc),
                onClick = onAstronauta,
            )
            TarjetaRol(
                icono = R.drawable.ic_medico,
                descripcionIcono = stringResource(R.string.cd_medico),
                titulo = stringResource(R.string.rol_medico),
                descripcion = stringResource(R.string.rol_medico_desc),
                onClick = onMedico,
            )
        }
    }
}

@Composable
private fun TarjetaRol(
    @DrawableRes icono: Int,
    descripcionIcono: String,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icono),
                contentDescription = descripcionIcono,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Column {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectorRolPreview() {
    SpaceDriftersTheme {
        SelectorRolScreen(onAstronauta = {}, onMedico = {})
    }
}
