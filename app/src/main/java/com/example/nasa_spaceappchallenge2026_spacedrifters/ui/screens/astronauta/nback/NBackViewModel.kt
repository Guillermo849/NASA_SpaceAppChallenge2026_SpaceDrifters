package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.nback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.SesionNueva
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ConfigNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ControladorNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.FaseNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.GeneradorNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.temporizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random

/**
 * Flujo completo del N-Back:
 * instrucciones → (práctica 1-back → práctica 2-back, solo la primera vez) → prueba 2-back → fin.
 * Cada sesión (también cada práctica) se guarda en Room. Si se sale de la app o se pulsa atrás
 * durante una sesión, queda interrumpida (valida = false). La lógica está en [ControladorNBack].
 */
class NBackViewModel(
    private val repositorio: CognitivoRepository,
    private val astronautaId: Long,
    /** Ámbito de la app: los guardados terminan aunque se cierre la pantalla. */
    private val ambitoGuardado: CoroutineScope,
    private val config: ConfigNBack = ConfigNBack(),
    private val random: Random = Random(),
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    enum class TipoSesion { PRACTICA_1BACK, PRACTICA_2BACK, REAL }

    sealed interface Etapa {
        data object Cargando : Etapa
        data class Instrucciones(val conPractica: Boolean) : Etapa
        data class EnCurso(val controlador: ControladorNBack, val tipo: TipoSesion) : Etapa
        /** Pausa entre sesiones: antes de la práctica 2-back o antes de la prueba real. */
        data class Intermedia(val siguiente: TipoSesion) : Etapa
        data object Guardando : Etapa
        /** [estado] null = no se pudo guardar. */
        data class Terminada(val estado: EstadoCognitivo?) : Etapa
        data object Interrumpida : Etapa
    }

    private val _etapa = MutableStateFlow<Etapa>(Etapa.Cargando)
    val etapa: StateFlow<Etapa> = _etapa.asStateFlow()

    private val _fase = MutableStateFlow<FaseNBack>(FaseNBack.Preparada.INSTANCIA)
    /** Fase del estímulo en curso (forma, pantalla vacía, aviso...). */
    val fase: StateFlow<FaseNBack> = _fase.asStateFlow()

    private val temporizador = viewModelScope.temporizador()
    private var controlador: ControladorNBack? = null
    private var inicioEn = 0L

    init {
        viewModelScope.launch {
            val yaPractico = runCatching { repositorio.haHechoPractica(astronautaId, TipoPrueba.NBACK) }
                .getOrDefault(false)
            _etapa.value = Etapa.Instrucciones(conPractica = !yaPractico)
        }
    }

    /** Botón "Empezar" / "Seguir" de las instrucciones o de las pausas entre sesiones. */
    fun empezar() {
        when (val actual = _etapa.value) {
            is Etapa.Instrucciones ->
                lanzar(if (actual.conPractica) TipoSesion.PRACTICA_1BACK else TipoSesion.REAL)
            is Etapa.Intermedia -> lanzar(actual.siguiente)
            else -> Unit
        }
    }

    /** Sale de la app o pulsa atrás a mitad de sesión: se guarda como no válida. */
    fun interrumpir() {
        if (_etapa.value !is Etapa.EnCurso) return
        val actual = controlador ?: return
        actual.detener()
        controlador = null
        guardar(actual, valida = false)
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
        inicioEn = reloj()
        _fase.value = FaseNBack.Preparada.INSTANCIA
        val nuevo = ControladorNBack(secuencia, tipo != TipoSesion.REAL, config, temporizador) { fase ->
            _fase.value = fase
            if (fase is FaseNBack.Terminada) terminar(tipo)
        }
        controlador = nuevo
        _etapa.value = Etapa.EnCurso(nuevo, tipo)
        nuevo.empezar()
    }

    private fun terminar(tipo: TipoSesion) {
        val actual = controlador ?: return
        controlador = null
        val guardado = guardar(actual, valida = true)
        when (tipo) {
            TipoSesion.PRACTICA_1BACK -> _etapa.value = Etapa.Intermedia(TipoSesion.PRACTICA_2BACK)
            TipoSesion.PRACTICA_2BACK -> _etapa.value = Etapa.Intermedia(TipoSesion.REAL)
            TipoSesion.REAL -> {
                _etapa.value = Etapa.Guardando
                viewModelScope.launch { _etapa.value = Etapa.Terminada(guardado.await()) }
            }
        }
    }

    private fun guardar(c: ControladorNBack, valida: Boolean): Deferred<EstadoCognitivo?> {
        val sesion = SesionNueva(
            astronautaId = astronautaId,
            inicioEn = inicioEn,
            finEn = reloj(),
            esPractica = c.esPractica(),
            valida = valida,
        )
        val resultados = c.resultados
        val nivel = c.nivel
        return guardarEnSegundoPlano(ambitoGuardado) { repositorio.guardarNBack(sesion, resultados, nivel) }
    }
}
