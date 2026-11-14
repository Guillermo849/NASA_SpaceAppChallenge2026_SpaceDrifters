package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nasa_spaceappchallenge2026_spacedrifters.R
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.MedicoRepository
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
import java.time.ZoneId

/** Perfil de un astronauta para el médico: pestaña "Pruebas cognitivas" y sus acciones. */
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilAstronautaViewModel(
    private val repositorio: MedicoRepository,
    val astronautaId: Long,
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class Datos1(
        val astronauta: com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Astronauta?,
        val sesiones: List<com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.SesionConResumen>,
        val llegadas: List<Long>,
        val bases: List<com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.LineaBaseCognitiva>,
        val registros: List<com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario>,
    )

    val perfil: StateFlow<PerfilMedicoUi> = relojCada(30_000)
        .flatMapLatest { ahora ->
            combine(
                combine(
                    repositorio.astronauta(astronautaId),
                    repositorio.sesionesRecibidas(astronautaId, ahora),
                    repositorio.llegadasPendientes(astronautaId, ahora),
                    repositorio.lineaBase(astronautaId),
                    repositorio.registros(astronautaId),
                ) { a, s, l, b, r -> Datos1(a, s, l, b, r) },
                repositorio.alertasRecibidas(astronautaId, ahora),
                repositorio.pruebasPendientes(astronautaId),
                repositorio.notas(astronautaId),
            ) { d, alertas, pendientes, notas ->
                construirPerfil(d.astronauta, d.sesiones, d.llegadas, d.bases, d.registros, alertas, pendientes, notas, ahora, zona)
            }
        }
        .catch { emit(PerfilMedicoUi(cargando = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilMedicoUi())

    private val _mensaje = MutableStateFlow<Int?>(null)
    /** Confirmación de la última acción (texto de strings.xml), o null. */
    val mensaje: StateFlow<Int?> = _mensaje.asStateFlow()

    fun mensajeVisto() {
        _mensaje.value = null
    }

    fun confirmarAlerta(alertaId: Long) = accion(R.string.medico_msg_alerta_confirmada) {
        repositorio.confirmarAlerta(alertaId)
    }

    fun pedirOtraPrueba(alertaId: Long) = accion(R.string.medico_msg_prueba_pedida) {
        repositorio.pedirOtraPrueba(alertaId)
    }

    fun programarPrueba(tipo: TipoPrueba) = accion(R.string.medico_msg_prueba_pedida) {
        repositorio.programarPrueba(astronautaId, tipo)
    }

    fun recalcularLineaBase() = accion(R.string.medico_msg_linea_base) {
        repositorio.recalcularLineaBase(astronautaId)
    }

    fun anadirNota(texto: String) = accion(R.string.medico_msg_nota) {
        repositorio.anadirNota(astronautaId, texto)
    }

    private fun accion(@StringRes ok: Int, bloque: suspend () -> Unit) {
        viewModelScope.launch {
            _mensaje.value = if (runCatching { bloque() }.isSuccess) ok else R.string.medico_msg_error
        }
    }
}
