package com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AlertaConNombre
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.AlertSource
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.AlertStatus
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.Severity
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.ContextoReglas
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.MotorReglas
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Alertas por reglas (assets/reglas.json) sobre la tabla alert del equipo, con source NEURO.
 * [evaluar] se llama después de guardar una prueba válida o un registro diario: monta el contexto
 * del astronauta, pasa las reglas (MotorReglas, Java) y guarda una alerta por cada regla que se
 * cumple, salvo que ya haya otra activa de la misma regla para ese astronauta.
 */
class AlertasRepository(
    private val db: AppDatabase,
    private val reglas: List<Regla>,
    private val retrasoMisionMs: Long,
    /** Las lecturas para la UI esperan a la carga de los datos de ejemplo. */
    private val esperarDatos: suspend () -> Unit = {},
) {
    private val alertas get() = db.alertasDao()

    /** Alertas activas que afectan a este astronauta o que tiene asignadas. */
    fun activasPara(astronautaId: Long): Flow<List<AlertaConNombre>> = flow {
        esperarDatos()
        emitAll(alertas.activasPara(astronautaId))
    }

    /** Evalúa las reglas para este astronauta y guarda las alertas nuevas. Devuelve las creadas. */
    suspend fun evaluar(
        astronautaId: Long,
        ahora: Long = System.currentTimeMillis(),
        zona: ZoneId = ZoneId.systemDefault(),
    ): List<AlertEntity> {
        val contexto = contexto(astronautaId, Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate(), zona)
        val nuevas = mutableListOf<AlertEntity>()
        for (cumplida in MotorReglas.evaluar(reglas, contexto)) {
            val regla = cumplida.regla
            if (alertas.contarActivas(astronautaId, regla.id) > 0) continue
            val alerta = aAlerta(astronautaId, cumplida, contexto, ahora)
            alerta.id = alertas.insertar(alerta)
            nuevas += alerta
        }
        return nuevas
    }

    private suspend fun contexto(astronautaId: Long, hoy: LocalDate, zona: ZoneId): ContextoReglas {
        val sesiones = db.cognitivoDao().resumenesMisionRecientes(astronautaId, MAX_SESIONES).mapNotNull { r ->
            val estado = EstadoCognitivo.entries.firstOrNull { it.name == r.estado } ?: return@mapNotNull null
            ContextoReglas.Sesion(
                r.sesionId,
                r.tipo.name,
                Instant.ofEpochMilli(r.fechaEn).atZone(zona).toLocalDate(),
                estado,
                CognitivoRepository.metricaPrincipal(r.tipo),
                when (r.tipo) {
                    TipoPrueba.STROOP -> r.efectoStroopMs
                    TipoPrueba.NBACK -> r.dPrima
                    TipoPrueba.PVT -> r.pvtTrMedioMs
                },
            )
        }
        val trip = db.tripulacionDao()
        val registros = trip.registrosRecientes(astronautaId, hoy.minusDays(DIAS_DE_REGISTRO - 1L).toString())
        val noches = registros.map { ContextoReglas.Noche(LocalDate.parse(it.fecha), it.horasSueno) }
        val sintomasHoy = registros.firstOrNull { it.fecha == hoy.toString() }?.listaSintomas.orEmpty().map { it.name }.toSet()
        val suenoHabitual = trip.astronauta(astronautaId).first()?.horasSuenoHabituales
        val agua = registros.map { ContextoReglas.DiaAgua(LocalDate.parse(it.fecha), it.aguaLitros) }
        val bienestar = db.bienestarDao().recientes(astronautaId, hoy.minusDays(DIAS_DE_REGISTRO - 1L).toString()).map {
            ContextoReglas.DiaBienestar(LocalDate.parse(it.fecha), it.animo, it.estres, it.aislamiento)
        }
        return ContextoReglas(hoy, sesiones, noches, suenoHabitual, sintomasHoy, agua, bienestar)
    }

    /**
     * "Hablar con apoyo psicológico": alerta para el médico, sin regla detrás. Devuelve false si ya
     * hay una petición activa (no se duplica).
     */
    suspend fun solicitarApoyo(astronautaId: Long, ahora: Long = System.currentTimeMillis()): Boolean {
        if (alertas.contarActivas(astronautaId, REGLA_SOLICITUD_APOYO) > 0) return false
        val explicacion = ExplicacionAlerta(
            regla = REGLA_SOLICITUD_APOYO,
            prioridad = Regla.Prioridad.HOY.name,
            umbral = UmbralRegla(estados = emptyList()),
            sesiones = emptyList(),
            fuentes = listOf(FUENTE_BIENESTAR),
        )
        alertas.insertar(
            AlertEntity().apply {
                this.astronautId = astronautaId
                source = AlertSource.NEURO.name
                metric = REGLA_SOLICITUD_APOYO
                message = "El astronauta pide hablar con el equipo de apoyo psicológico"
                severity = Severity.WARNING.name
                generatedAt = ahora
                status = AlertStatus.ACTIVE.name
                ruleId = REGLA_SOLICITUD_APOYO
                priority = Regla.Prioridad.HOY.name
                steps = ""
                this.explanation = JSON.encodeToString(ExplicacionAlerta.serializer(), explicacion)
                arrivesEarthAt = ahora + retrasoMisionMs
            },
        )
        return true
    }

    private suspend fun aAlerta(
        astronautaId: Long,
        cumplida: MotorReglas.ReglaCumplida,
        contexto: ContextoReglas,
        ahora: Long,
    ): AlertEntity {
        val regla = cumplida.regla
        val disparo = cumplida.sesiones.firstOrNull()
        val explicacion = ExplicacionAlerta(
            regla = regla.id,
            prioridad = regla.prioridad.name,
            umbral = UmbralRegla(
                estados = regla.estados.map { it.name }.sorted(),
                sesionesSeguidas = regla.sesionesSeguidas,
                nochesBajoBase = regla.nochesBajoBase,
                sintomas = regla.sintomas.toList(),
                diasSeguidos = regla.diasSeguidos,
                aguaMinLitros = regla.aguaMinLitros.takeIf { it > 0 },
                animoMax = regla.bienestar?.animoMax,
                estresMin = regla.bienestar?.estresMin,
                aislamientoMin = regla.bienestar?.aislamientoMin,
            ),
            sesiones = cumplida.sesiones.map {
                DatoSesion(it.sesionId, it.prueba, it.fecha.toString(), it.estado.name, it.metrica, it.valor)
            },
            noches = cumplida.noches.map { DatoNoche(it.fecha.toString(), it.horas) },
            suenoHabitual = if (cumplida.noches.isNotEmpty()) contexto.suenoHabitual else null,
            sintomas = cumplida.sintomas.toList(),
            agua = cumplida.agua.map { DatoAgua(it.fecha.toString(), it.litros) },
            bienestar = cumplida.bienestar.map { DatoBienestar(it.fecha.toString(), it.animo, it.estres, it.aislamiento) },
            fuentes = buildList {
                addAll(cumplida.sesiones.map { it.prueba }.distinct())
                if (cumplida.noches.isNotEmpty() || cumplida.sintomas.isNotEmpty() || cumplida.agua.isNotEmpty()) {
                    add(FUENTE_REGISTRO)
                }
                if (cumplida.bienestar.isNotEmpty()) add(FUENTE_BIENESTAR)
            },
        )
        val aguaHoy = cumplida.agua.firstOrNull()
        return AlertEntity().apply {
            this.astronautId = astronautaId
            source = AlertSource.NEURO.name
            sourceRecordId = disparo?.sesionId
            metric = disparo?.metrica ?: when {
                aguaHoy != null -> METRICA_AGUA
                cumplida.bienestar.isNotEmpty() -> FUENTE_BIENESTAR
                else -> regla.id
            }
            value = disparo?.valor ?: aguaHoy?.litros
            message = MENSAJES[regla.id] ?: regla.id
            severity = when (regla.prioridad) {
                Regla.Prioridad.AHORA -> Severity.CRITICAL
                Regla.Prioridad.HOY -> Severity.WARNING
                else -> Severity.INFO
            }.name
            generatedAt = ahora
            status = AlertStatus.ACTIVE.name
            ruleId = regla.id
            priority = regla.prioridad.name
            steps = regla.pasos.joinToString(",")
            this.explanation = JSON.encodeToString(ExplicacionAlerta.serializer(), explicacion)
            assignedAstronautId = regla.asignarA?.let { db.tripulacionDao().idConCargo(it) }
            arrivesEarthAt = ahora + retrasoMisionMs
        }
    }

    companion object {
        /** Sesiones recientes que se miran (de sobra para "3 seguidas" de cada prueba). */
        private const val MAX_SESIONES = 30
        /** Días de registro diario que se miran (de sobra para "3 noches"). */
        private const val DIAS_DE_REGISTRO = 7
        const val FUENTE_REGISTRO = "REGISTRO_DIARIO"
        /** alert.metric de las reglas de hidratación (alert.value = litros anotados hoy). */
        const val METRICA_AGUA = "AGUA_LITROS"
        /** Fuente de los datos del check-in de bienestar psicológico. */
        const val FUENTE_BIENESTAR = "BIENESTAR"
        /** rule_id de la petición "Hablar con apoyo psicológico" (no está en reglas.json). */
        const val REGLA_SOLICITUD_APOYO = "solicitud-apoyo"
        private val JSON = Json { encodeDefaults = true }

        /** Resumen legible guardado en alert.message (la app muestra los textos de strings.xml por rule_id). */
        private val MENSAJES = mapOf(
            "stroop-tendencia" to "Stroop: 3 sesiones seguidas en Vigilar o Alerta",
            "nback-tendencia" to "N-Back: 3 sesiones seguidas en Vigilar o Alerta",
            "fatiga-cognitiva-combinada" to "N-Back en Vigilar o Alerta y 3 noches durmiendo menos de lo habitual",
            "concentracion-sintomas" to "Prueba cognitiva en Alerta y síntomas de concentración o dolor de cabeza hoy",
            "hidratacion-baja" to "Menos de 1,5 L de líquido dos días seguidos",
            "bienestar-bajo" to "Ánimo bajo, estrés alto o aislamiento alto dos días seguidos",
        )

        fun leerExplicacion(texto: String?): ExplicacionAlerta? =
            texto?.let { runCatching { JSON.decodeFromString(ExplicacionAlerta.serializer(), it) }.getOrNull() }
    }
}

/** "Por qué ha saltado": regla, umbral, datos concretos y su fuente. Se guarda en alert.explanation. */
@Serializable
data class ExplicacionAlerta(
    val regla: String,
    val prioridad: String,
    val umbral: UmbralRegla,
    /** Sesiones que la disparan, de la más reciente a la más antigua. */
    val sesiones: List<DatoSesion>,
    /** Noches de sueño implicadas, si las hay. */
    val noches: List<DatoNoche> = emptyList(),
    val suenoHabitual: Double? = null,
    /** Síntomas del registro de hoy implicados, si los hay. */
    val sintomas: List<String> = emptyList(),
    /** Líquido anotado en los días implicados (del más reciente al más antiguo), si los hay. */
    val agua: List<DatoAgua> = emptyList(),
    /** Check-ins de bienestar implicados (del más reciente al más antiguo), si los hay. */
    val bienestar: List<DatoBienestar> = emptyList(),
    /** De dónde salen los datos: "STROOP", "NBACK", "REGISTRO_DIARIO", "BIENESTAR". */
    val fuentes: List<String>,
)

@Serializable
data class UmbralRegla(
    val estados: List<String>,
    val sesionesSeguidas: Int = 0,
    val nochesBajoBase: Int = 0,
    val sintomas: List<String> = emptyList(),
    val diasSeguidos: Int = 0,
    /** Litros por debajo de los cuales cuenta un día (reglas de hidratación). */
    val aguaMinLitros: Double? = null,
    /** Reglas de bienestar (escalas 1–5). */
    val animoMax: Int? = null,
    val estresMin: Int? = null,
    val aislamientoMin: Int? = null,
)

@Serializable
data class DatoAgua(val fecha: String, val litros: Double?)

@Serializable
data class DatoBienestar(val fecha: String, val animo: Int?, val estres: Int?, val aislamiento: Int?)

@Serializable
data class DatoSesion(
    val sesionId: Long,
    val prueba: String,
    val fecha: String,
    val estado: String,
    val metrica: String,
    val valor: Double?,
)

@Serializable
data class DatoNoche(val fecha: String, val horas: Double?)
