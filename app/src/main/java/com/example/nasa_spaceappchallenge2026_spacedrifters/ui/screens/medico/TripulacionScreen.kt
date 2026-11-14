package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.EnConstruccion
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.components.PantallaBase
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.theme.SpaceDriftersTheme

/** Vista de tripulación del médico de vuelo. Vacía por ahora. */
@Composable
fun TripulacionScreen(onAtras: () -> Unit) {
    PantallaBase(
        titulo = stringResource(R.string.medico_tripulacion_titulo),
        onAtras = onAtras,
    ) { padding ->
        EnConstruccion(padding)
    }
}

@Preview
@Composable
private fun TripulacionPreview() {
    SpaceDriftersTheme { TripulacionScreen(onAtras = {}) }
}
