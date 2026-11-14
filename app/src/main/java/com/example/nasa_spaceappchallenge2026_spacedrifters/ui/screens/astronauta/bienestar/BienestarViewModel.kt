package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.bienestar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.BienestarDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoActividadBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.BienestarRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.ReglasBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.evaluarSinFallar
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Formulario del check-in de hoy y estado de la petición de apoyo. */
data class BienestarUi(
    val cargando: Boolean = true,
    /** Escalas 1–5; null = aún sin responder. */
    val animo: Int? = null,
    val estres: Int? = null,
    val aislamiento: Int? = null,
    /** Ya hay check-in guardado hoy (se muestran las sugerencias). */
    val guardadoHoy: Boolean = false,
    val guardando: Boolean = false,
    val error: Boolean = false,
    /** Sugerencias según lo guardado (no según lo que se está eligiendo). */
    val sugerencias: List<ReglasBienestar.Sugerencia> = emptyList(),
    /** Hay una petición de apoyo psicológico activa. */
    val apoyoPedido: Boolean = false,
) {
    val puedeGuardar: Boolean
        get() = !cargando && !guardando && animo != null && estres != null && aislamiento != null
}

/** Herramientas usadas en los últimos 7 días. */
data class SemanaBienestarUi(val respiraciones: Int = 0, val partidas: Int = 0)

/** Bienestar psicológico: check-in de hoy (ánimo, estrés, aislamiento) y petición de apoyo. */
class BienestarViewModel(
    private val repositorio: BienestarRepository,
    private val alertas: AlertasRepository,
    private val astronautaId: Long,
    /** La petición de apoyo se guarda aunque se cierre la pantalla. */
    private val ambito: CoroutineScope,
    zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val hoy: LocalDate = LocalDate.now(zona)

    private val _ui = MutableStateFlow(BienestarUi())
    val ui: StateFlow<BienestarUi> = _ui.asStateFlow()

    val semana: StateFlow<SemanaBienestarUi> =
        repositorio.actividadesDesde(astronautaId, hoy.minusDays(6).atStartOfDay(zona).toInstant().toEpochMilli())
            .map { lista ->
                SemanaBienestarUi(
                    respiraciones = lista.count { it.tipo == TipoActividadBienestar.RESPIRACION.name },
                    partidas = lista.count { it.tipo == TipoActividadBienestar.JUEGO_PAREJAS.name },
                )
            }
            .catch { emit(SemanaBienestarUi()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SemanaBienestarUi())

    init {
        viewModelScope.launch {
            runCatching {
                val existente = repositorio.delDia(astronautaId, hoy).first()
                val apoyo = alertas.activasPara(astronautaId).first()
                    .any { it.alerta.ruleId == AlertasRepository.REGLA_SOLICITUD_APOYO && it.alerta.astronautId == astronautaId }
                BienestarUi(
                    cargando = false,
                    animo = existente?.animo?.takeIf(ReglasBienestar::valida),
                    estres = existente?.estres?.takeIf(ReglasBienestar::valida),
                    aislamiento = existente?.aislamiento?.takeIf(ReglasBienestar::valida),
                    guardadoHoy = existente != null,
                    sugerencias = existente?.let { ReglasBienestar.sugerencias(it.animo, it.estres, it.aislamiento) }.orEmpty(),
                    apoyoPedido = apoyo,
                )
            }.onSuccess { _ui.value = it }
                .onFailure { _ui.value = BienestarUi(cargando = false) }
        }
    }

    fun elegirAnimo(v: Int) { if (ReglasBienestar.valida(v)) _ui.update { it.copy(animo = v) } }
    fun elegirEstres(v: Int) { if (ReglasBienestar.valida(v)) _ui.update { it.copy(estres = v) } }
    fun elegirAislamiento(v: Int) { if (ReglasBienestar.valida(v)) _ui.update { it.copy(aislamiento = v) } }

    fun guardar() {
        val actual = _ui.value
        if (!actual.puedeGuardar) return
        _ui.update { it.copy(guardando = true, error = false) }
        viewModelScope.launch {
            runCatching {
                repositorio.guardar(
                    BienestarDiario(
                        astronautaId = astronautaId,
                        fecha = hoy.toString(),
                        animo = actual.animo,
                        estres = actual.estres,
                        aislamiento = actual.aislamiento,
                        guardadoEn = System.currentTimeMillis(),
                    ),
                )
            }.onSuccess {
                // Dos días seguidos con el ánimo bajo, estrés o aislamiento alto avisan al médico.
                alertas.evaluarSinFallar(astronautaId)
                _ui.update {
                    it.copy(
                        guardando = false,
                        guardadoHoy = true,
                        sugerencias = ReglasBienestar.sugerencias(actual.animo, actual.estres, actual.aislamiento),
                    )
                }
            }.onFailure { _ui.update { it.copy(guardando = false, error = true) } }
        }
    }

    /** "Hablar con apoyo psicológico": avisa al médico (una sola petición activa). */
    fun pedirApoyo() {
        _ui.update { it.copy(apoyoPedido = true) }
        guardarEnSegundoPlano(ambito) { alertas.solicitarApoyo(astronautaId) }
    }
}
