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
 * ("igual o peor") y una bioquímica real de NASA (LSDS-8): sin deshidratación → ecocardiograma →
 * VS bajo → nivel 4; con deshidratación → rehidratación oral.
 */
@RunWith(AndroidJUnit4::class)
class OficialMedicoFlujoTest {

    @get:Rule(order = 0)
    val datos = ConDatosEjemplo()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    /**
     * Pulsa "Introducir resultado" en la fila de la prueba [nombre] (el botón a su misma altura).
     * [indice]: qué aparición de ese nombre, si hay varias (la de Maya, que va primero, es la 0).
     */
    private fun introducir(nombre: String, indice: Int = 0) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(nombre))
        val fila = compose.onAllNodesWithText(nombre)[indice].fetchSemanticsNode().boundsInRoot
        val botones = compose.onAllNodesWithText(texto(R.string.oficial_introducir)).fetchSemanticsNodes()
        val i = botones.indexOfFirst { it.boundsInRoot.center.y in (fila.top - 60f)..(fila.bottom + 60f) }
        compose.onAllNodesWithText(texto(R.string.oficial_introducir))[i].performClick()
    }

    private fun abrirProtocolosABordo(quien: String) {
        compose.entrarComoMedico()
        compose.waitUntil(5_000) { hay(texto(R.string.oficial_titulo)) }
        compose.onNodeWithText(texto(R.string.oficial_titulo)).performClick()
        compose.waitUntil(5_000) { hay(quien) }
    }

    /** Maya: repetir constantes → "igual o peor". */
    private fun mayaRepeticionIgualOPeor() {
        introducir(texto(R.string.prueba_repeticion_constantes))
        compose.waitUntil(5_000) { hay(texto(R.string.resultado_elige)) }
        compose.onNodeWithText(texto(R.string.opcion_igual_o_peor)).performClick()
        compose.onNodeWithText(texto(R.string.resultado_guardar)).performClick()
    }

    /** Maya: bioquímica con la muestra real de NASA de su tripulante (C001) en [momento]. */
    private fun mayaBioquimicaNasa(momento: String) {
        compose.waitUntil(5_000) { hay(texto(R.string.oficial_introducir)) }
        introducir(texto(R.string.prueba_bioquimica))
        compose.waitUntil(5_000) { hay(texto(R.string.bq_titulo)) }
        assertTrue(hay(texto(R.string.bq_explicacion, "C001")))
        compose.onNodeWithText(momento).performClick()
        compose.onNodeWithText(texto(R.string.bq_usar)).performScrollTo().performClick()
    }

    @Test
    fun leo_hemogramaRealDeNasa_alteradoConHemoglobinaBaja() {
        abrirProtocolosABordo("Leo Martín")
        introducir(texto(R.string.prueba_hemograma))

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
    fun repeticionBioquimicaNormalYEcocardio_subeANivel4() {
        // El médico (en la Tierra o a bordo) abre "Protocolos a bordo" desde la tripulación.
        // Maya va primero (actualizada más tarde): repetir las constantes y una bioquímica.
        abrirProtocolosABordo("Maya Okafor")
        mayaRepeticionIgualOPeor()
        // Bioquímica real de C001 el día después de aterrizar: todo en rango → no es deshidratación.
        mayaBioquimicaNasa("R+1")

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

    @Test
    fun repeticionYBioquimicaConDeshidratacion_rehidratar() {
        abrirProtocolosABordo("Maya Okafor")
        mayaRepeticionIgualOPeor()
        // Bioquímica real de C001 en R+194: urea alta → deshidratación.
        mayaBioquimicaNasa("R+194")

        // Sin ecocardiograma: rehidratación oral (nivel 3) y el resultado guardado con su muestra.
        compose.waitUntil(5_000) { hay(texto(R.string.accion_rehidratacion_oral)) }
        assertFalse(hay(texto(R.string.prueba_ecocardio)))
        val dao = ApplicationProvider.getApplicationContext<SpaceDriftersApp>().container.database.fisiologicoDao()
        val bq = runBlocking { dao.realizadasDe(1L) }.single { it.tipo == TipoPruebaClinica.BIOQUIMICA }
        assertTrue(bq.resultado!!, bq.resultado!!.contains("DESHIDRATACION") && bq.resultado!!.contains("C001_serum_R+194"))
    }
}
