package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.bienestar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ActividadBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoActividadBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.BienestarRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.bienestar.JuegoParejas
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random

/** Una carta tal y como se pinta. */
data class CartaUi(val cuerpo: CuerpoCeleste, val bocaArriba: Boolean, val emparejada: Boolean)

data class JuegoUi(
    val cartas: List<CartaUi> = emptyList(),
    val intentos: Int = 0,
    val parejasEncontradas: Int = 0,
    val parejas: Int = JuegoParejas.PAREJAS_POR_DEFECTO,
    val terminado: Boolean = false,
)

/** "Parejas orbitales": juego de memoria para desconectar unos minutos (no es una prueba). */
class JuegoParejasViewModel(
    private val repositorio: BienestarRepository,
    private val astronautaId: Long,
    private val ambito: CoroutineScope,
    private val random: Random = Random(),
) : ViewModel() {

    private var juego = JuegoParejas(JuegoParejas.PAREJAS_POR_DEFECTO, random)
    private var inicio = System.currentTimeMillis()

    private val _ui = MutableStateFlow(aUi())
    val ui: StateFlow<JuegoUi> = _ui.asStateFlow()

    fun voltear(carta: Int) {
        when (juego.voltear(carta)) {
            JuegoParejas.Resultado.FALLO -> viewModelScope.launch {
                delay(PAUSA_FALLO_MS)
                juego.ocultarFallo()
                _ui.value = aUi()
            }
            JuegoParejas.Resultado.PAREJA -> if (juego.isTerminado) registrar()
            else -> Unit
        }
        _ui.value = aUi()
    }

    fun otraPartida() {
        juego = JuegoParejas(JuegoParejas.PAREJAS_POR_DEFECTO, random)
        inicio = System.currentTimeMillis()
        _ui.value = aUi()
    }

    private fun registrar() {
        val intentos = juego.intentos
        val duracionS = ((System.currentTimeMillis() - inicio) / 1000).toInt()
        guardarEnSegundoPlano(ambito) {
            repositorio.registrarActividad(
                ActividadBienestar(
                    astronautaId = astronautaId,
                    tipo = TipoActividadBienestar.JUEGO_PAREJAS.name,
                    instante = System.currentTimeMillis(),
                    duracionS = duracionS,
                    detalle = intentos,
                ),
            )
        }
    }

    private fun aUi() = JuegoUi(
        cartas = List(juego.numCartas) { i ->
            CartaUi(CuerpoCeleste.entries[juego.simbolo(i)], juego.estaBocaArriba(i), juego.estaEmparejada(i))
        },
        intentos = juego.intentos,
        parejasEncontradas = juego.parejasEncontradas,
        parejas = juego.parejas,
        terminado = juego.isTerminado,
    )

    companion object {
        /** Tiempo que se ven las dos cartas de un fallo antes de darles la vuelta. */
        const val PAUSA_FALLO_MS = 1_000L
    }
}
