package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test instrumentado de ejemplo: al entrar como médico se abre la tripulación. */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun login_medicoVaATripulacion() {
        val actividad = compose.activity
        compose.entrarComoMedico()
        compose.onNodeWithText(actividad.getString(R.string.medico_tripulacion_titulo)).assertExists()
    }
}
