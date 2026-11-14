package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.oficial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ProtocoloEnCurso
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.PruebaParaResultado
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta.guardarEnSegundoPlano
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Protocolos abiertos de toda la tripulación con sus pruebas pendientes. null = cargando. */
class OficialMedicoViewModel(repositorio: FisiologicoRepository) : ViewModel() {
    val protocolos: StateFlow<List<ProtocoloEnCurso>?> = repositorio.enCurso()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Estado del formulario de resultado. */
sealed interface EstadoResultado {
    data object Cargando : EstadoResultado
    data object NoExiste : EstadoResultado
    data class Listo(val datos: PruebaParaResultado) : EstadoResultado
}

/** Formulario de resultado de una prueba (opciones cerradas o VS y GC del ecocardio). */
class ResultadoPruebaViewModel(
    private val repositorio: FisiologicoRepository,
    private val pruebaId: Long,
    /** Para que el guardado termine aunque se cierre la pantalla. */
    private val ambitoGuardado: CoroutineScope,
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoResultado>(EstadoResultado.Cargando)
    val estado: StateFlow<EstadoResultado> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.value = repositorio.pruebaParaResultado(pruebaId)?.let { EstadoResultado.Listo(it) }
                ?: EstadoResultado.NoExiste
        }
    }

    /** Guarda una opción cerrada ("RETROGRADO", "ELEVADO"...). */
    fun guardarOpcion(tipo: TipoPruebaClinica, opcion: String) =
        guardar(InterpretePruebas.resultadoCerrado(tipo.name, opcion))

    /** Guarda el ecocardio. */
    fun guardarEcocardio(vsMl: Double, gcLmin: Double) =
        guardar(InterpretePruebas.resultadoEcocardio(vsMl, gcLmin))

    private fun guardar(valores: Map<String, Any>) {
        guardarEnSegundoPlano(ambitoGuardado) {
            repositorio.guardarResultado(pruebaId, valores, realizadaPor = repositorio.oficialMedicoId())
        }
    }
}
