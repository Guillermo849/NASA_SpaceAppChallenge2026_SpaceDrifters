package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.config.ObjetivosDiarios
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoEjercicio
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
    /** Litros bebidos ayer. null = sin anotar (se guarda como "Sin datos"). */
    val aguaLitros: Double? = null,
    /** % del menú comido ayer (0, 25, 50, 75, 100). null = sin anotar. */
    val ingesta: Int? = null,
    /** Minutos de ejercicio ayer. null = sin anotar. */
    val ejercicioMinutos: Int? = null,
    val ejercicios: Set<TipoEjercicio> = emptySet(),
    val objetivos: ObjetivosDiarios = ObjetivosDiarios(),
    /** Ya había registro de hoy: se está editando. */
    val esEdicion: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val error: Boolean = false,
) {
    val puedeGuardar: Boolean get() = !cargando && !guardando && somnolencia != null
}

/**
 * Registro diario del astronauta para hoy: sueño, somnolencia, síntomas y, opcionales,
 * hidratación, nutrición y actividad física de ayer.
 */
class RegistroDiarioViewModel(
    private val repositorio: TripulacionRepository,
    /** Evalúa las reglas de alerta después de guardar el registro. */
    private val alertas: AlertasRepository,
    private val astronautaId: Long,
    private val objetivos: ObjetivosDiarios = ObjetivosDiarios(),
    zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val hoy: LocalDate = LocalDate.now(zona)

    private val _ui = MutableStateFlow(RegistroUi(objetivos = objetivos))
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
                        aguaLitros = existente.aguaLitros,
                        ingesta = existente.ingestaPorcentaje?.takeIf(ReglasRegistro::ingestaValida),
                        ejercicioMinutos = existente.ejercicioMinutos,
                        ejercicios = existente.listaEjercicios.toSet(),
                        objetivos = objetivos,
                        esEdicion = true,
                    )
                } else {
                    RegistroUi(
                        cargando = false,
                        horasSueno = ReglasRegistro.redondearSueno(habituales ?: 7.0),
                        objetivos = objetivos,
                    )
                }
            }.onSuccess { _ui.value = it }
                .onFailure { _ui.value = RegistroUi(cargando = false, objetivos = objetivos) }
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

    /** [pasos] cuartos de litro más o menos; sin anotar empieza en el objetivo. */
    fun sumarAgua(pasos: Int) = _ui.update {
        it.copy(aguaLitros = ReglasRegistro.ajustarAgua(it.aguaLitros, pasos, objetivos.aguaLitros))
    }

    /** Volver a pulsar la opción elegida la desmarca (queda "Sin datos"). */
    fun elegirIngesta(porcentaje: Int) {
        if (!ReglasRegistro.ingestaValida(porcentaje)) return
        _ui.update { it.copy(ingesta = if (it.ingesta == porcentaje) null else porcentaje) }
    }

    /** [pasos] cuartos de hora más o menos; sin anotar empieza en el objetivo. */
    fun sumarEjercicio(pasos: Int) = _ui.update {
        val minutos = ReglasRegistro.ajustarEjercicio(it.ejercicioMinutos, pasos, objetivos.ejercicioMinutos)
        it.copy(ejercicioMinutos = minutos, ejercicios = if (minutos == 0) emptySet() else it.ejercicios)
    }

    fun alternarEjercicio(tipo: TipoEjercicio) = _ui.update {
        val tipos = if (tipo in it.ejercicios) it.ejercicios - tipo else it.ejercicios + tipo
        // Marcar un tipo sin minutos (o con 0): se parte del objetivo, que el astronauta ajusta.
        val minutos = if (tipos.isNotEmpty() && (it.ejercicioMinutos ?: 0) == 0) {
            objetivos.ejercicioMinutos
        } else {
            it.ejercicioMinutos
        }
        it.copy(ejercicios = tipos, ejercicioMinutos = minutos)
    }

    /** "No hice ejercicio": 0 minutos, sin tipos. */
    fun sinEjercicio() = _ui.update { it.copy(ejercicioMinutos = 0, ejercicios = emptySet()) }

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
                        aguaLitros = actual.aguaLitros,
                        ingestaPorcentaje = actual.ingesta,
                        ejercicioMinutos = actual.ejercicioMinutos,
                        ejercicioTipos = RegistroDiario.unirEjercicios(actual.ejercicios),
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
