package com.example.nasa_spaceappchallenge2026_spacedrifters.data.config

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.ConfigCognitiva
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Contenido de assets/config_mision.json. Cambiar los umbrales o el retraso no requiere tocar código. */
@Serializable
data class ConfigMision(
    val retrasoMisionMin: Long = 18,
    val umbralVigilarDesviaciones: Double = 1.0,
    val umbralAlertaDesviaciones: Double = 2.0,
    val minSesionesLineaBase: Int = 3,
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
