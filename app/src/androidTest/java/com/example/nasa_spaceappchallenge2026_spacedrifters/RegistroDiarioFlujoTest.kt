package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Registro diario: desde el panel, rellenarlo, guardarlo y verlo resumido en el panel. */
@RunWith(AndroidJUnit4::class)
class RegistroDiarioFlujoTest {

    @get:Rule(order = 0)
    val limpia = BaseDeDatosLimpia()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun hacerRegistro_seGuardaYSeVeEnElPanel() {
        compose.onNodeWithText(texto(R.string.rol_astronauta)).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.registro_pendiente)) }
        compose.onNodeWithText(texto(R.string.registro_hacer)).performClick()

        compose.waitUntil(5_000) { hay(texto(R.string.registro_sueno_pregunta)) }
        // Sin somnolencia elegida no se puede guardar.
        compose.onNodeWithText(texto(R.string.registro_guardar)).performScrollTo().assertIsNotEnabled()

        compose.onNodeWithContentDescription("7. ${texto(R.string.kss_7)}").performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.sintoma_fatiga)).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.registro_guardar)).performScrollTo().assertIsEnabled().performClick()

        // De vuelta en el panel, con el resumen del registro.
        compose.waitUntil(5_000) { hay(texto(R.string.registro_kss_valor, 7)) }
        compose.onNodeWithText(texto(R.string.sintoma_fatiga)).assertExists()
        compose.onNodeWithText(texto(R.string.registro_editar)).assertExists()
    }
}
