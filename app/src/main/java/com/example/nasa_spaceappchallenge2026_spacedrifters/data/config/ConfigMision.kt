package com.example.nasa_spaceappchallenge2026_spacedrifters.data.config

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.ConfigCognitiva
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Objetivos diarios del registro (hidratación y ejercicio). */
@Serializable
data class ObjetivosDiarios(
    val aguaLitros: Double = 2.0,
    val ejercicioMinutos: Int = 120,
)

/** Contenido de assets/config_mision.json. Cambiar los umbrales o el retraso no requiere tocar código. */
@Serializable
data class ConfigMision(
    val retrasoMisionMin: Long = 18,
    val umbralVigilarDesviaciones: Double = 1.0,
    val umbralAlertaDesviaciones: Double = 2.0,
    val minSesionesLineaBase: Int = 3,
    /** astronaut.id → tripulante de Inspiration4 ("C002") cuyas analíticas reales (LSDS-7, LSDS-8) se usan en la demo. */
    val tripulanteNasa: Map<String, String> = emptyMap(),
    /** Parámetros de las analíticas de NASA que no se leen (ver config_mision.json). */
    val parametrosNasaExcluidos: List<String> = emptyList(),
    val objetivosDiarios: ObjetivosDiarios = ObjetivosDiarios(),
) {
    fun aConfigCognitiva() = ConfigCognitiva(
        retrasoMisionMin * 60_000L,
        umbralVigilarDesviaciones,
        umbralAlertaDesviaciones,
        minSesionesLineaBase,
    )

    companion object {
        private const val ARCHIVO = "config_mision.json"
        private val json = Json { ignoreUnknownKeys = true }

        fun leer(context: Context): ConfigMision =
            context.assets.open(ARCHIVO).bufferedReader().use { json.decodeFromString(it.readText()) }
    }
}
