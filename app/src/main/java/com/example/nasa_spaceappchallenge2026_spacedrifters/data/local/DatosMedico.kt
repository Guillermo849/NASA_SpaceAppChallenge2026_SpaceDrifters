// Datos del médico de vuelo — Room: pruebas extra pedidas y notas, y las consultas de su perfil

package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Prueba extra que pide el médico. Aparece en "Pruebas de hoy" del astronauta hasta que la hace. */
@Entity(tableName = "prueba_programada", indices = [Index("astronautaId")])
data class PruebaProgramada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** astronaut.id */
    val astronautaId: Long,
    val tipo: TipoPrueba,
    val pedidaEn: Long,
    /** Alerta que la motivó ("Pedir otra prueba"), o null si se programó a mano. */
    val alertaId: Long? = null,
    /** Cuándo la hizo (primera sesión válida posterior), o null si sigue pendiente. */
    val hechaEn: Long? = null,
)

/** Nota del médico sobre un astronauta. */
@Entity(tableName = "nota_medica", indices = [Index("astronautaId")])
data class NotaMedica(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val astronautaId: Long,
    val texto: String,
    val creadaEn: Long,
)

/** Una fila de la tabla de sesiones del médico: la sesión y, si es válida, su resumen. */
data class SesionConResumen(
    val sesionId: Long,
    val astronautaId: Long,
    val tipo: TipoPrueba,
    val inicioEn: Long,
    val valida: Boolean,
    val esLineaBase: Boolean,
    val llegaTierraEn: Long,
    val estado: String?,
    val efectoStroopMs: Double?,
    val porcentajeAciertos: Double?,
    val tiempoMedioMs: Double?,
    val erroresNoCoincidentes: Int?,
    val dPrima: Double?,
    val aciertosCoincidencias: Double?,
    val falsasAlarmas: Int?,
    val tiempoAciertosMs: Double?,
    val pvtTrMedioMs: Double? = null,
    val pvtTrP90Ms: Double? = null,
    val pvtLapsos: Int? = null,
    val pvtAnticipaciones: Int? = null,
)

@Dao
interface MedicoDao {
    // ---- Sesiones que ya han llegado a la Tierra (sin prácticas), de la más reciente a la más antigua ----
    @Query(
        "SELECT s.id AS sesionId, s.astronautaId, s.tipo, s.inicioEn, s.valida, s.esLineaBase, s.llegaTierraEn, " +
            "r.estado, r.efectoStroopMs, r.porcentajeAciertos, r.tiempoMedioMs, r.erroresNoCoincidentes, " +
            "r.dPrima, r.aciertosCoincidencias, r.falsasAlarmas, r.tiempoAciertosMs, " +
            "r.pvtTrMedioMs, r.pvtTrP90Ms, r.pvtLapsos, r.pvtAnticipaciones " +
            "FROM sesion_prueba s LEFT JOIN resumen_sesion r ON r.sesionId = s.id " +
            "WHERE s.astronautaId = :id AND s.esPractica = 0 AND s.llegaTierraEn <= :ahora " +
            "ORDER BY s.inicioEn DESC"
    )
    fun sesionesRecibidas(id: Long, ahora: Long): Flow<List<SesionConResumen>>

    @Query(
        "SELECT s.id AS sesionId, s.astronautaId, s.tipo, s.inicioEn, s.valida, s.esLineaBase, s.llegaTierraEn, " +
            "r.estado, r.efectoStroopMs, r.porcentajeAciertos, r.tiempoMedioMs, r.erroresNoCoincidentes, " +
            "r.dPrima, r.aciertosCoincidencias, r.falsasAlarmas, r.tiempoAciertosMs, " +
            "r.pvtTrMedioMs, r.pvtTrP90Ms, r.pvtLapsos, r.pvtAnticipaciones " +
            "FROM sesion_prueba s LEFT JOIN resumen_sesion r ON r.sesionId = s.id WHERE s.id = :sesionId"
    )
    suspend fun sesion(sesionId: Long): SesionConResumen?

    // Sesiones (sin prácticas) que aún vienen de camino: llegan después de :ahora
    @Query("SELECT llegaTierraEn FROM sesion_prueba WHERE astronautaId = :id AND esPractica = 0 AND llegaTierraEn > :ahora ORDER BY llegaTierraEn")
    fun llegadasPendientes(id: Long, ahora: Long): Flow<List<Long>>

    // ---- Pruebas extra ----
    @Insert
    suspend fun programar(p: PruebaProgramada): Long

    @Query("SELECT * FROM prueba_programada WHERE astronautaId = :id AND hechaEn IS NULL ORDER BY pedidaEn")
    fun pendientes(id: Long): Flow<List<PruebaProgramada>>

    // La primera sesión válida posterior a la petición la cumple
    @Query("UPDATE prueba_programada SET hechaEn = :hechaEn WHERE astronautaId = :id AND tipo = :tipo AND hechaEn IS NULL AND pedidaEn <= :hechaEn")
    suspend fun marcarHechas(id: Long, tipo: TipoPrueba, hechaEn: Long)

    // ---- Notas ----
    @Insert
    suspend fun anadirNota(n: NotaMedica): Long

    @Query("SELECT * FROM nota_medica WHERE astronautaId = :id ORDER BY creadaEn DESC")
    fun notas(id: Long): Flow<List<NotaMedica>>
}
