// Tripulación y registro diario — Room
// La tripulación vive en las tablas del equipo (app_user, mission, astronaut; ver entity/).
// Aquí: el registro diario y las consultas que necesitan el panel, las pruebas y las reglas.

package com.example.nasa_spaceappchallenge2026_spacedrifters.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AppUserEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AstronautEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.MissionEntity
import kotlinx.coroutines.flow.Flow

/** Cargo a bordo (astronaut.crew_role guarda su nombre). El texto visible sale de strings.xml. */
enum class CargoTripulante { COMANDANTE, INGENIERIA_VUELO, OFICIAL_MEDICO, ESPECIALISTA_MISION }

/**
 * Lo que la app necesita de un astronauta, leído de astronaut + mission (no es una tabla).
 * Los null se muestran como "Sin datos".
 */
data class Astronauta(
    val id: Long,
    val nombre: String,
    val cargo: CargoTripulante?,
    /** Horas de sueño habituales (su base para las reglas de sueño). */
    val horasSuenoHabituales: Double?,
    /** Inicio de su misión (epoch ms): el día 1 de misión. */
    val inicioMision: Long?,
)

/** Síntomas del registro diario. Se guardan por nombre, separados por comas. */
enum class Sintoma { DOLOR_CABEZA, DIFICULTAD_CONCENTRACION, FATIGA, NAUSEAS }

/** Lo que el astronauta anota cada día. Los null son "Sin datos", nunca "bien". */
@Entity(
    tableName = "registro_diario",
    primaryKeys = ["astronautaId", "fecha"],
    indices = [Index("astronautaId")],
)
data class RegistroDiario(
    /** astronaut.id */
    val astronautaId: Long,
    /** Día en formato ISO, "2026-11-14". */
    val fecha: String,
    /** Horas dormidas la noche anterior. */
    val horasSueno: Double?,
    /** Escala de somnolencia de Karolinska (KSS): 1 = muy despierto … 9 = muy somnoliento. */
    val somnolencia: Int?,
    /** Nombres de [Sintoma] separados por comas; vacío si ninguno. */
    val sintomas: String = "",
) {
    val listaSintomas: List<Sintoma>
        get() = sintomas.split(',').filter { it.isNotBlank() }.mapNotNull { nombre ->
            Sintoma.entries.firstOrNull { it.name == nombre.trim() }
        }

    companion object {
        fun unirSintomas(sintomas: Collection<Sintoma>) = sintomas.joinToString(",") { it.name }
    }
}

@Dao
interface TripulacionDao {
    // ---- Tripulación (tablas del equipo) ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(u: AppUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMision(m: MissionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAstronauta(a: AstronautEntity): Long

    @Query("SELECT COUNT(*) FROM astronaut")
    suspend fun contarAstronautas(): Int

    @Query(
        "SELECT a.id AS id, a.first_name || ' ' || a.last_name AS nombre, a.crew_role AS cargo, " +
            "a.usual_sleep_hours AS horasSuenoHabituales, m.start_date AS inicioMision " +
            "FROM astronaut a LEFT JOIN mission m ON m.id = a.mission_id ORDER BY a.id"
    )
    fun astronautas(): Flow<List<Astronauta>>

    @Query(
        "SELECT a.id AS id, a.first_name || ' ' || a.last_name AS nombre, a.crew_role AS cargo, " +
            "a.usual_sleep_hours AS horasSuenoHabituales, m.start_date AS inicioMision " +
            "FROM astronaut a LEFT JOIN mission m ON m.id = a.mission_id WHERE a.id = :id"
    )
    fun astronauta(id: Long): Flow<Astronauta?>

    // ---- Registro diario ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRegistros(r: List<RegistroDiario>)

    // Registro diario desde una fecha ISO (incluida), del más reciente al más antiguo
    @Query("SELECT * FROM registro_diario WHERE astronautaId = :id AND fecha >= :desde ORDER BY fecha DESC")
    fun registrosDesde(id: Long, desde: String): Flow<List<RegistroDiario>>

    @Query("SELECT * FROM registro_diario WHERE astronautaId = :id AND fecha = :fecha")
    suspend fun registroDe(id: Long, fecha: String): RegistroDiario?

    // El mismo registro como flujo: se actualiza solo al guardar
    @Query("SELECT * FROM registro_diario WHERE astronautaId = :id AND fecha = :fecha")
    fun registroDelDia(id: Long, fecha: String): Flow<RegistroDiario?>

    // Registros desde una fecha ISO (incluida), para las reglas
    @Query("SELECT * FROM registro_diario WHERE astronautaId = :id AND fecha >= :desde")
    suspend fun registrosRecientes(id: Long, desde: String): List<RegistroDiario>

    // Tripulante con un cargo (p. ej. la oficial médico de a bordo, para asignarle alertas)
    @Query("SELECT id FROM astronaut WHERE crew_role = :cargo ORDER BY id LIMIT 1")
    suspend fun idConCargo(cargo: String): Long?
}
