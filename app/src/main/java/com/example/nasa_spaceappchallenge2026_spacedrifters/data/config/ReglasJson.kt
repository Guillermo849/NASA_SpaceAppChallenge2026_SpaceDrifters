package com.example.nasa_spaceappchallenge2026_spacedrifters.data.config

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.UmbralesBienestar
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Formato de assets/reglas.json. Las reglas se cambian ahí, no en el código. */
@Serializable
data class ReglasJson(val reglas: List<ReglaJson>) {

    fun aReglas(): List<Regla> = reglas.map { it.aRegla() }

    companion object {
        const val ARCHIVO = "reglas.json"
        private val json = Json { ignoreUnknownKeys = true }

        fun desdeTexto(texto: String): List<Regla> = json.decodeFromString<ReglasJson>(texto).aReglas()

        fun leer(context: Context): List<Regla> =
            context.assets.open(ARCHIVO).bufferedReader().use { desdeTexto(it.readText()) }
    }
}

@Serializable
data class ReglaJson(
    val id: String,
    val tipo: Regla.Tipo,
    val pruebas: List<String> = emptyList(),
    val estados: List<EstadoCognitivo> = emptyList(),
    val sesionesSeguidas: Int = 0,
    val nochesBajoBase: Int = 0,
    val sintomas: List<String> = emptyList(),
    val prioridad: Regla.Prioridad,
    val pasos: List<String> = emptyList(),
    val asignarA: String? = null,
    /** HIDRATACION_BAJA: días seguidos por debajo de [aguaMinLitros]. */
    val diasSeguidos: Int = 0,
    val aguaMinLitros: Double = 0.0,
    /** BIENESTAR_BAJO (escalas 1–5): un día cuenta si ánimo ≤ animoMax, estrés ≥ estresMin o aislamiento ≥ aislamientoMin. */
    val animoMax: Int? = null,
    val estresMin: Int? = null,
    val aislamientoMin: Int? = null,
) {
    fun aRegla() = Regla(
        id, tipo, pruebas, estados.toSet(), sesionesSeguidas, nochesBajoBase, sintomas.toSet(), prioridad, pasos, asignarA,
        diasSeguidos, aguaMinLitros,
        if (tipo == Regla.Tipo.BIENESTAR_BAJO) UmbralesBienestar(animoMax, estresMin, aislamientoMin) else null,
    )
}
