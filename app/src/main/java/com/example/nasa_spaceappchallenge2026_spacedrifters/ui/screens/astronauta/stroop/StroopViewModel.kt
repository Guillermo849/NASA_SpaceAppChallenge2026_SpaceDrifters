package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.stroop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ConfigStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ControladorStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.FaseStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.GeneradorStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ResultadoEnsayoStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.temporizador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

/**
 * Flujo completo de la prueba Stroop:
 * instrucciones → (práctica → fin de práctica, solo la primera vez) → prueba → resultado.
 * Si se sale de la app o se pulsa atrás durante una prueba, queda interrumpida.
 * La lógica y los tiempos están en [ControladorStroop] (Java, en domain).
 */
class StroopViewModel(
    private val config: ConfigStroop = ConfigStroop(),
    private val random: Random = Random(),
) : ViewModel() {

    sealed interface Etapa {
        data class Instrucciones(val conPractica: Boolean) : Etapa
        data class EnCurso(val controlador: ControladorStroop) : Etapa
        data object FinPractica : Etapa
        data class Terminada(val resultados: List<ResultadoEnsayoStroop>) : Etapa
        data object Interrumpida : Etapa
    }

    // De momento la práctica se hace siempre: cuando las sesiones se guarden en Room (paso 4)
    // se consultará si este astronauta ya la hizo.
    private val _etapa = MutableStateFlow<Etapa>(Etapa.Instrucciones(conPractica = true))
    val etapa: StateFlow<Etapa> = _etapa.asStateFlow()

    private val _fase = MutableStateFlow<FaseStroop>(FaseStroop.Preparada.INSTANCIA)
    /** Fase del ensayo en curso (cruz, estímulo, aviso...). */
    val fase: StateFlow<FaseStroop> = _fase.asStateFlow()

    private val temporizador = viewModelScope.temporizador()

    private var controlador: ControladorStroop? = null

    /** Botón "Empezar" de las instrucciones o del fin de la práctica. */
    fun empezar() {
        when (val actual = _etapa.value) {
            is Etapa.Instrucciones -> lanzar(esPractica = actual.conPractica)
            Etapa.FinPractica -> lanzar(esPractica = false)
            else -> Unit
        }
    }

    /** Sale de la app o pulsa atrás a mitad de prueba: la sesión no cuenta. */
    fun interrumpir() {
        if (_etapa.value !is Etapa.EnCurso) return
        controlador?.detener()
        controlador = null
        _etapa.value = Etapa.Interrumpida
    }

    override fun onCleared() {
        controlador?.detener()
    }

    private fun lanzar(esPractica: Boolean) {
        val secuencia = if (esPractica) {
            GeneradorStroop.practica(config, random)
        } else {
            GeneradorStroop.sesion(config, random)
        }
        val nuevo = ControladorStroop(secuencia, esPractica, config, temporizador) { fase ->
            _fase.value = fase
            if (fase is FaseStroop.Terminada) terminar(esPractica)
        }
        controlador = nuevo
        _etapa.value = Etapa.EnCurso(nuevo)
        nuevo.empezar()
    }

    private fun terminar(esPractica: Boolean) {
        val resultados = controlador?.resultados.orEmpty()
        controlador = null
        _etapa.value = if (esPractica) Etapa.FinPractica else Etapa.Terminada(resultados)
    }
}
