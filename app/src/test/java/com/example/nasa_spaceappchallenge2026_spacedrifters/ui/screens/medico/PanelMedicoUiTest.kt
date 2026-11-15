package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Panel del médico: estado general de cada tripulante y orden por urgencia. */
class PanelMedicoUiTest {

    private fun fila(
        nombre: String = "A",
        alertas: Int = 0,
        stroop: EstadoCognitivo? = null,
        nback: EstadoCognitivo? = null,
        nivel: Int? = null,
        constantes: List<ConstanteMiniUi> = emptyList(),
    ) = FilaTripulanteUi(nombre.length.toLong(), nombre, null, alertas, stroop, nback, nivel, constantes)

    private fun constante(fuera: Boolean) =
        ConstanteMiniUi(MetricaConstante.FC, listOf(0L to 60.0), 60.0, fuera = fuera, alto = fuera)

    @Test
    fun sinNada_sinDatos_nuncaNormal() {
        assertEquals(EstadoCognitivo.SIN_DATOS, fila().estadoGeneral)
        assertEquals(EstadoCognitivo.SIN_DATOS, fila(stroop = EstadoCognitivo.SIN_DATOS).estadoGeneral)
    }

    @Test
    fun todoBien_normal() {
        assertEquals(
            EstadoCognitivo.NORMAL,
            fila(stroop = EstadoCognitivo.NORMAL, nback = EstadoCognitivo.NORMAL, constantes = listOf(constante(false))).estadoGeneral,
        )
    }

    @Test
    fun vigilar_porPruebaNivelAvisoOConstante() {
        assertEquals(EstadoCognitivo.VIGILAR, fila(stroop = EstadoCognitivo.VIGILAR).estadoGeneral)
        assertEquals(EstadoCognitivo.VIGILAR, fila(nivel = 3).estadoGeneral)
        assertEquals(EstadoCognitivo.VIGILAR, fila(alertas = 1).estadoGeneral)
        assertEquals(EstadoCognitivo.VIGILAR, fila(constantes = listOf(constante(true))).estadoGeneral)
        // Nivel 1 (informativo) no basta.
        assertEquals(EstadoCognitivo.NORMAL, fila(nivel = 1).estadoGeneral)
    }

    @Test
    fun alerta_porNivel4OPruebaEnAlerta() {
        assertEquals(EstadoCognitivo.ALERTA, fila(nivel = 4).estadoGeneral)
        assertEquals(EstadoCognitivo.ALERTA, fila(nback = EstadoCognitivo.ALERTA, alertas = 2).estadoGeneral)
    }

    @Test
    fun ordenPorUrgencia() {
        val orden = ordenarPorUrgencia(
            listOf(
                fila("Normal", stroop = EstadoCognitivo.NORMAL),
                fila("SinDatos"),
                fila("Vigilar", alertas = 1),
                fila("AlertaUnAviso", nback = EstadoCognitivo.ALERTA, alertas = 1),
                fila("AlertaDosAvisos", nback = EstadoCognitivo.ALERTA, alertas = 2),
            ),
        ).map { it.nombre }
        assertEquals(listOf("AlertaDosAvisos", "AlertaUnAviso", "Vigilar", "Normal", "SinDatos"), orden)
    }

    @Test
    fun resumenDelPanel() {
        val panel = PanelMedicoUi(
            tripulacion = listOf(fila("A", nivel = 4, alertas = 2), fila("B", alertas = 1), fila("C", stroop = EstadoCognitivo.NORMAL)),
            diaMision = 14,
            retrasoMin = 18,
            actualizadoEn = 0,
        )
        assertEquals(1, panel.cuantos(EstadoCognitivo.ALERTA))
        assertEquals(1, panel.cuantos(EstadoCognitivo.VIGILAR))
        assertEquals(1, panel.cuantos(EstadoCognitivo.NORMAL))
        assertEquals(3, panel.avisos)
        assertEquals(1, panel.conProtocolo)
    }
}
