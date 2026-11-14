package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.emergencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.Emergencia
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * Signos de alarma y emergencias (nivel 5). La pantalla de emergencia lo usa desde la Activity,
 * así sale encima de cualquier pantalla y en cualquier rol; el panel del astronauta, para el botón
 * "Tengo un síntoma de alarma".
 */
class EmergenciaViewModel(
    private val repositorio: FisiologicoRepository,
    private val astronautaId: Long,
    /** Para que activar o atender una emergencia termine aunque se cierre la pantalla. */
    private val ambitoGuardado: CoroutineScope,
) : ViewModel() {

    /** Emergencias sin atender de toda la tripulación. */
    val emergencias: StateFlow<List<Emergencia>> = repositorio.emergencias()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Los signos de alarma que se pueden marcar (assets/protocolos.json). */
    val signosAlarma: List<String> get() = repositorio.signosAlarma

    /** "Tengo un síntoma de alarma": nivel 5 al instante para el astronauta que usa la app. */
    fun activar(signos: Set<String>) {
        if (signos.isEmpty()) return
        guardarEnSegundoPlano(ambitoGuardado) { repositorio.activarEmergencia(astronautaId, signos) }
    }

    /** La tripulación confirma que la emergencia está atendida. */
    fun atender(astronautaId: Long) {
        guardarEnSegundoPlano(ambitoGuardado) {
            repositorio.marcarAtendida(astronautaId, FisiologicoRepository.CONFIRMA_TRIPULACION)
        }
    }
}
