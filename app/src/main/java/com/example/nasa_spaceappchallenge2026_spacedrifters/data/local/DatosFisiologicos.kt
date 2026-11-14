package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.BloodPressureEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ContinuousMeasurementEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.EstadoProtocolo
import kotlinx.coroutines.flow.Flow

/*
 * Módulo fisiológico: protocolos de decisión (assets/protocolos.json), sus cambios de nivel, las
 * pruebas que piden a bordo y el balance hídrico. Las constantes (FC, SpO2, FR) siguen en las tablas
 * del equipo: continuous_measurement, hourly_summary y blood_pressure.
 * astronautaId = astronaut.id, sin claves foráneas (como el resto de nuestras tablas).
 */

/** Pruebas que un protocolo puede pedir; el nombre coincide con el de assets/protocolos.json. */
enum class TipoPruebaClinica {
    ECO_DOPPLER_YUGULAR,
    ECOCARDIO,
    ECG,
    DIMERO_D,
    HEMOGRAMA,
    COAGULACION,
    /** Repetir FC, PA, SpO2 y FR tras un reposo (el tiempo lo indica la acción del protocolo). */
    REPETICION_CONSTANTES,
}

/** Un protocolo en marcha para un astronauta: en qué nodo del árbol está y con qué nivel (1–5). */
@Entity(tableName = "protocolo_activo", indices = [Index("astronautaId", "estado")])
data class ProtocoloActivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val astronautaId: Long,
    /** id del protocolo en assets/protocolos.json, p. ej. "trombosis-yugular". */
    val protocoloId: String,
    val nodoActual: String,
    val nivel: Int,
    val iniciadoEn: Long,
    val actualizadoEn: Long,
    val estado: EstadoProtocolo,
    /**
     * Nivel más bajo que propone el árbol (p. ej. el eco doppler sale normal). No se aplica hasta
     * que lo confirme la oficial médico o el médico; null = no hay bajada pendiente.
     */
    val nivelPropuesto: Int? = null,
    /** Hechos que ya se sabían (JSON: {"EDEMA_FACIAL_CERVICAL": true, ...}); se suman a los nuevos al avanzar. */
    val hechos: String = "{}",
    /** Nodos por los que ha pasado, en orden y separados por comas (para dibujar los pasos). */
    val recorrido: String = "",
) {
    val pasosRecorridos: List<String> get() = recorrido.split(',').filter { it.isNotBlank() }
}

/** Historial de un protocolo: cada cambio de nivel con su hora, el dato que lo provocó y quién lo confirmó. */
@Entity(tableName = "evento_protocolo", indices = [Index("protocoloActivoId")])
data class EventoProtocolo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val protocoloActivoId: Long,
    val fechaHora: Long,
    val nodo: String,
    val nivelAntes: Int,
    val nivelDespues: Int,
    /** Dato que lo provoca, p. ej. "CEFALEA" o "ECO_DOPPLER_YUGULAR=RETROGRADO". */
    val motivo: String,
    /** null = automático; si no, quién lo confirmó (p. ej. "OFICIAL_MEDICO:3" o "MEDICO"). */
    val confirmadoPor: String? = null,
    /** Cuándo lo ve el médico en la Tierra (fechaHora + retraso de la misión). */
    val llegaTierraEn: Long,
)

/** Prueba pedida por un protocolo; queda pendiente hasta que la oficial médico introduce el resultado. */
@Entity(tableName = "prueba_clinica", indices = [Index("astronautaId", "tipo"), Index("protocoloActivoId")])
data class PruebaClinica(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val astronautaId: Long,
    /** Protocolo que la pidió (sigue desde ese nodo al guardar el resultado); null = pedida por el médico. */
    val protocoloActivoId: Long? = null,
    val tipo: TipoPruebaClinica,
    val pedidaEn: Long,
    /** null = pendiente. */
    val realizadaEn: Long? = null,
    /** JSON, p. ej. {"flujo":"RETROGRADO","trombo":false} o {"vs_ml":62,"gc_lmin":4.1}. */
    val resultado: String? = null,
    /** astronaut.id de quien la hizo (normalmente la oficial médico). */
    val realizadaPor: Long? = null,
    /** Cuándo llega el resultado a la Tierra; null mientras está pendiente. */
    val llegaTierraEn: Long? = null,
)

/** Líquido ingerido y eliminado en un día (para el protocolo de alteraciones hemodinámicas). */
@Entity(tableName = "balance_hidrico", primaryKeys = ["astronautaId", "fecha"])
data class BalanceHidrico(
    val astronautaId: Long,
    /** Fecha local ISO, "2026-11-14". */
    val fecha: String,
    val ingeridoMl: Int,
    val eliminadoMl: Int,
) {
    val balanceMl: Int get() = ingeridoMl - eliminadoMl
}

/** Una medida continua (continuous_measurement): metrica = HR, SPO2 o RR. */
data class MedidaFila(val metrica: String, val valor: Double, val instante: Long)

/** Una toma de tensión (blood_pressure). */
data class TensionFila(val sistolica: Int?, val diastolica: Int?, val instante: Long)

/** Un protocolo en emergencia, para la pantalla de emergencia (ver [FisiologicoDao.emergencias]). */
data class EmergenciaFila(
    val protocoloActivoId: Long,
    val astronautaId: Long,
    val nombre: String?,
    /** Signos de alarma o dato que la provocó, p. ej. "DOLOR_TORACICO+SINCOPE". */
    val motivo: String?,
    val desde: Long?,
    val llegaTierraEn: Long?,
)

@Dao
interface FisiologicoDao {

    // ---- Protocolos ----

    @Insert
    suspend fun insertarProtocolo(protocolo: ProtocoloActivo): Long

    @Update
    suspend fun actualizarProtocolo(protocolo: ProtocoloActivo)

    @Query("SELECT * FROM protocolo_activo WHERE id = :id")
    suspend fun protocolo(id: Long): ProtocoloActivo?

    /** Protocolos no cerrados de un astronauta, el más grave primero. */
    @Query("SELECT * FROM protocolo_activo WHERE astronautaId = :astronautaId AND estado != 'CERRADO' ORDER BY nivel DESC, actualizadoEn DESC")
    fun abiertosDe(astronautaId: Long): Flow<List<ProtocoloActivo>>

    @Query("SELECT * FROM protocolo_activo WHERE astronautaId = :astronautaId AND estado != 'CERRADO'")
    suspend fun abiertosAhora(astronautaId: Long): List<ProtocoloActivo>

    /** Protocolos no cerrados de toda la tripulación (vista de la oficial médico y protocolo ambiental). */
    @Query("SELECT * FROM protocolo_activo WHERE estado != 'CERRADO' ORDER BY nivel DESC, actualizadoEn DESC")
    fun abiertos(): Flow<List<ProtocoloActivo>>

    /**
     * Protocolos en emergencia (nivel 5) sin atender, de toda la tripulación: la pantalla de
     * emergencia sale en todos los dispositivos de a bordo. Con el nombre del astronauta y el
     * último paso a nivel 5 (motivo, hora y llegada a la Tierra).
     */
    @Query(
        "SELECT p.id AS protocoloActivoId, p.astronautaId AS astronautaId, " +
            "a.first_name || ' ' || a.last_name AS nombre, e.motivo AS motivo, " +
            "e.fechaHora AS desde, e.llegaTierraEn AS llegaTierraEn " +
            "FROM protocolo_activo p " +
            "LEFT JOIN astronaut a ON a.id = p.astronautaId " +
            "LEFT JOIN evento_protocolo e ON e.id = (SELECT e2.id FROM evento_protocolo e2 " +
            "WHERE e2.protocoloActivoId = p.id AND e2.nivelDespues = 5 ORDER BY e2.fechaHora DESC, e2.id DESC LIMIT 1) " +
            "WHERE p.nodoActual = 'EMERGENCIA' AND p.estado != 'CERRADO' " +
            "ORDER BY p.actualizadoEn DESC",
    )
    fun emergencias(): Flow<List<EmergenciaFila>>

    // ---- Historial ----

    @Insert
    suspend fun insertarEventos(eventos: List<EventoProtocolo>)

    @Query("SELECT * FROM evento_protocolo WHERE protocoloActivoId = :protocoloActivoId ORDER BY fechaHora, id")
    fun eventosDe(protocoloActivoId: Long): Flow<List<EventoProtocolo>>

    // ---- Pruebas ----

    @Insert
    suspend fun insertarPrueba(prueba: PruebaClinica): Long

    @Update
    suspend fun actualizarPrueba(prueba: PruebaClinica)

    @Query("SELECT * FROM prueba_clinica WHERE id = :id")
    suspend fun prueba(id: Long): PruebaClinica?

    /** Pruebas pendientes de toda la tripulación, las más antiguas primero. */
    @Query("SELECT * FROM prueba_clinica WHERE realizadaEn IS NULL ORDER BY pedidaEn")
    fun pendientes(): Flow<List<PruebaClinica>>

    @Query("SELECT * FROM prueba_clinica WHERE protocoloActivoId = :protocoloActivoId ORDER BY pedidaEn")
    suspend fun pruebasDeProtocolo(protocoloActivoId: Long): List<PruebaClinica>

    /** Todas las pruebas ya hechas por un astronauta (para comparar con las anteriores). */
    @Query("SELECT * FROM prueba_clinica WHERE astronautaId = :astronautaId AND realizadaEn IS NOT NULL ORDER BY realizadaEn")
    suspend fun realizadasDe(astronautaId: Long): List<PruebaClinica>

    /** Resultados de un tipo de prueba, el más reciente primero (para comparar con el anterior). */
    @Query("SELECT * FROM prueba_clinica WHERE astronautaId = :astronautaId AND tipo = :tipo AND realizadaEn IS NOT NULL ORDER BY realizadaEn DESC")
    fun realizadas(astronautaId: Long, tipo: TipoPruebaClinica): Flow<List<PruebaClinica>>

    // ---- Pestaña "Fisiológico" del médico ----

    /** Todos los protocolos de un astronauta (abiertos y cerrados), el más reciente primero. */
    @Query("SELECT * FROM protocolo_activo WHERE astronautaId = :astronautaId ORDER BY (estado = 'CERRADO'), actualizadoEn DESC")
    fun protocolosDe(astronautaId: Long): Flow<List<ProtocoloActivo>>

    @Query(
        "SELECT e.* FROM evento_protocolo e JOIN protocolo_activo p ON p.id = e.protocoloActivoId " +
            "WHERE p.astronautaId = :astronautaId ORDER BY e.fechaHora, e.id",
    )
    fun eventosDeAstronauta(astronautaId: Long): Flow<List<EventoProtocolo>>

    @Query("SELECT * FROM prueba_clinica WHERE astronautaId = :astronautaId ORDER BY pedidaEn, id")
    fun pruebasDeAstronauta(astronautaId: Long): Flow<List<PruebaClinica>>

    // ---- Constantes (tablas del equipo: continuous_measurement y blood_pressure) ----

    @Insert
    suspend fun insertarMedidas(medidas: List<ContinuousMeasurementEntity>)

    @Insert
    suspend fun insertarTensiones(tensiones: List<BloodPressureEntity>)

    /** FC (HR), SpO2 (SPO2) y FR (RR) desde una fecha, en orden. */
    @Query(
        "SELECT type AS metrica, value AS valor, timestamp AS instante FROM continuous_measurement " +
            "WHERE astronaut_id = :astronautaId AND timestamp >= :desde ORDER BY timestamp",
    )
    fun medidasDesde(astronautaId: Long, desde: Long): Flow<List<MedidaFila>>

    /** Tensiones tomadas (status DONE) desde una fecha, en orden. */
    @Query(
        "SELECT systolic AS sistolica, diastolic AS diastolica, taken_at AS instante FROM blood_pressure " +
            "WHERE astronaut_id = :astronautaId AND status = 'DONE' AND taken_at >= :desde ORDER BY taken_at",
    )
    fun tensionesDesde(astronautaId: Long, desde: Long): Flow<List<TensionFila>>

    // ---- Balance hídrico ----

    @Upsert
    suspend fun guardarBalance(balance: BalanceHidrico)

    @Query("SELECT * FROM balance_hidrico WHERE astronautaId = :astronautaId AND fecha >= :desde ORDER BY fecha DESC")
    suspend fun balancesDesde(astronautaId: Long, desde: String): List<BalanceHidrico>
}
