package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.TripulacionRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.evaluarSinFallar
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.registro.ReglasRegistro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Lo que pinta el formulario del registro diario. */
data class RegistroUi(
    val cargando: Boolean = true,
    val horasSueno: Double = 7.0,
    /** Escala KSS 1–9. null = aún no elegida: no se guarda un valor que no ha elegido el astronauta. */
    val somnolencia: Int? = null,
    /** Vacío = "Ninguno". */
    val sintomas: Set<Sintoma> = emptySet(),
    /** Ya había registro de hoy: se está editando. */
    val esEdicion: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val error: Boolean = false,
) {
    val puedeGuardar: Boolean get() = !cargando && !guardando && somnolencia != null
}

/** Registro diario del astronauta para hoy: horas de sueño, somnolencia y síntomas. */
class RegistroDiarioViewModel(
    private val repositorio: TripulacionRepository,
    /** Evalúa las reglas de alerta después de guardar el registro. */
    private val alertas: AlertasRepository,
    private val astronautaId: Long,
    zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val hoy: LocalDate = LocalDate.now(zona)

    private val _ui = MutableStateFlow(RegistroUi())
    val ui: StateFlow<RegistroUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val existente = repositorio.registroDelDia(astronautaId, hoy).first()
                val habituales = repositorio.astronauta(astronautaId).first()?.horasSuenoHabituales
                if (existente != null) {
                    RegistroUi(
                        cargando = false,
                        horasSueno = ReglasRegistro.redondearSueno(existente.horasSueno ?: habituales ?: 7.0),
                        somnolencia = existente.somnolencia?.takeIf(ReglasRegistro::somnolenciaValida),
                        sintomas = existente.listaSintomas.toSet(),
                        esEdicion = true,
                    )
                } else {
                    RegistroUi(cargando = false, horasSueno = ReglasRegistro.redondearSueno(habituales ?: 7.0))
                }
            }.onSuccess { _ui.value = it }
                .onFailure { _ui.value = RegistroUi(cargando = false) }
        }
    }

    /** [pasos] medias horas más (o menos, si es negativo). */
    fun sumarSueno(pasos: Int) = _ui.update { it.copy(horasSueno = ReglasRegistro.ajustarSueno(it.horasSueno, pasos)) }

    fun elegirSomnolencia(kss: Int) {
        if (ReglasRegistro.somnolenciaValida(kss)) _ui.update { it.copy(somnolencia = kss) }
    }

    fun alternarSintoma(sintoma: Sintoma) = _ui.update {
        it.copy(sintomas = if (sintoma in it.sintomas) it.sintomas - sintoma else it.sintomas + sintoma)
    }

    /** "Ninguno": quita todos los síntomas marcados. */
    fun sinSintomas() = _ui.update { it.copy(sintomas = emptySet()) }

    fun guardar() {
        val actual = _ui.value
        if (!actual.puedeGuardar) return
        _ui.update { it.copy(guardando = true, error = false) }
        viewModelScope.launch {
            runCatching {
                repositorio.guardarRegistro(
                    RegistroDiario(
                        astronautaId = astronautaId,
                        fecha = hoy.toString(),
                        horasSueno = actual.horasSueno,
                        somnolencia = actual.somnolencia,
                        sintomas = RegistroDiario.unirSintomas(actual.sintomas),
                    ),
                )
            }.onSuccess {
                // El sueño y los síntomas pueden completar una regla (fatiga, concentración).
                alertas.evaluarSinFallar(astronautaId)
                _ui.update { it.copy(guardando = false, guardado = true) }
            }
                .onFailure { _ui.update { it.copy(guardando = false, error = true) } }
        }
    }
}
