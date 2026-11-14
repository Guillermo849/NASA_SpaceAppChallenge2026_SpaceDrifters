package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** Inicio del astronauta: qué pruebas de hoy ya están hechas (se actualiza solo al guardar). */
class InicioAstronautaViewModel(
    repositorio: CognitivoRepository,
    astronautaId: Long,
    zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val inicioDelDia: Long = LocalDate.now(zona).atStartOfDay(zona).toInstant().toEpochMilli()

    val hechasHoy: StateFlow<Set<TipoPrueba>> = repositorio.tiposHechosDesde(astronautaId, inicioDelDia)
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
}
