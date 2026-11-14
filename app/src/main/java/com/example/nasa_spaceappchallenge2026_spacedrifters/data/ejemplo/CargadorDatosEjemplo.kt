package com.example.nasa_spaceappchallenge2026_spacedrifters.data.ejemplo

import androidx.room.withTransaction
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.AppDatabase
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AppUserEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AstronautEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.MissionEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.BloodPressureEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.ContinuousMeasurementEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.BloodPressureStatus
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.MeasurementType
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.model.Role
import java.text.Normalizer
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.BienestarDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoEjercicio
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.FisiologicoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.BalanceHidrico
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPruebaClinica
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.InterpretePruebas
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.protocolos.Hechos
import kotlinx.coroutines.flow.first
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.SesionNueva
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.ejemplo.GeneradorEnsayosEjemplo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.ConfigNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.GeneradorNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.nback.SecuenciaNBack
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.ConfigStroop
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.stroop.GeneradorStroop
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Random
import kotlin.math.roundToInt

// ---- Formato de assets/datos_ejemplo.json ----

@Serializable
data class DatosEjemplo(
    val diasMision: Int,
    val tripulacion: List<TripulanteEjemplo>,
)

@Serializable
data class PerfilStroopEjemplo(val tiempoCoincidentesMs: Double, val efectoMs: Double, val desviacionMs: Double)

@Serializable
data class PerfilNBackEjemplo(val dPrima: Double, val desviacion: Double, val tiempoMs: Double)

@Serializable
data class TripulanteEjemplo(
    val id: Long,
    val nombre: String,
    val cargo: CargoTripulante,
    val horasSuenoHabituales: Double,
    /** Semilla del generador: los mismos datos en cada instalación. */
    val semilla: Long,
    /** Antigüedad de su última sesión de hoy (la de Maya, < 18 min, sigue de camino a la Tierra). */
    val minutosDesdeUltimaSesion: Long = 100,
    val stroop: PerfilStroopEjemplo,
    val nback: PerfilNBackEjemplo,
    /** Desviaciones respecto a su media por día de misión; positivo = peor. */
    val stroopZ: List<Double>,
    val nbackZ: List<Double>,
    val horasSueno: List<Double?>,
    val somnolencia: List<Int?>,
    /** Día de misión ("1".."14") → síntomas de ese día. */
    val sintomas: Map<String, List<Sintoma>> = emptyMap(),
    /** Hábitos del día anterior anotados en el registro de cada día; vacío o null = sin datos. */
    val aguaLitros: List<Double?> = emptyList(),
    /** % del menú comido. */
    val ingesta: List<Int?> = emptyList(),
    val ejercicioMinutos: List<Int?> = emptyList(),
    /** Check-in de bienestar psicológico por día; null = sin check-ins. */
    val bienestar: BienestarEjemplo? = null,
    /** Constantes vitales simuladas (FC, PAS, PAD, SPO2, FR); null = sin constantes. */
    val constantes: ConstantesEjemplo? = null,
)

/** Escalas 1–5 por día de misión: ánimo (1 = muy bajo), estrés y aislamiento (1 = nada). */
@Serializable
data class BienestarEjemplo(
    val animo: List<Int?>,
    val estres: List<Int?>,
    val aislamiento: List<Int?>,
)

/** Constantes de un tripulante: su valor habitual y, por día de misión ("13"), los que cambian. */
@Serializable
data class ConstantesEjemplo(
    val base: Map<String, Double>,
    val dias: Map<String, Map<String, Double>> = emptyMap(),
)

/**
 * Carga los datos de ejemplo la primera vez que arranca la app (base de datos vacía), para que
 * haya contenido desde el principio. El día 14 de misión es hoy.
 *
 * Los ensayos se generan (GeneradorEnsayosEjemplo, Java) y se guardan con [CognitivoRepository]
 * igual que una prueba real, en orden: prácticas → línea base → misión. Así los resúmenes, las
 * líneas base y los estados salen de los ensayos.
 */
class CargadorDatosEjemplo(
    private val db: AppDatabase,
    private val repositorio: CognitivoRepository,
    /** Si se pasa, al terminar evalúa las reglas para cada tripulante (alertas de la demo). */
    private val alertas: AlertasRepository? = null,
    /**
     * Si se pasa, abre los protocolos fisiológicos de la demo (ver [protocolosDemo]). Tiene que ser
     * un repositorio que no espere a estos mismos datos de ejemplo.
     */
    private val fisiologico: FisiologicoRepository? = null,
) {
    private val configStroop = ConfigStroop()
    private val configNBack = ConfigNBack()

    /** Devuelve true si ha cargado datos; false si ya había tripulación. */
    suspend fun cargarSiVacia(
        json: String,
        ahora: Long = System.currentTimeMillis(),
        zona: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (db.tripulacionDao().contarAstronautas() > 0) return false
        val datos = JSON.decodeFromString<DatosEjemplo>(json)
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val lanzamiento = hoy.minusDays(datos.diasMision - 1L)
        db.withTransaction {
            // Insertar el usuario administrador la primera vez (prototipo: contraseña en texto plano).
            insertarAdmin(ahora)

            // Una misión para toda la tripulación: empieza el día 1 (hoy es el último día).
            val misionId = db.tripulacionDao().insertarMision(
                MissionEntity().apply {
                    name = NOMBRE_MISION
                    startDate = lanzamiento.atStartOfDay(zona).toInstant().toEpochMilli()
                },
            )
            val ultimaSesion = datos.tripulacion.mapIndexed { orden, t ->
                t.id to cargar(t, orden, datos.diasMision, misionId, ahora, zona)
            }
            // Con todo cargado, las reglas ven el estado de hoy (no se evalúan sesión a sesión).
            // Cada alerta se genera cuando terminó su última sesión, como habría pasado a bordo:
            // así llega a la Tierra con esos datos (la de Maya, aún de camino; la de Leo, ya recibida).
            alertas?.let { a -> ultimaSesion.forEach { (id, instante) -> a.evaluar(id, instante, zona) } }
        }
        fisiologico?.let { protocolosDemo(it, ahora, zona) }
        return true
    }

    /**
     * Protocolos fisiológicos de la demo (el paso 7 del plan los completará):
     * - Leo: edema facial + cefalea → eco doppler con flujo retrógrado sin trombo → analítica
     *   pendiente (protocolo 1, nivel 3).
     * - Maya: FC alta y PA baja con mareo y balance hídrico negativo → repitiendo las mediciones
     *   (protocolo 2, nivel 3).
     */
    private suspend fun protocolosDemo(f: FisiologicoRepository, ahora: Long, zona: ZoneId) {
        val hora = 3_600_000L
        f.evaluar(LEO, Hechos().con("EDEMA_FACIAL_CERVICAL", true).con("CEFALEA", true), ahora - 3 * hora)
        db.fisiologicoDao().pendientes().first()
            .firstOrNull { it.astronautaId == LEO && it.tipo == TipoPruebaClinica.ECO_DOPPLER_YUGULAR }
            ?.let { doppler ->
                f.guardarResultado(
                    doppler.id,
                    InterpretePruebas.resultadoCerrado(InterpretePruebas.ECO_DOPPLER_YUGULAR, "RETROGRADO"),
                    realizadaPor = ANE,
                    ahora = ahora - 2 * hora,
                )
            }

        f.evaluar(
            MAYA,
            Hechos().con("FC_ALTA", true).con("PAS_BAJA", true).con("PAS_ALTA", false).con("MAREO", true),
            ahora - 40 * 60_000L,
        )
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        db.fisiologicoDao().guardarBalance(BalanceHidrico(MAYA, hoy.toString(), ingeridoMl = 1500, eliminadoMl = 2100))
        db.fisiologicoDao().guardarBalance(BalanceHidrico(MAYA, hoy.minusDays(1).toString(), ingeridoMl = 1800, eliminadoMl = 2250))
    }

    /**
     * Constantes de los [dias] de misión: 4 medidas al día (7, 12, 17 y 22 h; hoy, solo las que ya
     * han pasado) con algo de ruido. FC, SpO2 y FR van a continuous_measurement (HR, SPO2, RR) y la
     * tensión a blood_pressure. Con su propio generador para no cambiar los ensayos cognitivos.
     */
    private suspend fun constantes(id: Long, c: ConstantesEjemplo, semilla: Long, dias: Int, ahora: Long, zona: ZoneId) {
        val random = Random(semilla + 1_000)
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val lanzamiento = hoy.minusDays(dias - 1L)
        val medidas = mutableListOf<ContinuousMeasurementEntity>()
        val tensiones = mutableListOf<BloodPressureEntity>()
        for (dia in 1..dias) {
            val delDia = c.base + c.dias[dia.toString()].orEmpty()
            fun valor(metrica: String, ruido: Double) = delDia[metrica]?.let { it + random.nextGaussian() * ruido }
            for (hora in HORAS_CONSTANTES) {
                val instante = lanzamiento.plusDays(dia - 1L).atTime(hora, 0).atZone(zona).toInstant().toEpochMilli()
                if (instante > ahora) continue
                val fc = valor("FC", 2.5)
                fun medida(tipo: MeasurementType, v: Double?) = v?.let {
                    medidas += ContinuousMeasurementEntity().apply {
                        astronautId = id
                        type = tipo.name
                        value = Math.round(it * 10) / 10.0
                        timestamp = instante
                        source = FUENTE_SIMULADA
                    }
                }
                medida(MeasurementType.HR, fc)
                medida(MeasurementType.SPO2, valor("SPO2", 0.5)?.coerceAtMost(100.0))
                medida(MeasurementType.RR, valor("FR", 0.8))
                val pas = valor("PAS", 3.0)
                val pad = valor("PAD", 2.0)
                if (pas != null && pad != null) {
                    tensiones += BloodPressureEntity().apply {
                        astronautId = id
                        scheduledFor = instante
                        takenAt = instante
                        systolic = pas.roundToInt()
                        diastolic = pad.roundToInt()
                        pulse = fc?.roundToInt()
                        status = BloodPressureStatus.DONE.name
                    }
                }
            }
        }
        db.fisiologicoDao().insertarMedidas(medidas)
        db.fisiologicoDao().insertarTensiones(tensiones)
    }

    /** Carga un tripulante y devuelve el inicio de su última sesión de misión. */
    private suspend fun cargar(t: TripulanteEjemplo, orden: Int, dias: Int, misionId: Long, ahora: Long, zona: ZoneId): Long {
        listOf(t.stroopZ, t.nbackZ, t.horasSueno, t.somnolencia).forEach {
            require(it.size == dias) { "${t.nombre}: se esperaban $dias días y hay ${it.size}" }
        }
        listOf(t.aguaLitros, t.ingesta, t.ejercicioMinutos).forEach {
            require(it.isEmpty() || it.size == dias) { "${t.nombre}: se esperaban $dias días de hábitos y hay ${it.size}" }
        }
        insertarTripulante(t, misionId, ahora)
        t.constantes?.let { constantes(t.id, it, t.semilla, dias, ahora, zona) }

        val random = Random(t.semilla)
        val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
        val lanzamiento = hoy.minusDays(dias - 1L)
        fun a(fecha: LocalDate, hora: Int, minuto: Int) = fecha.atTime(hora, minuto).atZone(zona).toInstant().toEpochMilli()

        // Familiarización, 4 semanas antes del lanzamiento: Stroop + las dos prácticas del N-Back.
        val diaPractica = lanzamiento.minusDays(28)
        stroop(t, a(diaPractica, 9, 0), esPractica = true, esLineaBase = false, z = 0.0, random = random)
        nbackPerfecto(t, a(diaPractica, 9, 10), GeneradorNBack.practica1Back(configNBack, random), random)
        nbackPerfecto(t, a(diaPractica, 9, 15), GeneradorNBack.practica2Back(configNBack, random), random)

        // Línea base antes del lanzamiento: media − 1, media y media + 1 desviación.
        listOf(21L, 14L, 7L).zip(listOf(-1.0, 0.0, 1.0)).forEach { (diasAntes, z) ->
            val fecha = lanzamiento.minusDays(diasAntes)
            stroop(t, a(fecha, 9, 0), esPractica = false, esLineaBase = true, z = z, random = random)
            nback(t, a(fecha, 9, 20), esLineaBase = true, z = z, random = random)
        }

        // Misión: una sesión diaria de cada prueba. Las de hoy, en el pasado reciente.
        val inicioHoy = hoy.atStartOfDay(zona).toInstant().toEpochMilli()
        var ultimaSesion = inicioHoy
        for (dia in 1..dias) {
            val fecha = lanzamiento.plusDays(dia - 1L)
            val (inicioStroop, inicioNBack) = if (fecha == hoy) {
                val nback = (ahora - t.minutosDesdeUltimaSesion * MINUTO).coerceIn(inicioHoy, ahora - MINUTO)
                (nback - 35 * MINUTO).coerceAtLeast(inicioHoy) to nback
            } else {
                a(fecha, 8, 5 * orden) to a(fecha, 8, 20 + 5 * orden)
            }
            stroop(t, inicioStroop, esPractica = false, esLineaBase = false, z = t.stroopZ[dia - 1], random = random)
            nback(t, inicioNBack, esLineaBase = false, z = t.nbackZ[dia - 1], random = random)
            ultimaSesion = inicioNBack
        }

        db.tripulacionDao().insertarRegistros(
            (1..dias).map { dia ->
                RegistroDiario(
                    astronautaId = t.id,
                    fecha = lanzamiento.plusDays(dia - 1L).toString(),
                    horasSueno = t.horasSueno[dia - 1],
                    somnolencia = t.somnolencia[dia - 1],
                    sintomas = RegistroDiario.unirSintomas(t.sintomas[dia.toString()].orEmpty()),
                    aguaLitros = t.aguaLitros.getOrNull(dia - 1),
                    ingestaPorcentaje = t.ingesta.getOrNull(dia - 1),
                    ejercicioMinutos = minutos(t, dia),
                    ejercicioTipos = RegistroDiario.unirEjercicios(
                        when {
                            (minutos(t, dia) ?: 0) == 0 -> emptyList()
                            dia % 2 == 0 -> listOf(TipoEjercicio.CINTA, TipoEjercicio.FUERZA)
                            else -> listOf(TipoEjercicio.BICICLETA, TipoEjercicio.FUERZA)
                        },
                    ),
                )
            },
        )

        // Check-in de bienestar: por la noche (el de hoy, antes de su última sesión).
        t.bienestar?.let { b ->
            listOf(b.animo, b.estres, b.aislamiento).forEach {
                require(it.size == dias) { "${t.nombre}: se esperaban $dias días de bienestar y hay ${it.size}" }
            }
            db.bienestarDao().guardar(
                (1..dias).map { dia ->
                    val fecha = lanzamiento.plusDays(dia - 1L)
                    BienestarDiario(
                        astronautaId = t.id,
                        fecha = fecha.toString(),
                        animo = b.animo[dia - 1],
                        estres = b.estres[dia - 1],
                        aislamiento = b.aislamiento[dia - 1],
                        guardadoEn = if (fecha == hoy) ultimaSesion - MINUTO else a(fecha, 21, 0),
                    )
                },
            )
        }
        return ultimaSesion
    }

    private fun minutos(t: TripulanteEjemplo, dia: Int): Int? = t.ejercicioMinutos.getOrNull(dia - 1)

    /** Stroop a [z] desviaciones de su media (positivo = más efecto, más lento y más errores). */
    private suspend fun stroop(t: TripulanteEjemplo, inicio: Long, esPractica: Boolean, esLineaBase: Boolean, z: Double, random: Random) {
        val secuencia = if (esPractica) GeneradorStroop.practica(configStroop, random) else GeneradorStroop.sesion(configStroop, random)
        val resultados = GeneradorEnsayosEjemplo.stroop(
            secuencia,
            t.stroop.tiempoCoincidentesMs + z * 15,
            t.stroop.efectoMs + z * t.stroop.desviacionMs,
            if (esPractica) 0 else (1 + z).roundToInt().coerceIn(0, 6),
            if (z > 2) 1 else 0,
            random,
        )
        repositorio.guardarStroop(sesion(t, inicio, esPractica, esLineaBase), resultados)
    }

    /** N-Back 2-back con la d′ más cercana a media − [z] desviaciones. */
    private suspend fun nback(t: TripulanteEjemplo, inicio: Long, esLineaBase: Boolean, z: Double, random: Random) {
        val secuencia = GeneradorNBack.sesion(configNBack, random)
        val coincidencias = secuencia.contarCoincidencias()
        val (aciertos, falsasAlarmas) = GeneradorEnsayosEjemplo.aciertosYFalsasAlarmasPara(
            t.nback.dPrima - z * t.nback.desviacion,
            coincidencias,
            configNBack.puntuables - coincidencias,
        ).let { it[0] to it[1] }
        val resultados = GeneradorEnsayosEjemplo.nback(secuencia, aciertos, falsasAlarmas, t.nback.tiempoMs + z * 30, random)
        repositorio.guardarNBack(sesion(t, inicio, esPractica = false, esLineaBase = esLineaBase), resultados, secuencia.nivel)
    }

    /** Práctica del N-Back sin fallos. */
    private suspend fun nbackPerfecto(t: TripulanteEjemplo, inicio: Long, secuencia: SecuenciaNBack, random: Random) {
        val resultados = GeneradorEnsayosEjemplo.nback(secuencia, secuencia.contarCoincidencias(), 0, t.nback.tiempoMs, random)
        repositorio.guardarNBack(sesion(t, inicio, esPractica = true, esLineaBase = false), resultados, secuencia.nivel)
    }

    /**
     * Usuario de tipo ADMIN para acceso al panel médico.
     * Prototipo: contraseña guardada en texto plano.
     */
    private suspend fun insertarAdmin(ahora: Long) {
        // No insertar si ya existe (por si se llama dos veces).
        if (db.appUserDao().findByUsername(USUARIO_ADMIN) != null) return
        db.appUserDao().insert(
            AppUserEntity().apply {
                username = USUARIO_ADMIN
                passwordHash = CONTRASENA_ADMIN
                role = Role.ADMIN.name
                createdAt = ahora
            },
        )
    }

    /** Usuario (rol ASTRONAUT) + astronauta en las tablas del equipo, con el id del JSON. */
    private suspend fun insertarTripulante(t: TripulanteEjemplo, misionId: Long, ahora: Long) {
        val dao = db.tripulacionDao()
        val usuario = nombreUsuario(t.nombre)
        val usuarioId = dao.insertarUsuario(
            AppUserEntity().apply {
                username = usuario
                // Prototipo: la contraseña es el mismo nombre de usuario.
                passwordHash = usuario
                role = Role.ASTRONAUT.name
                createdAt = ahora
            },
        )
        val partes = t.nombre.trim().split(' ', limit = 2)
        dao.insertarAstronauta(
            AstronautEntity().apply {
                id = t.id
                userId = usuarioId
                missionId = misionId
                firstName = partes[0]
                lastName = partes.getOrElse(1) { "" }
                crewRole = t.cargo.name
                usualSleepHours = t.horasSuenoHabituales
            },
        )
    }

    /** "Leo Martín" → "leo.martin". */
    private fun nombreUsuario(nombre: String): String =
        Normalizer.normalize(nombre.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("\\s+"), ".")

    private fun sesion(t: TripulanteEjemplo, inicio: Long, esPractica: Boolean, esLineaBase: Boolean) = SesionNueva(
        astronautaId = t.id,
        inicioEn = inicio,
        finEn = inicio + 2 * MINUTO,
        esPractica = esPractica,
        valida = true,
        esLineaBase = esLineaBase,
    )

    companion object {
        const val ARCHIVO = "datos_ejemplo.json"
        const val NOMBRE_MISION = "SpaceDrifters"
        // ids de assets/datos_ejemplo.json
        private val HORAS_CONSTANTES = listOf(7, 12, 17, 22)
        const val FUENTE_SIMULADA = "SIMULADO"
        private const val MAYA = 1L
        private const val LEO = 2L
        private const val ANE = 3L
        /** Credenciales del admin por defecto (prototipo). */
        const val USUARIO_ADMIN = "admin"
        const val CONTRASENA_ADMIN = "admin1234"
        private const val MINUTO = 60_000L
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
