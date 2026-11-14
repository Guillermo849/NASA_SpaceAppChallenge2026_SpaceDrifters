package com.example.nasa_spaceappchallenge2026_spacedrifters

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Bienestar: check-in desde el panel, sugerencias, petición de apoyo, respiración y juego. */
@RunWith(AndroidJUnit4::class)
class BienestarFlujoTest {

    // Con los datos de ejemplo: Maya existe en astronaut (la alerta de apoyo la referencia) y ya
    // ha hecho el check-in de hoy (ánimo normal, algo de estrés, poco aislamiento).
    @get:Rule(order = 0)
    val datos = ConDatosEjemplo()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun texto(id: Int, vararg args: Any) = compose.activity.getString(id, *args)
    private fun hay(t: String) = compose.onAllNodes(hasText(t)).fetchSemanticsNodes().isNotEmpty()

    private fun abrirBienestar() {
        compose.entrarComoAstronauta()
        compose.waitUntil(5_000) { hay(texto(R.string.bienestar_abrir)) }
        compose.onNodeWithText(texto(R.string.bienestar_abrir)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.bienestar_animo_pregunta)) }
    }

    @Test
    fun checkin_sugerenciasYPeticionDeApoyo() {
        abrirBienestar()
        // El de hoy ya está hecho: sin sugerencias (todo dentro de lo esperable) y se puede actualizar.
        compose.onNodeWithText(texto(R.string.bienestar_todo_bien)).performScrollTo().assertExists()

        // Ánimo bajo, bastante estrés y poco aislamiento.
        compose.onNodeWithText(texto(R.string.animo_2)).performScrollTo().performClick()
        compose.onAllNodesWithText(texto(R.string.intensidad_4))[0].performScrollTo().performClick()
        compose.onAllNodesWithText(texto(R.string.intensidad_2))[1].performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.bienestar_actualizar)).performScrollTo().assertIsEnabled().performClick()

        // Sugiere hablar con apoyo y la respiración (no las ideas de conexión).
        compose.waitUntil(5_000) { hay(texto(R.string.bienestar_para_hoy)) }
        compose.onNodeWithText(texto(R.string.sugerencia_apoyo)).assertExists()
        compose.onNodeWithText(texto(R.string.sugerencia_respiracion)).assertExists()
        assertEquals(false, hay(texto(R.string.sugerencia_conexion)))

        // Pedir apoyo, con confirmación.
        compose.onAllNodesWithText(texto(R.string.bienestar_apoyo_boton))[0].performScrollTo().performClick()
        compose.onNodeWithText(texto(R.string.bienestar_apoyo_enviar)).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.bienestar_apoyo_pedido)) }

        val container = ApplicationProvider.getApplicationContext<SpaceDriftersApp>().container
        compose.waitUntil(5_000) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    container.database.alertasDao().deAstronauta(container.astronautaActualId)
                        .any { it.ruleId == AlertasRepository.REGLA_SOLICITUD_APOYO }
                }
            }
        }

        // De vuelta en el panel, con el resumen en texto.
        compose.onNodeWithContentDescription(texto(R.string.cd_atras)).performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.bienestar_abrir)) }
        compose.onNodeWithText(texto(R.string.animo_2)).assertExists()
    }

    @Test
    fun respiracion_empiezaPorInspirarYSePuedeTerminar() {
        abrirBienestar()
        compose.onAllNodesWithText(texto(R.string.bienestar_empezar))[0].performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.respiracion_duracion)) }
        compose.onNodeWithText(texto(R.string.respiracion_minutos, 1)).performClick()
        // La sesión avanza fotograma a fotograma: con el reloj automático, el test la haría
        // correr entera de golpe. Se avanza a mano.
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(texto(R.string.bienestar_empezar)).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText(texto(R.string.respiracion_inspira)).assertExists()
        // A los 4 s, mantener el aire.
        compose.mainClock.advanceTimeBy(4_000)
        compose.onNodeWithText(texto(R.string.respiracion_manten)).assertExists()
        compose.onNodeWithText(texto(R.string.respiracion_terminar)).performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText(texto(R.string.respiracion_hecho)).assertExists()
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun juego_darLaVueltaAUnaCarta() {
        abrirBienestar()
        compose.onNodeWithText(texto(R.string.bienestar_jugar)).performScrollTo().performClick()
        compose.waitUntil(5_000) { hay(texto(R.string.juego_marcador, 0, 6, 0)) }
        compose.onNodeWithContentDescription(texto(R.string.cd_carta_boca_abajo, 1)).performClick()
        // Ya no está boca abajo: se ve qué cuerpo celeste es.
        compose.waitUntil(2_000) {
            compose.onAllNodes(hasContentDescription(texto(R.string.cd_carta_boca_abajo, 1)))
                .fetchSemanticsNodes().isEmpty()
        }
        val nombres = listOf(
            R.string.cuerpo_tierra, R.string.cuerpo_luna, R.string.cuerpo_marte,
            R.string.cuerpo_saturno, R.string.cuerpo_sol, R.string.cuerpo_cometa,
        ).map { texto(R.string.cd_carta_boca_arriba, 1, texto(it)) }
        assertTrue(nombres.any { compose.onAllNodes(hasContentDescription(it)).fetchSemanticsNodes().isNotEmpty() })
    }
}
