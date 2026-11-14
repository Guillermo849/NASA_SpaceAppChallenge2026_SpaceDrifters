package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.SesionConResumen
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Un ensayo en la lista del detalle. Los códigos ("ROJO", "TRIANGULO") se traducen en la pantalla. */
data class FilaEnsayoUi(
    val numero: Int,
    /** Stroop: la palabra. N-Back: la forma. */
    val estimulo: String,
    /** Stroop: el color de la tinta. N-Back: null. */
    val tinta: String?,
    /** Stroop: coincidente. N-Back: era coincidencia. */
    val coincidente: Boolean,
    /** Stroop: el color tocado (null = omisión). N-Back: no se usa. */
    val respuesta: String?,
    /** N-Back: pulsó "Coincide". */
    val pulsado: Boolean,
    val correcto: Boolean,
    val tiempoMs: Long?,
)

data class DetalleSesionUi(
    val cargando: Boolean = true,
    val sesion: SesionConResumen? = null,
    val ensayos: List<FilaEnsayoUi> = emptyList(),
)

/** Detalle de una sesión para el médico: sus métricas y todos sus ensayos. */
class DetalleSesionViewModel(
    repositorio: MedicoRepository,
    sesionId: Long,
) : ViewModel() {

    private val _ui = MutableStateFlow(DetalleSesionUi())
    val ui: StateFlow<DetalleSesionUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            _ui.value = runCatching {
                val sesion = repositorio.sesion(sesionId)
                val ensayos = when (sesion?.tipo) {
                    TipoPrueba.STROOP -> repositorio.ensayosStroop(sesionId).map {
                        FilaEnsayoUi(it.numero, it.palabra, it.colorTinta, it.coincidente, it.respuesta, it.respuesta != null, it.correcto, it.tiempoMs)
                    }
                    TipoPrueba.NBACK -> repositorio.ensayosNBack(sesionId).map {
                        FilaEnsayoUi(it.numero, it.estimulo, null, it.esCoincidencia, null, it.pulsado, it.esCoincidencia == it.pulsado, it.tiempoMs)
                    }
                    null -> emptyList()
                }
                DetalleSesionUi(cargando = false, sesion = sesion, ensayos = ensayos)
            }.getOrElse { DetalleSesionUi(cargando = false) }
        }
    }
}
