package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.ActividadBienestar
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.BienestarDiario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/** Bienestar psicológico: check-in diario y herramientas usadas (respiración, juego). */
class BienestarRepository(
    private val db: AppDatabase,
    /** Espera a que termine la carga de los datos de ejemplo del primer arranque. */
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val dao get() = db.bienestarDao()

    /** Check-in de un día (null si aún no lo ha hecho). Se actualiza solo al guardar. */
    fun delDia(astronautaId: Long, dia: LocalDate): Flow<BienestarDiario?> = flow {
        esperarDatos()
        emitAll(dao.delDia(astronautaId, dia.toString()))
    }

    /** Guarda (o sustituye) el check-in de ese astronauta y día. */
    suspend fun guardar(b: BienestarDiario) {
        esperarDatos()
        dao.guardar(listOf(b))
    }

    suspend fun registrarActividad(a: ActividadBienestar) {
        esperarDatos()
        dao.registrarActividad(a)
    }

    fun actividadesDesde(astronautaId: Long, desde: Long): Flow<List<ActividadBienestar>> = flow {
        esperarDatos()
        emitAll(dao.actividadesDesde(astronautaId, desde))
    }
}
