package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AlertaConNombre
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.LineaBaseCognitiva
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ResumenSesion
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.TripulacionRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Rendimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.BienestarRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ConstantesRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ProtocoloEnCurso
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Panel del astronauta: cabecera, pruebas de hoy y rendimiento de los últimos 30 días. */
class InicioAstronautaViewModel(
    cognitivo: CognitivoRepository,
    tripulacion: TripulacionRepository,
    alertas: AlertasRepository,
    /** Pruebas extra que ha pedido el médico. */
    medico: MedicoRepository,
    astronautaId: Long,
    private val retrasoMin: Long,
    /** Protocolos fisiológicos (tarjeta de arriba del panel). */
    fisiologico: FisiologicoRepository? = null,
    /** Check-in de bienestar psicológico (tarjeta "Bienestar"). */
    bienestar: BienestarRepository? = null,
    /** Últimas constantes (tarjeta "Mis constantes"). */
    constantes: ConstantesRepository? = null,
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** Protocolos fisiológicos abiertos de este astronauta, el más grave primero. */
    val protocolos: StateFlow<List<ProtocoloEnCurso>> =
        (fisiologico?.enCurso()?.map { todos -> todos.filter { it.protocolo.astronautaId == astronautaId } } ?: flowOf(emptyList()))
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val hoy: LocalDate = LocalDate.now(zona)
    private val inicioDeHoy: Long = hoy.atStartOfDay(zona).toInstant().toEpochMilli()
    private val inicioHace30Dias: Long = hoy.minusDays(Periodo.MES.dias - 1L).atStartOfDay(zona).toInstant().toEpochMilli()

    /** Lo de hoy: cabecera, avisos, pruebas hechas y registro diario. */
    private val hoyUi: Flow<PanelAstronautaUi> = combine(
        tripulacion.astronauta(astronautaId),
        cognitivo.tiposHechosDesde(astronautaId, inicioDeHoy),
        tripulacion.registroDelDia(astronautaId, hoy),
        alertas.activasPara(astronautaId),
        medico.pruebasPendientes(astronautaId),
    ) { astronauta, hechas, registro, activas, extra ->
        PanelAstronautaUi(
            avisos = activas.map { aAviso(it, astronautaId) },
            pruebasExtra = extra.map { it.tipo }.toSet(),
            nombre = astronauta?.nombre,
            cargo = astronauta?.cargo,
            // El día 1 es el de inicio de su misión (tabla mission).
            diaMision = astronauta?.inicioMision?.let { diaDeMision(it) },
            retrasoMin = retrasoMin,
            hechasHoy = hechas,
            registroHoy = registro?.let {
                RegistroHoyUi(
                    horasSueno = it.horasSueno,
                    somnolencia = it.somnolencia,
                    sintomas = it.listaSintomas,
                    aguaLitros = it.aguaLitros,
                    ingesta = it.ingestaPorcentaje,
                    ejercicioMinutos = it.ejercicioMinutos,
                    ejercicios = it.listaEjercicios,
                )
            },
        )
    }

    private val rendimientosFlow: Flow<List<RendimientoUi>> = combine(
        rendimiento(cognitivo, astronautaId, TipoPrueba.STROOP),
        rendimiento(cognitivo, astronautaId, TipoPrueba.NBACK),
        rendimiento(cognitivo, astronautaId, TipoPrueba.PVT),
    ) { stroop, nback, pvt -> listOf(stroop, nback, pvt) }

    val panel: StateFlow<PanelAstronautaUi> = combine(
        hoyUi,
        rendimientosFlow,
        bienestar?.delDia(astronautaId, hoy) ?: flowOf(null),
        constantes?.ultimas(astronautaId) ?: flowOf(null),
    ) { base, rendimientos, checkin, ultimas ->
        base.copy(
            rendimiento = rendimientos,
            bienestarHoy = checkin?.let { BienestarHoyUi(it.animo, it.estres, it.aislamiento) },
            constantes = ultimas,
        )
    }
        .catch { emit(PanelAstronautaUi(retrasoMin = retrasoMin)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PanelAstronautaUi(retrasoMin = retrasoMin))

    private fun rendimiento(cognitivo: CognitivoRepository, astronautaId: Long, tipo: TipoPrueba): Flow<RendimientoUi> =
        combine(
            cognitivo.resumenesMisionDesde(astronautaId, tipo, inicioHace30Dias),
            cognitivo.lineaBasePrincipal(astronautaId, tipo),
        ) { resumenes, base -> aRendimiento(tipo, resumenes, base) }

    private fun diaDeMision(inicio: Long): Int {
        val primerDia = Instant.ofEpochMilli(inicio).atZone(zona).toLocalDate()
        return ChronoUnit.DAYS.between(primerDia, hoy).toInt() + 1
    }

    companion object {
        /** Alerta guardada → aviso del panel. "Propio" = es sobre este astronauta (no asignada por otro). */
        fun aAviso(a: AlertaConNombre, astronautaId: Long): AvisoUi {
            val alerta = a.alerta
            return AvisoUi(
                id = alerta.id,
                reglaId = alerta.ruleId.orEmpty(),
                prioridad = Regla.Prioridad.entries.firstOrNull { it.name == alerta.priority },
                pasos = alerta.steps.orEmpty().split(',').filter { it.isNotBlank() },
                esPropio = alerta.astronautId == astronautaId,
                nombreAfectado = a.nombreAstronauta,
            )
        }

        /** Resúmenes → puntos de la gráfica ("más alto = mejor"). Las sesiones sin métrica no se pintan. */
        fun aRendimiento(tipo: TipoPrueba, resumenes: List<ResumenSesion>, base: LineaBaseCognitiva?): RendimientoUi {
            val direccion = CognitivoRepository.direccion(tipo)
            val puntos = resumenes.mapNotNull { r ->
                val valor = when (tipo) {
                    TipoPrueba.STROOP -> r.efectoStroopMs
                    TipoPrueba.NBACK -> r.dPrima
                    TipoPrueba.PVT -> r.pvtTrMedioMs
                } ?: return@mapNotNull null
                val puntuacion = if (base != null) {
                    Rendimiento.puntuacion(valor, base.media, base.desviacion, direccion)
                } else {
                    Rendimiento.orientado(valor, direccion)
                }
                val estado = EstadoCognitivo.entries.firstOrNull { it.name == r.estado } ?: EstadoCognitivo.SIN_DATOS
                PuntoRendimiento(r.fechaEn, puntuacion, estado)
            }
            return RendimientoUi(tipo, tieneLineaBase = base != null, puntos = puntos)
        }
    }
}
