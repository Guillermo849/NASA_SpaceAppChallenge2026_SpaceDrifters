package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.constantes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ConstantesRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.LecturaConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ResultadoConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.TripulacionRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.ReglasConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos del formulario, por métrica de [ReglasConstantes] ("FC", "PAS", "PAD", "SPO2", "FR"). */
val CAMPOS_CONSTANTES = listOf(
    ReglasConstantes.FC,
    ReglasConstantes.PAS,
    ReglasConstantes.PAD,
    ReglasConstantes.SPO2,
    ReglasConstantes.FR,
)

/** Por qué no se puede guardar. */
enum class ErrorConstantes { NINGUNA, FUERA_DE_LO_POSIBLE, TENSION_INCOHERENTE, AL_GUARDAR }

data class ConstantesUi(
    /** Nombre del astronauta medido (null mientras carga o si no existe). */
    val nombre: String? = null,
    /** Texto de cada campo tal como se escribe; vacío = no medido. */
    val campos: Map<String, String> = CAMPOS_CONSTANTES.associateWith { "" },
    /** Campos con un valor imposible (para marcarlos). */
    val camposMal: Set<String> = emptySet(),
    /** Vacío = "Ninguno". */
    val sintomas: Set<String> = emptySet(),
    val ejercicioUltimaHora: Boolean = false,
    val estresUltimaHora: Boolean = false,
    val error: ErrorConstantes? = null,
    val guardando: Boolean = false,
    /** Tras guardar: cómo han salido y qué protocolos se han abierto. */
    val resultado: ResultadoConstantes? = null,
    /** Lo guardado, para enseñarlo en el resultado. */
    val guardadas: Map<String, Int> = emptyMap(),
)

/**
 * Tomar constantes de un astronauta: FC, tensión, SpO2 y FR, más síntomas y contexto. Al guardar se
 * evalúan frente a su línea base y pueden abrir protocolos (ver [ConstantesRepository]).
 */
class ConstantesViewModel(
    private val repositorio: ConstantesRepository,
    tripulacion: TripulacionRepository,
    private val astronautaId: Long,
    /** El guardado termina aunque se cierre la pantalla. */
    private val ambito: CoroutineScope,
    /** Retraso con la Tierra (minutos), para decir cuándo lo recibe el médico. */
    val retrasoMin: Long = 18,
) : ViewModel() {

    private val _ui = MutableStateFlow(ConstantesUi())
    val ui: StateFlow<ConstantesUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val nombre = runCatching { tripulacion.astronauta(astronautaId).first()?.nombre }.getOrNull()
            _ui.update { it.copy(nombre = nombre) }
        }
    }

    /** Solo cifras, como mucho 3 (ninguna constante pasa de 260). */
    fun escribir(campo: String, texto: String) {
        val limpio = texto.filter(Char::isDigit).take(3)
        _ui.update { it.copy(campos = it.campos + (campo to limpio), camposMal = it.camposMal - campo, error = null) }
    }

    fun alternarSintoma(codigo: String) = _ui.update {
        it.copy(sintomas = if (codigo in it.sintomas) it.sintomas - codigo else it.sintomas + codigo)
    }

    fun sinSintomas() = _ui.update { it.copy(sintomas = emptySet()) }

    fun alternarEjercicio() = _ui.update { it.copy(ejercicioUltimaHora = !it.ejercicioUltimaHora) }

    fun alternarEstres() = _ui.update { it.copy(estresUltimaHora = !it.estresUltimaHora) }

    fun guardar() {
        val actual = _ui.value
        if (actual.guardando || actual.resultado != null) return
        val valores = actual.campos.mapValues { (_, t) -> t.toIntOrNull() }
        val imposibles = valores.filter { (campo, v) -> !ReglasConstantes.valida(campo, v) }.keys
        val error = when {
            valores.values.all { it == null } -> ErrorConstantes.NINGUNA
            imposibles.isNotEmpty() -> ErrorConstantes.FUERA_DE_LO_POSIBLE
            !ReglasConstantes.tensionCoherente(valores[ReglasConstantes.PAS], valores[ReglasConstantes.PAD]) ->
                ErrorConstantes.TENSION_INCOHERENTE
            else -> null
        }
        if (error != null) {
            _ui.update { it.copy(error = error, camposMal = imposibles) }
            return
        }
        val lectura = LecturaConstantes(
            fc = valores[ReglasConstantes.FC],
            pas = valores[ReglasConstantes.PAS],
            pad = valores[ReglasConstantes.PAD],
            spo2 = valores[ReglasConstantes.SPO2],
            fr = valores[ReglasConstantes.FR],
            sintomas = actual.sintomas,
            ejercicioUltimaHora = actual.ejercicioUltimaHora,
            estresUltimaHora = actual.estresUltimaHora,
        )
        _ui.update { it.copy(guardando = true, error = null) }
        val guardado = guardarEnSegundoPlano(ambito) { repositorio.guardar(astronautaId, lectura) }
        viewModelScope.launch {
            val resultado = guardado.await()
            _ui.update {
                if (resultado == null) {
                    it.copy(guardando = false, error = ErrorConstantes.AL_GUARDAR)
                } else {
                    it.copy(
                        guardando = false,
                        resultado = resultado,
                        guardadas = valores.filterValues { v -> v != null }.mapValues { (_, v) -> v!! },
                    )
                }
            }
        }
    }
}
