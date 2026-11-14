package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.NotaMedica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoMedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.UmbralesFisiologicos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pestaña "Fisiológico" del perfil de un astronauta, para el médico. Se refresca cada 30 s. */
@OptIn(ExperimentalCoroutinesApi::class)
class FisiologicoMedicoViewModel(
    private val repositorio: FisiologicoMedicoRepository,
    private val medico: MedicoRepository,
    private val umbrales: UmbralesFisiologicos,
    private val astronautaId: Long,
) : ViewModel() {

    val fisiologico: StateFlow<FisiologicoMedicoUi> = relojCada(30_000)
        .flatMapLatest { ahora ->
            val desde = ahora - 30 * 86_400_000L
            combine(
                repositorio.protocolos(astronautaId),
                repositorio.eventos(astronautaId),
                repositorio.pruebas(astronautaId),
                repositorio.medidas(astronautaId, desde),
                repositorio.tensiones(astronautaId, desde),
            ) { protocolos, eventos, pruebas, medidas, tensiones ->
                // La hora de ahora (no la del último tic): así lo que acaba de hacer el médico se ve ya.
                val momento = maxOf(ahora, System.currentTimeMillis())
                construirFisiologico(protocolos, eventos, pruebas, medidas, tensiones, umbrales, repositorio.retrasoMisionMs, momento)
            }
        }
        .catch { emit(FisiologicoMedicoUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FisiologicoMedicoUi())

    /** Indicaciones del médico (las mismas notas que en "Pruebas cognitivas"). */
    val notas: StateFlow<List<NotaMedica>> = medico.notas(astronautaId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _mensaje = MutableStateFlow<Int?>(null)
    val mensaje: StateFlow<Int?> = _mensaje.asStateFlow()

    fun mensajeVisto() {
        _mensaje.value = null
    }

    fun confirmarNivel(protocoloActivoId: Long) = accion(R.string.fisio_msg_nivel_confirmado) {
        repositorio.confirmarNivel(protocoloActivoId)
    }

    fun bajarNivel(protocoloActivoId: Long, nivel: Int) = accion(R.string.fisio_msg_nivel_bajado) {
        repositorio.bajarNivel(protocoloActivoId, nivel)
    }

    fun pedirPrueba(protocoloActivoId: Long, tipo: TipoPruebaClinica) = accion(R.string.medico_msg_prueba_pedida) {
        repositorio.pedirPrueba(protocoloActivoId, tipo)
    }

    fun anadirIndicacion(texto: String) = accion(R.string.medico_msg_nota) {
        medico.anadirNota(astronautaId, texto)
    }

    private fun accion(@StringRes ok: Int, bloque: suspend () -> Unit) {
        viewModelScope.launch {
            _mensaje.value = if (runCatching { bloque() }.isSuccess) ok else R.string.medico_msg_error
        }
    }
}
