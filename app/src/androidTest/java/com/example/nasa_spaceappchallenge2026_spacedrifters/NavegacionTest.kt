package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test instrumentado de ejemplo: el selector de rol navega a la pantalla del médico. */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun selectorRol_navegaATripulacion() {
        val actividad = compose.activity
        compose.onNodeWithText(actividad.getString(R.string.rol_medico)).performClick()
        compose.onNodeWithText(actividad.getString(R.string.medico_tripulacion_titulo)).assertExists()
    }
}
