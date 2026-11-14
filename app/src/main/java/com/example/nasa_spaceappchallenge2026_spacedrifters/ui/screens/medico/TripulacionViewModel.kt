package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Tripulación vista desde la Tierra: un resumen por tripulante con lo ya recibido. */
@OptIn(ExperimentalCoroutinesApi::class)
class TripulacionViewModel(repositorio: MedicoRepository) : ViewModel() {

    /** null mientras carga. */
    val tripulacion: StateFlow<List<FilaTripulanteUi>?> = relojCada(30_000)
        .flatMapLatest { ahora ->
            repositorio.tripulacion().flatMapLatest { lista ->
                if (lista.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        lista.map { a ->
                            combine(
                                repositorio.contarAlertasRecibidas(a.id, ahora),
                                repositorio.sesionesRecibidas(a.id, ahora),
                            ) { alertas, sesiones -> aFilaTripulante(a, alertas, sesiones) }
                        },
                    ) { it.toList() }
                }
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
