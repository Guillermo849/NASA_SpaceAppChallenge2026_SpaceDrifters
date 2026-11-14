package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import kotlinx.coroutines.runBlocking
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El perfil del médico ve los protocolos de la demo y mete resultados: Maya repite constantes
 * ("igual o peor") → se pide un ecocardiograma → VS bajo → nivel 4.
 */
@RunWith(AndroidJUnit4::class)
class OficialMedicoFlujoTest {

    @get:Rule(order = 0)
    val datos = ConDatosEjemplo()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun leo_hemogramaRealDeNasa_alteradoConHemoglobinaBaja() {
        compose.entrarComoMedico()
        compose.waitUntil(5_000) { hay(texto(R.string.oficial_titulo)) }
        compose.onNodeWithText(texto(R.string.oficial_titulo)).performClick()
        compose.waitUntil(5_000) { hay("Leo Martín") }
        val hemograma = texto(R.string.prueba_hemograma)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(hemograma))
        // El botón "Introducir resultado" de la fila del hemograma (a su misma altura).
        val fila = compose.onNodeWithText(hemograma).fetchSemanticsNode().boundsInRoot
        val botones = compose.onAllNodesWithText(texto(R.string.oficial_introducir)).fetchSemanticsNodes()
        val i = botones.indexOfFirst { it.boundsInRoot.center.y in (fila.top - 60f)..(fila.bottom + 60f) }
        compose.onAllNodesWithText(texto(R.string.oficial_introducir))[i].performClick()

        // Hemogramas reales del tripulante C002 (Leo); R+82 tiene hemoglobina y hematocrito bajos.
        compose.waitUntil(5_000) { hay(texto(R.string.hemo_titulo)) }
        compose.onNodeWithText("R+82").performClick()
        val alterado = compose.activity.resources.getQuantityString(R.plurals.hemo_resumen_alterado, 2, 2)
        compose.waitUntil(5_000) { hay(alterado) }
        compose.onNodeWithText(texto(R.string.hemo_usar)).performScrollTo().performClick()

        // Guardado como ALTERADO con su muestra; el protocolo sigue esperando dímero D y coagulación.
        val dao = ApplicationProvider.getApplicationContext<SpaceDriftersApp>().container.database.fisiologicoDao()
        compose.waitUntil(5_000) {
            runBlocking { dao.realizadasDe(2L) }.any { it.tipo == TipoPruebaClinica.HEMOGRAMA }
        }
        val hecho = runBlocking { dao.realizadasDe(2L) }.single { it.tipo == TipoPruebaClinica.HEMOGRAMA }
        assertTrue(hecho.resultado!!, hecho.resultado!!.contains("ALTERADO") && hecho.resultado!!.contains("R+82"))
    }

    @Test
    fun maya_veSuProtocoloArribaDelPanel_yNoElDeLaOficial() {
        compose.entrarComoAstronauta()
        compose.waitUntil(5_000) { hay(texto(R.string.protocolo_hemodinamico)) }
        assertTrue(hay(texto(R.string.nivel_con_numero, 3, texto(R.string.nivel_3))))
        assertTrue(hay(texto(R.string.astronauta_protocolo_que_hacer)))
        assertTrue(hay(texto(R.string.accion_revisar_balance_hidrico_24h)))
        // La gestión de los protocolos de toda la tripulación es solo del perfil del médico.
        assertFalse(hay(texto(R.string.oficial_titulo)))
    }

    @Test
    fun repeticionYEcocardio_subeANivel4() {
        // El médico (en la Tierra o a bordo) abre "Protocolos a bordo" desde la tripulación.
        compose.entrarComoMedico()
        compose.waitUntil(5_000) { hay(texto(R.string.oficial_titulo)) }
        compose.onNodeWithText(texto(R.string.oficial_titulo)).performClick()
        compose.waitUntil(5_000) { hay("Maya Okafor") }
        // Maya va primero (actualizada más tarde): su prueba pendiente es repetir las constantes.
        compose.onNodeWithText(texto(R.string.prueba_repeticion_constantes)).assertExists()
        compose.onAllNodesWithText(texto(R.string.oficial_introducir))[0].performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.resultado_elige)) }
        compose.onNodeWithText(texto(R.string.opcion_igual_o_peor)).performClick()
        compose.onNodeWithText(texto(R.string.resultado_guardar)).performClick()

        // De vuelta en la lista: ahora pide un ecocardiograma.
        compose.waitUntil(5_000) { hay(texto(R.string.prueba_ecocardio)) }
        compose.onAllNodesWithText(texto(R.string.oficial_introducir))[0].performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.resultado_vs)) }
        compose.onNodeWithText(texto(R.string.resultado_vs)).performTextInput("50")
        compose.onNodeWithText(texto(R.string.resultado_gc)).performTextInput("4,6")
        compose.onNodeWithText(texto(R.string.resultado_guardar)).performClick()

        // VS por debajo de la referencia: nivel 4, alerta.
        val nivel4 = texto(R.string.nivel_con_numero, 4, texto(R.string.nivel_4))
        compose.waitUntil(5_000) { hay(nivel4) }
        assertTrue(hay(texto(R.string.accion_posible_alteracion_hemodinamica)))
    }
}
