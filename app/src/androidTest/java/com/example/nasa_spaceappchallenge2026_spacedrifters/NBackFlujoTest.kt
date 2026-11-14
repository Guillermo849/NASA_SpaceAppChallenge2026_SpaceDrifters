package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorre el N-Back de verdad: instrucciones → práctica 1-back → un toque en "Coincide" en el
 * primer estímulo (que nunca puede ser coincidencia) → aviso de falsa alarma.
 */
@RunWith(AndroidJUnit4::class)
class NBackFlujoTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    @Test
    fun practica_pulsarEnElPrimerEstimuloEsFalsaAlarma() {
        compose.onNodeWithText(texto(R.string.rol_astronauta)).performClick()
        // En "Pruebas de hoy" el segundo "Empezar" es el del N-Back.
        compose.onAllNodesWithText(texto(R.string.empezar))[1].performClick()
        compose.onNodeWithText(texto(R.string.nback_instrucciones)).assertExists()
        compose.onNodeWithText(texto(R.string.empezar)).performClick()
        compose.onNodeWithText(texto(R.string.nback_recordatorio_1)).assertExists()

        // Espera a que se dibuje la primera forma (tras la pausa inicial).
        val formas = listOf(
            R.string.forma_triangulo, R.string.forma_circulo, R.string.forma_cuadrado, R.string.forma_estrella,
            R.string.forma_rombo, R.string.forma_hexagono, R.string.forma_cruz, R.string.forma_media_luna,
        ).map { texto(it) }
        compose.waitUntil(timeoutMillis = 3_000) {
            formas.any { compose.onAllNodes(hasContentDescription(it)).fetchSemanticsNodes().isNotEmpty() }
        }
        compose.onNodeWithText(texto(R.string.prueba_progreso_practica, 1, 15)).assertExists()

        compose.onNodeWithText(texto(R.string.nback_coincide)).performClick()

        // El aviso sale al cerrarse la ventana de 2,5 s.
        compose.waitUntil(timeoutMillis = 4_000) {
            compose.onAllNodes(hasText(texto(R.string.nback_feedback_falsa_alarma))).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
