package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ConfigNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ControladorNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FaseNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.GeneradorNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ResultadoEnsayoNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.temporizador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

/**
 * Flujo completo del N-Back:
 * instrucciones → (práctica 1-back → práctica 2-back, solo la primera vez) → prueba 2-back → fin.
 * Si se sale de la app o se pulsa atrás durante una sesión, queda interrumpida.
 * La lógica y los tiempos están en [ControladorNBack] (Java, en domain).
 */
class NBackViewModel(
    private val config: ConfigNBack = ConfigNBack(),
    private val random: Random = Random(),
) : ViewModel() {

    enum class TipoSesion { PRACTICA_1BACK, PRACTICA_2BACK, REAL }

    sealed interface Etapa {
        data class Instrucciones(val conPractica: Boolean) : Etapa
        data class EnCurso(val controlador: ControladorNBack, val tipo: TipoSesion) : Etapa
        /** Pausa entre sesiones: antes de la práctica 2-back o antes de la prueba real. */
        data class Intermedia(val siguiente: TipoSesion) : Etapa
        data class Terminada(val resultados: List<ResultadoEnsayoNBack>) : Etapa
        data object Interrumpida : Etapa
    }

    // De momento la práctica se hace siempre: cuando las sesiones se guarden en Room (paso 4)
    // se consultará si este astronauta ya la hizo.
    private val _etapa = MutableStateFlow<Etapa>(Etapa.Instrucciones(conPractica = true))
    val etapa: StateFlow<Etapa> = _etapa.asStateFlow()

    private val _fase = MutableStateFlow<FaseNBack>(FaseNBack.Preparada.INSTANCIA)
    /** Fase del estímulo en curso (forma, pantalla vacía, aviso...). */
    val fase: StateFlow<FaseNBack> = _fase.asStateFlow()

    private val temporizador = viewModelScope.temporizador()
    private var controlador: ControladorNBack? = null

    /** Botón "Empezar" / "Seguir" de las instrucciones o de las pausas entre sesiones. */
    fun empezar() {
        when (val actual = _etapa.value) {
            is Etapa.Instrucciones ->
                lanzar(if (actual.conPractica) TipoSesion.PRACTICA_1BACK else TipoSesion.REAL)
            is Etapa.Intermedia -> lanzar(actual.siguiente)
            else -> Unit
        }
    }

    /** Sale de la app o pulsa atrás a mitad de sesión: no cuenta. */
    fun interrumpir() {
        if (_etapa.value !is Etapa.EnCurso) return
        controlador?.detener()
        controlador = null
        _etapa.value = Etapa.Interrumpida
    }

    override fun onCleared() {
        controlador?.detener()
    }

    private fun lanzar(tipo: TipoSesion) {
        val secuencia = when (tipo) {
            TipoSesion.PRACTICA_1BACK -> GeneradorNBack.practica1Back(config, random)
            TipoSesion.PRACTICA_2BACK -> GeneradorNBack.practica2Back(config, random)
            TipoSesion.REAL -> GeneradorNBack.sesion(config, random)
        }
        val esPractica = tipo != TipoSesion.REAL
        _fase.value = FaseNBack.Preparada.INSTANCIA
        val nuevo = ControladorNBack(secuencia, esPractica, config, temporizador) { fase ->
            _fase.value = fase
            if (fase is FaseNBack.Terminada) terminar(tipo)
        }
        controlador = nuevo
        _etapa.value = Etapa.EnCurso(nuevo, tipo)
        nuevo.empezar()
    }

    private fun terminar(tipo: TipoSesion) {
        val resultados = controlador?.resultados.orEmpty()
        controlador = null
        _etapa.value = when (tipo) {
            TipoSesion.PRACTICA_1BACK -> Etapa.Intermedia(TipoSesion.PRACTICA_2BACK)
            TipoSesion.PRACTICA_2BACK -> Etapa.Intermedia(TipoSesion.REAL)
            TipoSesion.REAL -> Etapa.Terminada(resultados)
        }
    }
}
