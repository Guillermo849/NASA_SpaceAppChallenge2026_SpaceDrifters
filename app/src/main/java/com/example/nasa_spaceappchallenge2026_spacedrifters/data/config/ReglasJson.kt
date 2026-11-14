package com.example.nasa_spaceappchallenge2026_spacedrifters.data.config

import android.content.Context
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.cognitivo.EstadoCognitivo
import com.example.nasa_spaceappchallenge2026_spacedrifters.domain.reglas.Regla
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
    val pruebas: List<String>,
    val estados: List<EstadoCognitivo>,
    val sesionesSeguidas: Int = 0,
    val nochesBajoBase: Int = 0,
    val sintomas: List<String> = emptyList(),
    val prioridad: Regla.Prioridad,
    val pasos: List<String> = emptyList(),
    val asignarA: String? = null,
) {
    fun aRegla() = Regla(
        id, tipo, pruebas, estados.toSet(), sesionesSeguidas, nochesBajoBase, sintomas.toSet(), prioridad, pasos, asignarA,
    )
}
