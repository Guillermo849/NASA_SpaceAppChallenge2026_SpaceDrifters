package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.MedidaFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TensionFila
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.BloodPressureEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ContinuousMeasurementEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.ReglasConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.UmbralesFisiologicos
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.BloodPressureStatus
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.MeasurementType
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/** Una toma de constantes hecha a mano. null = no medida. */
data class LecturaConstantes(
    /** Frecuencia cardiaca (lpm). */
    val fc: Int? = null,
    /** Tensión sistólica y diastólica (mmHg). */
    val pas: Int? = null,
    val pad: Int? = null,
    /** Saturación de oxígeno (%). */
    val spo2: Int? = null,
    /** Frecuencia respiratoria (rpm). */
    val fr: Int? = null,
    /** Síntomas de ahora (códigos de [ConstantesRepository.SINTOMAS]); los no marcados cuentan como "no". */
    val sintomas: Set<String> = emptySet(),
    val ejercicioUltimaHora: Boolean = false,
    val estresUltimaHora: Boolean = false,
) {
    val vacia: Boolean get() = listOf(fc, pas, pad, spo2, fr).all { it == null }
}

/** Un protocolo que la toma ha abierto o en el que sigue. */
data class ProtocoloTocado(val protocoloId: String, val nivel: Int, val nuevo: Boolean)

data class ResultadoConstantes(
    /** Estado de cada métrica medida ("FC", "PAS", "SPO2", "FR"); sin entrada = sin umbral o sin datos. */
    val estados: Map<String, ReglasConstantes.Estado>,
    val protocolos: List<ProtocoloTocado>,
)

/** Últimos valores registrados de un astronauta (para el panel). */
data class UltimasConstantes(
    val fc: Double?,
    val pas: Int?,
    val pad: Int?,
    val spo2: Double?,
    val fr: Double?,
    /** Instante de la toma más reciente. */
    val instante: Long,
)

/**
 * Constantes introducidas a mano (astronauta o médico): se guardan en las tablas del equipo
 * (continuous_measurement: HR, SPO2, RR; blood_pressure), se comparan con la línea base de esa
 * persona y los umbrales de config_umbrales.json, y los hechos que salen (FC_ALTA, PAS_BAJA…),
 * junto con los síntomas y el contexto, abren o hacen avanzar los protocolos fisiológicos.
 */
class ConstantesRepository(
    private val db: AppDatabase,
    private val umbrales: UmbralesFisiologicos,
    private val fisiologico: FisiologicoRepository,
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val dao get() = db.fisiologicoDao()

    suspend fun guardar(astronautaId: Long, l: LecturaConstantes, ahora: Long = System.currentTimeMillis()): ResultadoConstantes {
        esperarDatos()
        require(!l.vacia) { "Sin constantes que guardar" }

        dao.insertarMedidas(
            listOfNotNull(
                l.fc?.let { medida(astronautaId, MeasurementType.HR, it, ahora) },
                l.spo2?.let { medida(astronautaId, MeasurementType.SPO2, it, ahora) },
                l.fr?.let { medida(astronautaId, MeasurementType.RR, it, ahora) },
            ),
        )
        if (l.pas != null || l.pad != null) {
            dao.insertarTensiones(
                listOf(
                    BloodPressureEntity().apply {
                        astronautId = astronautaId
                        scheduledFor = ahora
                        takenAt = ahora
                        systolic = l.pas
                        diastolic = l.pad
                        pulse = l.fc
                        status = BloodPressureStatus.DONE.name
                    },
                ),
            )
        }

        // Línea base de cada constante con todo su historial (incluida esta toma).
        val medidas = dao.medidasDesde(astronautaId, 0).first()
        val tensiones = dao.tensionesDesde(astronautaId, 0).first()
        val bases = buildMap {
            base(medidas, MeasurementType.HR)?.let { put(ReglasConstantes.FC, it) }
            base(medidas, MeasurementType.SPO2)?.let { put(ReglasConstantes.SPO2, it) }
            base(medidas, MeasurementType.RR)?.let { put(ReglasConstantes.FR, it) }
            baseTension(tensiones)?.let { put(ReglasConstantes.PAS, it) }
        }
        val valores = buildMap {
            l.fc?.let { put(ReglasConstantes.FC, it.toDouble()) }
            l.pas?.let { put(ReglasConstantes.PAS, it.toDouble()) }
            l.spo2?.let { put(ReglasConstantes.SPO2, it.toDouble()) }
            l.fr?.let { put(ReglasConstantes.FR, it.toDouble()) }
        }
        val hechos = umbrales.evaluar(valores, bases)
        val estados = ReglasConstantes.estados(umbrales, hechos, valores.keys)

        // Síntomas y contexto: lo no marcado es un "no" explícito (el astronauta ha respondido).
        SINTOMAS.forEach { hechos.con(it, it in l.sintomas) }
        hechos.con(EJERCICIO_ULTIMA_HORA, l.ejercicioUltimaHora)
        hechos.con(ESTRES_ULTIMA_HORA, l.estresUltimaHora)

        val antes = dao.abiertosAhora(astronautaId).map { it.protocoloId }.toSet()
        val protocolos = fisiologico.evaluar(astronautaId, hechos, ahora).map {
            ProtocoloTocado(it.protocoloId, it.nivel, nuevo = it.protocoloId !in antes)
        }
        return ResultadoConstantes(estados, protocolos)
    }

    /** Últimas constantes registradas (de cualquier fecha), o null si no hay ninguna. */
    fun ultimas(astronautaId: Long): Flow<UltimasConstantes?> = flow {
        esperarDatos()
        emitAll(
            combine(dao.medidasDesde(astronautaId, 0), dao.tensionesDesde(astronautaId, 0)) { medidas, tensiones ->
                fun ultima(t: MeasurementType) = medidas.lastOrNull { it.metrica == t.name }
                val tension = tensiones.lastOrNull()
                val instantes = listOfNotNull(medidas.lastOrNull()?.instante, tension?.instante)
                if (instantes.isEmpty()) return@combine null
                UltimasConstantes(
                    fc = ultima(MeasurementType.HR)?.valor,
                    pas = tension?.sistolica,
                    pad = tension?.diastolica,
                    spo2 = ultima(MeasurementType.SPO2)?.valor,
                    fr = ultima(MeasurementType.RR)?.valor,
                    instante = instantes.max(),
                )
            },
        )
    }

    private fun medida(astronautaId: Long, tipo: MeasurementType, valor: Int, ahora: Long) =
        ContinuousMeasurementEntity().apply {
            astronautId = astronautaId
            type = tipo.name
            value = valor.toDouble()
            timestamp = ahora
            source = FUENTE_MANUAL
        }

    private fun base(medidas: List<MedidaFila>, tipo: MeasurementType): Double? {
        val suyas = medidas.filter { it.metrica == tipo.name }
        return UmbralesFisiologicos.lineaBase(
            suyas.map { it.instante }.toLongArray(),
            suyas.map { it.valor }.toDoubleArray(),
            VENTANA_LINEA_BASE_MS,
        )
    }

    private fun baseTension(tensiones: List<TensionFila>): Double? {
        val conSistolica = tensiones.filter { it.sistolica != null }
        return UmbralesFisiologicos.lineaBase(
            conSistolica.map { it.instante }.toLongArray(),
            conSistolica.map { it.sistolica!!.toDouble() }.toDoubleArray(),
            VENTANA_LINEA_BASE_MS,
        )
    }

    companion object {
        /** continuous_measurement.source de lo introducido a mano. */
        const val FUENTE_MANUAL = "MANUAL"
        private const val VENTANA_LINEA_BASE_MS = UmbralesFisiologicos.DIAS_LINEA_BASE * 86_400_000L

        /**
         * Síntomas que se preguntan al tomar las constantes: los que usan los protocolos de
         * assets/protocolos.json (los signos de alarma van por el botón de alarma, no aquí).
         */
        val SINTOMAS = listOf("MAREO", "FATIGA", "PRESINCOPE", "CEFALEA", "DISNEA", "EDEMA_FACIAL_CERVICAL")
        const val EJERCICIO_ULTIMA_HORA = "EJERCICIO_ULTIMA_HORA"
        const val ESTRES_ULTIMA_HORA = "ESTRES_ULTIMA_HORA"
    }
}
