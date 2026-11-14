package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.MeasurementType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tomar constantes a mano: Ane (base FC 58, PAS 112, sin protocolos) mete FC 110, tensión 88/60 y
 * mareo → FC alta y PA baja → se abre el protocolo hemodinámico (nivel 3, repetir + bioquímica).
 */
@RunWith(AndroidJUnit4::class)
class ConstantesFlujoTest {

    @get:Rule(order = 0)
    val datos = ConDatosEjemplo()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<SpaceDriftersApp>().container
    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    /** El campo de texto con esa etiqueta. */
    private fun campo(etiqueta: Int) = compose.onNode(hasSetTextAction() and hasText(texto(etiqueta)))

    @Test
    fun ane_tomaConFcAltaYTensionBaja_abreElProtocoloHemodinamico() {
        compose.entrarComoAstronauta("ane.etxeberria")
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_mis)) }
        compose.onNodeWithText(texto(R.string.constantes_tomar)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_ayuda)) }

        // Sin nada escrito no se guarda.
        compose.onNodeWithText(texto(R.string.constantes_guardar)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_error_ninguna)) }

        // Un error al teclear (720 rpm) se marca y no se guarda.
        campo(R.string.constante_fr).performScrollTo().performTextInput("720")
        compose.onNodeWithText(texto(R.string.constantes_guardar)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_error_imposible)) }
        assertTrue(hay(texto(R.string.constantes_rango_posible, 4, 60)))

        campo(R.string.constante_fc).performScrollTo().performTextInput("110")
        campo(R.string.constante_pas).performScrollTo().performTextInput("88")
        campo(R.string.constante_pad).performScrollTo().performTextInput("60")
        campo(R.string.constante_spo2).performScrollTo().performTextInput("98")
        // Corrige la FR.
        campo(R.string.constante_fr).performScrollTo().performTextReplacement("14")
        compose.onNodeWithText(texto(R.string.sintoma_c_mareo)).performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.constantes_guardar)).performScrollTo().performClick()

        // Resultado: FC alta, sistólica baja, el resto dentro; se abre el protocolo hemodinámico.
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_guardadas)) }
        assertTrue(hay(texto(R.string.constantes_estado_alto)))
        assertTrue(hay(texto(R.string.constantes_estado_bajo)))
        assertTrue(hay(texto(R.string.constantes_protocolo_nuevo, texto(R.string.protocolo_hemodinamico))))
        assertTrue(hay(texto(R.string.nivel_con_numero, 3, texto(R.string.nivel_3))))

        val ane = 3L
        runBlocking {
            withContext(Dispatchers.IO) {
                val db = container.database
                val p = db.fisiologicoDao().abiertosAhora(ane).single()
                assertEquals("hemodinamico", p.protocoloId)
                assertEquals("p2a-repetir", p.nodoActual)
                val hr = db.fisiologicoDao().medidasDesde(ane, 0).first().last { it.metrica == MeasurementType.HR.name }
                assertEquals(110.0, hr.valor, 1e-9)
                val tension = db.fisiologicoDao().tensionesDesde(ane, 0).first().last()
                assertEquals(88, tension.sistolica)
                assertEquals(60, tension.diastolica)
            }
        }

        // De vuelta en el panel: la última toma y el protocolo arriba.
        compose.onNodeWithText(texto(R.string.bienestar_volver)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay("88/60") }
        assertTrue(hay(texto(R.string.protocolo_hemodinamico)))
    }

    @Test
    fun medico_anadeConstantesDesdeElPerfil() {
        compose.entrarComoMedico()
        compose.waitUntil(5_000) { hay("Leo Martín") }
        compose.onNodeWithText("Leo Martín").performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.medico_pestana_fisiologico)) }
        compose.onNodeWithText(texto(R.string.medico_pestana_fisiologico)).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_anadir)) }
        compose.onNodeWithText(texto(R.string.constantes_anadir)).performScrollTo().performClick()
        // La toma es de Leo (no del usuario que ha iniciado sesión).
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_ayuda)) }
        compose.waitUntil(5_000) { hay("Leo Martín") }
        campo(R.string.constante_spo2).performScrollTo().performTextInput("97")
        compose.onNodeWithText(texto(R.string.constantes_guardar)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.constantes_guardadas)) }
        assertTrue(hay(texto(R.string.constantes_llega_medico, 18)))
        val ultima = runBlocking {
            withContext(Dispatchers.IO) { container.database.fisiologicoDao().medidasDesde(2L, 0).first().last() }
        }
        assertEquals(MeasurementType.SPO2.name, ultima.metrica)
        assertEquals(97.0, ultima.valor, 1e-9)
    }
}
