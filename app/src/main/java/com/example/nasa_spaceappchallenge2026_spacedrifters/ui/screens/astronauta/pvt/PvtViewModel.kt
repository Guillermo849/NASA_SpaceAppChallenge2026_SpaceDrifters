package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.pvt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.SesionNueva
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt.ConfigPvt
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt.ControladorPvt
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.pvt.FasePvt
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.evaluarSinFallar
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
 * Flujo completo de la prueba PVT:
 * instrucciones → (práctica → fin de práctica, solo la primera vez) → prueba → resultado.
 *
 * La lógica real (ISI aleatorio, medición del TR, lapsos) está en [ControladorPvt] (Java).
 * El ViewModel solo gestiona las etapas de la pantalla y el guardado.
 */
class PvtViewModel(
    private val repositorio: CognitivoRepository,
    private val astronautaId: Long,
    private val ambitoGuardado: CoroutineScope,
    private val alertas: AlertasRepository,
    private val config: ConfigPvt = ConfigPvt(),
    private val random: Random = Random(),
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    sealed interface Etapa {
        data object Cargando : Etapa
        data class Instrucciones(val conPractica: Boolean) : Etapa
        data class EnCurso(val controlador: ControladorPvt) : Etapa
        data object FinPractica : Etapa
        data object Guardando : Etapa
        /** [estado] null = no se pudo guardar. */
        data class Terminada(val estado: EstadoCognitivo?) : Etapa
        data object Interrumpida : Etapa
    }

    private val _etapa = MutableStateFlow<Etapa>(Etapa.Cargando)
    val etapa: StateFlow<Etapa> = _etapa.asStateFlow()

    private val _fase = MutableStateFlow<FasePvt>(FasePvt.Preparada.INSTANCIA)
    /** Fase del ensayo en curso (ISI, estímulo, feedback...). */
    val fase: StateFlow<FasePvt> = _fase.asStateFlow()

    private val temporizador = viewModelScope.temporizador()
    private var controlador: ControladorPvt? = null
    private var inicioEn = 0L

    init {
        viewModelScope.launch {
            val yaPractico = runCatching {
                repositorio.haHechoPractica(astronautaId, TipoPrueba.PVT)
            }.getOrDefault(false)
            _etapa.value = Etapa.Instrucciones(conPractica = !yaPractico)
        }
    }

    /** Botón "Empezar" de las instrucciones o del fin de la práctica. */
    fun empezar() {
        when (val actual = _etapa.value) {
            is Etapa.Instrucciones -> lanzar(esPractica = actual.conPractica)
            Etapa.FinPractica -> lanzar(esPractica = false)
            else -> Unit
        }
    }

    /** Sale de la app o pulsa atrás a mitad de prueba: se guarda como no válida. */
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

    private fun lanzar(esPractica: Boolean) {
        inicioEn = reloj()
        _fase.value = FasePvt.Preparada.INSTANCIA
        val nuevo = ControladorPvt(config, esPractica, random, temporizador) { fase ->
            _fase.value = fase
            if (fase is FasePvt.Terminada) terminar(esPractica)
        }
        controlador = nuevo
        _etapa.value = Etapa.EnCurso(nuevo)
        nuevo.empezar()
    }

    private fun terminar(esPractica: Boolean) {
        val actual = controlador ?: return
        controlador = null
        val guardado = guardar(actual, valida = true)
        if (esPractica) {
            _etapa.value = Etapa.FinPractica
        } else {
            _etapa.value = Etapa.Guardando
            viewModelScope.launch { _etapa.value = Etapa.Terminada(guardado.await()) }
        }
    }

    private fun guardar(c: ControladorPvt, valida: Boolean): Deferred<EstadoCognitivo?> {
        val sesion = SesionNueva(
            astronautaId = astronautaId,
            inicioEn = inicioEn,
            finEn = reloj(),
            esPractica = c.esPractica(),
            valida = valida,
        )
        val resultados = c.resultados
        return guardarEnSegundoPlano(ambitoGuardado) {
            val estado = repositorio.guardarPvt(sesion, resultados)
            if (sesion.cuenta) alertas.evaluarSinFallar(astronautaId)
            estado
        }
    }
}
