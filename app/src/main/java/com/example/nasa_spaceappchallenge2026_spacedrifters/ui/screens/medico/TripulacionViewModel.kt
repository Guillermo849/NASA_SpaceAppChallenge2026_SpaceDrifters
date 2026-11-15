package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Astronauta
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoMedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.UmbralesFisiologicos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Panel del médico: un resumen por tripulante con lo ya recibido en la Tierra (pruebas cognitivas,
 * avisos, nivel de sus protocolos y constantes). Se refresca cada 30 s.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripulacionViewModel(
    private val repositorio: MedicoRepository,
    /** Protocolos y constantes (null = el panel sale sin la parte fisiológica). */
    private val fisiologico: FisiologicoMedicoRepository? = null,
    private val umbrales: UmbralesFisiologicos? = null,
    private val retrasoMin: Long = 18,
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** null mientras carga. */
    val panel: StateFlow<PanelMedicoUi?> = relojCada(30_000)
        .flatMapLatest { ahora ->
            repositorio.tripulacion().flatMapLatest { lista ->
                val filas: Flow<List<FilaTripulanteUi>> = if (lista.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(lista.map { fila(it, ahora) }) { it.toList() }
                }
                combine(filas, flowOf(lista)) { f, tripulacion ->
                    PanelMedicoUi(
                        tripulacion = ordenarPorUrgencia(f),
                        diaMision = tripulacion.firstNotNullOfOrNull { it.inicioMision }?.let { diaDeMision(it, ahora) },
                        retrasoMin = retrasoMin,
                        actualizadoEn = ahora,
                    )
                }
            }
        }
        .catch { emit(PanelMedicoUi(emptyList(), null, retrasoMin, System.currentTimeMillis())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun fila(a: Astronauta, ahora: Long): Flow<FilaTripulanteUi> {
        val cognitivo = combine(
            repositorio.contarAlertasRecibidas(a.id, ahora),
            repositorio.sesionesRecibidas(a.id, ahora),
        ) { alertas, sesiones -> alertas to sesiones }
        if (fisiologico == null || umbrales == null) {
            return combine(cognitivo, flowOf(Unit)) { (alertas, sesiones), _ -> aFilaTripulante(a, alertas, sesiones, null, ahora) }
        }
        // 30 días, como la pestaña "Fisiológico": la línea base son los 7 primeros de la misión.
        val desde = ahora - 30 * 86_400_000L
        val fisio = combine(
            fisiologico.protocolos(a.id),
            fisiologico.eventos(a.id),
            fisiologico.pruebas(a.id),
            fisiologico.medidas(a.id, desde),
            fisiologico.tensiones(a.id, desde),
        ) { protocolos, eventos, pruebas, medidas, tensiones ->
            construirFisiologico(protocolos, eventos, pruebas, medidas, tensiones, umbrales, fisiologico.retrasoMisionMs, ahora)
        }
        return combine(cognitivo, fisio) { (alertas, sesiones), f -> aFilaTripulante(a, alertas, sesiones, f, ahora) }
    }

    private fun diaDeMision(inicio: Long, ahora: Long): Int {
        val primerDia = Instant.ofEpochMilli(inicio).atZone(zona).toLocalDate()
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        return ChronoUnit.DAYS.between(primerDia, hoy).toInt() + 1
    }
}
