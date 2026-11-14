package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** "Tengo un síntoma de alarma" → pantalla de emergencia (nivel 5) → atendida. */
@RunWith(AndroidJUnit4::class)
class EmergenciaFlujoTest {

    @get:Rule(order = 0)
    val limpia = BaseDeDatosLimpia()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun sintomaDeAlarma_abreLaEmergenciaYSeCierraAlAtenderla() {
        compose.entrarComoAstronauta()
        compose.onNodeWithText(texto(R.string.alarma_boton)).performClick()

        // Sin marcar nada no se puede activar.
        compose.onNodeWithText(texto(R.string.alarma_activar)).assertExists()
        compose.onNodeWithText(texto(R.string.signo_dolor_toracico)).performClick()
        compose.onNodeWithText(texto(R.string.signo_sincope)).performClick()
        compose.onNodeWithText(texto(R.string.alarma_activar)).performClick()

        // Pantalla de emergencia con el motivo y los pasos.
        compose.waitUntil(5_000) { hay(texto(R.string.emergencia_titulo)) }
        compose.onNodeWithText(texto(R.string.emergencia_nivel)).assertExists()
        compose.onNodeWithText(texto(R.string.signo_dolor_toracico), substring = true).assertExists()
        compose.onNodeWithText(texto(R.string.emergencia_paso_1)).assertExists()

        // Atendida (con confirmación): desaparece.
        compose.onNodeWithText(texto(R.string.emergencia_atendida)).performClick()
        compose.onNodeWithText(texto(R.string.emergencia_atendida_si)).performClick()
        compose.waitUntil(5_000) { !hay(texto(R.string.emergencia_nivel)) }
        compose.onNodeWithText(texto(R.string.alarma_boton)).assertExists()
    }
}
