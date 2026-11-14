package com.example.nasa_spaceappchallenge2026_spacedrifters.ui.screens.astronauta

import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.CargoTripulante
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.Sintoma
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoEjercicio
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.repository.UltimasConstantes
import com.example.nasa_spaceappchallenge2026_spacedrifters.data.local.TipoPrueba
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla

/**
 * Todo lo que pinta el panel del astronauta. Las pantallas solo conocen estos modelos, no las
 * tablas: si cambia la base de datos, solo cambia cómo los rellena el ViewModel.
 * Los null se muestran como "Sin datos", nunca como si estuviera bien.
 */
data class PanelAstronautaUi(
    val nombre: String? = null,
    val cargo: CargoTripulante? = null,
    /** Día de misión (1 = primer día), o null si aún no hay sesiones de misión. */
    val diaMision: Int? = null,
    val retrasoMin: Long? = null,
    /** Alertas activas sobre este astronauta o asignadas a él, de la más reciente a la más antigua. */
    val avisos: List<AvisoUi> = emptyList(),
    val hechasHoy: Set<TipoPrueba> = emptySet(),
    /** Pruebas extra que ha pedido el médico y aún no ha hecho. */
    val pruebasExtra: Set<TipoPrueba> = emptySet(),
    /** Registro diario de hoy, o null si aún no lo ha hecho. */
    val registroHoy: RegistroHoyUi? = null,
    /** Check-in de bienestar de hoy, o null si aún no lo ha hecho. */
    val bienestarHoy: BienestarHoyUi? = null,
    /** Últimas constantes registradas, o null si no hay ninguna. */
    val constantes: UltimasConstantes? = null,
    /** Una entrada por prueba, con las sesiones de los últimos 30 días. */
    val rendimiento: List<RendimientoUi> = emptyList(),
)

/** Una alerta activa en el panel del astronauta. */
data class AvisoUi(
    val id: Long,
    /** Regla de assets/reglas.json ("nback-tendencia"...): de ella sale el título. */
    val reglaId: String,
    val prioridad: Regla.Prioridad?,
    /** Códigos de pasos recomendados; vacío = solo "se ha enviado a tu médico". */
    val pasos: List<String>,
    /** true = es sobre este astronauta; false = es de otro tripulante y se la han asignado. */
    val esPropio: Boolean,
    /** Nombre del tripulante afectado (para las asignadas). */
    val nombreAfectado: String?,
)

/** Check-in de bienestar de hoy (escalas 1–5; null = sin datos). */
data class BienestarHoyUi(val animo: Int?, val estres: Int?, val aislamiento: Int?)

/** Resumen del registro diario de hoy. Los null se muestran como "Sin datos". */
data class RegistroHoyUi(
    val horasSueno: Double?,
    /** Escala KSS 1–9. */
    val somnolencia: Int?,
    /** Vacío = ninguno. */
    val sintomas: List<Sintoma>,
    /** Litros bebidos ayer. */
    val aguaLitros: Double? = null,
    /** % del menú comido ayer. */
    val ingesta: Int? = null,
    val ejercicioMinutos: Int? = null,
    val ejercicios: List<TipoEjercicio> = emptyList(),
)

data class RendimientoUi(
    val tipo: TipoPrueba,
    /** Con línea base, la puntuación de los puntos está en desviaciones y el rango habitual es −1..+1. */
    val tieneLineaBase: Boolean,
    /** Sesiones de misión, de la más antigua a la más reciente. */
    val puntos: List<PuntoRendimiento>,
)

data class PuntoRendimiento(
    val fechaEn: Long,
    /** Más alto = mejor, en todas las pruebas (ver domain/cognitivo/Rendimiento). */
    val puntuacion: Double,
    val estado: EstadoCognitivo,
)

/** Periodos que puede elegir el astronauta en "Mi rendimiento". */
enum class Periodo(val dias: Int) { SEMANA(7), MES(30) }
