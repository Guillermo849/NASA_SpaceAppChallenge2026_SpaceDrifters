package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabaseAntiguo
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Astronauta
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/**
 * Tripulación y registro diario. Mínimo por ahora: cuando llegue la base de datos nueva del
 * equipo, este es el sitio a adaptar (las pantallas no dependen de las tablas).
 */
class TripulacionRepository(
    private val db: AppDatabaseAntiguo,
    /** Espera a que termine la carga de los datos de ejemplo del primer arranque. */
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val dao get() = db.tripulacionDao()

    fun astronauta(id: Long): Flow<Astronauta?> = flow {
        esperarDatos()
        emitAll(dao.astronauta(id))
    }

    /** Registro diario de un día (null si aún no lo ha hecho). Se actualiza solo al guardar. */
    fun registroDelDia(astronautaId: Long, dia: LocalDate): Flow<RegistroDiario?> = flow {
        esperarDatos()
        emitAll(dao.registroDelDia(astronautaId, dia.toString()))
    }

    /** Guarda (o sustituye) el registro de ese astronauta y día. */
    suspend fun guardarRegistro(registro: RegistroDiario) {
        esperarDatos()
        dao.insertarRegistros(listOf(registro))
    }
}
