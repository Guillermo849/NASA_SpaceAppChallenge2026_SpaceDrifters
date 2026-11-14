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

/**
 * De principio a fin: práctica + 40 ensayos del Stroop respondiendo a todo, frase de resultado
 * (sin línea base todavía) y, al volver, la prueba aparece como "Hecha hoy". Tarda ~1 minuto.
 */
@RunWith(AndroidJUnit4::class)
class StroopCompletoTest {

    @get:Rule(order = 0)
    val limpia = BaseDeDatosLimpia()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private val colores by lazy {
        listOf(R.string.color_rojo, R.string.color_azul, R.string.color_verde, R.string.color_amarillo).map { texto(it) }
    }

    private fun hay(t: String) = compose.onAllNodes(hasText(t)).fetchSemanticsNodes().isNotEmpty()

    /** La palabra del estímulo: un nombre de color que no es un botón. */
    private fun hayEstimulo() = colores.any {
        compose.onAllNodes(hasText(it) and !hasClickAction()).fetchSemanticsNodes().isNotEmpty()
    }

    /** Responde a cada estímulo (siempre ROJO) hasta que aparece el texto [fin]. */
    private fun responderHasta(fin: String) {
        while (!hay(fin)) {
            compose.waitUntil(timeoutMillis = 6_000) { hayEstimulo() || hay(fin) }
            if (hayEstimulo()) {
                compose.onAllNodes(hasText(colores[0]) and hasClickAction()).onFirst().performClick()
                compose.waitUntil(timeoutMillis = 6_000) { !hayEstimulo() || hay(fin) }
            }
        }
    }

    @Test
    fun stroopCompleto_guardaYMarcaComoHecha() {
        compose.onNodeWithText(texto(R.string.rol_astronauta)).performClick()
        compose.onAllNodesWithText(texto(R.string.empezar))[0].performClick()
        compose.waitUntil(timeoutMillis = 3_000) { hay(texto(R.string.stroop_practica_aviso)) }
        compose.onNodeWithText(texto(R.string.empezar)).performClick()

        responderHasta(texto(R.string.fin_practica_titulo))
        compose.onNodeWithText(texto(R.string.empezar_prueba)).performClick()

        responderHasta(texto(R.string.prueba_terminada_titulo))
        // Sin sesiones de línea base, nunca "dentro de tu rango".
        compose.onNodeWithText(texto(R.string.resultado_sin_linea_base)).assertExists()

        compose.onNodeWithText(texto(R.string.volver_panel)).performClick()
        compose.waitUntil(timeoutMillis = 3_000) { hay(texto(R.string.prueba_hecha_hoy)) }
        compose.onNodeWithText(texto(R.string.repetir)).assertExists()
    }
}
