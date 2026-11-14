package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** El médico ve la tripulación, el perfil de cada astronauta y puede confirmar una alerta. */
@RunWith(AndroidJUnit4::class)
class MedicoFlujoTest {

    @get:Rule(order = 0)
    val datos = ConDatosEjemplo()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun abrirPerfil(nombre: String) {
        compose.onNodeWithText(texto(R.string.rol_medico)).performClick()
        compose.waitUntil(5_000) { hay(nombre) }
        compose.onNodeWithText(nombre).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.medico_pestana_cognitivas)) && hay(texto(R.string.medico_ultima_sesion)) }
    }

    @Test
    fun maya_suUltimaSesionVieneDeCamino() {
        abrirPerfil("Maya Okafor")
        // "1 sesión de camino, llega en 13 min"
        val enCamino = compose.activity.resources.getQuantityString(R.plurals.medico_en_camino, 1, 1, 0).substringBefore("0")
        compose.waitUntil(5_000) { hay(enCamino.substringBefore(",")) }
    }

    @Test
    fun leo_confirmarSuAlerta() {
        abrirPerfil("Leo Martín")
        val lista = compose.onNode(hasScrollAction())
        lista.performScrollToNode(hasText(texto(R.string.medico_regla_concentracion)))
        compose.onNodeWithText(texto(R.string.medico_por_que)).assertExists()

        lista.performScrollToNode(hasText(texto(R.string.medico_confirmar)))
        compose.onNodeWithText(texto(R.string.medico_confirmar)).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.medico_sin_alertas_activas)) }
    }

    @Test
    fun abrirElDetalleDeUnaSesion() {
        abrirPerfil("Ane Etxeberria")
        val lista = compose.onNode(hasScrollAction())
        lista.performScrollToNode(hasText(texto(R.string.medico_valida_si), substring = true))
        compose.onAllNodes(hasText(texto(R.string.medico_valida_si), substring = true))[0].performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.medico_ensayos_titulo, 40).substringBefore("(")) }
    }
}
