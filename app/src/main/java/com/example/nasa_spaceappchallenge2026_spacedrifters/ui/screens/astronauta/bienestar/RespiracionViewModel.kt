package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.bienestar

import androidx.lifecycle.ViewModel
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ActividadBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoActividadBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.BienestarRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import kotlinx.coroutines.CoroutineScope

/** Respiración guiada: la pantalla lleva el tiempo; aquí solo se anota la sesión hecha. */
class RespiracionViewModel(
    private val repositorio: BienestarRepository,
    private val astronautaId: Long,
    private val ambito: CoroutineScope,
) : ViewModel() {

    private var registrada = false

    /** Anota la sesión una sola vez (al terminar o al salir tras al menos un ciclo). */
    fun registrar(ciclos: Int, duracionMs: Long) {
        if (registrada || ciclos < 1) return
        registrada = true
        guardarEnSegundoPlano(ambito) {
            repositorio.registrarActividad(
                ActividadBienestar(
                    astronautaId = astronautaId,
                    tipo = TipoActividadBienestar.RESPIRACION.name,
                    instante = System.currentTimeMillis(),
                    duracionS = (duracionMs / 1000).toInt(),
                    detalle = ciclos,
                ),
            )
        }
    }

    /** Para empezar otra sesión desde la misma pantalla. */
    fun nueva() {
        registrada = false
    }
}
