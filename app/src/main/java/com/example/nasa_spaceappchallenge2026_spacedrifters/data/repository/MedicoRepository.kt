package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Astronauta
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EnsayoNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EnsayoPvt
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.EnsayoStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.LineaBaseCognitiva
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.NotaMedica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaProgramada
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.SesionConResumen
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Lo que ve y hace el médico de vuelo desde la Tierra. Todo lo que viene de la nave se filtra por
 * su hora de llegada (llegaTierraEn / arrives_earth_at <= ahora): el médico ve los datos con el
 * retraso de la misión.
 */
class MedicoRepository(
    private val db: AppDatabase,
    private val cognitivo: CognitivoRepository,
    /** Espera a que termine la carga de los datos de ejemplo del primer arranque. */
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val medico get() = db.medicoDao()

    private fun <T> trasCargar(origen: () -> Flow<T>): Flow<T> = flow {
        esperarDatos()
        emitAll(origen())
    }

    // ---- Lectura ----

    fun tripulacion(): Flow<List<Astronauta>> = trasCargar { db.tripulacionDao().astronautas() }

    fun astronauta(id: Long): Flow<Astronauta?> = trasCargar { db.tripulacionDao().astronauta(id) }

    /** Sesiones (sin prácticas) que ya han llegado a la Tierra, de la más reciente a la más antigua. */
    fun sesionesRecibidas(id: Long, ahora: Long): Flow<List<SesionConResumen>> =
        trasCargar { medico.sesionesRecibidas(id, ahora) }

    /** Horas de llegada de las sesiones que aún vienen de camino, de la más próxima a la más lejana. */
    fun llegadasPendientes(id: Long, ahora: Long): Flow<List<Long>> = trasCargar { medico.llegadasPendientes(id, ahora) }

    fun lineaBase(id: Long): Flow<List<LineaBaseCognitiva>> = trasCargar { db.cognitivoDao().lineaBase(id) }

    /** Todo el registro diario del astronauta (para cruzar sueño y somnolencia con cada sesión). */
    fun registros(id: Long): Flow<List<RegistroDiario>> = trasCargar { db.tripulacionDao().registrosDesde(id, "0000-01-01") }

    fun alertasRecibidas(id: Long, ahora: Long): Flow<List<AlertEntity>> = trasCargar { db.alertasDao().recibidasActivas(id, ahora) }

    fun contarAlertasRecibidas(id: Long, ahora: Long): Flow<Int> = trasCargar { db.alertasDao().contarRecibidasActivas(id, ahora) }

    fun pruebasPendientes(id: Long): Flow<List<PruebaProgramada>> = trasCargar { medico.pendientes(id) }

    fun notas(id: Long): Flow<List<NotaMedica>> = trasCargar { medico.notas(id) }

    suspend fun sesion(sesionId: Long): SesionConResumen? = medico.sesion(sesionId)

    suspend fun ensayosStroop(sesionId: Long): List<EnsayoStroop> = db.cognitivoDao().ensayosStroop(sesionId)

    suspend fun ensayosNBack(sesionId: Long): List<EnsayoNBack> = db.cognitivoDao().ensayosNBack(sesionId)

    suspend fun ensayosPvt(sesionId: Long): List<EnsayoPvt> = db.cognitivoDao().ensayosPvt(sesionId)

    // ---- Acciones ----

    /** "Confirmar": el médico da la alerta por vista. */
    suspend fun confirmarAlerta(alertaId: Long, ahora: Long = System.currentTimeMillis()) =
        db.alertasDao().confirmar(alertaId, ahora)

    /**
     * "Pedir otra prueba": programa una prueba extra de la prueba que disparó la alerta (si salta por
     * Stroop, Stroop; si por N-Back, N-Back). Devuelve la prueba programada, o null si no se sabe cuál.
     */
    suspend fun pedirOtraPrueba(alertaId: Long, ahora: Long = System.currentTimeMillis()): TipoPrueba? {
        val alerta = db.alertasDao().porId(alertaId) ?: return null
        val prueba = AlertasRepository.leerExplicacion(alerta.explanation)?.sesiones?.firstOrNull()?.prueba
            ?: alerta.sourceRecordId?.let { medico.sesion(it)?.tipo?.name }
        val tipo = TipoPrueba.entries.firstOrNull { it.name == prueba } ?: return null
        programarPrueba(alerta.astronautId, tipo, ahora, alertaId)
        return tipo
    }

    /** "Programar prueba extra": aparece en "Pruebas de hoy" del astronauta hasta que la hace. */
    suspend fun programarPrueba(astronautaId: Long, tipo: TipoPrueba, ahora: Long = System.currentTimeMillis(), alertaId: Long? = null) {
        medico.programar(PruebaProgramada(astronautaId = astronautaId, tipo = tipo, pedidaEn = ahora, alertaId = alertaId))
    }

    /** "Recalcular línea base" de las dos pruebas, con sus sesiones de línea base actuales. */
    suspend fun recalcularLineaBase(astronautaId: Long) {
        TipoPrueba.entries.forEach { cognitivo.recalcularLineaBase(astronautaId, it) }
    }

    suspend fun anadirNota(astronautaId: Long, texto: String, ahora: Long = System.currentTimeMillis()) {
        val limpio = texto.trim()
        if (limpio.isNotEmpty()) medico.anadirNota(NotaMedica(astronautaId = astronautaId, texto = limpio, creadaEn = ahora))
    }
}
