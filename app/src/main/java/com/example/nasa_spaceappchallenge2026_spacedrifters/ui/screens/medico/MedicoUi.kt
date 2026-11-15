package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.medico

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Astronauta
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.LineaBaseCognitiva
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.NotaMedica
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.PruebaProgramada
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.RegistroDiario
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.SesionConResumen
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.entity.AlertEntity
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.AlertasRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.CognitivoRepository
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.ExplicacionAlerta
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.Tendencia
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/** Emite la hora actual al empezar y luego cada [cadaMs]: para que "llega en 12 min" avance solo. */
fun relojCada(cadaMs: Long): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(cadaMs)
    }
}

// ---- Panel del médico: tripulación ----

/** Una constante en miniatura: los últimos días recibidos y si el último valor sale de su franja. */
data class ConstanteMiniUi(
    val metrica: MetricaConstante,
    /** (instante, valor) de los últimos [DIAS_MINIATURA] días, del más antiguo al más reciente. */
    val puntos: List<Pair<Long, Double>>,
    val ultimo: Double?,
    /** El último valor está fuera de su franja (frente a su línea base). */
    val fuera: Boolean,
    /** Fuera por arriba (true) o por abajo (false); solo tiene sentido si [fuera]. */
    val alto: Boolean,
    /** Tiene línea base y franja con la que comparar; si no, no se dice "en rango". */
    val conReferencia: Boolean = true,
)

data class FilaTripulanteUi(
    val id: Long,
    val nombre: String,
    val cargo: CargoTripulante?,
    /** Alertas activas que ya han llegado a la Tierra. */
    val alertasActivas: Int,
    /** Estado de la última sesión recibida de cada prueba (null = sin sesiones). */
    val ultimoStroop: EstadoCognitivo?,
    val ultimoNBack: EstadoCognitivo?,
    /** Nivel más alto de sus protocolos fisiológicos recibidos; null = ninguno abierto. */
    val nivelFisiologico: Int? = null,
    /** FC, PAS, SpO2 y FR recibidas (las que tenga). */
    val constantes: List<ConstanteMiniUi> = emptyList(),
) {
    val estadoGeneral: EstadoCognitivo get() = estadoGeneral(this)
}

/** Lo que pinta el panel del médico. */
data class PanelMedicoUi(
    /** Ordenada de la situación más urgente a la menos. */
    val tripulacion: List<FilaTripulanteUi>,
    /** Día de misión (1 = primer día), o null si no se sabe. */
    val diaMision: Int?,
    val retrasoMin: Long,
    /** Cuándo se ha calculado (lo recibido hasta entonces). */
    val actualizadoEn: Long,
) {
    fun cuantos(estado: EstadoCognitivo) = tripulacion.count { it.estadoGeneral == estado }
    val avisos: Int get() = tripulacion.sumOf { it.alertasActivas }
    val conProtocolo: Int get() = tripulacion.count { it.nivelFisiologico != null }
}

/** Días que se dibujan en las miniaturas de constantes del panel. */
const val DIAS_MINIATURA = 7

/**
 * Situación general de un tripulante, para ordenar y colorear el panel (siempre con su texto):
 * Alerta = algún protocolo en nivel 4–5 o alguna prueba cognitiva en Alerta; Vigilar = nivel 2–3,
 * alguna prueba en Vigilar, avisos activos o alguna constante fuera de su franja; Normal si hay
 * datos y nada de lo anterior; Sin datos si no ha llegado nada (nunca se da por "bien").
 */
fun estadoGeneral(f: FilaTripulanteUi): EstadoCognitivo {
    val cognitivo = listOfNotNull(f.ultimoStroop, f.ultimoNBack)
    val nivel = f.nivelFisiologico ?: 0
    val hayDatos = cognitivo.any { it != EstadoCognitivo.SIN_DATOS } || f.nivelFisiologico != null ||
        f.constantes.any { it.ultimo != null } || f.alertasActivas > 0
    return when {
        nivel >= 4 || EstadoCognitivo.ALERTA in cognitivo -> EstadoCognitivo.ALERTA
        nivel >= 2 || EstadoCognitivo.VIGILAR in cognitivo || f.alertasActivas > 0 || f.constantes.any { it.fuera } ->
            EstadoCognitivo.VIGILAR
        hayDatos -> EstadoCognitivo.NORMAL
        else -> EstadoCognitivo.SIN_DATOS
    }
}

/** Primero Alerta, luego Vigilar, Normal y Sin datos; a igualdad, más avisos y nivel más alto primero. */
fun ordenarPorUrgencia(filas: List<FilaTripulanteUi>): List<FilaTripulanteUi> {
    val orden = listOf(EstadoCognitivo.ALERTA, EstadoCognitivo.VIGILAR, EstadoCognitivo.NORMAL, EstadoCognitivo.SIN_DATOS)
    return filas.sortedWith(
        compareBy<FilaTripulanteUi> { orden.indexOf(it.estadoGeneral) }
            .thenByDescending { it.alertasActivas }
            .thenByDescending { it.nivelFisiologico ?: 0 }
            .thenBy { it.nombre },
    )
}

// ---- Perfil: pestaña "Pruebas cognitivas" ----

data class PerfilMedicoUi(
    val cargando: Boolean = true,
    val nombre: String? = null,
    val cargo: CargoTripulante? = null,
    val diaMision: Int? = null,
    /** Sesiones que aún vienen de camino, o null si no hay ninguna. */
    val enCamino: EnCaminoUi? = null,
    /** Una entrada por prueba: tarjeta de resumen + gráfica. */
    val pruebas: List<PruebaMedicoUi> = emptyList(),
    val alertas: List<AlertaMedicoUi> = emptyList(),
    /** Tabla de sesiones, de la más reciente a la más antigua. */
    val sesiones: List<FilaSesionUi> = emptyList(),
    /** Pruebas extra pedidas que el astronauta aún no ha hecho. */
    val pruebasPendientes: List<TipoPrueba> = emptyList(),
    val notas: List<NotaMedica> = emptyList(),
)

data class EnCaminoUi(val sesiones: Int, val minutosHastaLaPrimera: Long)

data class PruebaMedicoUi(
    val tipo: TipoPrueba,
    val ultimaFecha: Long?,
    /** Métrica principal de la última sesión (efecto Stroop en ms, d′). */
    val ultimoValor: Double?,
    val ultimoEstado: EstadoCognitivo?,
    val tendencia: Tendencia.Sentido,
    val media: Double?,
    val desviacion: Double?,
    /** Sesiones de los últimos 30 días con métrica, de la más antigua a la más reciente. */
    val puntos: List<PuntoMedico>,
)

data class PuntoMedico(
    val sesionId: Long,
    val fechaEn: Long,
    val valor: Double,
    val estado: EstadoCognitivo,
    val esLineaBase: Boolean,
)

data class AlertaMedicoUi(
    val id: Long,
    val reglaId: String,
    val prioridad: Regla.Prioridad?,
    val generadaEn: Long,
    /** "Por qué ha saltado" (null si no se pudo leer). */
    val explicacion: ExplicacionAlerta?,
)

data class FilaSesionUi(
    val sesionId: Long,
    val tipo: TipoPrueba,
    val fechaEn: Long,
    /** Métrica principal (null si la sesión no es válida o no da para calcularla). */
    val valor: Double?,
    /** % de aciertos (Stroop) o de coincidencias detectadas (N-Back). */
    val aciertos: Double?,
    val estado: EstadoCognitivo?,
    val valida: Boolean,
    val esLineaBase: Boolean,
    /** Del registro diario de ese día. */
    val horasSueno: Double?,
    val somnolencia: Int?,
    /** Hábitos del día anterior, anotados en el registro de ese día. */
    val aguaLitros: Double? = null,
    val ingesta: Int? = null,
    val ejercicioMinutos: Int? = null,
)

/** Métrica principal de una sesión según su prueba. */
fun valorPrincipal(s: SesionConResumen): Double? = when (s.tipo) {
    TipoPrueba.STROOP -> s.efectoStroopMs
    TipoPrueba.NBACK -> s.dPrima
    TipoPrueba.PVT -> s.pvtTrMedioMs
}

private fun estadoDe(texto: String?): EstadoCognitivo? = EstadoCognitivo.entries.firstOrNull { it.name == texto }

/**
 * Un tripulante en el panel del médico.
 * @param fisiologico su pestaña "Fisiológico" ya construida con lo recibido (nivel y constantes), o null
 */
fun aFilaTripulante(
    a: Astronauta,
    alertas: Int,
    sesiones: List<SesionConResumen>,
    fisiologico: FisiologicoMedicoUi? = null,
    ahora: Long = System.currentTimeMillis(),
): FilaTripulanteUi {
    fun ultimo(tipo: TipoPrueba) =
        sesiones.firstOrNull { it.tipo == tipo && !it.esLineaBase && it.estado != null }?.let { estadoDe(it.estado) }
    val desde = ahora - DIAS_MINIATURA * 86_400_000L
    val constantes = fisiologico?.series.orEmpty().filter { it.puntos.isNotEmpty() }.map { s ->
        val ultimo = s.ultimo
        ConstanteMiniUi(
            metrica = s.metrica,
            puntos = s.puntos.filter { it.first >= desde },
            ultimo = ultimo,
            fuera = ultimo != null && s.fuera(ultimo),
            alto = ultimo != null && s.base != null && ultimo > s.base,
            conReferencia = s.franja != null,
        )
    }
    return FilaTripulanteUi(
        id = a.id,
        nombre = a.nombre,
        cargo = a.cargo,
        alertasActivas = alertas,
        ultimoStroop = ultimo(TipoPrueba.STROOP),
        ultimoNBack = ultimo(TipoPrueba.NBACK),
        nivelFisiologico = fisiologico?.nivelActual,
        constantes = constantes,
    )
}

/** Junta todo lo recibido en la Tierra en lo que pinta la pestaña "Pruebas cognitivas". */
fun construirPerfil(
    astronauta: Astronauta?,
    sesiones: List<SesionConResumen>,
    llegadas: List<Long>,
    bases: List<LineaBaseCognitiva>,
    registros: List<RegistroDiario>,
    alertas: List<AlertEntity>,
    pendientes: List<PruebaProgramada>,
    notas: List<NotaMedica>,
    ahora: Long,
    zona: ZoneId,
): PerfilMedicoUi {
    val hoy = Instant.ofEpochMilli(ahora).atZone(zona).toLocalDate()
    val hace30Dias = hoy.minusDays(29).atStartOfDay(zona).toInstant().toEpochMilli()
    val registroDelDia = registros.associateBy { it.fecha }
    fun dia(instante: Long) = Instant.ofEpochMilli(instante).atZone(zona).toLocalDate()

    val pruebas = TipoPrueba.entries.map { tipo ->
        val base = bases.firstOrNull { it.tipo == tipo && it.metrica == CognitivoRepository.metricaPrincipal(tipo) }
        val deMision = sesiones.filter { it.tipo == tipo && !it.esLineaBase && it.estado != null }
        val ultima = deMision.firstOrNull()
        PruebaMedicoUi(
            tipo = tipo,
            ultimaFecha = ultima?.inicioEn,
            ultimoValor = ultima?.let(::valorPrincipal),
            ultimoEstado = ultima?.let { estadoDe(it.estado) },
            tendencia = Tendencia.calcular(deMision.map(::valorPrincipal), CognitivoRepository.direccion(tipo), base?.desviacion ?: 0.0),
            media = base?.media,
            desviacion = base?.desviacion,
            puntos = sesiones
                .filter { it.tipo == tipo && it.inicioEn >= hace30Dias }
                .mapNotNull { s ->
                    val valor = valorPrincipal(s) ?: return@mapNotNull null
                    PuntoMedico(s.sesionId, s.inicioEn, valor, estadoDe(s.estado) ?: EstadoCognitivo.SIN_DATOS, s.esLineaBase)
                }
                .sortedBy { it.fechaEn },
        )
    }

    return PerfilMedicoUi(
        cargando = false,
        nombre = astronauta?.nombre,
        cargo = astronauta?.cargo,
        diaMision = astronauta?.inicioMision?.let { ChronoUnit.DAYS.between(dia(it), hoy).toInt() + 1 },
        enCamino = llegadas.takeIf { it.isNotEmpty() }?.let {
            EnCaminoUi(it.size, ceil((it.first() - ahora) / 60_000.0).toLong().coerceAtLeast(1))
        },
        pruebas = pruebas,
        alertas = alertas.map {
            AlertaMedicoUi(
                id = it.id,
                reglaId = it.ruleId.orEmpty(),
                prioridad = Regla.Prioridad.entries.firstOrNull { p -> p.name == it.priority },
                generadaEn = it.generatedAt,
                explicacion = AlertasRepository.leerExplicacion(it.explanation),
            )
        },
        sesiones = sesiones.map { s ->
            val registro = registroDelDia[dia(s.inicioEn).toString()]
            FilaSesionUi(
                sesionId = s.sesionId,
                tipo = s.tipo,
                fechaEn = s.inicioEn,
                valor = valorPrincipal(s),
                aciertos = when (s.tipo) {
                    TipoPrueba.STROOP -> s.porcentajeAciertos
                    TipoPrueba.NBACK -> s.aciertosCoincidencias
                    TipoPrueba.PVT -> null
                },
                estado = estadoDe(s.estado),
                valida = s.valida,
                esLineaBase = s.esLineaBase,
                horasSueno = registro?.horasSueno,
                somnolencia = registro?.somnolencia,
                aguaLitros = registro?.aguaLitros,
                ingesta = registro?.ingestaPorcentaje,
                ejercicioMinutos = registro?.ejercicioMinutos,
            )
        },
        pruebasPendientes = pendientes.map { it.tipo }.distinct(),
        notas = notas,
    )
}
