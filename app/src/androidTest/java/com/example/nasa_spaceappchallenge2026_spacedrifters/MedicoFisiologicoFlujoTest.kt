package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pestaña "Fisiológico" del perfil: protocolo, constantes, y confirmar o bajar el nivel. */
@RunWith(AndroidJUnit4::class)
class MedicoFisiologicoFlujoTest {

    @get:Rule(order = 0)
    val datos = ConDatosEjemplo()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun abrirFisiologicoDe(nombre: String) {
        compose.entrarComoMedico()
        compose.waitUntil(5_000) { hay(nombre) }
        compose.onNodeWithText(nombre).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.medico_pestana_fisiologico)) }
        compose.onNodeWithText(texto(R.string.medico_pestana_fisiologico)).performClick()
    }

    @Test
    fun maya_protocoloConstantesYBajarNivel() {
        abrirFisiologicoDe("Maya Okafor")
        compose.waitUntil(5_000) { hay(texto(R.string.protocolo_hemodinamico)) }
        val nivel3 = texto(R.string.nivel_con_numero, 3, texto(R.string.nivel_3))
        assertTrue(hay(nivel3))
        // Línea de tiempo con el "por qué" en texto, y las constantes.
        assertTrue(hay(texto(R.string.hecho_fc_alta)))
        assertTrue(hay(texto(R.string.fisio_fc)))
        assertTrue(hay(texto(R.string.fisio_pas)))

        compose.onNodeWithText(texto(R.string.fisio_confirmar_nivel)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.fisio_msg_nivel_confirmado)) }

        compose.onNodeWithText(texto(R.string.fisio_bajar_nivel)).performScrollTo().performClick()
        val nivel2 = texto(R.string.nivel_con_numero, 2, texto(R.string.nivel_2))
        compose.onAllNodesWithText(nivel2).fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
        compose.onAllNodesWithText(nivel2)[0].performClick()
        compose.onNodeWithText(texto(R.string.fisio_aceptar)).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.fisio_msg_nivel_bajado)) }
        compose.waitUntil(5_000) { hay(nivel2) && !hay(nivel3) }
    }

    @Test
    fun leo_comparaElEcoDoppler() {
        abrirFisiologicoDe("Leo Martín")
        compose.waitUntil(5_000) { hay(texto(R.string.protocolo_trombosis_yugular)) }
        // El eco doppler de la demo (retrógrado) ya ha llegado: aparece en la comparación.
        assertTrue(hay(texto(R.string.fisio_comparacion)))
        assertTrue(hay(texto(R.string.opcion_retrogrado)))
        assertTrue(hay(texto(R.string.fisio_sin_anterior)))
    }
}
