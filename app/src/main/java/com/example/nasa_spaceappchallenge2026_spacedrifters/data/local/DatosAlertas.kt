// Alertas por reglas — Room, sobre la tabla alert del equipo (entity/AlertEntity.java)

package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

/** Una alerta con el nombre del astronauta afectado (para "Asignada a ti: Leo Martín"). */
data class AlertaConNombre(
    @Embedded val alerta: AlertEntity,
    @ColumnInfo(name = "nombre_astronauta") val nombreAstronauta: String?,
)

@Dao
interface AlertasDao {
    @Insert
    suspend fun insertar(alerta: AlertEntity): Long

    // ¿Ya hay una alerta activa de esta regla para este astronauta? (para no repetirla)
    @Query("SELECT COUNT(*) FROM alert WHERE astronaut_id = :astronautaId AND rule_id = :reglaId AND status = 'ACTIVE'")
    suspend fun contarActivas(astronautaId: Long, reglaId: String): Int

    // Activas que afectan a un astronauta o que tiene asignadas, de la más reciente a la más antigua
    @Query(
        "SELECT al.*, a.first_name || ' ' || a.last_name AS nombre_astronauta FROM alert al " +
            "LEFT JOIN astronaut a ON a.id = al.astronaut_id " +
            "WHERE al.status = 'ACTIVE' AND al.rule_id IS NOT NULL " +
            "AND (al.astronaut_id = :astronautaId OR al.assigned_astronaut_id = :astronautaId) " +
            "ORDER BY al.generated_at DESC"
    )
    fun activasPara(astronautaId: Long): Flow<List<AlertaConNombre>>

    // Todas las alertas por reglas de un astronauta (activas o no), de la más reciente a la más antigua
    @Query("SELECT * FROM alert WHERE astronaut_id = :astronautaId AND rule_id IS NOT NULL ORDER BY generated_at DESC")
    suspend fun deAstronauta(astronautaId: Long): List<AlertEntity>

    // Médico (en la Tierra): activas que ya han llegado, de la más reciente a la más antigua
    @Query(
        "SELECT * FROM alert WHERE astronaut_id = :astronautaId AND rule_id IS NOT NULL AND status = 'ACTIVE' " +
            "AND (arrives_earth_at IS NULL OR arrives_earth_at <= :ahora) ORDER BY generated_at DESC"
    )
    fun recibidasActivas(astronautaId: Long, ahora: Long): Flow<List<AlertEntity>>

    // Médico: cuántas alertas activas han llegado de cada astronauta (para la lista de tripulación)
    @Query(
        "SELECT COUNT(*) FROM alert WHERE astronaut_id = :astronautaId AND rule_id IS NOT NULL AND status = 'ACTIVE' " +
            "AND (arrives_earth_at IS NULL OR arrives_earth_at <= :ahora)"
    )
    fun contarRecibidasActivas(astronautaId: Long, ahora: Long): Flow<Int>

    // "Confirmar": el médico la da por vista
    @Query("UPDATE alert SET status = 'ACKNOWLEDGED', acknowledged_at = :ahora WHERE id = :alertaId")
    suspend fun confirmar(alertaId: Long, ahora: Long)

    @Query("SELECT * FROM alert WHERE id = :alertaId")
    suspend fun porId(alertaId: Long): AlertEntity?
}
