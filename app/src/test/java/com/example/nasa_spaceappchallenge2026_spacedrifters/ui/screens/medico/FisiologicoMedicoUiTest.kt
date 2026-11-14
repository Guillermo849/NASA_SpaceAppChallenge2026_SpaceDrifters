package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ConfigUmbrales
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EventoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.MedidaFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TensionFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Pestaña "Fisiológico": el médico solo ve lo que ya ha llegado a la Tierra. */
class FisiologicoMedicoUiTest {

    private val umbrales = ConfigUmbrales.desdeTexto(File("src/main/assets/config_umbrales.json").readText())
    private val min = 60_000L
    private val dia = 86_400_000L
    private val retraso = 18 * min
    private val ahora = 100 * dia

    private val protocolo = ProtocoloActivo(
        id = 1, astronautaId = 1, protocoloId = "hemodinamico", nodoActual = "p2a-repetir", nivel = 3,
        iniciadoEn = ahora - 60 * min, actualizadoEn = ahora - 5 * min, estado = EstadoProtocolo.ESPERANDO_PRUEBA,
    )

    private fun evento(id: Long, cuando: Long, antes: Int, despues: Int, llega: Long = cuando + retraso, quien: String? = null) =
        EventoProtocolo(id, 1, cuando, "p2a-sintomas", antes, despues, "FC_ALTA+PAS_BAJA", quien, llega)

    private fun construir(
        eventos: List<EventoProtocolo>,
        pruebas: List<PruebaClinica> = emptyList(),
        medidas: List<MedidaFila> = emptyList(),
        tensiones: List<TensionFila> = emptyList(),
        protocolos: List<ProtocoloActivo> = listOf(protocolo),
    ) = construirFisiologico(protocolos, eventos, pruebas, medidas, tensiones, umbrales, retraso, ahora)

    @Test
    fun elNivelEsElDelUltimoCambioQueHaLlegado() {
        // 1→2 hace 60 min (ya llegó); 2→3 hace 5 min (llega dentro de 13).
        val ui = construir(listOf(evento(1, ahora - 60 * min, 1, 2), evento(2, ahora - 5 * min, 2, 3)))
        val p = ui.protocolos.single()
        assertEquals(2, p.nivel)
        assertEquals(1, p.enCamino)
        assertEquals(2, ui.nivelActual)
        assertEquals(1, p.tiempo.size)
    }

    @Test
    fun unProtocoloRecienAbiertoNoSeVeTodavia() {
        val nuevo = protocolo.copy(iniciadoEn = ahora - 5 * min)
        assertTrue(construir(listOf(evento(1, ahora - 5 * min, 1, 2)), protocolos = listOf(nuevo)).protocolos.isEmpty())
    }

    @Test
    fun loQueHaceElMedicoSeVeAlMomento() {
        val ui = construir(
            listOf(
                evento(1, ahora - 60 * min, 1, 3),
                evento(2, ahora, 3, 2, llega = ahora, quien = "MEDICO"),
            ),
        )
        assertEquals(2, ui.protocolos.single().nivel)
        assertEquals(0, ui.protocolos.single().enCamino)
    }

    @Test
    fun pruebas_pedidasYResultadosDeCamino() {
        val hecha = PruebaClinica(
            id = 1, astronautaId = 1, protocoloActivoId = 1, tipo = TipoPruebaClinica.REPETICION_CONSTANTES,
            pedidaEn = ahora - 50 * min, realizadaEn = ahora - 2 * min, resultado = """{"valor":"MEJOR"}""",
            llegaTierraEn = ahora + 16 * min,
        )
        val p = construir(listOf(evento(1, ahora - 60 * min, 1, 3)), pruebas = listOf(hecha)).protocolos.single()
        // Pedida (ya llegó) pero su resultado aún viene de camino: sigue pendiente para el médico.
        assertTrue(p.tiempo.any { it is ItemTiempoUi.PruebaPedida })
        assertFalse(p.tiempo.any { it is ItemTiempoUi.PruebaHecha })
        assertEquals(listOf(hecha), p.pendientes)
        assertEquals(1, p.enCamino)
    }

    @Test
    fun constantes_baseDeLosPrimeros7DiasYFranja() {
        // FC 60 los 7 primeros días, 100 después; la última medida, de hace 5 min, aún no ha llegado.
        val medidas = (0 until 10).map { d -> MedidaFila("HR", if (d < 7) 60.0 else 100.0, ahora - (10 - d) * dia) } +
            MedidaFila("HR", 140.0, ahora - 5 * min)
        val tensiones = listOf(TensionFila(120, 80, ahora - 2 * dia))
        val ui = construir(emptyList(), medidas = medidas, tensiones = tensiones, protocolos = emptyList())
        val fc = ui.series.single { it.metrica == MetricaConstante.FC }
        assertEquals(60.0, fc.base!!, 1e-9)
        assertEquals(10, fc.puntos.size)
        assertEquals(100.0, fc.ultimo!!, 1e-9)
        assertTrue("100 lpm está fuera de 48–72", fc.fuera(100.0))
        assertEquals(1, ui.series.single { it.metrica == MetricaConstante.PAS }.puntos.size)
        // Sin medidas: serie vacía, nunca "bien".
        assertNull(ui.series.single { it.metrica == MetricaConstante.SPO2 }.ultimo)
        assertNull(ui.nivelActual)
    }

    @Test
    fun comparacion_ultimaYAnteriorRecibidas() {
        fun doppler(id: Long, cuando: Long, flujo: String) = PruebaClinica(
            id = id, astronautaId = 1, tipo = TipoPruebaClinica.ECO_DOPPLER_YUGULAR, pedidaEn = cuando - min,
            realizadaEn = cuando, resultado = """{"flujo":"$flujo","trombo":false}""", llegaTierraEn = cuando + retraso,
        )
        val ui = construir(
            emptyList(),
            pruebas = listOf(doppler(1, ahora - 10 * dia, "NORMAL"), doppler(2, ahora - dia, "RETROGRADO"), doppler(3, ahora - min, "TROMBO")),
            protocolos = emptyList(),
        )
        val c = ui.comparaciones.single()
        // La de hace 1 min aún no ha llegado.
        assertEquals(2L, c.ultima.id)
        assertEquals(1L, c.anterior!!.id)
    }
}
