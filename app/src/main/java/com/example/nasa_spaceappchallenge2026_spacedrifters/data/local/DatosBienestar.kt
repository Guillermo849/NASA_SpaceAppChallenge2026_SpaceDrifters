// Bienestar psicológico — Room
// Check-in diario (ánimo, estrés, aislamiento) y herramientas usadas (respiración, juego).

package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Check-in de bienestar de un día. Escalas 1–5 (ver ReglasBienestar); null = sin datos.
 * ánimo: 1 = muy mal … 5 = muy bien. estrés y aislamiento: 1 = nada … 5 = muchísimo.
 */
@Entity(
    tableName = "bienestar_diario",
    primaryKeys = ["astronautaId", "fecha"],
    indices = [Index("astronautaId")],
)
data class BienestarDiario(
    /** astronaut.id */
    val astronautaId: Long,
    /** Día en formato ISO, "2026-11-14". */
    val fecha: String,
    val animo: Int?,
    val estres: Int?,
    val aislamiento: Int?,
    /** Cuándo se guardó (epoch ms). */
    val guardadoEn: Long,
)

/** Herramientas de bienestar. Se guardan por nombre. */
enum class TipoActividadBienestar { RESPIRACION, JUEGO_PAREJAS }

/** Una vez que el astronauta ha usado una herramienta (para el resumen "Esta semana"). */
@Entity(tableName = "actividad_bienestar", indices = [Index("astronautaId")])
data class ActividadBienestar(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val astronautaId: Long,
    /** Nombre de [TipoActividadBienestar]. */
    val tipo: String,
    val instante: Long,
    val duracionS: Int,
    /** Juego: intentos que ha necesitado. Respiración: ciclos completos. */
    val detalle: Int? = null,
)

@Dao
interface BienestarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(b: List<BienestarDiario>)

    @Query("SELECT * FROM bienestar_diario WHERE astronautaId = :id AND fecha = :fecha")
    fun delDia(id: Long, fecha: String): Flow<BienestarDiario?>

    // Desde una fecha ISO (incluida), para las reglas
    @Query("SELECT * FROM bienestar_diario WHERE astronautaId = :id AND fecha >= :desde")
    suspend fun recientes(id: Long, desde: String): List<BienestarDiario>

    @Insert
    suspend fun registrarActividad(a: ActividadBienestar): Long

    @Query("SELECT * FROM actividad_bienestar WHERE astronautaId = :id AND instante >= :desde ORDER BY instante DESC")
    fun actividadesDesde(id: Long, desde: Long): Flow<List<ActividadBienestar>>
}
