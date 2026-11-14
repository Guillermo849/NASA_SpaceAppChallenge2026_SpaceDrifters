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

// ---- Lista de tripulación ----

data class FilaTripulanteUi(
    val id: Long,
    val nombre: String,
    val cargo: CargoTripulante?,
    /** Alertas activas que ya han llegado a la Tierra. */
    val alertasActivas: Int,
    /** Estado de la última sesión recibida de cada prueba (null = sin sesiones). */
    val ultimoStroop: EstadoCognitivo?,
    val ultimoNBack: EstadoCognitivo?,
)

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
)

/** Métrica principal de una sesión según su prueba. */
fun valorPrincipal(s: SesionConResumen): Double? = when (s.tipo) {
    TipoPrueba.STROOP -> s.efectoStroopMs
    TipoPrueba.NBACK -> s.dPrima
}

private fun estadoDe(texto: String?): EstadoCognitivo? = EstadoCognitivo.entries.firstOrNull { it.name == texto }

fun aFilaTripulante(a: Astronauta, alertas: Int, sesiones: List<SesionConResumen>): FilaTripulanteUi {
    fun ultimo(tipo: TipoPrueba) =
        sesiones.firstOrNull { it.tipo == tipo && !it.esLineaBase && it.estado != null }?.let { estadoDe(it.estado) }
    return FilaTripulanteUi(a.id, a.nombre, a.cargo, alertas, ultimo(TipoPrueba.STROOP), ultimo(TipoPrueba.NBACK))
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
                aciertos = if (s.tipo == TipoPrueba.STROOP) s.porcentajeAciertos else s.aciertosCoincidencias,
                estado = estadoDe(s.estado),
                valida = s.valida,
                esLineaBase = s.esLineaBase,
                horasSueno = registro?.horasSueno,
                somnolencia = registro?.somnolencia,
            )
        },
        pruebasPendientes = pendientes.map { it.tipo }.distinct(),
        notas = notas,
    )
}
