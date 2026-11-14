package com.example.nasa_spaceappchallenge2026_spacedrifters.data.config

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.Umbral
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.fisiologico.UmbralesFisiologicos
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Formato de assets/config_umbrales.json. Los umbrales se cambian ahí, no en el código. */
@Serializable
data class ConfigUmbrales(
    val umbrales: List<UmbralJson>,
    /** Referencia por métrica para quien aún no tiene línea base propia (p. ej. VS, GC). */
    val referencias: Map<String, Double> = emptyMap(),
) {

    fun aUmbrales() = UmbralesFisiologicos(umbrales.map { it.aUmbral() }, referencias)

    companion object {
        const val ARCHIVO = "config_umbrales.json"
        private val json = Json { ignoreUnknownKeys = true }

        @JvmStatic
        fun desdeTexto(texto: String): UmbralesFisiologicos = json.decodeFromString<ConfigUmbrales>(texto).aUmbrales()

        fun leer(context: Context): UmbralesFisiologicos =
            context.assets.open(ARCHIVO).bufferedReader().use { desdeTexto(it.readText()) }
    }
}

@Serializable
data class UmbralJson(
    val hecho: String,
    val metrica: String,
    val sentido: Umbral.Sentido,
    val desviacionPorcentaje: Double? = null,
    val desviacionPuntos: Double? = null,
    val absoluto: Double? = null,
) {
    fun aUmbral() = Umbral(hecho, metrica, sentido, desviacionPorcentaje, desviacionPuntos, absoluto)
}
