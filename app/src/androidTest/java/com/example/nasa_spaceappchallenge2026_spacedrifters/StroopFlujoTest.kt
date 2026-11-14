package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Recorre el Stroop de verdad: instrucciones → práctica → un toque → aviso de acierto o fallo. */
@RunWith(AndroidJUnit4::class)
class StroopFlujoTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    @Test
    fun practica_tocarUnColorMuestraElAviso() {
        compose.onNodeWithText(texto(R.string.rol_astronauta)).performClick()
        // En "Pruebas de hoy" el primer "Empezar" es el del Stroop.
        compose.onAllNodesWithText(texto(R.string.empezar))[0].performClick()
        compose.onNodeWithText(texto(R.string.stroop_instrucciones)).assertExists()
        compose.onNodeWithText(texto(R.string.empezar)).performClick()

        compose.onNodeWithText(texto(R.string.prueba_progreso_practica, 1, 8)).assertExists()

        // Espera a que aparezca la palabra (texto de color que NO es un botón).
        val colores = listOf(R.string.color_rojo, R.string.color_azul, R.string.color_verde, R.string.color_amarillo)
            .map { texto(it) }
        compose.waitUntil(timeoutMillis = 3_000) {
            colores.any { c ->
                compose.onAllNodes(hasText(c) and !hasClickAction()).fetchSemanticsNodes().isNotEmpty()
            }
        }

        compose.onAllNodes(hasText(texto(R.string.color_rojo)) and hasClickAction()).onFirst().performClick()

        val avisos = listOf(R.string.feedback_correcto, R.string.feedback_incorrecto).map { texto(it) }
        compose.waitUntil(timeoutMillis = 2_000) {
            avisos.any { compose.onAllNodes(hasText(it)).fetchSemanticsNodes().isNotEmpty() }
        }
    }
}
