package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import androidx.room.withTransaction
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EventoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.MedidaFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TensionFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.MotorProtocolos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Pestaña "Fisiológico" del médico: protocolos, historial, pruebas y constantes de un astronauta,
 * y las acciones del médico (confirmar o bajar el nivel, pedir otra prueba). Lo que viene de a
 * bordo se filtra por su llegada a la Tierra al construir la pantalla; lo que hace el médico se ve
 * al momento (llegaTierraEn = ahora).
 */
class FisiologicoMedicoRepository(
    private val db: AppDatabase,
    /** Retraso de la misión: las constantes no guardan su llegada, se calcula con esto. */
    val retrasoMisionMs: Long,
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val dao get() = db.fisiologicoDao()

    private fun <T> trasCargar(origen: () -> Flow<T>): Flow<T> = flow {
        esperarDatos()
        emitAll(origen())
    }

    fun protocolos(astronautaId: Long): Flow<List<ProtocoloActivo>> = trasCargar { dao.protocolosDe(astronautaId) }

    fun eventos(astronautaId: Long): Flow<List<EventoProtocolo>> = trasCargar { dao.eventosDeAstronauta(astronautaId) }

    fun pruebas(astronautaId: Long): Flow<List<PruebaClinica>> = trasCargar { dao.pruebasDeAstronauta(astronautaId) }

    fun medidas(astronautaId: Long, desde: Long): Flow<List<MedidaFila>> = trasCargar { dao.medidasDesde(astronautaId, desde) }

    fun tensiones(astronautaId: Long, desde: Long): Flow<List<TensionFila>> = trasCargar { dao.tensionesDesde(astronautaId, desde) }

    /** El médico confirma el nivel actual (y descarta la bajada que proponía el protocolo, si la había). */
    suspend fun confirmarNivel(protocoloActivoId: Long, ahora: Long = System.currentTimeMillis()) =
        cambiarNivel(protocoloActivoId, nuevo = null, motivo = MOTIVO_CONFIRMADO, ahora = ahora)

    /** El médico baja el nivel (bajar nunca es automático). */
    suspend fun bajarNivel(protocoloActivoId: Long, nivel: Int, ahora: Long = System.currentTimeMillis()) =
        cambiarNivel(protocoloActivoId, nuevo = nivel, motivo = MOTIVO_BAJADO, ahora = ahora)

    private suspend fun cambiarNivel(protocoloActivoId: Long, nuevo: Int?, motivo: String, ahora: Long) {
        db.withTransaction {
            val p = requireNotNull(dao.protocolo(protocoloActivoId)) { "No existe el protocolo $protocoloActivoId" }
            val nivel = nuevo ?: p.nivel
            require(nivel <= p.nivel) { "El médico solo confirma o baja el nivel" }
            val c = MotorProtocolos.confirmarNivel(p.nodoActual, p.nivel, nivel, motivo, CONFIRMA_MEDICO)
            dao.insertarEventos(
                listOf(
                    EventoProtocolo(
                        protocoloActivoId = p.id, fechaHora = ahora, nodo = c.nodo, nivelAntes = c.nivelAntes,
                        nivelDespues = c.nivelDespues, motivo = c.motivo, confirmadoPor = c.confirmadoPor,
                        llegaTierraEn = ahora,
                    ),
                ),
            )
            dao.actualizarProtocolo(p.copy(nivel = nivel, nivelPropuesto = null, actualizadoEn = ahora))
        }
    }

    /** El médico pide una prueba dentro de un protocolo: aparece en "Protocolos a bordo" como pendiente. */
    suspend fun pedirPrueba(protocoloActivoId: Long, tipo: TipoPruebaClinica, ahora: Long = System.currentTimeMillis()) {
        val p = requireNotNull(dao.protocolo(protocoloActivoId)) { "No existe el protocolo $protocoloActivoId" }
        val yaPendiente = dao.pruebasDeProtocolo(p.id).any { it.tipo == tipo && it.realizadaEn == null }
        if (!yaPendiente) {
            dao.insertarPrueba(PruebaClinica(astronautaId = p.astronautaId, protocoloActivoId = p.id, tipo = tipo, pedidaEn = ahora))
        }
    }

    companion object {
        const val CONFIRMA_MEDICO = "MEDICO"
        const val MOTIVO_CONFIRMADO = "CONFIRMADO_POR_MEDICO"
        const val MOTIVO_BAJADO = "BAJADO_POR_MEDICO"
    }
}
