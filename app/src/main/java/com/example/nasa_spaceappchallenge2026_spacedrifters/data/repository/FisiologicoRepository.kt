package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import androidx.room.withTransaction
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EventoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ProtocoloActivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Cambio
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.MotorProtocolos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.NivelGravedad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Una emergencia sin atender de un astronauta (puede venir de varios protocolos a la vez). */
data class Emergencia(
    val astronautaId: Long,
    val nombre: String?,
    /** Signos de alarma o datos que la provocaron, sin repetir: "DOLOR_TORACICO", "SINCOPE"... */
    val motivos: List<String>,
    val desde: Long?,
    /** Cuándo le llega el aviso al médico en la Tierra. */
    val llegaTierraEn: Long?,
)

/**
 * Protocolos fisiológicos: guarda lo que decide [MotorProtocolos] (nodo, nivel, historial) en
 * Room. De momento, la parte de signos de alarma y emergencias (paso 3 del plan).
 */
class FisiologicoRepository(
    private val db: AppDatabase,
    private val motor: MotorProtocolos,
    private val retrasoMisionMs: Long,
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val dao get() = db.fisiologicoDao()

    /** Signos de alarma que se comprueban antes que cualquier protocolo (assets/protocolos.json). */
    val signosAlarma: List<String> get() = motor.signosAlarma

    /** Emergencias sin atender de toda la tripulación, la más reciente primero. */
    fun emergencias(): Flow<List<Emergencia>> = dao.emergencias().map { filas ->
        filas.groupBy { it.astronautaId }.map { (id, deEste) ->
            Emergencia(
                astronautaId = id,
                nombre = deEste.first().nombre,
                motivos = deEste.flatMap { it.motivo.orEmpty().split('+') }.filter { it.isNotBlank() }.distinct(),
                desde = deEste.mapNotNull { it.desde }.minOrNull(),
                llegaTierraEn = deEste.mapNotNull { it.llegaTierraEn }.minOrNull(),
            )
        }
    }

    /**
     * El astronauta tiene uno o varios signos de alarma: todos sus protocolos abiertos pasan a
     * nivel 5 (el motor mira los signos de alarma antes que nada). Si no tenía ninguno abierto, se
     * abre uno de emergencia directa. No espera a la Tierra: el aviso sale con la máxima prioridad.
     */
    suspend fun activarEmergencia(astronautaId: Long, signos: Set<String>, ahora: Long = System.currentTimeMillis()) {
        require(signos.isNotEmpty()) { "Falta el signo de alarma" }
        esperarDatos()
        val hechos = Hechos().apply { signos.forEach { con(it, true) } }
        require(motor.signosAlarmaPresentes(hechos).isNotEmpty()) { "No es un signo de alarma: $signos" }

        db.withTransaction {
            val abiertos = dao.abiertosAhora(astronautaId)
            for (p in abiertos) {
                if (p.protocoloId == PROTOCOLO_EMERGENCIA) {
                    // Ya estaba en emergencia directa: se añade el signo nuevo al historial.
                    dao.insertarEventos(listOf(evento(p.id, Cambio(MotorProtocolos.NODO_EMERGENCIA, p.nivel, NIVEL_5, signos.joinToString("+"), null), ahora)))
                    dao.actualizarProtocolo(p.copy(actualizadoEn = ahora))
                    continue
                }
                val r = motor.avanzar(p.protocoloId, p.nodoActual, p.nivel, hechos)
                dao.actualizarProtocolo(
                    p.copy(nodoActual = r.nodo, nivel = r.nivel, estado = r.estado, nivelPropuesto = r.nivelPropuesto, actualizadoEn = ahora),
                )
                dao.insertarEventos(r.cambios.map { evento(p.id, it, ahora) })
            }
            if (abiertos.isEmpty()) {
                val id = dao.insertarProtocolo(
                    ProtocoloActivo(
                        astronautaId = astronautaId,
                        protocoloId = PROTOCOLO_EMERGENCIA,
                        nodoActual = MotorProtocolos.NODO_EMERGENCIA,
                        nivel = NIVEL_5,
                        iniciadoEn = ahora,
                        actualizadoEn = ahora,
                        estado = EstadoProtocolo.ABIERTO,
                    ),
                )
                val motivo = motor.signosAlarmaPresentes(hechos).joinToString("+")
                dao.insertarEventos(
                    listOf(evento(id, Cambio(MotorProtocolos.NODO_EMERGENCIA, MotorProtocolos.NIVEL_INICIAL, NIVEL_5, motivo, null), ahora)),
                )
            }
        }
    }

    /**
     * La tripulación ha atendido la emergencia de un astronauta: sus protocolos en emergencia se
     * cierran y queda en el historial quién lo ha confirmado. Bajar de nivel 5 siempre es manual.
     */
    suspend fun marcarAtendida(astronautaId: Long, confirmadoPor: String, ahora: Long = System.currentTimeMillis()) {
        db.withTransaction {
            for (p in dao.abiertosAhora(astronautaId).filter { it.nodoActual == MotorProtocolos.NODO_EMERGENCIA }) {
                val cambio = MotorProtocolos.confirmarNivel(p.nodoActual, p.nivel, p.nivel, MOTIVO_ATENDIDA, confirmadoPor)
                dao.insertarEventos(listOf(evento(p.id, cambio, ahora)))
                dao.actualizarProtocolo(p.copy(estado = EstadoProtocolo.CERRADO, nivelPropuesto = null, actualizadoEn = ahora))
            }
        }
    }

    private fun evento(protocoloActivoId: Long, c: Cambio, ahora: Long) = EventoProtocolo(
        protocoloActivoId = protocoloActivoId,
        fechaHora = ahora,
        nodo = c.nodo,
        nivelAntes = c.nivelAntes,
        nivelDespues = c.nivelDespues,
        motivo = c.motivo,
        confirmadoPor = c.confirmadoPor,
        llegaTierraEn = ahora + retrasoMisionMs,
    )

    companion object {
        /** protocoloId de una emergencia sin protocolo previo (botón "Tengo un síntoma de alarma"). */
        const val PROTOCOLO_EMERGENCIA = "emergencia"
        const val MOTIVO_ATENDIDA = "EMERGENCIA_ATENDIDA"
        /** Quién confirma desde la pantalla de emergencia (todavía no hay inicio de sesión). */
        const val CONFIRMA_TRIPULACION = "TRIPULACION"
        private val NIVEL_5 = NivelGravedad.EMERGENCIA.numero
    }
}
